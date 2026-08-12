// Zombiez Companion V2 — backend API (Cloudflare Worker)
// Matches the mod's network contracts exactly. Storage: Workers KV (binding: ZZC).
// Secret:  DISCORD_WEBHOOK_URL   (set via: wrangler secret put DISCORD_WEBHOOK_URL)
// Vars:    LATEST_VERSION, DOWNLOAD_URL   (in wrangler.toml [vars])

const PRESENCE_TTL = 120;              // seconds a presence entry stays "online"
const PING_TTL = 60 * 60 * 24 * 30;    // 30 days
const SPAWN_CAP = 500;                 // keep last N spawn timestamps (long history for interval stats)
const SPAWN_DEDUP_MS = 300000;         // 5 min: ignore near-duplicate spawns (mirrors the mod)
const FEEDBACK_COOLDOWN = 60;          // seconds between feedbacks per uuid
const MAX_MSG = 5000;

const json = (obj, status = 200) =>
  new Response(JSON.stringify(obj), { status, headers: { "content-type": "application/json" } });
const noContent = () => new Response(null, { status: 204 });

// Durable Object class for the realtime hub (WebSocket push). Must be re-exported from the entry module.
export { Hub } from "./hub.js";

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";
    const method = request.method;
    try {
      // Per-IP flood guard at the edge (no KV cost). Blocks request storms before any handler runs.
      if (env.API_LIMITER) {
        const ip = request.headers.get("CF-Connecting-IP") || "unknown";
        const { success } = await env.API_LIMITER.limit({ key: ip });
        if (!success) return json({ error: "rate_limited" }, 429);
      }

      if (method === "GET" && path === "/") return new Response("Zombiez Companion V2 API — ok");

      // Realtime: WebSocket upgrade -> forwarded to the single shared Hub Durable Object.
      if (path === "/ws") {
        if (request.headers.get("Upgrade") !== "websocket") return json({ error: "expected_websocket" }, 426);
        return env.HUB.getByName("global").fetch(request);
      }
      // Discord slash-command interactions endpoint (Discord POSTs here; ed25519-signed).
      if (path === "/discord" && method === "POST") return handleDiscord(request, env);

      if (path === "/version" && method === "GET") return json({ latest: env.LATEST_VERSION || null, url: env.DOWNLOAD_URL || null });
      if (path === "/ping" && method === "POST") return handlePing(request, env);
      if (path === "/spawns" && method === "GET") return getSpawns(env);
      if (path === "/spawns/stats" && method === "GET") return getSpawnStats(env);
      if (path === "/spawns" && method === "POST") return postSpawn(request, env);
      if (path === "/presence" && method === "GET") return getPresence(url, env);
      if (path === "/presence" && method === "POST") return postPresence(request, env);
      if (path.startsWith("/presence/") && method === "DELETE") return delPresence(path, env);
      if (path === "/friends" && method === "GET") return getFriends(url, env);
      if (path === "/friends/resolve" && method === "GET") return resolveFriendName(url, env);
      if (path === "/friends/announce" && method === "POST") return announceFriendName(request, env);
      if (path === "/friends/request" && method === "POST") return postFriendRequest(request, env);
      if (path === "/friends/accept" && method === "POST") return postFriendAccept(request, env);
      if (path === "/friends/decline" && method === "POST") return postFriendDecline(request, env);
      if (path === "/friends/remove" && method === "POST") return postFriendRemove(request, env);
      if (path === "/group" && method === "GET") return getGroup(url, env);
      if (path === "/group/create" && method === "POST") return groupCreate(request, env);
      if (path === "/group/invite" && method === "POST") return groupInvite(request, env);
      if (path === "/group/accept" && method === "POST") return groupAccept(request, env);
      if (path === "/group/decline" && method === "POST") return groupDecline(request, env);
      if (path === "/group/leave" && method === "POST") return groupLeave(request, env);
      if (path === "/group/kick" && method === "POST") return groupKick(request, env);
      if (path === "/group/transfer" && method === "POST") return groupTransfer(request, env);
      if (path === "/group/pings" && method === "GET") return getGroupPings(url, env);
      if (path === "/group/ping" && method === "POST") return groupPing(request, env);
      if (path === "/group/ping/clear" && method === "POST") return groupPingClear(request, env);
      if (path === "/group/action" && method === "POST") return groupActionPost(request, env);
      if (path === "/leaderboard" && method === "POST") return postLeaderboard(request, env);
      if (path === "/leaderboard" && method === "GET") return getLeaderboard(env);
      if (path === "/feedback" && method === "POST") return handleFeedback(request, env);

      return json({ error: "not_found" }, 404);
    } catch (e) {
      return json({ error: "server_error", detail: String(e) }, 500);
    }
  },

  // Cron (every minute): refresh the Discord "who's online" roster message.
  async scheduled(event, env, ctx) {
    ctx.waitUntil(updateRoster(env));
  },
};

