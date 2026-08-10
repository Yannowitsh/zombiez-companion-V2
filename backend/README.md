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

## Updating the update-notification

Edit `LATEST_VERSION` / `DOWNLOAD_URL` in `wrangler.toml`, then `wrangler deploy` again.

## Endpoints (match the mod)

| Method | Path | Purpose |
|---|---|---|
| GET | `/version` | latest version for the in-game update notice |
| POST | `/ping` | anonymous usage ping |
| GET/POST | `/spawns` | crowd-sourced marchand / world-boss spawn timestamps |
| GET/POST/DELETE | `/presence` | who's online (auto-expires after 120s) |
| POST/GET | `/leaderboard` | player stats leaderboard |
| POST | `/feedback` | forwards to your Discord webhook (rate-limited 1/min per user) |
