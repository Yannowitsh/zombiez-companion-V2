// Realtime hub (Durable Object). One shared instance ("global") holds every connected client's
// WebSocket, so the server can push instantly instead of clients polling.
//   - broadcast(text): center-screen toast to everyone (triggered from a Discord slash command).
//   - group pings: clients send {type:"ping",...}; relayed only to sockets in the same group.
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
    } else if (msg.type === "ping") {
      this.relayPing(ws, msg);
    } else if (msg.type === "action") {
      this.relayAction(ws, msg);
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

  // ---- internal ----

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
