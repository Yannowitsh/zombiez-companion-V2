/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_124
 *  net.minecraft.class_2558
 *  net.minecraft.class_2558$class_2559
 *  net.minecraft.class_2561
 *  net.minecraft.class_2583
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_3417
 *  net.minecraft.class_370
 *  net.minecraft.class_370$class_9037
 *  net.minecraft.class_374
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 */
package io.github.keoz5.zombiezcompanion.modules.telemetry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.TelemetryConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.net.HttpClients;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_124;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3417;
import net.minecraft.class_370;
import net.minecraft.class_374;
import net.minecraft.class_5250;
import net.minecraft.class_5348;

public final class TelemetryModule
implements Module {
    public static final String ID = "telemetry";
    private static final String ENDPOINT = "https://zombiez-companion-api.keoz5.workers.dev";
    private static final long PING_INTERVAL_MS = 86400000L;
    private static final long FIRST_DELAY_MS = 30000L;
    private static final long CHAT_REPEAT_MS = 1200000L;
    private static final long POPUP_MS = 10000L;
    private static final long VERSION_FETCH_INTERVAL_MS = 180000L;
    private ConfigManager configManager;
    private long nextPingMs;
    private long nextVersionFetchMs;
    private volatile boolean updateAvailable;
    private volatile String latestVersion;
    private volatile String latestUrl;
    private volatile String notifiedVersion;
    private long nextNoticeAt;
    private long popupUntil;
    private boolean popupShown;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "T\u00e9l\u00e9m\u00e9trie";
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.COMFORT;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean hasOptions() {
        return false;
    }

    @Override
    public boolean hidden() {
        return true;
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
        this.ensureUuid();
    }

    public TelemetryConfig config() {
        return this.configManager.get().telemetry;
    }

    public void ensureUuid() {
        TelemetryConfig cfg = this.config();
        if (cfg.uuid == null || cfg.uuid.isBlank()) {
            cfg.uuid = UUID.randomUUID().toString();
            this.configManager.save();
        }
    }

    @Override
    public void onClientTick(class_310 client) {
        long now = System.currentTimeMillis();
        if (now >= this.nextPingMs) {
            this.sendPing();
            this.nextPingMs = now + 86400000L;
        }
        if (now >= this.nextVersionFetchMs) {
            this.nextVersionFetchMs = now + 180000L;
            this.fetchVersion();
        }
        if (this.updateAvailable) {
            if (this.nextNoticeAt == 0L) {
                this.nextNoticeAt = now + 30000L;
            } else if (now >= this.nextNoticeAt) {
                boolean first = !this.popupShown;
                this.fireNotice(client, first);
                if (first) {
                    this.popupUntil = now + 10000L;
                    this.popupShown = true;
                }
                this.nextNoticeAt = now + 1200000L;
            }
        }
    }

    @Override
    public void onJoinWorld() {
        this.nextVersionFetchMs = 0L;
    }

    @Override
    public void onLeaveWorld() {
        this.updateAvailable = false;
        this.latestVersion = null;
        this.latestUrl = null;
        this.notifiedVersion = null;
        this.nextNoticeAt = 0L;
        this.popupUntil = 0L;
        this.popupShown = false;
        this.nextVersionFetchMs = 0L;
    }

    private void fetchVersion() {
        this.getAsync("https://zombiez-companion-api.keoz5.workers.dev/version").thenAccept(jsonStr -> {
            String url;
            String latest;
            if (jsonStr == null) {
                return;
            }
            try {
                JsonObject obj = JsonParser.parseString((String)jsonStr).getAsJsonObject();
                latest = obj.has("latest") && !obj.get("latest").isJsonNull() ? obj.get("latest").getAsString() : null;
                url = obj.has("url") && !obj.get("url").isJsonNull() ? obj.get("url").getAsString() : null;
            }
            catch (Exception e) {
                return;
            }
            if (latest == null || latest.isBlank()) {
                return;
            }
            if (TelemetryModule.isOutdated(TelemetryModule.modVersion(), latest)) {
                if (!latest.equals(this.notifiedVersion)) {
                    this.latestVersion = latest;
                    this.latestUrl = url;
                    this.notifiedVersion = latest;
                    this.updateAvailable = true;
                    this.popupShown = false;
                    this.nextNoticeAt = 0L;
                }
            } else {
                this.updateAvailable = false;
                this.notifiedVersion = null;
            }
        });
    }

    private void fireNotice(class_310 mc, boolean full) {
        if (mc.field_1724 == null) {
            return;
        }
        String local = TelemetryModule.modVersion();
        if (full) {
            class_370.method_27024((class_374)mc.method_1566(), (class_370.class_9037)class_370.class_9037.field_47588, (class_2561)class_2561.method_43471((String)"zombiezcompanion.update.toast.title"), (class_2561)class_2561.method_43469((String)"zombiezcompanion.update.toast.desc", (Object[])new Object[]{this.latestVersion}));
        }
        mc.field_1705.method_1743().method_1812((class_2561)class_2561.method_43469((String)"zombiezcompanion.update.available", (Object[])new Object[]{this.latestVersion, local}).method_27692(class_124.field_1065));
        if (this.latestUrl != null && !this.latestUrl.isBlank()) {
            class_5250 link = class_2561.method_43471((String)"zombiezcompanion.update.download").method_10862(class_2583.field_24360.method_10977(class_124.field_1075).method_30938(Boolean.valueOf(true)).method_10958(new class_2558(class_2558.class_2559.field_11749, this.latestUrl)));
            mc.field_1705.method_1743().method_1812((class_2561)link);
        }
        class_5250 discord = class_2561.method_43471((String)"zombiezcompanion.update.discord").method_10862(class_2583.field_24360.method_10977(class_124.field_1075).method_30938(Boolean.valueOf(true)).method_10958(new class_2558(class_2558.class_2559.field_11749, "https://discord.gg/mqJp9CKcEX")));
        mc.field_1705.method_1743().method_1812((class_2561)discord);
        if (full) {
            mc.field_1724.method_5783(class_3417.field_14627, 0.6f, 1.4f);
        }
    }

    @Override
    public void onHudRender(class_332 ctx, float tickDelta) {
        if (!this.updateAvailable || System.currentTimeMillis() >= this.popupUntil) {
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc.field_1755 != null || mc.field_1690.field_1842) {
            return;
        }
        class_327 tr = mc.field_1772;
        int sw = ctx.method_51421();
        int sh = ctx.method_51443();
        class_5250 line1 = class_2561.method_43469((String)"zombiezcompanion.update.banner.title", (Object[])new Object[]{this.latestVersion});
        class_5250 line2 = class_2561.method_43471((String)"zombiezcompanion.update.banner.sub");
        int textW = Math.max(tr.method_27525((class_5348)line1), tr.method_27525((class_5348)line2));
        int half = textW / 2 + 12;
        float scale = 1.6f;
        ctx.method_51448().method_22903();
        ctx.method_51448().method_22904((double)sw / 2.0, (double)sh * 0.3, 0.0);
        ctx.method_51448().method_22905(scale, scale, 1.0f);
        ctx.method_25294(-half, -4, half, 30, -804647918);
        ctx.method_25294(-half, -4, half, -1, -19712);
        ctx.method_49601(-half, -4, half * 2, 34, -19712);
        ctx.method_27534(tr, (class_2561)line1, 0, 3, -10934);
        ctx.method_27534(tr, (class_2561)line2, 0, 16, -1);
        ctx.method_51448().method_22909();
    }

    static boolean isOutdated(String local, String latest) {
        if (local == null || latest == null || local.equals("?")) {
            return false;
        }
        int[] a = TelemetryModule.versionParts(local);
        int[] b = TelemetryModule.versionParts(latest);
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; ++i) {
            int vb;
            int va = i < a.length ? a[i] : 0;
            int n2 = vb = i < b.length ? b[i] : 0;
            if (va == vb) continue;
            return va < vb;
        }
        return false;
    }

    private static int[] versionParts(String v) {
        String[] toks = v.split("[^0-9]+");
        ArrayList<Integer> out = new ArrayList<Integer>();
        for (String t : toks) {
            if (t.isEmpty()) continue;
            try {
                out.add(Integer.parseInt(t));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = (Integer)out.get(i);
        }
        return arr;
    }

    private void sendPing() {
        String body = this.pingBody();
        if (body != null) {
            this.postAsync("https://zombiez-companion-api.keoz5.workers.dev/ping", body);
        }
    }

    private String pingBody() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        StringBuilder mods = new StringBuilder();
        for (Module m : mm.modules()) {
            if (!mm.isEnabled(m.id())) continue;
            if (mods.length() > 0) {
                mods.append(',');
            }
            mods.append(m.id());
        }
        return String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"modVersion\":\"%s\",\"mcVersion\":\"%s\",\"modules\":\"%s\",\"locale\":\"%s\"}", TelemetryModule.escape(this.config().uuid), TelemetryModule.escape(TelemetryModule.modVersion()), TelemetryModule.escape(TelemetryModule.mcVersion()), TelemetryModule.escape(mods.toString()), TelemetryModule.escape(class_310.method_1551().field_1690.field_1883));
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer("zombiezcompanion").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    private static String mcVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
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

    public static TelemetryModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module m : mm.modules()) {
            if (!(m instanceof TelemetryModule)) continue;
            TelemetryModule t = (TelemetryModule)m;
            return t;
        }
        return null;
    }
}

