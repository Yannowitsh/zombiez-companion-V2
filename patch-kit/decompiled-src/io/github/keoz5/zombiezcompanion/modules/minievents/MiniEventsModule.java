/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
 *  net.minecraft.class_1297
 *  net.minecraft.class_1304
 *  net.minecraft.class_1309
 *  net.minecraft.class_1642
 *  net.minecraft.class_1657
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1921
 *  net.minecraft.class_2338
 *  net.minecraft.class_238
 *  net.minecraft.class_2382
 *  net.minecraft.class_243
 *  net.minecraft.class_2561
 *  net.minecraft.class_2586
 *  net.minecraft.class_2611
 *  net.minecraft.class_2818
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_337
 *  net.minecraft.class_345
 *  net.minecraft.class_4184
 *  net.minecraft.class_437
 *  net.minecraft.class_4587
 *  net.minecraft.class_4587$class_4665
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_4604
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 */
package io.github.keoz5.zombiezcompanion.modules.minievents;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.config.MiniEventsConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.hud.HudAnchor;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.mixin.BossBarHudAccessor;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapData;
import io.github.keoz5.zombiezcompanion.modules.minievents.MiniEventsOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.minievents.SpawnSync;
import io.github.keoz5.zombiezcompanion.net.HttpClients;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1642;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1921;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_2382;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2586;
import net.minecraft.class_2611;
import net.minecraft.class_2818;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_337;
import net.minecraft.class_345;
import net.minecraft.class_4184;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4604;
import net.minecraft.class_5250;
import net.minecraft.class_5348;