async function readJson(request) {
  try { return await request.json(); } catch { return null; }
}

// --- Discord slash commands (interactions endpoint) ----------------------
// Discord POSTs every interaction here, signed with ed25519. We verify the signature against the
// app's public key, answer the initial PING (type 1), and handle /broadcast (type 2) by pushing a
// toast to all connected clients via the Hub DO. Response types: 1 = PONG, 4 = message reply.
function hexToBytes(hex) {
  const clean = String(hex || "").trim();
  const out = new Uint8Array(clean.length >> 1);
  for (let i = 0; i < out.length; i++) out[i] = parseInt(clean.substr(i * 2, 2), 16);
  return out;
}

async function verifyEd25519(pubHex, sigHex, msgBytes) {
  const pub = hexToBytes(pubHex);
  const sig = hexToBytes(sigHex);
  // Workers accept "Ed25519"; older runtimes used "NODE-ED25519". Try both for robustness.
  const algos = [{ name: "Ed25519" }, { name: "NODE-ED25519", namedCurve: "NODE-ED25519" }];
  for (const algo of algos) {
    try {
      const key = await crypto.subtle.importKey("raw", pub, algo, false, ["verify"]);
      return await crypto.subtle.verify(algo.name, key, sig, msgBytes);
    } catch { /* try next */ }
  }
  return false;
}

async function handleDiscord(request, env) {
  if (!env.DISCORD_PUBLIC_KEY) return json({ error: "not_configured" }, 500);
  const sig = request.headers.get("X-Signature-Ed25519");
  const ts = request.headers.get("X-Signature-Timestamp");
  const raw = await request.text();
  if (!sig || !ts) return new Response("missing signature", { status: 401 });

  const valid = await verifyEd25519(env.DISCORD_PUBLIC_KEY, sig, new TextEncoder().encode(ts + raw));
  if (!valid) return new Response("invalid signature", { status: 401 });

  let body;
  try { body = JSON.parse(raw); } catch { return json({ error: "bad_request" }, 400); }

  if (body.type === 1) return json({ type: 1 }); // PING -> PONG (required by Discord)

  if (body.type === 2 && body.data && body.data.name === "broadcast") {
    // Optional admin gate: if DISCORD_ADMIN_IDS is set, only those users may broadcast.
    const admins = String(env.DISCORD_ADMIN_IDS || "").split(",").map((s) => s.trim()).filter(Boolean);
    const invoker = (body.member && body.member.user && body.member.user.id) || (body.user && body.user.id) || "";
    if (admins.length && !admins.includes(invoker)) {
      return json({ type: 4, data: { content: "⛔ Non autorisé.", flags: 64 } });
    }
    const opt = (body.data.options || []).find((o) => o.name === "message");
    const msg = opt ? String(opt.value || "") : "";
    if (!msg) return json({ type: 4, data: { content: "Message vide.", flags: 64 } });
    let n = 0;
    try { n = await env.HUB.getByName("global").broadcast(msg); } catch { /* hub unreachable */ }
    return json({ type: 4, data: { content: `📢 Envoyé à ${n} joueur(s) : ${msg}`, flags: 64 } });
  }

  return json({ type: 4, data: { content: "Commande inconnue.", flags: 64 } });
}

async function handlePing(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid) return json({ error: "bad_request" }, 400);
  await env.ZZC.put(`ping:${b.uuid}`, JSON.stringify({ ...b, last: Date.now() }), { expirationTtl: PING_TTL });
  return noContent();
}

async function getSpawns(env) {
  const [m, w] = await Promise.all([env.ZZC.get("spawns:marchand"), env.ZZC.get("spawns:world_boss")]);
  return json({ marchand: m ? JSON.parse(m) : [], world_boss: w ? JSON.parse(w) : [] });
}

