package io.github.keoz5.zombiezcompanion.modules.friends;

import io.github.keoz5.zombiezcompanion.ModInfo;
import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.FriendsConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.modules.telemetry.PresenceCache;
import io.github.keoz5.zombiezcompanion.net.HttpClients;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Friends / group system. Players befriend each other by real Minecraft account UUID (backend-stored,
 * request + accept). Once friends, each side locally toggles which friends to display; visible friends
 * who are online in the same dimension are drawn as a moving waypoint (far) or a compact HUD dot (near),
 * and can be quick-teleported to via the nearest refuge — the same mechanism as the WB/merchant tp.
 */
public final class FriendsModule
implements Module {
    public static final String ID = "friends";
    private static final long POLL_MS = 15000L;
    private static final int FRIEND_COLOR = 0xFF33C1FF;

    private ConfigManager configManager;
    private long nextPollMs;
    private boolean announced;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Amis";
    }

    @Override
    public String description() {
        return Component.translatable((String)"zombiezcompanion.module.friends.desc").getString();
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
        return List.of("amis", "friends", "groupe", "group", "team", "équipe", "tp", "waypoint");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new FriendsScreen(parent, this, this.configManager);
    }

    public FriendsConfig config() {
        return this.configManager.get().friends;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    public static FriendsModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module m : mm.modules()) {
            if (m instanceof FriendsModule) {
                return (FriendsModule)m;
            }
        }
        return null;
    }

    // --- Identity -----------------------------------------------------------

    public static String selfMcUuid() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getUUID().toString() : null;
    }

    private static String selfName() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getName().getString() : "?";
    }

    // --- Presence-cadence hooks (queried by PlayersModule) ------------------

    private static boolean featureActive() {
        ConfigManager cm = ZombieZCompanionClient.configManager();
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        return cm != null && mm != null && mm.isEnabled(ID) && cm.get().friends.showFriends;
    }

    /** True when the roster should keep refreshing so friends can be located and the add-friend list stays live. */
    public static boolean wantsPresenceRefresh() {
        return FriendsModule.featureActive();
    }

    private static PresenceCache.Presence onlinePresence(String mcuuid) {
        if (mcuuid == null || mcuuid.isBlank()) {
            return null;
        }
        for (PresenceCache.Presence p : PresenceCache.presences()) {
            if (mcuuid.equals(p.mcuuid())) {
                return p;
            }
        }
        return null;
    }

    // --- Polling ------------------------------------------------------------

    @Override
    public void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null || !ZombieZDetector.isOnZombieZ()) {
            return;
        }
        // Register our pseudo -> account-uuid once per session so friends can add us by name while offline.
        if (!this.announced) {
            this.announce();
            this.announced = true;
        }
        long now = System.currentTimeMillis();
        if (now >= this.nextPollMs) {
            this.refresh();
            this.nextPollMs = now + POLL_MS;
        }
    }

    @Override
    public void onLeaveWorld() {
        FriendsCache.clear();
        this.announced = false;
    }

    @Override
    public void onDisable() {
        FriendsCache.clear();
        this.announced = false;
    }

    public void refresh() {
        String self = FriendsModule.selfMcUuid();
        if (self == null) {
            return;
        }
        this.getAsync(ModInfo.API_BASE + "/friends?uuid=" + self).thenAccept(resp -> {
            if (resp != null) {
                FriendsCache.update(resp);
            }
        });
    }

    private void refreshSoon() {
        this.nextPollMs = 0L;
        this.refresh();
    }

    // --- Mutations ----------------------------------------------------------

    public void sendRequest(String toUuid, String toName) {
        String self = FriendsModule.selfMcUuid();
        if (self == null || toUuid == null || toUuid.equals(self)) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"from\":\"%s\",\"fromName\":\"%s\",\"to\":\"%s\",\"toName\":\"%s\"}", esc(self), esc(FriendsModule.selfName()), esc(toUuid), esc(toName));
        this.postThenRefresh(ModInfo.API_BASE + "/friends/request", body);
    }

    /** Resolve a username to an account UUID via the backend directory, then send a friend request. */
    public void sendRequestByName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        String self = FriendsModule.selfMcUuid();
        if (name.isEmpty() || self == null) {
            return;
        }
        String url = ModInfo.API_BASE + "/friends/resolve?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        this.getAsync(url).thenAccept(resp -> {
            if (resp == null) {
                FriendsModule.chat("zombiezcompanion.friends.msg.not_found", name);
                return;
            }
            try {
                JsonObject o = JsonParser.parseString((String)resp).getAsJsonObject();
                String uuid = o.has("uuid") ? o.get("uuid").getAsString() : null;
                String resolved = o.has("name") ? o.get("name").getAsString() : name;
                if (uuid == null || uuid.equals(self)) {
                    FriendsModule.chat("zombiezcompanion.friends.msg.not_found", name);
                    return;
                }
                this.sendRequest(uuid, resolved);
                FriendsModule.chat("zombiezcompanion.friends.msg.sent", resolved);
            }
            catch (Exception e) {
                FriendsModule.chat("zombiezcompanion.friends.msg.not_found", name);
            }
        });
    }

    private void announce() {
        String self = FriendsModule.selfMcUuid();
        Minecraft mc = Minecraft.getInstance();
        if (self == null || mc.player == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"mcuuid\":\"%s\",\"name\":\"%s\"}", esc(self), esc(mc.player.getName().getString()));
        this.postAsync(ModInfo.API_BASE + "/friends/announce", body);
    }

    private static void chat(String key, Object arg) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.gui != null) {
                mc.gui.getChat().addClientSystemMessage((Component)Component.translatable((String)key, (Object[])new Object[]{arg}));
            }
        });
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

    public void accept(String fromUuid) {
        String self = FriendsModule.selfMcUuid();
        if (self == null || fromUuid == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"name\":\"%s\",\"from\":\"%s\"}", esc(self), esc(FriendsModule.selfName()), esc(fromUuid));
        this.postThenRefresh(ModInfo.API_BASE + "/friends/accept", body);
    }

    public void decline(String fromUuid) {
        String self = FriendsModule.selfMcUuid();
        if (self == null || fromUuid == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"from\":\"%s\"}", esc(self), esc(fromUuid));
        this.postThenRefresh(ModInfo.API_BASE + "/friends/decline", body);
    }

    public void removeFriend(String friendUuid) {
        String self = FriendsModule.selfMcUuid();
        if (self == null || friendUuid == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"friend\":\"%s\"}", esc(self), esc(friendUuid));
        this.postThenRefresh(ModInfo.API_BASE + "/friends/remove", body);
    }

    private void postThenRefresh(String url, String body) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5L)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpClients.SHARED.sendAsync(req, HttpResponse.BodyHandlers.discarding()).whenComplete((r, t) -> this.refreshSoon());
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    // --- Local visibility ---------------------------------------------------

    public boolean isVisible(String uuid) {
        return !this.config().hidden.contains(uuid);
    }

    public void setVisible(String uuid, boolean visible) {
        FriendsConfig cfg = this.config();
        if (visible) {
            cfg.hidden.remove(uuid);
        } else if (!cfg.hidden.contains(uuid)) {
            cfg.hidden.add(uuid);
        }
        this.configManager.save();
    }

    /** Quick-teleport to a friend via the nearest refuge (same mechanism as the WB/merchant tp). */
    public static void tpTo(PresenceCache.Presence p) {
        if (p != null) {
            ZombieZCompanionClient.quickTpTo(p.x(), p.z(), p.dim());
        }
    }

    public static PresenceCache.Presence presenceOf(String mcuuid) {
        return FriendsModule.onlinePresence(mcuuid);
    }

    // --- Rendering ----------------------------------------------------------

    @Override
    public void onHudRender(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null || client.options.hideGui) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        FriendsConfig cfg = this.config();
        if (!cfg.showFriends) {
            return;
        }
        String playerDim = client.level != null ? client.level.dimension().identifier().toString() : "";
        Set<String> hidden = new HashSet<String>(cfg.hidden);
        int near = Math.max(0, cfg.nearHudRange);
        for (FriendsCache.Friend f : FriendsCache.friends()) {
            if (hidden.contains(f.uuid())) continue;
            PresenceCache.Presence p = FriendsModule.onlinePresence(f.uuid());
            if (p == null) continue;
            if (!playerDim.isEmpty() && !p.dim().isEmpty() && !playerDim.equals(p.dim())) continue;
            double dist = Math.sqrt(client.player.distanceToSqr(p.x(), p.y(), p.z()));
            if (dist >= (double)near) {
                WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, p.x(), p.y(), p.z(), f.name(), FRIEND_COLOR);
            } else {
                FriendsModule.renderNearDot(ctx, client, p, f.name());
            }
        }
    }

    /** Compact marker for a nearby friend: a small dot at the projected position plus their name. */
    private static void renderNearDot(GuiGraphicsExtractor ctx, Minecraft client, PresenceCache.Presence p, String name) {
        Camera camera = client.gameRenderer.getMainCamera();
        int screenW = ctx.guiWidth();
        int screenH = ctx.guiHeight();
        double[] s = FriendsModule.project(camera, screenW, screenH, p.x(), p.y() + 2.2, p.z());
        if (s == null) {
            return;
        }
        int x = (int)Math.round(s[0]);
        int y = (int)Math.round(s[1]);
        int r = 4;
        for (int dy = -r; dy <= r; ++dy) {
            int half = r - Math.abs(dy);
            ctx.fill(x - half, y + dy, x + half + 1, y + dy + 1, FRIEND_COLOR);
        }
        ctx.outline(x - r - 1, y - r - 1, 2 * r + 3, 2 * r + 3, 0x80000000);
        Font tr = client.font;
        String label = name == null ? "?" : name;
        int w = tr.width(label);
        ctx.text(tr, (Component)Component.literal((String)label), x - w / 2, y - r - 12, FRIEND_COLOR);
    }

    /** Projects a world point to screen coordinates, or null if it is behind the camera. */
    private static double[] project(Camera camera, int screenW, int screenH, double wx, double wy, double wz) {
        Vec3 cam = camera.position();
        Vec3 to = new Vec3(wx - cam.x, wy - cam.y, wz - cam.z);
        Vec3 forward = Vec3.directionFromRotation((float)camera.xRot(), (float)camera.yaw()).normalize();
        Vec3 right = Vec3.directionFromRotation((float)0.0f, (float)(camera.yaw() + 90.0f)).normalize();
        Vec3 up = right.cross(forward).normalize();
        double depth = to.dot(forward);
        if (depth <= 0.05) {
            return null;
        }
        double xCam = to.dot(right);
        double yCam = to.dot(up);
        double vfov = Math.toRadians(Math.max(12.0, Math.min(110.0, camera.getFov())));
        double aspect = (double)screenW / Math.max(1.0, (double)screenH);
        double halfH = Math.tan(vfov / 2.0) * depth;
        double halfW = halfH * aspect;
        double xNorm = xCam / Math.max(0.001, halfW);
        double yNorm = yCam / Math.max(0.001, halfH);
        double sx = (double)screenW / 2.0 + xNorm * (double)screenW / 2.0;
        double sy = (double)screenH / 2.0 - yNorm * (double)screenH / 2.0;
        return new double[]{sx, sy};
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
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
}
