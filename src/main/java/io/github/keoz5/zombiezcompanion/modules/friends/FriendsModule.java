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
import io.github.keoz5.zombiezcompanion.ui.Colors;
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
import net.minecraft.client.player.AbstractClientPlayer;
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
    /** Default marker tint; per-friend overrides live in {@link Colors} under id {@code "friend:"+uuid}. */
    public static final int FRIEND_COLOR = 0xFF33C1FF;

    /** Per-friend marker color (the player's override, or the default tint). */
    public static int colorOf(String uuid) {
        return Colors.get("friend:" + uuid, FRIEND_COLOR);
    }

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
        String style = cfg.markerStyle == null ? "auto" : cfg.markerStyle;
        for (FriendsCache.Friend f : FriendsCache.friends()) {
            double tx, ty, tz;
            if (hidden.contains(f.uuid())) continue;
            // Prefer the friend's real loaded entity (instant, client-side, no server round-trip); fall back to
            // the presence snapshot only when they are out of render distance / not loaded.
            AbstractClientPlayer ent = FriendsModule.loadedPlayer(client, f.uuid());
            boolean loaded = ent != null;
            if (loaded) {
                tx = ent.xo + (ent.getX() - ent.xo) * (double)tickDelta;
                ty = ent.yo + (ent.getY() - ent.yo) * (double)tickDelta;
                tz = ent.zo + (ent.getZ() - ent.zo) * (double)tickDelta;
            } else {
                PresenceCache.Presence p = FriendsModule.onlinePresence(f.uuid());
                if (p == null) continue;
                if (!playerDim.isEmpty() && !p.dim().isEmpty() && !playerDim.equals(p.dim())) continue;
                tx = p.x();
                ty = p.y();
                tz = p.z();
            }
            double dist = Math.sqrt(client.player.distanceToSqr(tx, ty, tz));
            int color = FriendsModule.colorOf(f.uuid());
            int solid = 0xFF000000 | (color & 0xFFFFFF);
            boolean farStyle = "waypoint".equals(style) || dist >= (double)near;
            if (farStyle) {
                WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, tx, ty, tz, f.name(), solid);
            } else if ("box".equals(style) && loaded) {
                FriendsModule.renderEntityBox(ctx, client, tx, ty, tz, ent.getBbHeight(), dist, f.name(), color);
            } else {
                FriendsModule.renderCompactMarker(ctx, client, tx, ty, tz, dist, f.name(), solid);
            }
        }
    }

    /** The friend's real, currently-loaded client entity (by account UUID), or null if out of render distance. */
    private static AbstractClientPlayer loadedPlayer(Minecraft client, String mcuuid) {
        if (client.level == null || mcuuid == null) {
            return null;
        }
        for (AbstractClientPlayer pl : client.level.players()) {
            if (pl == client.player || pl.isRemoved()) continue;
            if (mcuuid.equals(pl.getUUID().toString())) {
                return pl;
            }
        }
        return null;
    }

    /**
     * Compact, always-visible marker for a nearby friend: edge-clamped like the waypoint (never disappears
     * when the friend is off-screen or behind the camera), drawn as a small diamond + "name [dist]" — no big box.
     */
    private static void renderCompactMarker(GuiGraphicsExtractor ctx, Minecraft client, double wx, double wy, double wz, double dist, String name, int color) {
        if (client.gameRenderer == null) {
            return;
        }
        Camera camera = client.gameRenderer.getMainCamera();
        int screenW = ctx.guiWidth();
        int screenH = ctx.guiHeight();
        int[] s = FriendsModule.projectClamped(camera, screenW, screenH, wx, wy + 1.45, wz);
        int x = s[0];
        int y = s[1];
        int r = 5;
        FriendsModule.drawDiamond(ctx, x, y, r + 1, 0xC0000000);
        FriendsModule.drawDiamond(ctx, x, y, r, color);
        ctx.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFFFFF);
        Font tr = client.font;
        String label = (name == null ? "?" : name) + " [" + (int)Math.round(dist) + "m]";
        int w = tr.width(label);
        int tx = (int)Math.max(2, Math.min(screenW - w - 2, x - w / 2));
        ctx.text(tr, (Component)Component.literal((String)label), tx, y - r - 12, color);
    }

    /**
     * Framed marker ("box" style) for a nearby friend: a screen-space rectangle over the player's hitbox,
     * edge-clamped so it never disappears. Height is measured from the projected hitbox when in view.
     */
    private static void renderEntityBox(GuiGraphicsExtractor ctx, Minecraft client, double footX, double footY, double footZ, double height, double dist, String name, int color) {
        if (client.gameRenderer == null) {
            return;
        }
        Camera camera = client.gameRenderer.getMainCamera();
        int screenW = ctx.guiWidth();
        int screenH = ctx.guiHeight();
        int[] c = FriendsModule.projectClamped(camera, screenW, screenH, footX, footY + height / 2.0, footZ);
        double[] top = FriendsModule.projectRaw(camera, screenW, screenH, footX, footY + height + 0.15, footZ);
        double[] bot = FriendsModule.projectRaw(camera, screenW, screenH, footX, footY, footZ);
        double h = top != null && bot != null ? Math.max(12.0, Math.abs(bot[1] - top[1])) : 22.0;
        double w = Math.max(10.0, h * 0.6);
        int x1 = (int)Math.round((double)c[0] - w / 2.0);
        int x2 = (int)Math.round((double)c[0] + w / 2.0);
        int y1 = (int)Math.round((double)c[1] - h / 2.0);
        int y2 = (int)Math.round((double)c[1] + h / 2.0);
        int frameCol = 0xFF000000 | (color & 0xFFFFFF);
        int fillCol = 0x30000000 | (color & 0xFFFFFF);
        ctx.fill(x1, y1, x2, y2, fillCol);
        ctx.outline(x1 - 1, y1 - 1, x2 - x1 + 2, y2 - y1 + 2, 0x80000000);
        ctx.outline(x1, y1, x2 - x1, y2 - y1, frameCol);
        Font tr = client.font;
        String label = (name == null ? "?" : name) + " [" + (int)Math.round(dist) + "m]";
        int lw = tr.width(label);
        int tx = (int)Math.max(2, Math.min(screenW - lw - 2, (double)c[0] - (double)lw / 2.0));
        ctx.text(tr, (Component)Component.literal((String)label), tx, y1 - 11, frameCol);
    }

    private static void drawDiamond(GuiGraphicsExtractor ctx, int x, int y, int radius, int color) {
        for (int dy = -radius; dy <= radius; ++dy) {
            int half = radius - Math.abs(dy);
            ctx.fill(x - half, y + dy, x + half + 1, y + dy + 1, color);
        }
    }

    /** Projects a world point to an edge-clamped screen position that never disappears (even behind the camera). */
    private static int[] projectClamped(Camera camera, int screenW, int screenH, double wx, double wy, double wz) {
        Vec3 cam = camera.position();
        Vec3 to = new Vec3(wx - cam.x, wy - cam.y, wz - cam.z);
        Vec3 forward = Vec3.directionFromRotation((float)camera.xRot(), (float)camera.yaw()).normalize();
        Vec3 right = Vec3.directionFromRotation(0.0f, (float)(camera.yaw() + 90.0f)).normalize();
        Vec3 up = right.cross(forward).normalize();
        double depth = to.dot(forward);
        double xCam = to.dot(right);
        double yCam = to.dot(up);
        double vfov = Math.toRadians(Math.max(12.0, Math.min(110.0, camera.getFov())));
        double aspect = (double)screenW / Math.max(1.0, (double)screenH);
        double xNorm;
        double yNorm;
        if (depth > 0.05) {
            double halfH = Math.tan(vfov / 2.0) * depth;
            double halfW = halfH * aspect;
            xNorm = xCam / Math.max(0.001, halfW);
            yNorm = yCam / Math.max(0.001, halfH);
        } else {
            double absX = Math.abs(xCam);
            xNorm = absX < 0.001 ? 0.0 : Math.signum(xCam) * Math.min(1.0, 0.4 + absX / Math.max(1.0, absX + 8.0));
            yNorm = -1.0;
        }
        double edgeScale = Math.max(1.0, Math.max(Math.abs(xNorm), Math.abs(yNorm)));
        xNorm /= edgeScale;
        yNorm /= edgeScale;
        xNorm = Math.max(-0.96, Math.min(0.96, xNorm));
        yNorm = Math.max(-0.9, Math.min(0.9, yNorm));
        int margin = 14;
        int x = (int)Math.max(margin, Math.min(screenW - margin, (int)Math.round((double)screenW / 2.0 + xNorm * (double)screenW / 2.0)));
        int y = (int)Math.max(margin, Math.min(screenH - margin, (int)Math.round((double)screenH / 2.0 - yNorm * (double)screenH / 2.0)));
        return new int[]{x, y};
    }

    /** Raw projection to screen coordinates, or null when the point is behind the camera. Used to size the box. */
    private static double[] projectRaw(Camera camera, int screenW, int screenH, double wx, double wy, double wz) {
        Vec3 cam = camera.position();
        Vec3 to = new Vec3(wx - cam.x, wy - cam.y, wz - cam.z);
        Vec3 forward = Vec3.directionFromRotation((float)camera.xRot(), (float)camera.yaw()).normalize();
        Vec3 right = Vec3.directionFromRotation(0.0f, (float)(camera.yaw() + 90.0f)).normalize();
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
        double sx = (double)screenW / 2.0 + xCam / Math.max(0.001, halfW) * (double)screenW / 2.0;
        double sy = (double)screenH / 2.0 - yCam / Math.max(0.001, halfH) * (double)screenH / 2.0;
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