// Observed interval stats per spawn type. Intervals are computed between consecutive
// (chronologically sorted) recorded spawns; likely "missed spawn" gaps (> 2x median) are
// dropped so a single hole doesn't inflate the max. Returns durations in milliseconds.
async function getSpawnStats(env) {
  const [m, w] = await Promise.all([env.ZZC.get("spawns:marchand"), env.ZZC.get("spawns:world_boss")]);
  return json({
    marchand: intervalStats(m ? JSON.parse(m) : []),
    world_boss: intervalStats(w ? JSON.parse(w) : []),
  });
}

function median(xs) {
  if (!xs.length) return 0;
  const s = [...xs].sort((a, b) => a - b);
  const n = s.length;
  return n % 2 ? s[(n - 1) / 2] : Math.round((s[n / 2 - 1] + s[n / 2]) / 2);
}

function intervalStats(list) {
  // Need >= 3 spawns (>= 2 intervals) for a meaningful min/max range.
  if (!Array.isArray(list) || list.length < 3) {
    return { n: 0, samples: Array.isArray(list) ? list.length : 0, min: null, max: null, median: null };
  }
  const sorted = [...list].sort((a, b) => a - b);
  const iv = [];
  for (let i = 1; i < sorted.length; i++) iv.push(sorted[i] - sorted[i - 1]);
  const med = median(iv);
  // Two-sided outlier filter: drop likely missed-spawn gaps (> 2x median) AND spurious short
  // intervals from near-duplicate captures (< 0.5x median), so both ends of the range stay meaningful.
  const filtered = med > 0 ? iv.filter((d) => d >= 0.5 * med && d <= 2 * med) : iv;
  const use = filtered.length ? filtered : iv;
  return {
    n: use.length,
    samples: list.length,
    min: Math.min(...use),
    max: Math.max(...use),
    median: median(use),
  };
}

async function postSpawn(request, env) {
  const b = await readJson(request);
  if (!b || (b.type !== "marchand" && b.type !== "world_boss") || typeof b.at !== "number")
    return json({ error: "bad_request" }, 400);
  const key = `spawns:${b.type}`;
  const cur = await env.ZZC.get(key);
  let list = cur ? JSON.parse(cur) : [];
  const lastAt = list.length ? list[list.length - 1] : 0;
  if (b.at - lastAt >= SPAWN_DEDUP_MS) {
    list.push(b.at);
    if (list.length > SPAWN_CAP) list = list.slice(list.length - SPAWN_CAP);
    await env.ZZC.put(key, JSON.stringify(list));
  }
  return noContent();
}

async function postPresence(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid) return json({ error: "bad_request" }, 400);
  const entry = {
    uuid: b.uuid, name: b.name || "?", server: b.server || "",
    x: b.x || 0, y: b.y || 0, z: b.z || 0, dim: b.dim || "",
    mcuuid: b.mcuuid || "", modVersion: b.modVersion || "", last_update: Date.now(),
  };
  await env.ZZC.put(`presence:${b.uuid}`, JSON.stringify(entry), { expirationTtl: PRESENCE_TTL });
  return noContent();
}

async function delPresence(path, env) {
  const uuid = decodeURIComponent(path.slice("/presence/".length));
  if (uuid) await env.ZZC.delete(`presence:${uuid}`);
  return noContent();
}

async function getPresence(url, env) {
  const server = url.searchParams.get("server");
  const list = await env.ZZC.list({ prefix: "presence:" });
  const presences = [];
  for (const k of list.keys) {
    const v = await env.ZZC.get(k.name);
    if (!v) continue;
    const p = JSON.parse(v);
    if (server && p.server && p.server !== server) continue;
    presences.push({
      uuid: p.uuid, name: p.name, x: p.x, y: p.y || 0, z: p.z,
      dim: p.dim || "", mcuuid: p.mcuuid || "", last_update: p.last_update,
    });
  }
  return json({ presences });
}

// --- "Who's online" Discord roster (cron, every minute) ------------------
// Edits a single message in the roster channel with the currently-online mod users. presence:* keys
// auto-expire after PRESENCE_TTL, so listing them yields exactly who is online. The message id is kept
// in KV so we edit (not spam); if the message was deleted, a new one is created.
const ROSTER_MSG_KEY = "discord:roster_msg";

