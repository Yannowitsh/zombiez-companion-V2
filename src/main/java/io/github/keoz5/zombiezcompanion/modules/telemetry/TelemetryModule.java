package io.github.keoz5.zombiezcompanion.modules.telemetry;

import io.github.keoz5.zombiezcompanion.ModInfo;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;

public final class TelemetryModule
implements Module {
    public static final String ID = "telemetry";
    private static final String ENDPOINT = ModInfo.API_BASE;
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
    public void onClientTick(Minecraft client) {
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
        this.getAsync(ModInfo.API_BASE + "/version").thenAccept(jsonStr -> {
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

    private void fireNotice(Minecraft mc, boolean full) {
        if (mc.player == null) {
            return;
        }
        String local = TelemetryModule.modVersion();
        if (full) {
            SystemToast.add((ToastManager)mc.getToastManager(), (SystemToast.SystemToastId)SystemToast.SystemToastId.PERIODIC_NOTIFICATION, (Component)Component.translatable((String)"zombiezcompanion.update.toast.title"), (Component)Component.translatable((String)"zombiezcompanion.update.toast.desc", (Object[])new Object[]{this.latestVersion}));
        }
        mc.gui.getChat().addClientSystemMessage((Component)Component.translatable((String)"zombiezcompanion.update.available", (Object[])new Object[]{this.latestVersion, local}).withStyle(ChatFormatting.GOLD));
        if (this.latestUrl != null && !this.latestUrl.isBlank()) {
            MutableComponent link = Component.translatable((String)"zombiezcompanion.update.download").setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(Boolean.valueOf(true)).withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(this.latestUrl))));
            mc.gui.getChat().addClientSystemMessage((Component)link);
        }
        MutableComponent discord = Component.translatable((String)"zombiezcompanion.update.discord").setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(Boolean.valueOf(true)).withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(ModInfo.DISCORD_URL))));
        mc.gui.getChat().addClientSystemMessage((Component)discord);
        if (full) {
            mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
        }
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor ctx, float tickDelta) {
        if (!this.updateAvailable || System.currentTimeMillis() >= this.popupUntil) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui) {
            return;
        }
        Font tr = mc.font;
        int sw = ctx.guiWidth();
        int sh = ctx.guiHeight();
        MutableComponent line1 = Component.translatable((String)"zombiezcompanion.update.banner.title", (Object[])new Object[]{this.latestVersion});
        MutableComponent line2 = Component.translatable((String)"zombiezcompanion.update.banner.sub");
        int textW = Math.max(tr.width((FormattedText)line1), tr.width((FormattedText)line2));
        int half = textW / 2 + 12;
        float scale = 1.6f;
        ctx.pose().pushMatrix();
        ctx.pose().translate((float)((double)sw / 2.0), (float)((double)sh * 0.3));
        ctx.pose().scale(scale, scale);
        ctx.fill(-half, -4, half, 30, -804647918);
        ctx.fill(-half, -4, half, -1, -19712);
        ctx.outline(-half, -4, half * 2, 34, -19712);
        ctx.centeredText(tr, (Component)line1, 0, 3, -10934);
        ctx.centeredText(tr, (Component)line2, 0, 16, -1);
        ctx.pose().popMatrix();
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
            this.postAsync(ModInfo.API_BASE + "/ping", body);
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
        return String.format(Locale.ROOT, "{\"uuid\":\"%s\",\"modVersion\":\"%s\",\"mcVersion\":\"%s\",\"modules\":\"%s\",\"locale\":\"%s\"}", TelemetryModule.escape(this.config().uuid), TelemetryModule.escape(TelemetryModule.modVersion()), TelemetryModule.escape(TelemetryModule.mcVersion()), TelemetryModule.escape(mods.toString()), TelemetryModule.escape(Minecraft.getInstance().options.languageCode));
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer(ModInfo.MOD_ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
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

