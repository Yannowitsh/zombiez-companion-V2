# Zombiez Companion V2 — Backend (Cloudflare Worker)

Free backend for the mod's network features (feedback → Discord, version check,
spawn timers, presence, leaderboard). Storage: Cloudflare KV (free tier).

## One-time setup

Requires [Node.js](https://nodejs.org) installed. All commands are run **inside this `backend/` folder**.

```bash
# 1. Install Wrangler (Cloudflare's CLI)
npm install -g wrangler

# 2. Log in (opens your browser)
wrangler login

# 3. Create the KV namespace, then paste the printed id into wrangler.toml (id = "...")
wrangler kv namespace create ZZC

# 4. Store the Discord webhook as a SECRET (never committed to the repo).
#    Paste your webhook URL when prompted.
wrangler secret put DISCORD_WEBHOOK_URL

# 5. Deploy
wrangler deploy
```

Step 5 prints your URL, e.g. `https://zombiezcompanion-api.<your-subdomain>.workers.dev`.
**Send me that URL** — I set it in the mod (`ModInfo.API_BASE`) and rebuild.

## Test it

```bash
curl https://zombiezcompanion-api.<your-subdomain>.workers.dev/version
# -> {"latest":"1.0.1","url":"..."}
```

## Rate limiting & billing safety

The worker rate-limits **per IP at the edge** (Cloudflare Rate Limiting bindings — free, no KV cost) to
guard against request floods / "denial of wallet":

- `API_LIMITER` — every request, **300 / 60s per IP** (covers reads and writes).
- `FEEDBACK_LIMITER` — feedback only, **5 / 60s per IP** (anti Discord spam; on top of the per-uuid cooldown).

Both are declared in `wrangler.toml` (`[[ratelimits]]`) and applied in `src/index.js`; over-limit requests
get `429 rate_limited`. Adjust the `limit`/`period` there if legitimate players ever get throttled.

> ⚠️ Cloudflare has **no hard spend cap** that auto-stops billing. Set a **billing/usage notification**
> in the dashboard (Manage Account → Notifications) so you're alerted well before any overage. On the
> Workers Paid plan ($5/mo) a small userbase stays far inside the included quotas.

## "Who's online" roster (cron)

A **Cron Trigger** (`crons = ["* * * * *"]` in `wrangler.toml`) runs every minute and **edits one Discord
message** with the currently-online mod users (from the auto-expiring `presence:*` keys). Set the target
channel's webhook and deploy:

```bash
wrangler secret put DISCORD_ROSTER_WEBHOOK_URL
wrangler deploy
```

The message id is stored in KV (`discord:roster_msg`) so it edits in place (no spam); if you delete the
message in Discord it recreates one. If the secret is unset, the cron is a no-op.

## Discord routing (per-channel, optional)

Discord messages are grouped by **kind**. Each kind uses its own webhook secret **if set**, otherwise it
falls back to the shared `DISCORD_WEBHOOK_URL`. A distinct username tags each kind even in a shared channel.

| Kind | Optional secret | Falls back to |
|---|---|---|
| `feedback` | `DISCORD_FEEDBACK_WEBHOOK_URL` | `DISCORD_WEBHOOK_URL` |
| `directory` (new mod user) | `DISCORD_DIRECTORY_WEBHOOK_URL` | `DISCORD_WEBHOOK_URL` |

To split a flow into its own channel, create a webhook there and store it — no code change, then redeploy:

```bash
wrangler secret put DISCORD_DIRECTORY_WEBHOOK_URL
wrangler deploy
```

Add a new kind by extending `DISCORD_KINDS` in `src/index.js`.

## Updating the update-notification

Edit `LATEST_VERSION` / `DOWNLOAD_URL` in `wrangler.toml`, then `wrangler deploy` again.

## Endpoints (match the mod)

| Method | Path | Purpose |
|---|---|---|
| GET | `/version` | latest version for the in-game update notice |
| POST | `/ping` | anonymous usage ping |
| GET/POST | `/spawns` | crowd-sourced marchand / world-boss spawn timestamps |
| GET | `/spawns/stats` | observed spawn-interval stats (min/max/median per type) |
| GET/POST/DELETE | `/presence` | who's online (auto-expires after 120s); carries `x/y/z/dim/mcuuid` |
| POST/GET | `/leaderboard` | player stats leaderboard |
| GET | `/friends?uuid=` | a player's friends + incoming/outgoing requests (keyed on MC account UUID) |
| GET | `/friends/resolve?name=` | resolve a username to its account UUID (name directory) |
| POST | `/friends/announce` | `{mcuuid, name}` — register name→uuid (long TTL); pings Discord once on a brand-new account |
| POST | `/friends/request` | `{from, fromName, to, toName}` — send a friend request |
| POST | `/friends/accept` | `{uuid, name, from}` — accept a pending request |
| POST | `/friends/decline` | `{uuid, from}` — decline/cancel a pending request |
| POST | `/friends/remove` | `{uuid, friend}` — remove an existing friend (both sides) |
| POST | `/feedback` | forwards to your Discord webhook (rate-limited 1/min per user) |