async function updateRoster(env) {
  const webhook = env.DISCORD_ROSTER_WEBHOOK_URL;
  if (!webhook) return;
  const list = await env.ZZC.list({ prefix: "presence:" });
  const names = [];
  for (const k of list.keys) {
    const v = await env.ZZC.get(k.name);
    if (!v) continue;
    try {
      const p = JSON.parse(v);
      if (p && p.name) names.push(String(p.name));
    } catch {}
  }
  names.sort((a, b) => a.localeCompare(b));
  const content = rosterContent(names);

  const msgId = await env.ZZC.get(ROSTER_MSG_KEY);
  if (msgId) {
    const r = await fetch(`${webhook}/messages/${msgId}`, {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ content, allowed_mentions: { parse: [] } }),
    });
    if (r.status !== 404) return;
    // Message was deleted in Discord — fall through and recreate.
  }
  const created = await fetch(`${webhook}?wait=true`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ content, username: "ZZC · En ligne", allowed_mentions: { parse: [] } }),
  });
  if (created.ok) {
    try {
      const msg = await created.json();
      if (msg && msg.id) await env.ZZC.put(ROSTER_MSG_KEY, String(msg.id));
    } catch {}
  }
}

function rosterContent(names) {
  const ts = `<t:${Math.floor(Date.now() / 1000)}:R>`;
  if (!names.length) {
    return `🔴 **Personne en ligne** · maj ${ts}`;
  }
  const header = `🟢 **En ligne (${names.length})** · maj ${ts}\n`;
  let body = "";
  let shown = 0;
  for (const n of names) {
    const add = (shown ? ", " : "") + n;
    if (header.length + body.length + add.length > 1900) break;
    body += add;
    ++shown;
  }
  if (shown < names.length) body += ` … (+${names.length - shown})`;
  return header + body;
}

async function postLeaderboard(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid) return json({ error: "bad_request" }, 400);
  await env.ZZC.put(`lb:${b.uuid}`, JSON.stringify({ ...b, updated: Date.now() }));
  return noContent();
}

async function getLeaderboard(env) {
  const list = await env.ZZC.list({ prefix: "lb:" });
  const rows = [];
  for (const k of list.keys) {
    const v = await env.ZZC.get(k.name);
    if (v) rows.push(JSON.parse(v));
  }
  rows.sort((a, b) => (b.points || 0) - (a.points || 0));
  return json({ leaderboard: rows });
}

// --- Discord routing -----------------------------------------------------
// Each message "kind" can go to its own channel: if the kind's secret is set it is used, otherwise it
// falls back to the shared DISCORD_WEBHOOK_URL. A distinct username tags each kind even in a shared
// channel. To split a flow out later, just add its secret (no code change). Extend by adding a kind here.
const DISCORD_KINDS = {
  feedback:  { secret: "DISCORD_FEEDBACK_WEBHOOK_URL",  username: "ZZC · Feedback" },
  directory: { secret: "DISCORD_DIRECTORY_WEBHOOK_URL", username: "ZZC · Annuaire" },
};

function discordTarget(env, kind) {
  const cfg = DISCORD_KINDS[kind] || {};
  const raw = (cfg.secret && env[cfg.secret]) || env.DISCORD_WEBHOOK_URL || "";
  // Trim stray whitespace/newlines a secret value may carry (e.g. from a piped `secret put`).
  const url = String(raw).trim();
  return url ? { url, username: cfg.username || "ZombieZ Companion" } : null;
}

// Posts one Discord message (<= 2000 chars) for a kind. Returns { ok, status }; never throws, so a
// malformed/invalid webhook URL degrades to ok=false instead of crashing the request (1101).
async function postDiscordMessage(env, kind, content) {
  const t = discordTarget(env, kind);
  if (!t) return { ok: false, status: 0 };
  try {
    const r = await fetch(t.url, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ content, username: t.username, allowed_mentions: { parse: [] } }),
    });
    return { ok: r.status >= 200 && r.status < 300, status: r.status };
  } catch (e) {
    return { ok: false, status: 0 };
  }
}

