// Realtime hub (Durable Object). One shared instance ("global") holds every connected client's
// WebSocket, so the server can push instantly instead of clients polling.
//   - broadcast(text): center-screen toast to everyone (triggered from a Discord slash command).
//   - group pings: clients send {type:"ping",...}; relayed only to sockets in the same group.
//   - presence: clients send {type:"pos",...}; kept on the connection's attachment and read back via
//     the presences()/presencesFor() RPCs (GET /presence, /presence/batch) — no KV involved. "Online"
//     is simply "has an open WebSocket"; closing it (webSocketClose, or just going offline) removes it.
// Uses the Hibernation API (ctx.acceptWebSocket) so idle connections are not billed.
import { DurableObject } from "cloudflare:workers";

export class Hub extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    // App-level keepalive: if a client sends the text "KA", the runtime auto-replies "KO"
    // without waking the DO (does not interrupt hibernation). Belt-and-suspenders next to
    // the protocol-level ping/pong the runtime already answers.
    this.ctx.setWebSocketAutoResponse(new WebSocketRequestResponsePair("KA", "KO"));
  }

  // The WebSocket upgrade must go through fetch() (that is where we receive the Request).
  async fetch(request) {
    const url = new URL(request.url);
    const pair = new WebSocketPair();
    const server = pair[1];
    // Per-connection metadata (survives hibernation, max 16 KB). Set from the connect query.
    server.serializeAttachment({
      uuid: (url.searchParams.get("uuid") || "").slice(0, 64),
      name: (url.searchParams.get("name") || "").slice(0, 32),
      group: (url.searchParams.get("group") || "").slice(0, 64),
    });
    this.ctx.acceptWebSocket(server);
    return new Response(null, { status: 101, webSocket: pair[0] });
  }

  // Client -> server. Three kinds: update our group, emit a group ping, or emit a group action
  // (the chief's follow-me: refuge/spawn teleport, relayed instantly to the group).
  async webSocketMessage(ws, message) {
    if (typeof message !== "string") return;
    let msg;
    try { msg = JSON.parse(message); } catch { return; }
    if (!msg || typeof msg.type !== "string") return;

    if (msg.type === "group") {
      const a = ws.deserializeAttachment() || {};
      a.group = String(msg.group || "").slice(0, 64);
      ws.serializeAttachment(a);
    } else if (msg.type === "identity") {
      // The client resolved its canonical (version-stable) account id after connecting; keep the
      // socket's uuid current so targeted friend pushes (notify) reach it.
      const a = ws.deserializeAttachment() || {};
      a.uuid = String(msg.uuid || "").slice(0, 64);
      ws.serializeAttachment(a);
    } else if (msg.type === "ping") {
      this.relayPing(ws, msg);
    } else if (msg.type === "action") {
      this.relayAction(ws, msg);
    } else if (msg.type === "pos") {
      this.setPos(ws, msg);
    }
  }

  async webSocketClose(ws, code, reason) {
    try { ws.close(code, reason); } catch {}
  }

  async webSocketError() {
    // best-effort; the runtime removes the socket from getWebSockets()
  }

  // ---- RPC, called from the Worker ----

  // Push a toast to every connected client. Returns how many sockets received it.
  broadcast(text, severity = "info") {
    const t = String(text || "").slice(0, 256);
    if (!t) return 0;
    const payload = JSON.stringify({ type: "toast", text: t, severity, ms: 5000 });
    let n = 0;
    for (const ws of this.ctx.getWebSockets()) {
      try { ws.send(payload); n++; } catch {}
    }
    return n;
  }

  // Nudge specific connected accounts to re-pull their friends list (instant friend request/accept).
  // Matches on the per-connection uuid attachment. Returns how many sockets were notified.
  notify(uuids) {
    const set = new Set((uuids || []).map((u) => String(u)));
    if (!set.size) return 0;
    const payload = JSON.stringify({ type: "friends" });
    let n = 0;
    for (const ws of this.ctx.getWebSockets()) {
      const a = ws.deserializeAttachment() || {};
      if (a.uuid && set.has(a.uuid)) {
        try { ws.send(payload); n++; } catch {}
      }
    }
    return n;
  }

  // Self-imposed daily write budget = a hard, globally-consistent wallet cap on the costly KV path.
  // In-memory counter on this single DO instance (stays warm under load, incl. an attack); resets on
  // the UTC day rollover. No storage writes -> zero extra cost, instant. If the DO is evicted it only
  // happens when idle (low usage), so resetting the count then is harmless. Returns false once over cap.
  charge() {
    const day = Math.floor(Date.now() / 86400000);
    if (this._budgetDay !== day) { this._budgetDay = day; this._budgetCount = 0; }
    const v = Number(this.env && this.env.WRITE_BUDGET_DAILY);
    const cap = Number.isFinite(v) && v > 0 ? v : 100000;
    if (this._budgetCount >= cap) return false;
    this._budgetCount++;
    return true;
  }

  // Read-only view of today's write budget usage (for GET /budget), so the cap can be right-sized.
  budgetStatus() {
    const day = Math.floor(Date.now() / 86400000);
    const count = this._budgetDay === day ? (this._budgetCount || 0) : 0;
    const v = Number(this.env && this.env.WRITE_BUDGET_DAILY);
    const cap = Number.isFinite(v) && v > 0 ? v : 100000;
    return { day, count, cap };
  }

  // Full presence roster (GET /presence): every connected socket with a live position, optionally
  // filtered to one server. In-memory only — no KV List/Read op. Capped like the old KV roster was.
  presences(server) {
    const out = [];
    for (const ws of this.ctx.getWebSockets()) {
      const a = ws.deserializeAttachment() || {};
      if (!a.pos || !a.uuid) continue;
      if (server && a.pos.server && a.pos.server !== server) continue;
      out.push(this.presenceOf(a));
      if (out.length >= 300) break;
    }
    return out;
  }

  // Targeted presence lookup (GET /presence/batch) for a known, small set of uuids — Friends/Groups
  // member tracking. Same in-memory source, just filtered instead of capped.
  presencesFor(uuids) {
    const set = new Set((uuids || []).map(String));
    if (!set.size) return [];
    const out = [];
    for (const ws of this.ctx.getWebSockets()) {
      const a = ws.deserializeAttachment() || {};
      if (!a.pos || !a.uuid || !set.has(a.uuid)) continue;
      out.push(this.presenceOf(a));
    }
    return out;
  }

  // ---- internal ----

  presenceOf(a) {
    // uuid === mcuuid here: the WS attachment's uuid is already the canonical account id (see the
    // "identity" message), unlike the old KV schema's separate per-install telemetry uuid.
    return {
      uuid: a.uuid, mcuuid: a.uuid, name: a.name || "?",
      x: a.pos.x, y: a.pos.y, z: a.pos.z, dim: a.pos.dim, last_update: a.pos.at,
    };
  }

  // Client -> server position update ({type:"pos",x,y,z,dim,server}), or {type:"pos",clear:true} to
  // stop appearing in the roster without closing the socket (AFK / broadcast toggled off).
  setPos(ws, msg) {
    const a = ws.deserializeAttachment() || {};
    if (msg.clear) {
      delete a.pos;
    } else {
      a.pos = {
        x: Number(msg.x) || 0, y: Number(msg.y) || 0, z: Number(msg.z) || 0,
        dim: String(msg.dim || "").slice(0, 64),
        server: String(msg.server || "").slice(0, 64),
        at: Date.now(),
      };
    }
    ws.serializeAttachment(a);
  }

  // Relay a ping to sockets sharing the sender's group (instant, no KV, no polling).
  relayPing(fromWs, msg) {
    const from = fromWs.deserializeAttachment() || {};
    if (!from.group) return;
    const payload = JSON.stringify({
      type: "ping",
      clear: !!msg.clear,
      x: Number(msg.x) || 0, y: Number(msg.y) || 0, z: Number(msg.z) || 0,
      dim: String(msg.dim || ""), cat: String(msg.cat || "").slice(0, 16),
      name: String(from.name || ""), from: from.uuid || "",
    });
    for (const ws of this.ctx.getWebSockets()) {
      const a = ws.deserializeAttachment() || {};
      if (a.group && a.group === from.group) {
        try { ws.send(payload); } catch {}
      }
    }
  }

  // Relay the chief's follow-action to sockets sharing the sender's group (instant). The sender's uuid
  // comes from the connection attachment ("from"), so followers can verify it is actually their chief.
  relayAction(fromWs, msg) {
    const from = fromWs.deserializeAttachment() || {};
    if (!from.group) return;
    const action = String(msg.action || "").slice(0, 32);
    const arg = String(msg.arg || "").slice(0, 32);
    if (!action) return;
    const payload = JSON.stringify({ type: "action", action, arg, from: from.uuid || "" });
    for (const ws of this.ctx.getWebSockets()) {
      const a = ws.deserializeAttachment() || {};
      if (a.group && a.group === from.group) {
        try { ws.send(payload); } catch {}
      }
    }
  }
}
