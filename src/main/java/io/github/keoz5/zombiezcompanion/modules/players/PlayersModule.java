package io.github.keoz5.zombiezcompanion.modules.players;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.PlayersConfig;
import io.github.keoz5.zombiezcompanion.config.TelemetryConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.modules.players.PlayersOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.stats.StatsModule;
import io.github.keoz5.zombiezcompanion.modules.telemetry.PresenceCache;
import io.github.keoz5.zombiezcompanion.net.HttpClients;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

public final class PlayersModule
implements Module {
    public static final String ID = "players";
    private static final String ENDPOINT = "https://zombiez-companion-api.keoz5.workers.dev";
    private static final long PRESENCE_INTERVAL_MS = 10000L;
    private static final long PRESENCE_REFRESH_MS = 5000L;
    private static final long LEADERBOARD_INTERVAL_MS = 60000L;
    private static final String SERVER_KEY = "rinaorc.com";
    private ConfigManager configManager;
    private long nextPresenceMs;
    private long nextRefreshMs;
    private long nextLeaderboardMs;
    private boolean presenceActive;
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
        return Text.translatable((String)"zombiezcompanion.module.players.desc").getString();
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
    public void onClientTick(MinecraftClient client) {
        boolean onServer;
        long now = System.currentTimeMillis();
        boolean bl = onServer = client.player != null && client.world != null && ZombieZDetector.isOnZombieZ();
        if (!onServer) {
            if (this.presenceActive) {
                this.deletePresence();
                this.presenceActive = false;
                PresenceCache.clear();
            }
            return;
        }
        PlayersConfig cfg = this.config();
        if (cfg.broadcastPosition && now >= this.nextPresenceMs) {
            this.sendPresence(client);
            this.nextPresenceMs = now + 10000L;
        } else if (!cfg.broadcastPosition && this.presenceActive) {
            this.deletePresence();
            this.presenceActive = false;
        }
        if (this.configManager.get().map.showModUsers && now >= this.nextRefreshMs) {
            this.refreshPresences();
            this.nextRefreshMs = now + 5000L;
        }
        if (cfg.broadcastPosition && now >= this.nextLeaderboardMs) {
            this.sendLeaderboard(client);
            this.nextLeaderboardMs = now + 60000L;
        }
    }

    private void sendLeaderboard(MinecraftClient client) {
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
        this.postAsync("https://zombiez-companion-api.keoz5.workers.dev/leaderboard", body);
    }

    private static String tabDisplayName(MinecraftClient client) {
        try {
            String disp;
            PlayerListEntry entry;
            ClientPlayNetworkHandler nh = client.getNetworkHandler();
            if (nh != null && (entry = nh.getPlayerListEntry(client.player.getUuid())) != null && entry.getDisplayName() != null && !(disp = entry.getDisplayName().getString().replaceAll("\u00a7.", "").trim()).isBlank()) {
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
    }

    @Override
    public void onLeaveWorld() {
        if (this.presenceActive) {
            this.deletePresence();
            this.presenceActive = false;
        }
        PresenceCache.clear();
    }

    private void sendPresence(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"name\":\"%s\",\"server\":\"%s\",\"x\":%.1f,\"z\":%.1f,\"modVersion\":\"%s\"}", PlayersModule.escape(this.selfUuid()), PlayersModule.escape(client.player.getName().getString()), SERVER_KEY, client.player.getX(), client.player.getZ(), PlayersModule.escape(PlayersModule.modVersion()));
        this.postAsync("https://zombiez-companion-api.keoz5.workers.dev/presence", body);
        this.presenceActive = true;
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer("zombiezcompanion").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    private void deletePresence() {
        this.deleteAsync("https://zombiez-companion-api.keoz5.workers.dev/presence/" + this.selfUuid());
    }

    private void refreshPresences() {
        this.getAsync("https://zombiez-companion-api.keoz5.workers.dev/presence?server=rinaorc.com").thenAccept(resp -> {
            if (resp != null) {
                PresenceCache.update(resp, this.selfUuid());
            }
        });
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

    private void deleteAsync(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5L)).DELETE().build();
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