async function handleFeedback(request, env) {
  const b = await readJson(request);
  if (!b || !b.message) return json({ error: "bad_request" }, 400);
  // Extra per-IP limiter (the uuid cooldown below is client-supplied, so it is spoofable).
  if (env.FEEDBACK_LIMITER) {
    const ip = request.headers.get("CF-Connecting-IP") || "unknown";
    const { success } = await env.FEEDBACK_LIMITER.limit({ key: ip });
    if (!success) return json({ error: "rate_limited" }, 429);
  }
  const uuid = b.uuid || "anon";
  if (await env.ZZC.get(`fbrate:${uuid}`)) return json({ error: "rate_limited" }, 429);
  await env.ZZC.put(`fbrate:${uuid}`, "1", { expirationTtl: FEEDBACK_COOLDOWN });

  if (!discordTarget(env, "feedback")) return json({ error: "not_configured" }, 500);
  const msg = String(b.message).slice(0, MAX_MSG);
  const header = [
    `**Feedback** — \`${b.category || "?"}\``,
    `**De:** ${b.name || "?"}  \`${uuid}\``,
    `**Versions:** mod ${b.mod_version || "?"} · MC ${b.mc_version || "?"} · ${b.locale || "?"}`,
  ].join("\n");
  const full = header + "\n\n" + msg;
  // Discord caps a webhook message at 2000 chars — split long feedback into parts.
  const chunks = [];
  for (let i = 0; i < full.length; i += 1900) chunks.push(full.slice(i, i + 1900));
  for (const c of chunks) {
    const r = await postDiscordMessage(env, "feedback", c);
    if (!r.ok) return json({ error: "discord_failed", status: r.status }, 502);
  }
  return json({ ok: true }, 200);
}

// --- Friends -------------------------------------------------------------
// Friendship graph keyed on the player's real Minecraft account UUID (stable
// across mod reinstalls). Three KV lists per user, no TTL (persistent):
//   friend:<uuid> = [{uuid, name}]              accepted friends
//   freq:<uuid>   = [{uuid, name, at}]          incoming pending requests
//   fout:<uuid>   = [{uuid, name, at}]          outgoing pending requests
const FRIEND_CAP = 200;

async function getArr(env, key) {
  const v = await env.ZZC.get(key);
  if (!v) return [];
  try { const a = JSON.parse(v); return Array.isArray(a) ? a : []; } catch { return []; }
}

async function putArr(env, key, arr) {
  await env.ZZC.put(key, JSON.stringify(arr.slice(0, FRIEND_CAP)));
}

function without(arr, uuid) {
  return arr.filter((e) => e && e.uuid !== uuid);
}

// Name directory (pseudo -> mcuuid), so a friend request can target a player by username even when
// they are offline / not in the presence roster. Written once per play session (see /friends/announce),
// long TTL, so it stays cheap on the free KV tier. Latest writer wins on a name.
const NAME_TTL = 60 * 60 * 24 * 30; // 30 days

async function announceFriendName(request, env) {
  const b = await readJson(request);
  if (!b || !b.mcuuid || !b.name) return json({ error: "bad_request" }, 400);
  const key = `name:${String(b.name).toLowerCase()}`;
  await env.ZZC.put(key, JSON.stringify({ uuid: b.mcuuid, name: b.name }), { expirationTtl: NAME_TTL });
  // Notify Discord once ever, the first time this account appears in the directory (persistent marker).
  const knownKey = `known:${b.mcuuid}`;
  const seen = await env.ZZC.get(knownKey);
  if (!seen) {
    await env.ZZC.put(knownKey, "1");
    await notifyNewDirectoryUser(env, String(b.name), String(b.mcuuid));
  }
  return noContent();
}