public final class MiniEventsModule
implements Module {
    public static final String ID = "mini_events";
    private static final long COLIS_TTL_MS = 300000L;
    private static final long COLIS_SEARCH_MS = 60000L;
    private static final long TOAST_MS = 3500L;
    private static final long TOAST_FADE_MS = 500L;
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final double MAX_DETECTION_RANGE = 100.0;
    private static final double COLIS_MAX_RANGE = 96.0;
    private static final String SPAWN_ENDPOINT = "https://zombiez-companion-api.keoz5.workers.dev";
    private static final long SPAWN_FETCH_INTERVAL_MS = 60000L;
    private long nextSpawnFetchMs;
    private final LinkedHashMap<UUID, ActiveEvent> entityEvents = new LinkedHashMap();
    private final LinkedHashMap<class_2338, ActiveEvent> colisEvents = new LinkedHashMap();
    private final Set<class_2338> knownEnderChests = new HashSet<class_2338>();
    private final ArrayDeque<Toast> toasts = new ArrayDeque();
    private int scanTick;
    private long colisSearchUntil;
    private long marchandHeaderUntil;
    private String marchandWaypointId;
    private long marchandExpiresAt;
    private long assautHeaderUntil;
    private String assautWaypointId;
    private long bossHeaderUntil;
    private String bossWaypointId;
    private String failleWaypointId;
    private long failleBossbarSeenAtMs;
    private ConfigManager configManager;
    private static final Pattern MARCHAND_COORDS = Pattern.compile("(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)");
    private static final Pattern MARCHAND_DURATION = Pattern.compile("(\\d+)\\s*:\\s*(\\d{1,2})");
    private static final long MARCHAND_HEADER_WINDOW_MS = 10000L;
    private static final long MARCHAND_DEFAULT_TTL_MS = 300000L;
    private static final Pattern BOSS_IDENTITY = Pattern.compile("(?i)identit\\S*\\s*:\\s*([^\\n|]+)");
    private static final long MARCHAND_SPAWN_DEDUP_MS = 300000L;
    private static final long DEFAULT_MIN_INTERVAL_MS = 1200000L;
    private static final long DEFAULT_MAX_INTERVAL_MS = 2400000L;
    private static final int MAX_SPAWN_HISTORY = 30;
    private static final long FAILLE_BOSSBAR_GRACE_MS = 3000L;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "\u00c9v\u00e9nements";
    }

    @Override
    public String description() {
        return class_2561.method_43471((String)"zombiezcompanion.module.mini_events.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.EVENTS;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("event", "\u00e9v\u00e9nement", "fuyeur", "colis", "faille", "pinata", "bombe", "jackpot", "marchand", "port\u00e9e", "d\u00e9tection");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
        WorldRenderEvents.LAST.register(this::renderBeacons);
    }

    @Override
    public class_437 createOptionsScreen(class_437 parent) {
        return new MiniEventsOptionsScreen(parent, this, this.configManager);
    }

    public static MiniEventsModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module m : mm.modules()) {
            if (!(m instanceof MiniEventsModule)) continue;
            MiniEventsModule e = (MiniEventsModule)m;
            return e;
        }
        return null;
    }

    public double[] activeEventTarget() {
        String id;
        if (this.configManager == null) {
            return null;
        }
        String string = this.bossWaypointId != null ? this.bossWaypointId : (id = this.marchandWaypointId != null ? this.marchandWaypointId : this.failleWaypointId);
        if (id == null) {
            return null;
        }
        for (MapConfig.Waypoint w : this.configManager.get().map.waypoints) {
            if (!id.equals(w.id)) continue;
            return new double[]{w.x, w.z};
        }
        return null;
    }

    public MiniEventsConfig config() {
        return this.configManager.get().miniEvents;
    }

    @Override
    public void onDisable() {
        if (this.marchandWaypointId != null) {
            this.removeMarchandWaypoint(this.configManager.get().map);
            this.configManager.save();
        }
        if (this.assautWaypointId != null) {
            this.removeAssautWaypoint(this.configManager.get().map);
            this.configManager.save();
        }
        if (this.bossWaypointId != null) {
            this.removeWorldBossWaypoint(this.configManager.get().map);
            this.configManager.save();
        }
        if (this.failleWaypointId != null) {
            this.removeFailleWaypoint(this.configManager.get().map);
            this.configManager.save();
        }
        this.reset();
    }

    @Override
    public void onLeaveWorld() {
        this.reset();
    }

    @Override
    public void onClientTick(class_310 client) {
        if (client.field_1724 == null || client.field_1687 == null || !ZombieZDetector.isOnZombieZ()) {
            this.reset();
            return;
        }
        if (this.config().worldBoss) {
            this.scanWorldBossBossbar(client);
        }
        if (this.config().faille) {
            this.scanFailleBossbar(client);
        }
        long nowMs = System.currentTimeMillis();
        if ((this.config().marchandTimer || this.config().worldBossTimer) && nowMs >= this.nextSpawnFetchMs) {
            this.fetchSpawns();
            this.nextSpawnFetchMs = nowMs + 60000L;
        }
        if (ZombieZMapData.isInSpawn(client.field_1724.method_23317(), client.field_1724.method_23321())) {
            this.entityEvents.clear();
            this.colisEvents.clear();
            this.scanTick = 0;
            long nowSpawn = System.currentTimeMillis();
            if (this.marchandWaypointId != null && this.marchandExpiresAt > 0L && nowSpawn > this.marchandExpiresAt) {
                this.removeMarchandWaypoint(this.configManager.get().map);
                this.configManager.save();
            }
            return;
        }
        MiniEventsConfig cfg = this.config();
        double range = Math.max(32.0, Math.min(100.0, (double)cfg.detectionRange));
        long now = System.currentTimeMillis();
        if (++this.scanTick >= 5) {
            this.scanTick = 0;
            List mobs = client.field_1687.method_8390(class_1309.class, client.field_1724.method_5829().method_1014(range), e -> !e.method_31481() && !(e instanceof class_1657));
            HashSet<UUID> present = new HashSet<UUID>();
            for (class_1309 mob : mobs) {
                UUID uuid2 = mob.method_5667();
                String name = MiniEventsModule.entityName(mob);
                MiniEventType type = MiniEventsModule.identifyType(name);
                if (type == null) {
                    type = MiniEventsModule.identifyByEquipment(mob);
                }
                if (type == null || !MiniEventsModule.isEnabled(cfg, type)) continue;
                present.add(uuid2);
                String label = name == null || name.isBlank() ? type.label : name;
                boolean fresh = !this.entityEvents.containsKey(uuid2);
                this.entityEvents.put(uuid2, new ActiveEvent(type, label, mob.method_23317(), mob.method_23318(), mob.method_23321(), 0L));
                if (!fresh) continue;
                this.pushToast(type);
            }
            this.entityEvents.keySet().removeIf(uuid -> !present.contains(uuid));
            if (cfg.colis) {
                this.scanForColis(client, (int)Math.min(range, 96.0), this.colisSearchUntil > now);
            }
        }
        this.colisEvents.entrySet().removeIf(e -> {
            if (now > ((ActiveEvent)e.getValue()).expiresAt) {
                return true;
            }
            class_2586 be = client.field_1687.method_8321((class_2338)e.getKey());
            return !(be instanceof class_2611);
        });
        if (this.marchandWaypointId != null && this.marchandExpiresAt > 0L && now > this.marchandExpiresAt) {
            this.removeMarchandWaypoint(this.configManager.get().map);
            this.configManager.save();
        }
        if (this.marchandWaypointId != null && !cfg.marchand) {
            this.removeMarchandWaypoint(this.configManager.get().map);
            this.configManager.save();
        }
        if (this.assautWaypointId != null && !cfg.assaut) {
            this.removeAssautWaypoint(this.configManager.get().map);
            this.configManager.save();
        }
        if (this.bossWaypointId != null && !cfg.worldBoss) {
            this.removeWorldBossWaypoint(this.configManager.get().map);
            this.configManager.save();
        }
        if (this.failleWaypointId != null && !cfg.faille) {
            this.removeFailleWaypoint(this.configManager.get().map);
            this.configManager.save();
        }
    }

    @Override
    public void onChatMessage(class_2561 message, boolean overlay) {
        boolean inSpawn;
        if (message == null) {
            return;
        }
        String txt = message.getString();
        if (txt == null) {
            return;
        }
        String ascii = MiniEventsModule.stripDiacritics(txt).toLowerCase(Locale.ROOT);
        MiniEventsConfig cfg = this.config();
        if (cfg.marchand) {
            this.handleMarchandLine(ascii);
        }
        if (cfg.assaut) {
            this.handleAssautLine(ascii);
        }
        if (cfg.worldBoss) {
            this.handleWorldBossLine(txt, ascii);
        }
        class_310 mc = class_310.method_1551();
        boolean bl = inSpawn = mc.field_1724 != null && ZombieZMapData.isInSpawn(mc.field_1724.method_23317(), mc.field_1724.method_23321());
        if (inSpawn) {
            return;
        }
        if (ascii.contains("colis express") || ascii.contains("colis pret")) {
            this.colisSearchUntil = System.currentTimeMillis() + 60000L;
        }
    }

    private void handleMarchandLine(String ascii) {
        Matcher m;
        long now = System.currentTimeMillis();
        if (ascii.contains("stock epuise") || ascii.contains("reparti en quete")) {
            if (this.marchandWaypointId != null) {
                this.removeMarchandWaypoint(this.configManager.get().map);
            }
            this.marchandHeaderUntil = 0L;
            return;
        }
        if (ascii.contains("marchand ambulant")) {
            this.marchandHeaderUntil = now + 10000L;
        }
        if (this.marchandHeaderUntil < now) {
            return;
        }
        if (ascii.contains("coordonn") && (m = MARCHAND_COORDS.matcher(ascii)).find()) {
            try {
                int x = Integer.parseInt(m.group(1));
                int y = Integer.parseInt(m.group(2));
                int z = Integer.parseInt(m.group(3));
                this.createMarchandWaypoint(x, y, z, now);
            }
            catch (NumberFormatException x) {
                // empty catch block
            }
        }
        if (this.marchandWaypointId != null && ascii.contains("disparait") && (m = MARCHAND_DURATION.matcher(ascii)).find()) {
            try {
                int min = Integer.parseInt(m.group(1));
                int sec = Integer.parseInt(m.group(2));
                this.marchandExpiresAt = now + ((long)min * 60L + (long)sec) * 1000L;
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
    }

    private void createMarchandWaypoint(int x, int y, int z, long now) {
        this.recordSpawn(false, now);
        MapConfig map = this.configManager.get().map;
        this.removeMarchandWaypoint(map);
        MapConfig.Waypoint wp = new MapConfig.Waypoint();
        wp.id = "marchand-" + now;
        String base = class_2561.method_43471((String)"zombiezcompanion.mini_events.marchand.label").getString();
        ZombieZMapData.Refuge near = ZombieZMapData.nearestRefuge(x, z);
        wp.label = near != null ? class_2561.method_43469((String)"zombiezcompanion.mini_events.marchand.label_tp", (Object[])new Object[]{base, near.name()}).getString() : base;
        wp.x = (double)x + 0.5;
        wp.y = y;
        wp.z = (double)z + 0.5;
        wp.colorRgb = MiniEventType.MARCHAND.colorRgb;
        wp.createdAt = now;
        wp.visible = true;
        map.waypoints.add(wp);
        this.marchandWaypointId = wp.id;
        this.marchandExpiresAt = now + 300000L;
        this.marchandHeaderUntil = 0L;
        this.configManager.save();
        this.pushToast(MiniEventType.MARCHAND);
    }

    private void removeMarchandWaypoint(MapConfig map) {
        if (this.marchandWaypointId == null) {
            return;
        }
        map.waypoints.removeIf(w -> this.marchandWaypointId.equals(w.id));
        if (map.guideTarget != null && this.marchandWaypointId.equals(map.guideTarget.label)) {
            map.guideTarget = null;
        }
        this.marchandWaypointId = null;
        this.marchandExpiresAt = 0L;
    }

    private void recordSpawn(boolean boss, long now) {
        List<Long> list;
        MiniEventsConfig cfg = this.config();
        if (cfg.marchandSpawns == null) {
            cfg.marchandSpawns = new ArrayList<Long>();
        }
        if (cfg.worldBossSpawns == null) {
            cfg.worldBossSpawns = new ArrayList<Long>();
        }
        List<Long> list2 = list = boss ? cfg.worldBossSpawns : cfg.marchandSpawns;
        if (!list.isEmpty() && now - list.get(list.size() - 1) < 300000L) {
            return;
        }
        list.add(now);
        while (list.size() > 30) {
            list.remove(0);
        }
        this.configManager.save();
        SpawnSync.addLocal(boss, now);
        this.postSpawn(boss, now);
    }

    private void fetchSpawns() {
        this.getAsync("https://zombiez-companion-api.keoz5.workers.dev/spawns").thenAccept(SpawnSync::update);
    }

    private void postSpawn(boolean boss, long now) {
        String body = "{\"type\":\"" + (boss ? "world_boss" : "marchand") + "\",\"at\":" + now + "}";
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://zombiez-companion-api.keoz5.workers.dev/spawns")).timeout(Duration.ofSeconds(5L)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
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

    private List<Long> effectiveSpawns(boolean boss) {
        List<Long> shared;
        List<Long> list = shared = boss ? SpawnSync.worldBoss() : SpawnSync.marchand();
        if (shared != null && !shared.isEmpty()) {
            return shared;
        }
        MiniEventsConfig cfg = this.config();
        List<Long> local = boss ? cfg.worldBossSpawns : cfg.marchandSpawns;
        return local != null ? local : List.of();
    }

    private void renderSpawnTimer(class_332 ctx, class_310 client, boolean boss, String elementId, String labelKey, double defaultFy) {
        Object nextStr;
        String elapsedStr;
        List<Long> spawns = this.effectiveSpawns(boss);
        long now = System.currentTimeMillis();
        if (spawns == null || spawns.isEmpty()) {
            elapsedStr = "\u2014";
            nextStr = "\u2014";
        } else {
            long elapsed;
            long last = spawns.get(spawns.size() - 1);
            elapsedStr = MiniEventsModule.fmtDuration(now - last);
            long minIv = 1200000L;
            long maxIv = 2400000L;
            if (spawns.size() >= 3) {
                long lo = Long.MAX_VALUE;
                long hi = Long.MIN_VALUE;
                for (int i = 1; i < spawns.size(); ++i) {
                    long d = spawns.get(i) - spawns.get(i - 1);
                    if (d < lo) {
                        lo = d;
                    }
                    if (d <= hi) continue;
                    hi = d;
                }
                minIv = lo;
                maxIv = hi;
            }
            int pct = (elapsed = now - last) <= minIv ? 0 : (elapsed >= maxIv ? 100 : (int)Math.round(100.0 * (double)(elapsed - minIv) / (double)Math.max(1L, maxIv - minIv)));
            nextStr = pct + "% (" + MiniEventsModule.fmtDuration(minIv) + "-" + MiniEventsModule.fmtDuration(maxIv) + ")";
        }
        class_5250 line = class_2561.method_43469((String)labelKey, (Object[])new Object[]{elapsedStr, nextStr});
        class_327 tr = client.field_1772;
        int baseW = tr.method_27525((class_5348)line) + 12;
        int baseH = 16;
        int screenW = ctx.method_51421();
        int screenH = ctx.method_51443();
        HudConfig hud = this.configManager.get().hud;
        double scale = HudAnchor.scale(hud, elementId);
        int sw = (int)Math.round((double)baseW * scale);
        int sh = (int)Math.round((double)baseH * scale);
        int x = HudAnchor.resolveX(hud, elementId, screenW, sw, 0.0);
        int y = HudAnchor.resolveY(hud, elementId, screenH, sh, defaultFy);
        HudElements.report(elementId, x, y, sw, sh);
        int accent = (boss ? MiniEventType.WORLD_BOSS.colorRgb : MiniEventType.MARCHAND.colorRgb) | 0xFF000000;
        ctx.method_51448().method_22903();
        ctx.method_51448().method_46416((float)x, (float)y, 0.0f);
        if (scale != 1.0) {
            ctx.method_51448().method_22905((float)scale, (float)scale, 1.0f);
        }
        ctx.method_25294(0, 0, baseW, baseH, -1442840576);
        ctx.method_25294(0, 0, baseW, 1, accent);
        ctx.method_27535(tr, (class_2561)line, 6, 4, -1);
        ctx.method_51448().method_22909();
    }

    private static String fmtDuration(long ms) {
        long totalMin = Math.max(0L, ms) / 60000L;
        if (totalMin < 60L) {
            return totalMin + "m";
        }
        long h = totalMin / 60L;
        long m = totalMin % 60L;
        return h + "h" + String.valueOf(m < 10L ? "0" + m : Long.valueOf(m));
    }

    private void handleAssautLine(String ascii) {
        Matcher m;
        long now = System.currentTimeMillis();
        if (ascii.contains("assaut du marche des sables")) {
            this.assautHeaderUntil = now + 10000L;
        }
        if (this.assautHeaderUntil < now) {
            return;
        }
        if (ascii.contains("coordonn") && (m = MARCHAND_COORDS.matcher(ascii)).find()) {
            try {
                int x = Integer.parseInt(m.group(1));
                int y = Integer.parseInt(m.group(2));
                int z = Integer.parseInt(m.group(3));
                this.createAssautWaypoint(x, y, z, now);
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
    }

    private void createAssautWaypoint(int x, int y, int z, long now) {
        MapConfig map = this.configManager.get().map;
        this.removeAssautWaypoint(map);
        MapConfig.Waypoint wp = new MapConfig.Waypoint();
        wp.id = "assaut-" + now;
        String base = class_2561.method_43471((String)"zombiezcompanion.mini_events.assaut.label").getString();
        ZombieZMapData.Refuge near = ZombieZMapData.nearestRefuge(x, z);
        wp.label = near != null ? class_2561.method_43469((String)"zombiezcompanion.mini_events.marchand.label_tp", (Object[])new Object[]{base, near.name()}).getString() : base;
        wp.x = (double)x + 0.5;
        wp.y = y;
        wp.z = (double)z + 0.5;
        wp.colorRgb = MiniEventType.ASSAUT.colorRgb;
        wp.createdAt = now;
        wp.visible = true;
        map.waypoints.add(wp);
        this.assautWaypointId = wp.id;
        this.assautHeaderUntil = 0L;
        this.configManager.save();
        this.pushToast(MiniEventType.ASSAUT);
    }

    private void removeAssautWaypoint(MapConfig map) {
        if (this.assautWaypointId == null) {
            return;
        }
        map.waypoints.removeIf(w -> this.assautWaypointId.equals(w.id));
        if (map.guideTarget != null && this.assautWaypointId.equals(map.guideTarget.label)) {
            map.guideTarget = null;
        }
        this.assautWaypointId = null;
    }

    private void handleWorldBossLine(String raw, String ascii) {
        Matcher m;
        long now = System.currentTimeMillis();
        if (ascii.contains("boss vaincu")) {
            if (this.bossWaypointId != null) {
                this.removeWorldBossWaypoint(this.configManager.get().map);
            }
            this.bossHeaderUntil = 0L;
            return;
        }
        if (ascii.contains("world boss detecte")) {
            this.bossHeaderUntil = now + 10000L;
        }
        if (this.bossHeaderUntil < now) {
            return;
        }
        if (ascii.contains("coordonn") && (m = MARCHAND_COORDS.matcher(ascii)).find()) {
            try {
                int x = Integer.parseInt(m.group(1));
                int y = Integer.parseInt(m.group(2));
                int z = Integer.parseInt(m.group(3));
                this.createWorldBossWaypoint(x, y, z, MiniEventsModule.extractBossName(raw), now);
                this.recordSpawn(true, now);
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
    }

    private static String extractBossName(String raw) {
        if (raw == null) {
            return null;
        }
        String clean = raw.replaceAll("\u00a7.", "");
        Matcher m = BOSS_IDENTITY.matcher(clean);
        if (m.find()) {
            String n = m.group(1).trim();
            return n.isEmpty() ? null : n;
        }
        return null;
    }

    private void createWorldBossWaypoint(int x, int y, int z, String name, long now) {
        MapConfig map = this.configManager.get().map;
        boolean wasActive = this.bossWaypointId != null;
        this.removeWorldBossWaypoint(map);
        MapConfig.Waypoint wp = new MapConfig.Waypoint();
        wp.id = "worldboss-" + now;
        String base = class_2561.method_43471((String)"zombiezcompanion.mini_events.world_boss.label").getString();
        String title = name != null && !name.isBlank() ? base + " : " + name : base;
        ZombieZMapData.Refuge near = ZombieZMapData.nearestRefuge(x, z);
        wp.label = near != null ? class_2561.method_43469((String)"zombiezcompanion.mini_events.marchand.label_tp", (Object[])new Object[]{title, near.name()}).getString() : title;
        wp.x = (double)x + 0.5;
        wp.y = y;
        wp.z = (double)z + 0.5;
        wp.colorRgb = MiniEventType.WORLD_BOSS.colorRgb;
        wp.createdAt = now;
        wp.visible = true;
        map.waypoints.add(wp);
        this.bossWaypointId = wp.id;
        this.bossHeaderUntil = 0L;
        this.configManager.save();
        if (!wasActive) {
            this.pushToast(MiniEventType.WORLD_BOSS);
        }
    }

    private void scanWorldBossBossbar(class_310 client) {
        if (this.bossWaypointId != null) {
            return;
        }
        class_337 hud = client.field_1705.method_1740();
        if (hud == null) {
            return;
        }
        Map<UUID, class_345> bars = ((BossBarHudAccessor)hud).getBossBars();
        if (bars == null || bars.isEmpty()) {
            return;
        }
        for (class_345 bar : bars.values()) {
            Matcher m;
            String ascii;
            if (bar.method_5414() == null || !(ascii = MiniEventsModule.stripDiacritics(bar.method_5414().getString()).toLowerCase(Locale.ROOT)).contains("world boss") || !(m = MARCHAND_COORDS.matcher(ascii)).find()) continue;
            try {
                int x = Integer.parseInt(m.group(1));
                int y = Integer.parseInt(m.group(2));
                int z = Integer.parseInt(m.group(3));
                long now = System.currentTimeMillis();
                this.createWorldBossWaypoint(x, y, z, null, now);
                this.recordSpawn(true, now);
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
            return;
        }
    }

    private void removeWorldBossWaypoint(MapConfig map) {
        if (this.bossWaypointId == null) {
            return;
        }
        map.waypoints.removeIf(w -> this.bossWaypointId.equals(w.id));
        if (map.guideTarget != null && this.bossWaypointId.equals(map.guideTarget.label)) {
            map.guideTarget = null;
        }
        this.bossWaypointId = null;
    }

    private void scanFailleBossbar(class_310 client) {
        class_337 hud = client.field_1705.method_1740();
        if (hud == null) {
            return;
        }
        Map<UUID, class_345> bars = ((BossBarHudAccessor)hud).getBossBars();
        long now = System.currentTimeMillis();
        boolean present = false;
        if (bars != null && !bars.isEmpty()) {
            for (class_345 bar : bars.values()) {
                String ascii;
                if (bar.method_5414() == null || !(ascii = MiniEventsModule.stripDiacritics(bar.method_5414().getString()).toLowerCase(Locale.ROOT)).contains("faille temporelle")) continue;
                present = true;
                break;
            }
        }
        if (present) {
            double[] anchor;
            this.failleBossbarSeenAtMs = now;
            if (this.failleWaypointId == null && (anchor = MiniEventsModule.findFailleAnchor(client)) != null) {
                this.createFailleWaypoint(anchor[0], anchor[1], anchor[2], now);
            }
        } else if (this.failleWaypointId != null && now - this.failleBossbarSeenAtMs > 3000L) {
            this.removeFailleWaypoint(this.configManager.get().map);
            this.configManager.save();
        }
    }

    private static double[] findFailleAnchor(class_310 client) {
        double[] dArray;
        if (client.field_1724 == null || client.field_1687 == null) {
            return null;
        }
        class_1309 best = null;
        double bestSq = Double.MAX_VALUE;
        List mobs = client.field_1687.method_8390(class_1309.class, client.field_1724.method_5829().method_1014(64.0), e -> !e.method_31481() && !(e instanceof class_1657));
        for (class_1309 mob : mobs) {
            double sq;
            String ascii;
            String name = MiniEventsModule.entityName(mob);
            if (name == null || !(ascii = MiniEventsModule.stripDiacritics(name).toLowerCase(Locale.ROOT)).contains("zombie temporel") || !((sq = mob.method_5858((class_1297)client.field_1724)) < bestSq)) continue;
            bestSq = sq;
            best = mob;
        }
        if (best == null) {
            dArray = null;
        } else {
            double[] dArray2 = new double[3];
            dArray2[0] = best.method_23317();
            dArray2[1] = best.method_23318();
            dArray = dArray2;
            dArray2[2] = best.method_23321();
        }
        return dArray;
    }

    private void createFailleWaypoint(double x, double y, double z, long now) {
        MapConfig map = this.configManager.get().map;
        this.removeFailleWaypoint(map);
        MapConfig.Waypoint wp = new MapConfig.Waypoint();
        wp.id = "faille-" + now;
        wp.label = MiniEventType.FAILLE.label;
        wp.x = x;
        wp.y = y;
        wp.z = z;
        wp.colorRgb = MiniEventType.FAILLE.colorRgb;
        wp.createdAt = now;
        wp.visible = true;
        map.waypoints.add(wp);
        this.failleWaypointId = wp.id;
        this.configManager.save();
        this.pushToast(MiniEventType.FAILLE);
    }

    private void removeFailleWaypoint(MapConfig map) {
        if (this.failleWaypointId == null) {
            return;
        }
        map.waypoints.removeIf(w -> this.failleWaypointId.equals(w.id));
        if (map.guideTarget != null && this.failleWaypointId.equals(map.guideTarget.label)) {
            map.guideTarget = null;
        }
        this.failleWaypointId = null;
        this.failleBossbarSeenAtMs = 0L;
    }

    private void scanForColis(class_310 client, int range, boolean searching) {
        if (client.field_1687 == null || client.field_1724 == null) {
            return;
        }
        class_2338 pp = client.field_1724.method_24515();
        int chunkRadius = (range >> 4) + 1;
        int pcx = pp.method_10263() >> 4;
        int pcz = pp.method_10260() >> 4;
        long now = System.currentTimeMillis();
        for (int cx = pcx - chunkRadius; cx <= pcx + chunkRadius; ++cx) {
            for (int cz = pcz - chunkRadius; cz <= pcz + chunkRadius; ++cz) {
                class_2818 class_28182 = client.field_1687.method_8497(cx, cz);
                if (!(class_28182 instanceof class_2818)) continue;
                class_2818 wc = class_28182;
                for (class_2586 be : wc.method_12214().values()) {
                    class_2338 pos;
                    if (!(be instanceof class_2611) || (pos = be.method_11016()).method_10262((class_2382)pp) > (double)range * (double)range || ZombieZMapData.isInAnyRefuge(pos.method_10263(), pos.method_10260())) continue;
                    class_2338 key = pos.method_10062();
                    boolean firstSight = this.knownEnderChests.add(key);
                    if (!searching || !firstSight) continue;
                    boolean fresh = !this.colisEvents.containsKey(key);
                    this.colisEvents.computeIfAbsent(key, k -> new ActiveEvent(MiniEventType.COLIS, class_2561.method_43471((String)"zombiezcompanion.mini_events.colis.label").getString(), (double)pos.method_10263() + 0.5, (double)pos.method_10264() + 0.5, (double)pos.method_10260() + 0.5, now + 300000L));
                    if (!fresh) continue;
                    this.pushToast(MiniEventType.COLIS);
                }
            }
        }
    }

    private static String entityName(class_1309 mob) {
        String s;
        class_2561 custom = mob.method_5797();
        if (custom != null && (s = custom.getString()) != null && !s.isBlank()) {
            return s;
        }
        class_2561 disp = mob.method_5477();
        return disp == null ? null : disp.getString();
    }

    private static MiniEventType identifyByEquipment(class_1309 mob) {
        if (!(mob instanceof class_1642)) {
            return null;
        }
        class_1799 head = mob.method_6118(class_1304.field_6169);
        if (head.method_31574(class_1802.field_8626)) {
            return MiniEventType.BOMBE;
        }
        return null;
    }

    private static MiniEventType identifyType(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String ascii = MiniEventsModule.stripDiacritics(name).toLowerCase(Locale.ROOT);
        if (ascii.contains("fuyeur dor")) {
            return MiniEventType.FUYEUR;
        }
        if (ascii.contains("jackpot")) {
            return MiniEventType.JACKPOT;
        }
        if (ascii.contains("pinata") || ascii.contains("zombie dor")) {
            return MiniEventType.PINATA;
        }
        if (ascii.contains("faille")) {
            return MiniEventType.FAILLE;
        }
        String stripped = name.replaceAll("[^\\p{ASCII}]", "").replaceAll("[^0-9a-zA-Z]", "").trim();
        if (stripped.matches("\\d{1,2}s?")) {
            return MiniEventType.BOMBE;
        }
        return null;
    }

    private static String stripDiacritics(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private static boolean isEnabled(MiniEventsConfig cfg, MiniEventType type) {
        return switch (type.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> cfg.fuyeur;
            case 1 -> cfg.colis;
            case 2 -> cfg.faille;
            case 3 -> cfg.pinata;
            case 4 -> cfg.bombe;
            case 5 -> cfg.jackpot;
            case 6 -> cfg.marchand;
            case 7 -> cfg.assaut;
            case 8 -> cfg.worldBoss;
        };
    }

    private void renderBeacons(WorldRenderContext ctx) {
        boolean failleActive;
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1687 == null) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        if (!ZombieZCompanionClient.moduleManager().isEnabled(ID)) {
            return;
        }
        boolean bl = failleActive = this.config().faille && this.failleWaypointId != null;
        if (this.entityEvents.isEmpty() && this.colisEvents.isEmpty() && !failleActive) {
            return;
        }
        class_4184 camera = ctx.camera();
        class_243 cam = camera.method_19326();
        class_4604 frustum = ctx.frustum();
        class_4587 matrices = ctx.matrixStack();
        class_4597.class_4598 immediate = mc.method_22940().method_23000();
        boolean drewAny = false;
        matrices.method_22903();
        for (ActiveEvent ev : this.entityEvents.values()) {
            if (!WaypointsModule.isBeaconVisible(frustum, ev.x, ev.y, ev.z)) continue;
            WaypointsModule.drawBeacon(matrices, immediate, camera, cam, mc.field_1772, ev.x, ev.y, ev.z, ev.label, 0xFF000000 | ev.type.colorRgb);
            drewAny = true;
        }
        for (ActiveEvent ev : this.colisEvents.values()) {
            if (!WaypointsModule.isBeaconVisible(frustum, ev.x, ev.y, ev.z)) continue;
            WaypointsModule.drawBeacon(matrices, immediate, camera, cam, mc.field_1772, ev.x, ev.y, ev.z, ev.label, 0xFF000000 | ev.type.colorRgb);
            drewAny = true;
        }
        if (failleActive) {
            drewAny |= MiniEventsModule.renderTemporalZombies(mc, matrices, immediate, cam);
        }
        matrices.method_22909();
        if (!drewAny) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth((float)4.0f);
        immediate.method_22993();
        RenderSystem.lineWidth((float)1.0f);
        RenderSystem.enableDepthTest();
    }

    private static boolean renderTemporalZombies(class_310 mc, class_4587 matrices, class_4597.class_4598 immediate, class_243 cam) {
        if (mc.field_1724 == null || mc.field_1687 == null) {
            return false;
        }
        int color = 0xFF000000 | MiniEventType.FAILLE.colorRgb;
        boolean drew = false;
        List mobs = mc.field_1687.method_8390(class_1309.class, mc.field_1724.method_5829().method_1014(96.0), e -> !e.method_31481() && !(e instanceof class_1657));
        for (class_1309 mob : mobs) {
            String ascii;
            String name = MiniEventsModule.entityName(mob);
            if (name == null || !(ascii = MiniEventsModule.stripDiacritics(name).toLowerCase(Locale.ROOT)).contains("zombie temporel")) continue;
            class_238 box = mob.method_5829();
            MiniEventsModule.drawBoxOutline(matrices, immediate, cam, box.field_1323, box.field_1322, box.field_1321, box.field_1320, box.field_1325, box.field_1324, color);
            drew = true;
        }
        return drew;
    }

    private static void drawBoxOutline(class_4587 matrices, class_4597.class_4598 immediate, class_243 cam, double wxMin, double wyMin, double wzMin, double wxMax, double wyMax, double wzMax, int color) {
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        float xMin = (float)(wxMin - cam.field_1352);
        float xMax = (float)(wxMax - cam.field_1352);
        float yMin = (float)(wyMin - cam.field_1351);
        float yMax = (float)(wyMax - cam.field_1351);
        float zMin = (float)(wzMin - cam.field_1350);
        float zMax = (float)(wzMax - cam.field_1350);
        MiniEventsModule.drawBoxLines(immediate, matrices, WaypointsModule.BEACON_LINES_BEHIND, xMin, yMin, zMin, xMax, yMax, zMax, r, g, b, 0.35f);
        MiniEventsModule.drawBoxLines(immediate, matrices, WaypointsModule.BEACON_LINES_FRONT, xMin, yMin, zMin, xMax, yMax, zMax, r, g, b, 0.95f);
    }

    private static void drawBoxLines(class_4597.class_4598 immediate, class_4587 matrices, class_1921 layer, float xMin, float yMin, float zMin, float xMax, float yMax, float zMax, float r, float g, float b, float a) {
        class_4588 lines = immediate.getBuffer(layer);
        class_4587.class_4665 entry = matrices.method_23760();
        MiniEventsModule.edge(lines, entry, xMin, yMin, zMin, xMax, yMin, zMin, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMax, yMin, zMin, xMax, yMin, zMax, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMax, yMin, zMax, xMin, yMin, zMax, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMin, yMin, zMax, xMin, yMin, zMin, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMin, yMax, zMin, xMax, yMax, zMin, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMax, yMax, zMin, xMax, yMax, zMax, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMax, yMax, zMax, xMin, yMax, zMax, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMin, yMax, zMax, xMin, yMax, zMin, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMin, yMin, zMin, xMin, yMax, zMin, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMax, yMin, zMin, xMax, yMax, zMin, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMax, yMin, zMax, xMax, yMax, zMax, r, g, b, a);
        MiniEventsModule.edge(lines, entry, xMin, yMin, zMax, xMin, yMax, zMax, r, g, b, a);
    }

    private static void edge(class_4588 lines, class_4587.class_4665 entry, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        lines.method_56824(entry, x1, y1, z1).method_22915(r, g, b, a).method_60831(entry, 0.0f, 1.0f, 0.0f);
        lines.method_56824(entry, x2, y2, z2).method_22915(r, g, b, a).method_60831(entry, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public void onHudRender(class_332 ctx, float tickDelta) {
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null || client.field_1755 != null) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        if (!client.field_1690.field_1842) {
            if (this.config().marchandTimer) {
                this.renderSpawnTimer(ctx, client, false, "marchand_timer", "zombiezcompanion.mini_events.marchand.timer", 0.3);
            }
            if (this.config().worldBossTimer) {
                this.renderSpawnTimer(ctx, client, true, "world_boss_timer", "zombiezcompanion.mini_events.world_boss.timer", 0.36);
            }
        }
        if (this.entityEvents.isEmpty() && this.colisEvents.isEmpty() && this.toasts.isEmpty()) {
            return;
        }
        for (ActiveEvent ev : this.entityEvents.values()) {
            WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, ev.x, ev.y, ev.z, ev.label, 0xFF000000 | ev.type.colorRgb);
        }
        for (ActiveEvent ev : this.colisEvents.values()) {
            WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, ev.x, ev.y, ev.z, ev.label, 0xFF000000 | ev.type.colorRgb);
        }
        this.renderToasts(ctx, client);
    }

    private void pushToast(MiniEventType type) {
        long now = System.currentTimeMillis();
        Toast last = this.toasts.peekLast();
        if (last != null && last.type == type && now - last.spawnedAt < 1500L) {
            return;
        }
        this.toasts.addLast(new Toast(type, now));
        while (this.toasts.size() > 4) {
            this.toasts.pollFirst();
        }
    }

    private void renderToasts(class_332 ctx, class_310 client) {
        if (this.toasts.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        this.toasts.removeIf(t -> now - t.spawnedAt > 3500L);
        if (this.toasts.isEmpty()) {
            return;
        }
        int screenW = client.method_22683().method_4486();
        int screenH = client.method_22683().method_4502();
        int boxW = 220;
        int boxH = 32;
        int gap = 6;
        int totalH = this.toasts.size() * (boxH + gap) - gap;
        HudConfig hud = this.configManager.get().hud;
        double scale = HudAnchor.scale(hud, "mini_events_toast");
        int scaledW = (int)Math.round((double)boxW * scale);
        int scaledTotalH = (int)Math.round((double)Math.max(boxH, totalH) * scale);
        int originX = HudAnchor.resolveX(hud, "mini_events_toast", screenW, scaledW, 0.5);
        int startY = HudAnchor.resolveY(hud, "mini_events_toast", screenH, scaledTotalH, 0.08);
        HudElements.report("mini_events_toast", originX, startY, scaledW, scaledTotalH);
        ctx.method_51448().method_22903();
        ctx.method_51448().method_46416((float)originX, (float)startY, 0.0f);
        if (scale != 1.0) {
            ctx.method_51448().method_22905((float)scale, (float)scale, 1.0f);
        }
        int i = 0;
        for (Toast t2 : this.toasts) {
            long age = now - t2.spawnedAt;
            float alpha = age < 500L ? (float)age / 500.0f : (age > 3000L ? (float)(3500L - age) / 500.0f : 1.0f);
            int a = (int)((alpha = Math.max(0.0f, Math.min(1.0f, alpha))) * 255.0f);
            if (a <= 0) {
                ++i;
                continue;
            }
            int x = 0;
            int y = i * (boxH + gap);
            int accent = a << 24 | t2.type.colorRgb & 0xFFFFFF;
            int bg = (int)(alpha * 200.0f) << 24;
            int border = (int)(alpha * 180.0f) << 24 | t2.type.colorRgb & 0xFFFFFF;
            int textCol = a << 24 | 0xFFFFFF;
            ctx.method_25294(x, y, x + boxW, y + boxH, bg);
            ctx.method_25294(x, y, x + 3, y + boxH, accent);
            ctx.method_49601(x, y, boxW, boxH, border);
            String title = class_2561.method_43471((String)"zombiezcompanion.mini_events.toast.spawn").getString();
            String label = t2.type.label;
            int titleW = client.field_1772.method_1727(title);
            int labelW = client.field_1772.method_1727(label);
            ctx.method_25303(client.field_1772, title, x + (boxW - titleW) / 2, y + 6, (int)(alpha * 200.0f) << 24 | 0xCCCCCC);
            ctx.method_25303(client.field_1772, label, x + (boxW - labelW) / 2, y + 18, textCol);
            ++i;
        }
        ctx.method_51448().method_22909();
    }

    private void reset() {
        this.entityEvents.clear();
        this.colisEvents.clear();
        this.knownEnderChests.clear();
        this.toasts.clear();
        this.scanTick = 0;
        this.colisSearchUntil = 0L;
        this.marchandHeaderUntil = 0L;
        this.assautHeaderUntil = 0L;
        this.bossHeaderUntil = 0L;
        this.failleBossbarSeenAtMs = 0L;
        this.nextSpawnFetchMs = 0L;
        SpawnSync.clear();
    }

    public static enum MiniEventType {
        FUYEUR("fuyeur", 16766720, "Fuyeur Dor\u00e9"),
        COLIS("colis", 3528703, "Colis Express"),
        FAILLE("faille", 12616956, "Faille Temporelle"),
        PINATA("pinata", 15485081, "Pinata Zombie"),
        BOMBE("bombe", 14427686, "Zombie Bombe"),
        JACKPOT("jackpot", 1096065, "Jackpot Zombie"),
        MARCHAND("marchand", 16096779, "Marchand Ambulant"),
        ASSAUT("assaut", 15381256, "Assaut du March\u00e9 des Sables"),
        WORLD_BOSS("world_boss", 0xFF3030, "World Boss");

        public final String key;
        public final int colorRgb;
        public final String label;

        private MiniEventType(String key, int colorRgb, String label) {
            this.key = key;
            this.colorRgb = colorRgb;
            this.label = label;
        }
    }

    private record ActiveEvent(MiniEventType type, String label, double x, double y, double z, long expiresAt) {
    }

    private record Toast(MiniEventType type, long spawnedAt) {
    }
}

