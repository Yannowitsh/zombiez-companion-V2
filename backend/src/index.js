// Zombiez Companion V2 — backend API (Cloudflare Worker)
// Matches the mod's network contracts exactly. Storage: Workers KV (binding: ZZC).
// Secret:  DISCORD_WEBHOOK_URL   (set via: wrangler secret put DISCORD_WEBHOOK_URL)
// Vars:    LATEST_VERSION, DOWNLOAD_URL   (in wrangler.toml [vars])

const PRESENCE_TTL = 120;              // seconds a presence entry stays "online"
const PING_TTL = 60 * 60 * 24 * 30;    // 30 days
const SPAWN_CAP = 200;                 // keep last N spawn timestamps (long history for interval stats)
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
      if (method === "GET" && path === "/") return new Response("Zombiez Companion V2 API — ok");

      if (path === "/version" && method === "GET") return json({ latest: env.LATEST_VERSION || null, url: env.DOWNLOAD_URL || null });
      if (path === "/ping" && method === "POST") return handlePing(request, env);
      if (path === "/spawns" && method === "GET") return getSpawns(env);
      if (path === "/spawns/stats" && method === "GET") return getSpawnStats(env);
      if (path === "/spawns" && method === "POST") return postSpawn(request, env);
      if (path === "/presence" && method === "GET") return getPresence(url, env);
      if (path === "/presence" && method === "POST") return postPresence(request, env);
      if (path.startsWith("/presence/") && method === "DELETE") return delPresence(path, env);
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
  const filtered = med > 0 ? iv.filter((d) => d <= 2 * med) : iv;
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
    x: b.x || 0, z: b.z || 0, modVersion: b.modVersion || "", last_update: Date.now(),
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
    presences.push({ uuid: p.uuid, name: p.name, x: p.x, z: p.z, last_update: p.last_update });
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

async function handleFeedback(request, env) {
  const b = await readJson(request);
  if (!b || !b.message) return json({ error: "bad_request" }, 400);
  const uuid = b.uuid || "anon";
  if (await env.ZZC.get(`fbrate:${uuid}`)) return json({ error: "rate_limited" }, 429);
  await env.ZZC.put(`fbrate:${uuid}`, "1", { expirationTtl: FEEDBACK_COOLDOWN });

  if (!env.DISCORD_WEBHOOK_URL) return json({ error: "not_configured" }, 500);
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
    const r = await fetch(env.DISCORD_WEBHOOK_URL, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ content: c, allowed_mentions: { parse: [] } }),
    });
    if (!(r.status >= 200 && r.status < 300)) return json({ error: "discord_failed", status: r.status }, 502);
  }
  return json({ ok: true }, 200);
}