async function notifyNewDirectoryUser(env, name, mcuuid) {
  const safeName = name.replace(/[`@]/g, "").slice(0, 64) || "?";
  try {
    await postDiscordMessage(env, "directory", `🆕 **Nouvel utilisateur du mod** dans l'annuaire : **${safeName}**  \`${mcuuid}\``);
  } catch (e) {
    // best-effort; never fail the announce because Discord is unreachable
  }
}

async function resolveFriendName(url, env) {
  const name = url.searchParams.get("name");
  if (!name) return json({ error: "bad_request" }, 400);
  const v = await env.ZZC.get(`name:${name.toLowerCase()}`);
  if (!v) return json({ error: "not_found" }, 404);
  return new Response(v, { status: 200, headers: { "content-type": "application/json" } });
}

async function getFriends(url, env) {
  const uuid = url.searchParams.get("uuid");
  if (!uuid) return json({ error: "bad_request" }, 400);
  const [friends, incoming, outgoing] = await Promise.all([
    getArr(env, `friend:${uuid}`),
    getArr(env, `freq:${uuid}`),
    getArr(env, `fout:${uuid}`),
  ]);
  return json({ friends, incoming, outgoing });
}

async function postFriendRequest(request, env) {
  const b = await readJson(request);
  if (!b || !b.from || !b.to || b.from === b.to) return json({ error: "bad_request" }, 400);
  const fromName = b.fromName || "?";
  const toName = b.toName || "?";
  // Already friends? no-op success.
  const friends = await getArr(env, `friend:${b.from}`);
  if (friends.some((e) => e.uuid === b.to)) return json({ ok: true, already: "friends" }, 200);
  // If the target already sent us a request, auto-accept instead of stacking.
  const mine = await getArr(env, `freq:${b.from}`);
  if (mine.some((e) => e.uuid === b.to)) return acceptFriendship(env, b.from, fromName, b.to);

  const inc = await getArr(env, `freq:${b.to}`);
  if (!inc.some((e) => e.uuid === b.from)) {
    inc.unshift({ uuid: b.from, name: fromName, at: Date.now() });
    await putArr(env, `freq:${b.to}`, inc);
  }
  const out = await getArr(env, `fout:${b.from}`);
  if (!out.some((e) => e.uuid === b.to)) {
    out.unshift({ uuid: b.to, name: toName, at: Date.now() });
    await putArr(env, `fout:${b.from}`, out);
  }
  return json({ ok: true }, 200);
}

// uuid accepts a pending request from `from`. accepterName is uuid's display name.
async function acceptFriendship(env, uuid, accepterName, from) {
  const inc = await getArr(env, `freq:${uuid}`);
  const req = inc.find((e) => e.uuid === from);
  if (!req) return json({ error: "no_request" }, 404);
  const fromName = req.name || "?";
  const [aFriends, bFriends] = await Promise.all([
    getArr(env, `friend:${uuid}`),
    getArr(env, `friend:${from}`),
  ]);
  if (!aFriends.some((e) => e.uuid === from)) aFriends.unshift({ uuid: from, name: fromName });
  if (!bFriends.some((e) => e.uuid === uuid)) bFriends.unshift({ uuid, name: accepterName });
  await Promise.all([
    putArr(env, `friend:${uuid}`, aFriends),
    putArr(env, `friend:${from}`, bFriends),
    putArr(env, `freq:${uuid}`, without(inc, from)),
    getArr(env, `fout:${from}`).then((o) => putArr(env, `fout:${from}`, without(o, uuid))),
  ]);
  return json({ ok: true }, 200);
}

async function postFriendAccept(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid || !b.from) return json({ error: "bad_request" }, 400);
  return acceptFriendship(env, b.uuid, b.name || "?", b.from);
}

async function postFriendDecline(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid || !b.from) return json({ error: "bad_request" }, 400);
  const [inc, out] = await Promise.all([
    getArr(env, `freq:${b.uuid}`),
    getArr(env, `fout:${b.from}`),
  ]);
  await Promise.all([
    putArr(env, `freq:${b.uuid}`, without(inc, b.from)),
    putArr(env, `fout:${b.from}`, without(out, b.uuid)),
  ]);
  return json({ ok: true }, 200);
}

async function postFriendRemove(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid || !b.friend) return json({ error: "bad_request" }, 400);
  const [a, c] = await Promise.all([
    getArr(env, `friend:${b.uuid}`),
    getArr(env, `friend:${b.friend}`),
  ]);
  await Promise.all([
    putArr(env, `friend:${b.uuid}`, without(a, b.friend)),
    putArr(env, `friend:${b.friend}`, without(c, b.uuid)),
  ]);
  return json({ ok: true }, 200);
}

// --- Groups --------------------------------------------------------------
// A party of up to MAX_GROUP members, keyed on account UUIDs. A player is in at most one group.
//   group:<gid>     = { id, chief, members:[{uuid,name}], createdAt }
//   gmember:<uuid>  = gid                 reverse index: which group a user is in
//   ginvite:<uuid>  = [{gid, from, fromName, at}]   pending group invites
const MAX_GROUP = 4;

async function loadGroup(env, gid) {
  if (!gid) return null;
  const v = await env.ZZC.get(`group:${gid}`);
  if (!v) return null;
  try {
    const g = JSON.parse(v);
    if (!Array.isArray(g.members)) g.members = [];
    return g;
  } catch { return null; }
}

