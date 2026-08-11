package io.github.keoz5.zombiezcompanion.modules.groups;

import io.github.keoz5.zombiezcompanion.ModInfo;
import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.GroupsConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.keoz5.zombiezcompanion.log.Log;
import io.github.keoz5.zombiezcompanion.log.LogCategory;
import io.github.keoz5.zombiezcompanion.modules.friends.FriendsModule;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.net.HttpClients;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Party/group system layered on top of Friends: a small (max {@value #MAX_MEMBERS}) team whose members
 * see each other and can act together. Group state lives on the backend, keyed on account UUIDs.
 */
public final class GroupsModule
implements Module {
    public static final String ID = "groups";
    public static final int MAX_MEMBERS = 4;
    private static final long POLL_MS = 12000L;
    /** Ping share poll cadence — fast, so a shared position lands within a couple seconds. */
    private static final long PING_POLL_MS = 2500L;
    /** Two ping-key presses within this window remove the ping instead of placing one. */
    private static final long PING_DOUBLE_MS = 500L;
    /** Max raycast distance (blocks) for "ping where I'm looking". */
    private static final double PING_REACH = 160.0;
    /** Member marker tint (greenish); the chief is drawn in {@link #CHIEF_COLOR}. */
    public static final int GROUP_COLOR = 0xFF33FF9E;
    public static final int CHIEF_COLOR = 0xFFFFC83D;
    /** Shared-ping marker tint (magenta). */
    public static final int PING_COLOR = 0xFFFF3DAE;
    /** Countdown (ms) before an auto group-dungeon join fires, so the player can cancel (sneak). */
    private static final long DUNGEON_COUNTDOWN_MS = 3000L;
    /** How long a "<chief> lance un Donjon" line stays valid to pair with the following "REJOINDRE" line. */
    private static final long DUNGEON_PAIR_WINDOW_MS = 6000L;
    /** Matches the server broadcast "⚔ <pseudo> lance un Donjon Niv. N !" (accent-insensitive on "Donjon"). */
    private static final Pattern DUNGEON_LAUNCH = Pattern.compile("(\\S+)\\s+lance un [Dd]onjon");

    private ConfigManager configManager;
    private long nextPollMs;
    private long nextPingPollMs;
    private long lastPingKeyMs;
    private boolean commandHookRegistered;
    // Follow-chief: last applied action id, so a broadcast action is replayed at most once.
    private String lastActionId = "";
    // Dungeon auto: the pseudo from the last "lance un Donjon" line, and a pending join countdown.
    private String lastDungeonChief = "";
    private long lastDungeonAt;
    private String pendingJoinCmd;
    private long joinCountdownEndMs;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Amis & Groupe";
    }

    @Override
    public String description() {
        return Component.translatable((String)"zombiezcompanion.module.groups.desc").getString();
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
        return List.of("amis", "friends", "annuaire", "groupe", "group", "team", "équipe", "party", "chef", "membres", "ping", "donjon", "tp");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
        // Follow-chief: when the local player (if chief) sends a "refuge tp <n>", broadcast it for followers.
        if (!this.commandHookRegistered) {
            this.commandHookRegistered = true;
            ClientSendMessageEvents.COMMAND.register(command -> this.onCommandSent(command));
        }
    }

    /** Called when the local player sends a command (without leading slash). */
    private void onCommandSent(String command) {
        if (command == null) {
            return;
        }
        String c = command.trim();
        // Match "refuge tp <arg>" (arg is a refuge number or "w2").
        if (c.startsWith("refuge tp ") && this.isChief()) {
            String arg = c.substring("refuge tp ".length()).trim();
            if (!arg.isEmpty()) {
                this.postAction("refuge", arg);
            }
        }
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new GroupsScreen(parent, this, this.configManager);
    }

    public GroupsConfig config() {
        return this.configManager.get().group;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    public static GroupsModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module m : mm.modules()) {
            if (m instanceof GroupsModule) {
                return (GroupsModule)m;
            }
        }
        return null;
    }

    private static String selfName() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getName().getString() : "?";
    }

    /** True when the roster should keep refreshing so group members can be located. */
    public static boolean wantsPresenceRefresh() {
        ConfigManager cm = ZombieZCompanionClient.configManager();
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        return cm != null && mm != null && mm.isEnabled(ID) && cm.get().group.showGroup && GroupsCache.inGroup();
    }

    // --- Polling ------------------------------------------------------------

    @Override
    public void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null || !ZombieZDetector.isOnZombieZ()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now >= this.nextPollMs) {
            this.refresh();
            this.nextPollMs = now + POLL_MS;
        }
        // Shared pings: poll fast while in a group so a partner's ping shows up near-instantly.
        if (GroupsCache.inGroup() && now >= this.nextPingPollMs) {
            this.refreshPings();
            this.nextPingPollMs = now + PING_POLL_MS;
        }
        // Dungeon-auto countdown: fire the join, or cancel it if the player sneaks.
        if (this.pendingJoinCmd != null) {
            if (client.player != null && client.player.isShiftKeyDown()) {
                this.pendingJoinCmd = null;
            } else if (now >= this.joinCountdownEndMs) {
                String cmd = this.pendingJoinCmd;
                this.pendingJoinCmd = null;
                if (client.getConnection() != null) {
                    client.getConnection().sendCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
                }
            }
        }
    }

    @Override
    public void onLeaveWorld() {
        GroupsCache.clear();
        PingCache.clear();
        this.pendingJoinCmd = null;
    }

    @Override
    public void onDisable() {
        GroupsCache.clear();
        PingCache.clear();
    }

    public void refresh() {
        String self = FriendsModule.selfMcUuid();
        if (self == null) {
            return;
        }
        this.getAsync(ModInfo.API_BASE + "/group?uuid=" + self).thenAccept(resp -> {
            if (resp != null) {
                GroupsCache.update(resp);
            }
        });
    }

    private void refreshSoon() {
        this.nextPollMs = 0L;
        this.refresh();
    }

    // --- Pings --------------------------------------------------------------

    public void refreshPings() {
        String self = FriendsModule.selfMcUuid();
        if (self == null) {
            return;
        }
        this.getAsync(ModInfo.API_BASE + "/group/pings?uuid=" + self).thenAccept(resp -> {
            if (resp != null) {
                PingCache.update(resp);
                this.handleAction(resp);
            }
        });
    }

    /** Parse the chief's latest broadcast action from the poll and replay it (follow-chief). */
    private void handleAction(String resp) {
        try {
            JsonObject obj = JsonParser.parseString((String)resp).getAsJsonObject();
            if (!obj.has("action") || obj.get("action").isJsonNull()) {
                return;
            }
            JsonObject a = obj.getAsJsonObject("action");
            String id = a.has("id") ? a.get("id").getAsString() : "";
            if (id.isEmpty() || id.equals(this.lastActionId)) {
                return;
            }
            // Mark seen up-front so an action is applied at most once, even if conditions fail.
            this.lastActionId = id;
            String by = a.has("by") ? a.get("by").getAsString() : "";
            String type = a.has("type") ? a.get("type").getAsString() : "";
            String arg = a.has("arg") ? a.get("arg").getAsString() : "";
            String self = FriendsModule.selfMcUuid();
            GroupsCache.Group g = GroupsCache.group();
            if (!this.config().followChief || g == null || self == null) {
                return;
            }
            if (self.equals(by) || !by.equals(g.chief())) {
                return; // only replay the chief's own actions, and never our own
            }
            if ("refuge".equals(type) && !arg.isEmpty()) {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    if (mc.getConnection() != null) {
                        mc.getConnection().sendCommand("refuge tp " + arg);
                    }
                });
            }
        }
        catch (Exception exception) {
            // ignore malformed action
        }
    }

    private void postAction(String type, String arg) {
        String self = FriendsModule.selfMcUuid();
        if (self == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"type\":\"%s\",\"arg\":\"%s\"}", esc(self), esc(type), esc(arg));
        this.postPing(ModInfo.API_BASE + "/group/action", body);
    }

    // --- Group dungeon auto -------------------------------------------------

    /**
     * Watches chat for the server's group-dungeon broadcast. The block spans two lines: a launch line
     * "⚔ &lt;pseudo&gt; lance un Donjon Niv. N" and a clickable "[ ▶ REJOINDRE ]" whose {@link ClickEvent}
     * carries the join command. We pair them, verify the launcher is our chief, and (if enabled) start a
     * cancelable 3s countdown before replaying the command. Always logs what it detects (debug capture).
     */
    @Override
    public void onChatMessage(Component message, boolean overlay) {
        if (message == null) {
            return;
        }
        String txt = message.getString();
        if (txt == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Matcher m = DUNGEON_LAUNCH.matcher(txt);
        if (m.find()) {
            this.lastDungeonChief = m.group(1);
            this.lastDungeonAt = now;
            return;
        }
        if (!txt.contains("REJOINDRE")) {
            return;
        }
        String cmd = GroupsModule.findClickCommand(message);
        // Debug capture (debug mode only): log every detected REJOINDRE with the extracted join command.
        if (this.configManager.get().debugMode) {
            Log.info("[ZZC][donjon] REJOINDRE — cmd=" + cmd + " | lanceur=" + this.lastDungeonChief
                    + " | chef=" + this.chiefName() + " | dungeonAuto=" + this.config().dungeonAuto);
        }
        if (cmd == null || !this.config().dungeonAuto) {
            return;
        }
        GroupsCache.Group g = GroupsCache.group();
        String self = FriendsModule.selfMcUuid();
        if (g == null || self == null || self.equals(g.chief())) {
            return; // not in a group, or we are the chief (already in the dungeon we launched)
        }
        String chief = this.chiefName();
        if (chief == null || this.lastDungeonChief == null || this.lastDungeonChief.isEmpty()) {
            return;
        }
        if (now - this.lastDungeonAt > DUNGEON_PAIR_WINDOW_MS || !chief.equalsIgnoreCase(this.lastDungeonChief)) {
            return; // launcher isn't our chief, or the launch line is stale
        }
        this.pendingJoinCmd = cmd;
        this.joinCountdownEndMs = now + DUNGEON_COUNTDOWN_MS;
        Log.debug(LogCategory.EVENT, "[ZZC][donjon] auto-join armé pour le donjon de " + chief);
    }

    /** The chief's pseudo (display name), or null. */
    private String chiefName() {
        GroupsCache.Group g = GroupsCache.group();
        if (g == null) {
            return null;
        }
        for (GroupsCache.Member mm : g.members()) {
            if (mm.uuid().equals(g.chief())) {
                return mm.name();
            }
        }
        return null;
    }

    /** Depth-first search for the first RUN_COMMAND click event in a chat component, or null. */
    private static String findClickCommand(Component c) {
        if (c == null) {
            return null;
        }
        Style s = c.getStyle();
        if (s != null) {
            ClickEvent ce = s.getClickEvent();
            if (ce instanceof ClickEvent.RunCommand rc) {
                return rc.command();
            }
        }
        for (Component sib : c.getSiblings()) {
            String r = GroupsModule.findClickCommand(sib);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /**
     * Ping key handler: single press drops a ping where the player is looking; a second press within
     * {@value #PING_DOUBLE_MS} ms removes it (so a double-tap toggles off). No chat message either way.
     */
    public void onPingKey() {
        if (!GroupsCache.inGroup()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean doubleTap = now - this.lastPingKeyMs <= PING_DOUBLE_MS;
        this.lastPingKeyMs = now;
        if (doubleTap) {
            this.clearPing();
        } else {
            this.placePing();
        }
    }

    private void placePing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        Vec3 eye = mc.player.getEyePosition(1.0f);
        Vec3 look = mc.player.getViewVector(1.0f);
        Vec3 end = eye.add(look.x * PING_REACH, look.y * PING_REACH, look.z * PING_REACH);
        BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        Vec3 point = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        String self = FriendsModule.selfMcUuid();
        String dim = mc.level.dimension().identifier().toString();
        if (self == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"name\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"dim\":\"%s\"}",
                esc(self), esc(selfName()), point.x, point.y, point.z, esc(dim));
        this.postPing(ModInfo.API_BASE + "/group/ping", body);
        if (mc.player != null) {
            mc.player.playSound((net.minecraft.sounds.SoundEvent)net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 0.5f, 1.7f);
        }
    }

    private void clearPing() {
        String self = FriendsModule.selfMcUuid();
        if (self == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\"}", esc(self));
        this.postPing(ModInfo.API_BASE + "/group/ping/clear", body);
    }

    private void postPing(String url, String body) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5L)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpClients.SHARED.sendAsync(req, HttpResponse.BodyHandlers.discarding()).whenComplete((r, t) -> {
                this.nextPingPollMs = 0L;
            });
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    // --- Mutations ----------------------------------------------------------

    public void createGroup() {
        String self = FriendsModule.selfMcUuid();
        if (self == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"name\":\"%s\"}", esc(self), esc(selfName()));
        this.postThenRefresh(ModInfo.API_BASE + "/group/create", body);
    }

    public void invite(String toUuid, String toName) {
        String self = FriendsModule.selfMcUuid();
        if (self == null || toUuid == null || toUuid.equals(self)) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"name\":\"%s\",\"to\":\"%s\",\"toName\":\"%s\"}", esc(self), esc(selfName()), esc(toUuid), esc(toName));
        this.postThenRefresh(ModInfo.API_BASE + "/group/invite", body);
    }

    public void acceptInvite(String gid) {
        String self = FriendsModule.selfMcUuid();
        if (self == null || gid == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"name\":\"%s\",\"gid\":\"%s\"}", esc(self), esc(selfName()), esc(gid));
        this.postThenRefresh(ModInfo.API_BASE + "/group/accept", body);
    }

    public void declineInvite(String gid) {
        String self = FriendsModule.selfMcUuid();
        if (self == null || gid == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"gid\":\"%s\"}", esc(self), esc(gid));
        this.postThenRefresh(ModInfo.API_BASE + "/group/decline", body);
    }

    public void leave() {
        String self = FriendsModule.selfMcUuid();
        if (self == null) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\"}", esc(self));
        this.postThenRefresh(ModInfo.API_BASE + "/group/leave", body);
    }

    public void kick(String target) {
        String self = FriendsModule.selfMcUuid();
        if (self == null || target == null || target.equals(self)) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"target\":\"%s\"}", esc(self), esc(target));
        this.postThenRefresh(ModInfo.API_BASE + "/group/kick", body);
    }

    public void transfer(String target) {
        String self = FriendsModule.selfMcUuid();
        if (self == null || target == null || target.equals(self)) {
            return;
        }
        String body = String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"target\":\"%s\"}", esc(self), esc(target));
        this.postThenRefresh(ModInfo.API_BASE + "/group/transfer", body);
    }

    /** True when the local player is the chief of the current group. */
    public boolean isChief() {
        GroupsCache.Group g = GroupsCache.group();
        String self = FriendsModule.selfMcUuid();
        return g != null && self != null && self.equals(g.chief());
    }

    // --- Networking helpers -------------------------------------------------

    private java.util.concurrent.CompletableFuture<String> getAsync(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5L)).GET().build();
            return HttpClients.SHARED.sendAsync(req, HttpResponse.BodyHandlers.ofString()).handle((resp, err) -> err == null && resp != null && resp.statusCode() == 200 ? resp.body() : null);
        }
        catch (Exception e) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
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

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
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
        // Dungeon auto-join countdown (shown even if group markers are hidden).
        this.renderJoinCountdown(ctx, client);
        GroupsConfig cfg = this.config();
        if (!cfg.showGroup) {
            return;
        }
        GroupsCache.Group g = GroupsCache.group();
        if (g == null) {
            return;
        }
        String self = FriendsModule.selfMcUuid();
        String playerDim = client.level != null ? client.level.dimension().identifier().toString() : "";
        int near = Math.max(0, cfg.nearHudRange);
        String style = cfg.markerStyle == null ? "auto" : cfg.markerStyle;
        for (GroupsCache.Member m : g.members()) {
            if (m.uuid().equals(self)) continue;
            int color = m.uuid().equals(g.chief()) ? CHIEF_COLOR : GROUP_COLOR;
            FriendsModule.renderPlayerMarker(ctx, client, tickDelta, playerDim, m.uuid(), m.name(), color, style, near);
        }
        // Shared pings: always a labeled beacon (a navigation target), regardless of marker style.
        for (PingCache.Ping p : PingCache.pings()) {
            if (!playerDim.isEmpty() && !p.dim().isEmpty() && !playerDim.equals(p.dim())) continue;
            String label = "⚑ " + (p.name() == null ? "?" : p.name());
            WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, p.x(), p.y(), p.z(), label, PING_COLOR);
        }
    }

    /** Centered "group dungeon in Ns — sneak to cancel" overlay while a join is pending. */
    private void renderJoinCountdown(GuiGraphicsExtractor ctx, Minecraft client) {
        if (this.pendingJoinCmd == null) {
            return;
        }
        long remain = this.joinCountdownEndMs - System.currentTimeMillis();
        int secs = (int)Math.max(1L, Math.ceil((double)remain / 1000.0));
        Font f = client.font;
        Component l1 = Component.translatable((String)"zombiezcompanion.groups.dungeon.countdown", (Object[])new Object[]{secs});
        Component l2 = Component.translatable((String)"zombiezcompanion.groups.dungeon.cancel");
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() / 2 - 34;
        ctx.text(f, l1, cx - f.width((net.minecraft.network.chat.FormattedText)l1) / 2, cy, CHIEF_COLOR);
        ctx.text(f, l2, cx - f.width((net.minecraft.network.chat.FormattedText)l2) / 2, cy + 12, 0xFFCFD6DD);
    }
}
