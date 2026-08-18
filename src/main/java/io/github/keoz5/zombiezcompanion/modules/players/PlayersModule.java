package io.github.keoz5.zombiezcompanion.modules.players;

import io.github.keoz5.zombiezcompanion.ModInfo;
import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.PlayersConfig;
import io.github.keoz5.zombiezcompanion.config.TelemetryConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.modules.friends.FriendsCache;
import io.github.keoz5.zombiezcompanion.modules.friends.FriendsModule;
import io.github.keoz5.zombiezcompanion.modules.groups.GroupsCache;
import io.github.keoz5.zombiezcompanion.modules.groups.GroupsModule;
import io.github.keoz5.zombiezcompanion.modules.groups.GroupsScreen;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.modules.players.PlayersOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.stats.StatsModule;
import io.github.keoz5.zombiezcompanion.modules.telemetry.PresenceCache;
import io.github.keoz5.zombiezcompanion.net.HttpClients;
import io.github.keoz5.zombiezcompanion.realtime.RealtimeClient;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class PlayersModule
implements Module {
    public static final String ID = "players";
    private static final String ENDPOINT = ModInfo.API_BASE;
    private static final long PRESENCE_INTERVAL_MS = 5000L;
    private static final long PRESENCE_REFRESH_MS = 2000L;
    private static final long LEADERBOARD_INTERVAL_MS = 60000L;
    // Only broadcast on real movement, with a periodic heartbeat so followers' last_update stays fresh.
    private static final double PRESENCE_MOVE_THRESHOLD = 3.0;
    private static final long PRESENCE_HEARTBEAT_MS = 90000L;
    // Anti-AFK: stop broadcasting presence after this long stationary (within PRESENCE_MOVE_THRESHOLD of
    // the anchor point) — a stationary player has nothing new to say; skip the pointless resend.
    private static final long AFK_THRESHOLD_MS = 180000L;
    // Cap how many uuids /presence/batch fans out per call (mirrors the backend's own cap).
    private static final int PRESENCE_BATCH_CAP = 50;
    private static final String SERVER_KEY = "rinaorc.com";
    private ConfigManager configManager;
    private long nextPresenceMs;
    private long nextRefreshMs;
    private long nextLeaderboardMs;
    private boolean presenceActive;
    private double lastPostX;
    private double lastPostZ;
    private String lastPostDim = "";
    private long lastPostMs;
    private double afkAnchorX;
    private double afkAnchorZ;
    private long afkSinceMs;
    private boolean afk;
    private static final Pattern PRESTIGE_RE = Pattern.compile("^\\s*\\[?(\\d+)\\]?\\s*");
    private static final Pattern NIV_SUFFIX_RE = Pattern.compile("\\s*Niv\\.?\\s*\\d+\\s*$");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Joueurs";
    }

    @Override
    public String description() {
        return Component.translatable((String)"zombiezcompanion.module.players.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.PLAYERS;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("joueurs", ID, "position", "pseudo", "coordonn\u00e9es", "diffusion", "broadcast", "visible", "mod users", "pr\u00e9sence");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
        this.ensureUuid();
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new PlayersOptionsScreen(parent, this, this.configManager);
    }

    public PlayersConfig config() {
        return this.configManager.get().players;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    private void ensureUuid() {
        TelemetryConfig tcfg = this.configManager.get().telemetry;
        if (tcfg.uuid == null || tcfg.uuid.isBlank()) {
            tcfg.uuid = UUID.randomUUID().toString();
            this.configManager.save();
        }
    }

    private String selfUuid() {
        return this.configManager.get().telemetry.uuid;
    }

    @Override
    public void onClientTick(Minecraft client) {
        boolean onServer;
        long now = System.currentTimeMillis();
        boolean bl = onServer = client.player != null && client.level != null && ZombieZDetector.isOnZombieZ();
        if (!onServer) {
            if (this.presenceActive) {
                this.deletePresence();
                this.presenceActive = false;
                PresenceCache.clear();
            }
            return;
        }
        this.updateAfkState(client, now);
        PlayersConfig cfg = this.config();
        if (cfg.broadcastPosition) {
            // AFK: skip resends but leave the last known position on the Hub attachment — it's still
            // accurate (we haven't moved), and keeping it is what lets us stay "online" for Discord/
            // friends while stationary instead of vanishing after AFK_THRESHOLD_MS.
            if (!this.afk && now >= this.nextPresenceMs) {
                this.maybeSendPresence(client, now);
                this.nextPresenceMs = now + PRESENCE_INTERVAL_MS;
            }
        } else if (this.presenceActive) {
            this.deletePresence();
            this.presenceActive = false;
        }
        // Friends/Groups presence needs only a small, known set of members, and only while the Amis &
        // Groupe screen is actually open to look at it — the targeted /presence/batch endpoint, gated
        // on screen visibility instead of polling in the background for the whole play session.
        boolean groupsScreenOpen = client.screen instanceof GroupsScreen;
        boolean wantsBatch = groupsScreenOpen && (FriendsModule.wantsPresenceRefresh() || GroupsModule.wantsPresenceRefresh());
        if (wantsBatch && now >= this.nextRefreshMs) {
            this.refreshPresencesBatch();
            this.nextRefreshMs = now + PRESENCE_REFRESH_MS;
        }
        if (cfg.broadcastPosition && now >= this.nextLeaderboardMs) {
            this.sendLeaderboard(client);
            this.nextLeaderboardMs = now + 60000L;
        }
    }

    private void sendLeaderboard(Minecraft client) {
        String name;
        if (client.player == null) {
            return;
        }
        StatsModule stats = StatsModule.get();
        if (stats == null) {
            return;
        }
        int level = stats.profilLevel();
        if (level < 0) {
            return;
        }
        String disp = PlayersModule.tabDisplayName(client);
        int prestige = -1;
        Matcher pm = PRESTIGE_RE.matcher(disp);
        if (pm.find()) {
            try {
                prestige = Integer.parseInt(pm.group(1));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
            disp = disp.substring(pm.end());
        }
        if ((name = NIV_SUFFIX_RE.matcher(disp).replaceAll("").trim()).isBlank()) {
            name = client.player.getName().getString();
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"name\":\"%s\",\"prestige\":%d,\"level\":%d,\"rodeur\":%d,\"kills\":%d,\"points\":%d}", PlayersModule.escape(this.selfUuid()), PlayersModule.escape(name), Math.max(0, prestige), level, Math.max(0, stats.rodeurLevel()), stats.killsTotal(), stats.pointsTotal());
        this.postAsync(ModInfo.API_BASE + "/leaderboard", body);
    }

    private static String tabDisplayName(Minecraft client) {
        try {
            String disp;
            PlayerInfo entry;
            ClientPacketListener nh = client.getConnection();
            if (nh != null && (entry = nh.getPlayerInfo(client.player.getUUID())) != null && entry.getTabListDisplayName() != null && !(disp = entry.getTabListDisplayName().getString().replaceAll("\u00a7.", "").trim()).isBlank()) {
                return disp;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return client.player.getName().getString();
    }

    @Override
    public void onDisable() {
        if (this.presenceActive) {
            this.deletePresence();
            this.presenceActive = false;
        }
        PresenceCache.clear();
        this.afkSinceMs = 0L;
    }

    @Override
    public void onLeaveWorld() {
        if (this.presenceActive) {
            this.deletePresence();
            this.presenceActive = false;
        }
        PresenceCache.clear();
        this.afkSinceMs = 0L;
    }

    /**
     * Broadcasts presence only when it matters (not every tick): when the player first appears, has
     * moved at least {@link #PRESENCE_MOVE_THRESHOLD} blocks, switched map, or when a heartbeat is due —
     * the Hub keeps our position on the WebSocket connection's attachment, no TTL to refresh server-side
     * anymore, but a periodic resend still helps a follower's client-side cache (last_update) stay fresh.
     * Called at most once per {@link #PRESENCE_INTERVAL_MS}.
     */
    private void maybeSendPresence(Minecraft client, long now) {
        if (client.player == null) {
            return;
        }
        double x = client.player.getX();
        double z = client.player.getZ();
        String dim = client.level != null ? client.level.dimension().identifier().toString() : "";
        boolean moved = Math.hypot(x - this.lastPostX, z - this.lastPostZ) >= PRESENCE_MOVE_THRESHOLD;
        boolean dimChanged = !dim.equals(this.lastPostDim);
        boolean heartbeat = now - this.lastPostMs >= PRESENCE_HEARTBEAT_MS;
        if (!this.presenceActive || moved || dimChanged || heartbeat) {
            this.sendPresence(client);
            this.lastPostX = x;
            this.lastPostZ = z;
            this.lastPostDim = dim;
            this.lastPostMs = now;
        }
    }

    /**
     * Broadcasts our position over the realtime WebSocket (Hub Durable Object) instead of a KV-backed
     * HTTP POST — see RealtimeClient#sendPos. The Hub keeps it on our connection's attachment and serves
     * it back via GET /presence and /presence/batch; no server-side write happens at all.
     */
    private void sendPresence(Minecraft client) {
        if (client.player == null) {
            return;
        }
        String dim = client.level != null ? client.level.dimension().identifier().toString() : "";
        RealtimeClient.sendPos(client.player.getX(), client.player.getY(), client.player.getZ(), dim, SERVER_KEY);
        this.presenceActive = true;
    }

    private void deletePresence() {
        RealtimeClient.sendPosClear();
    }

    /**
     * Targeted refresh for Friends/Groups: only the uuids that are actually needed (visible friends +
     * current group members), via /presence/batch — a get() per uuid server-side, no roster list().
     */
    private void refreshPresencesBatch() {
        LinkedHashSet<String> uuids = new LinkedHashSet<>();
        if (FriendsModule.wantsPresenceRefresh()) {
            FriendsModule fm = FriendsModule.get();
            for (FriendsCache.Friend f : FriendsCache.friends()) {
                if (fm == null || fm.isVisible(f.uuid())) {
                    uuids.add(f.uuid());
                }
            }
        }
        if (GroupsModule.wantsPresenceRefresh()) {
            uuids.addAll(GroupsCache.memberUuids());
        }
        if (uuids.isEmpty()) {
            return;
        }
        if (uuids.size() > PRESENCE_BATCH_CAP) {
            LinkedHashSet<String> capped = new LinkedHashSet<>();
            for (String u : uuids) {
                if (capped.size() >= PRESENCE_BATCH_CAP) break;
                capped.add(u);
            }
            uuids = capped;
        }
        String qs = URLEncoder.encode(String.join(",", uuids), StandardCharsets.UTF_8);
        this.getAsync(ModInfo.API_BASE + "/presence/batch?uuids=" + qs).thenAccept(resp -> {
            if (resp != null) {
                PresenceCache.update(resp, FriendsModule.selfMcUuid());
            }
        });
    }

    /**
     * Tracks how long the player has been stationary (within {@link #PRESENCE_MOVE_THRESHOLD} blocks of
     * the anchor). After {@link #AFK_THRESHOLD_MS} without real movement, presence broadcast pauses (no
     * more sends/heartbeat) but the last known position stays on the Hub — still online, just not
     * resending an unchanged position. Resumes immediately on the next real move.
     */
    private void updateAfkState(Minecraft client, long now) {
        if (client.player == null) {
            return;
        }
        double x = client.player.getX();
        double z = client.player.getZ();
        if (this.afkSinceMs == 0L || Math.hypot(x - this.afkAnchorX, z - this.afkAnchorZ) >= PRESENCE_MOVE_THRESHOLD) {
            this.afkAnchorX = x;
            this.afkAnchorZ = z;
            this.afkSinceMs = now;
            this.afk = false;
        } else {
            this.afk = now - this.afkSinceMs >= AFK_THRESHOLD_MS;
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void postAsync(String url, String body) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5L)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpClients.SHARED.sendAsync(req, HttpResponse.BodyHandlers.discarding()).exceptionally(t -> null);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private CompletableFuture<String> getAsync(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5L)).GET().build();
            return ((CompletableFuture)HttpClients.SHARED.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(r -> r.statusCode() == 200 ? (String)r.body() : null)).exceptionally(t -> null);
        }
        catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    public static PlayersModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module m : mm.modules()) {
            if (!(m instanceof PlayersModule)) continue;
            PlayersModule p = (PlayersModule)m;
            return p;
        }
        return null;
    }
}