async function saveGroup(env, g) {
  await env.ZZC.put(`group:${g.id}`, JSON.stringify(g));
}

async function getGroup(url, env) {
  const uuid = url.searchParams.get("uuid");
  if (!uuid) return json({ error: "bad_request" }, 400);
  const gid = await env.ZZC.get(`gmember:${uuid}`);
  let group = null;
  if (gid) {
    group = await loadGroup(env, gid);
    // Self-heal: the membership pointer references a dead group or a group we're no longer in.
    if (!group || !group.members.some((m) => m.uuid === uuid)) {
      await env.ZZC.delete(`gmember:${uuid}`);
      group = null;
    }
  }
  const invites = await getArr(env, `ginvite:${uuid}`);
  return json({ group, invites });
}

async function groupCreate(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid) return json({ error: "bad_request" }, 400);
  const existing = await env.ZZC.get(`gmember:${b.uuid}`);
  if (existing && (await loadGroup(env, existing))) return json({ error: "already_in_group" }, 409);
  const id = crypto.randomUUID();
  const g = { id, chief: b.uuid, members: [{ uuid: b.uuid, name: b.name || "?" }], createdAt: Date.now() };
  await saveGroup(env, g);
  await env.ZZC.put(`gmember:${b.uuid}`, id);
  return json({ ok: true, id }, 200);
}

async function groupInvite(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid || !b.to || b.uuid === b.to) return json({ error: "bad_request" }, 400);
  const g = await loadGroup(env, await env.ZZC.get(`gmember:${b.uuid}`));
  if (!g) return json({ error: "not_in_group" }, 404);
  if (g.chief !== b.uuid) return json({ error: "not_chief" }, 403);
  if (g.members.length >= MAX_GROUP) return json({ error: "group_full" }, 409);
  if (g.members.some((m) => m.uuid === b.to)) return json({ ok: true, already: "member" }, 200);
  const targetGid = await env.ZZC.get(`gmember:${b.to}`);
  if (targetGid && (await loadGroup(env, targetGid))) return json({ error: "target_in_group" }, 409);
  const invites = await getArr(env, `ginvite:${b.to}`);
  if (!invites.some((i) => i.gid === g.id)) {
    invites.unshift({ gid: g.id, from: b.uuid, fromName: b.name || "?", at: Date.now() });
    await putArr(env, `ginvite:${b.to}`, invites);
  }
  return json({ ok: true }, 200);
}

async function groupAccept(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid || !b.gid) return json({ error: "bad_request" }, 400);
  const cur = await env.ZZC.get(`gmember:${b.uuid}`);
  if (cur && (await loadGroup(env, cur))) return json({ error: "already_in_group" }, 409);
  const invites = await getArr(env, `ginvite:${b.uuid}`);
  if (!invites.some((i) => i.gid === b.gid)) return json({ error: "no_invite" }, 404);
  const g = await loadGroup(env, b.gid);
  if (!g) {
    await putArr(env, `ginvite:${b.uuid}`, invites.filter((i) => i.gid !== b.gid));
    return json({ error: "group_gone" }, 404);
  }
  if (g.members.length >= MAX_GROUP) return json({ error: "group_full" }, 409);
  if (!g.members.some((m) => m.uuid === b.uuid)) g.members.push({ uuid: b.uuid, name: b.name || "?" });
  await saveGroup(env, g);
  await env.ZZC.put(`gmember:${b.uuid}`, g.id);
  // Now in a group — drop every pending invite for this user.
  await env.ZZC.delete(`ginvite:${b.uuid}`);
  return json({ ok: true }, 200);
}

async function groupDecline(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid || !b.gid) return json({ error: "bad_request" }, 400);
  const invites = await getArr(env, `ginvite:${b.uuid}`);
  await putArr(env, `ginvite:${b.uuid}`, invites.filter((i) => i.gid !== b.gid));
  return json({ ok: true }, 200);
}

