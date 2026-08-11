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
      if (path === "/leaderboard" && method === "POST") return postLeaderboard(request, env);
      if (path === "/leaderboard" && method === "GET") return getLeaderboard(env);
      if (path === "/feedback" && method === "POST") return handleFeedback(request, env);

      return json({ error: "not_found" }, 404);
    } catch (e) {
      return json({ error: "server_error", detail: String(e) }, 500);
    }
  },
};

async function readJson(request) {
  try { return await request.json(); } catch { return null; }
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
