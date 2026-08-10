package io.github.keoz5.zombiezcompanion;

public final class ModInfo {
    public static final String MOD_ID = "zombiezcompanionv2";
    public static final String MOD_NAME = "Zombiez Companion V2";

    // Backend API base URL. All network features (feedback, version check, spawn timers,
    // presence/leaderboard) go through this single endpoint.
    // TODO: replace with your deployed Cloudflare Worker URL once it is live.
    public static final String API_BASE = "https://zombiezcompanion-api.CHANGEME.workers.dev";

    // Discord contact — opens yannowitsh's Discord profile. Swap for a server invite
    // (https://discord.gg/<code>) if you'd rather link a server.
    public static final String DISCORD_URL = "https://discord.com/users/236894641138565123";

    private ModInfo() {
    }
}