async function groupLeave(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid) return json({ error: "bad_request" }, 400);
  const gid = await env.ZZC.get(`gmember:${b.uuid}`);
  await env.ZZC.delete(`gmember:${b.uuid}`);
  const g = await loadGroup(env, gid);
  if (!g) return json({ ok: true }, 200);
  g.members = g.members.filter((m) => m.uuid !== b.uuid);
  if (g.members.length === 0) {
    await env.ZZC.delete(`group:${g.id}`);
    return json({ ok: true }, 200);
  }
  // Chief left — hand off to the oldest remaining member.
  if (g.chief === b.uuid) g.chief = g.members[0].uuid;
  await saveGroup(env, g);
  return json({ ok: true }, 200);
}

async function groupKick(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid || !b.target || b.uuid === b.target) return json({ error: "bad_request" }, 400);
  const g = await loadGroup(env, await env.ZZC.get(`gmember:${b.uuid}`));
  if (!g) return json({ error: "not_in_group" }, 404);
  if (g.chief !== b.uuid) return json({ error: "not_chief" }, 403);
  g.members = g.members.filter((m) => m.uuid !== b.target);
  await saveGroup(env, g);
  const tGid = await env.ZZC.get(`gmember:${b.target}`);
  if (tGid === g.id) await env.ZZC.delete(`gmember:${b.target}`);
  return json({ ok: true }, 200);
}

async function groupTransfer(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid || !b.target || b.uuid === b.target) return json({ error: "bad_request" }, 400);
  const g = await loadGroup(env, await env.ZZC.get(`gmember:${b.uuid}`));
  if (!g) return json({ error: "not_in_group" }, 404);
  if (g.chief !== b.uuid) return json({ error: "not_chief" }, 403);
  if (!g.members.some((m) => m.uuid === b.target)) return json({ error: "not_member" }, 404);
  g.chief = b.target;
  await saveGroup(env, g);
  return json({ ok: true }, 200);
}

// --- Group pings ---------------------------------------------------------
// One shared position marker per member: gping:<uuid> = {uuid,name,x,y,z,dim,at}. Expires after
// GPING_TTL so a disconnect doesn't leave a stale marker forever; members poll /group/pings.
const GPING_TTL = 60 * 30; // 30 minutes

async function groupPing(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid) return json({ error: "bad_request" }, 400);
  // Only meaningful while in a group, but store regardless; retrieval is group-scoped.
  const entry = {
    uuid: b.uuid, name: b.name || "?",
    x: b.x || 0, y: b.y || 0, z: b.z || 0, dim: b.dim || "", at: Date.now(),
  };
  await env.ZZC.put(`gping:${b.uuid}`, JSON.stringify(entry), { expirationTtl: GPING_TTL });
  return noContent();
}

async function groupPingClear(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid) return json({ error: "bad_request" }, 400);
  await env.ZZC.delete(`gping:${b.uuid}`);
  return noContent();
}

async function getGroupPings(url, env) {
  const uuid = url.searchParams.get("uuid");
  if (!uuid) return json({ error: "bad_request" }, 400);
  const g = await loadGroup(env, await env.ZZC.get(`gmember:${uuid}`));
  if (!g) return json({ pings: [], action: null });
  const pings = [];
  await Promise.all(g.members.map(async (m) => {
    const v = await env.ZZC.get(`gping:${m.uuid}`);
    if (v) {
      try { pings.push(JSON.parse(v)); } catch {}
    }
  }));
  // Latest chief action (follow-chief), short-lived — polled together with pings.
  let action = null;
  const av = await env.ZZC.get(`gaction:${g.id}`);
  if (av) { try { action = JSON.parse(av); } catch {} }
  return json({ pings, action });
}

// Chief broadcasts an action (e.g. a refuge tp) for followers to replay. KV enforces a 60s minimum TTL;
// clients de-dup by action id (lastActionId), so an action is still replayed at most once.
const GACTION_TTL = 60;

async function groupActionPost(request, env) {
  const b = await readJson(request);
  if (!b || !b.uuid || !b.type) return json({ error: "bad_request" }, 400);
  const g = await loadGroup(env, await env.ZZC.get(`gmember:${b.uuid}`));
  if (!g) return json({ error: "not_in_group" }, 404);
  if (g.chief !== b.uuid) return json({ error: "not_chief" }, 403);
  const action = { id: crypto.randomUUID(), type: String(b.type), arg: b.arg == null ? "" : String(b.arg), by: b.uuid, at: Date.now() };
  await env.ZZC.put(`gaction:${g.id}`, JSON.stringify(action), { expirationTtl: GACTION_TTL });
  return json({ ok: true }, 200);
}
