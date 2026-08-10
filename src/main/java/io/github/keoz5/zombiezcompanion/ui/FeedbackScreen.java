package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.ModInfo;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.net.HttpClients;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FeedbackScreen
extends Screen {
    private static final String ENDPOINT = ModInfo.API_BASE + "/feedback";
    private static final int MAX_LEN = 1500;
    private final Screen parent;
    private final ConfigManager configManager;
    private int panelX1;
    private int panelY1;
    private int panelX2;
    private int panelY2;
    private int titleY1;
    private int titleY2;
    private int contentY1;
    private int contentY2;
    private int footerY1;
    private int footerY2;
    private MultiLineEditBox editor;
    private StyledButton sendBtn;
    private String category = "suggestion";
    private String statusText = "";
    private int statusColor = -8353376;
    private boolean sending;

    public FeedbackScreen(Screen parent, ConfigManager configManager) {
        super((Component)Component.translatable((String)"zombiezcompanion.feedback.title"));
        this.parent = parent;
        this.configManager = configManager;
    }

    protected void init() {
        int margin = Math.max(4, Math.min(28, Math.min(this.width, this.height) / 16));
        int panelW = Math.min(560, this.width - 2 * margin);
        int panelH = Math.min(420, this.height - 2 * margin);
        this.panelX1 = (this.width - panelW) / 2;
        this.panelY1 = (this.height - panelH) / 2;
        this.panelX2 = this.panelX1 + panelW;
        this.panelY2 = this.panelY1 + panelH;
        this.titleY1 = this.panelY1;
        this.titleY2 = this.titleY1 + 42;
        this.footerY2 = this.panelY2;
        this.footerY1 = this.footerY2 - 34;
        this.contentY1 = this.titleY2;
        this.contentY2 = this.footerY1;
        this.addRenderableWidget(new StyledButton(this.panelX2 - 12 - 22, this.titleY1 + 10, 22, 22, (Component)Component.literal((String)"X"), b -> this.onClose(), -266723542, -265932737, -854792));
        int catY = this.contentY1 + 28;
        int catW = 120;
        int catGap = 8;
        int catX = this.panelX1 + 36;
        this.addCategory(catX, catY, catW, "bug", (Component)Component.translatable((String)"zombiezcompanion.feedback.cat.bug"));
        this.addCategory(catX + catW + catGap, catY, catW, "suggestion", (Component)Component.translatable((String)"zombiezcompanion.feedback.cat.suggestion"));
        this.addCategory(catX + 2 * (catW + catGap), catY, catW, "autre", (Component)Component.translatable((String)"zombiezcompanion.feedback.cat.other"));
        int editorY = catY + 32;
        int editorW = this.panelX2 - this.panelX1 - 72;
        int editorH = this.contentY2 - editorY - 24;
        this.editor = new MultiLineEditBox.Builder().setX(this.panelX1 + 36).setY(editorY).setPlaceholder((Component)Component.translatable((String)"zombiezcompanion.feedback.placeholder")).build(this.font, editorW, editorH, (Component)Component.translatable((String)"zombiezcompanion.feedback.placeholder"));
        this.editor.setCharacterLimit(1500);
        this.addRenderableWidget(this.editor);
        int btnH = 20;
        int btnY = this.footerY1 + (34 - btnH) / 2;
        this.addRenderableWidget(new StyledButton(this.panelX1 + 12, btnY, 100, btnH, (Component)Component.translatable((String)"zombiezcompanion.button.back"), b -> this.onClose(), -266723542, -265932737, -854792));
        this.sendBtn = new StyledButton(this.panelX2 - 12 - 120, btnY, 120, btnH, (Component)Component.translatable((String)"zombiezcompanion.feedback.send"), b -> this.send(), -11441921, -8874241, -854792);
        this.addRenderableWidget(this.sendBtn);
    }

    private void addCategory(int x, int y, int w, String key, Component label) {
        this.addRenderableWidget(new StyledButton(x, y, w, 22, this.catLabel(key, label), btn -> {
            this.category = key;
            this.rebuild();
        }, this.isActive(key) ? -11441921 : -266723542, this.isActive(key) ? -8874241 : -265932737, -854792));
    }

    private boolean isActive(String key) {
        return key.equals(this.category);
    }

    private Component catLabel(String key, Component base) {
        return base;
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    private void send() {
        if (this.sending) {
            return;
        }
        if (this.editor == null) {
            return;
        }
        String msg = this.editor.getValue().trim();
        if (msg.isEmpty()) {
            this.statusText = Component.translatable((String)"zombiezcompanion.feedback.empty").getString();
            this.statusColor = -32640;
            return;
        }
        this.sending = true;
        this.sendBtn.active = false;
        this.statusText = Component.translatable((String)"zombiezcompanion.feedback.sending").getString();
        this.statusColor = -8353376;
        String uuid = this.configManager.get().telemetry.uuid;
        Minecraft mc = Minecraft.getInstance();
        String name = mc.player != null ? mc.player.getGameProfile().name() : "?";
        String modV = FeedbackScreen.modVersion();
        String mcV = FeedbackScreen.mcVersion();
        String locale = mc.options != null ? mc.options.languageCode : "fr_fr";
        String body = "{\"uuid\":\"" + FeedbackScreen.escape(uuid) + "\",\"name\":\"" + FeedbackScreen.escape(name) + "\",\"category\":\"" + FeedbackScreen.escape(this.category) + "\",\"message\":\"" + FeedbackScreen.escape(msg) + "\",\"mod_version\":\"" + FeedbackScreen.escape(modV) + "\",\"mc_version\":\"" + FeedbackScreen.escape(mcV) + "\",\"locale\":\"" + FeedbackScreen.escape(locale) + "\"}";
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(ENDPOINT)).timeout(Duration.ofSeconds(10L)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            ((CompletableFuture)HttpClients.SHARED.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> this.onResponse(r.statusCode(), (String)r.body()))).exceptionally(t -> {
                this.onError();
                return null;
            });
        }
        catch (Exception e) {
            this.onError();
        }
    }

    private void onResponse(int code, String body) {
        Minecraft.getInstance().execute(() -> {
            this.sending = false;
            if (code == 200) {
                this.statusText = Component.translatable((String)"zombiezcompanion.feedback.sent").getString();
                this.statusColor = -8323200;
                if (this.editor != null) {
                    this.editor.setValue("");
                }
            } else if (code == 429) {
                this.statusText = Component.translatable((String)"zombiezcompanion.feedback.rate_limited").getString();
                this.statusColor = -32640;
            } else {
                this.statusText = Component.translatable((String)"zombiezcompanion.feedback.error", (Object[])new Object[]{code}).getString();
                this.statusColor = -32640;
            }
            if (this.sendBtn != null) {
                this.sendBtn.active = true;
            }
        });
    }

    private void onError() {
        Minecraft.getInstance().execute(() -> {
            this.sending = false;
            this.statusText = Component.translatable((String)"zombiezcompanion.feedback.network_error").getString();
            this.statusColor = -32640;
            if (this.sendBtn != null) {
                this.sendBtn.active = true;
            }
        });
    }

    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, -872415232);
        ctx.fill(this.panelX1 + 3, this.panelY1 + 6, this.panelX2 + 3, this.panelY2 + 6, -1442840576);
        ctx.fill(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.fill(this.panelX1, this.titleY1, this.panelX2, this.titleY2, -183232737);
        ctx.fill(this.panelX1, this.contentY1, this.panelX2, this.contentY2, -183825134);
        ctx.fill(this.panelX1, this.footerY1, this.panelX2, this.footerY2, -183232737);
        ctx.fill(this.panelX1, this.titleY1, this.panelX2, this.titleY1 + 2, -8874241);
        ctx.fill(this.panelX1, this.titleY1 + 2, this.panelX2, this.titleY1 + 3, 0x33FFFFFF);
        ctx.fill(this.panelX1, this.titleY2 - 1, this.panelX2, this.titleY2, -14736594);
        ctx.fill(this.panelX1, this.footerY1, this.panelX2, this.footerY1 + 1, -14736594);
        ctx.fill(this.panelX1, this.footerY1 + 1, this.panelX2, this.footerY1 + 2, 0x33FFFFFF);
        ctx.fill(this.panelX1 + 1, this.panelY1, this.panelX2 - 1, this.panelY1 + 1, -13880766);
        ctx.fill(this.panelX1 + 1, this.panelY2 - 1, this.panelX2 - 1, this.panelY2, -13880766);
        ctx.fill(this.panelX1, this.panelY1 + 1, this.panelX1 + 1, this.panelY2 - 1, -13880766);
        ctx.fill(this.panelX2 - 1, this.panelY1 + 1, this.panelX2, this.panelY2 - 1, -13880766);
        ctx.fill(this.panelX1, this.panelY1 - 1, this.panelX2, this.panelY1, 1148753663);
        ctx.fill(this.panelX1 - 1, this.panelY1, this.panelX1, this.panelY2, 1148753663);
        ctx.fill(this.panelX2, this.panelY1, this.panelX2 + 1, this.panelY2, 1148753663);
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.feedback.title"), this.panelX1 + 18, this.titleY1 + 10, -854792, true);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.feedback.subtitle"), this.panelX1 + 18, this.titleY1 + 23, -8353376, false);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.feedback.category"), this.panelX1 + 36, this.contentY1 + 14, -8874241, false);
        int len = this.editor == null ? 0 : this.editor.getValue().length();
        String counter = len + " / 1500";
        int cw = this.font.width(counter);
        ctx.text(this.font, (Component)Component.literal((String)counter), this.panelX2 - 36 - cw, this.contentY2 - 12, len > 1500 ? -32640 : -8353376, false);
        if (!this.statusText.isEmpty()) {
            ctx.text(this.font, (Component)Component.literal((String)this.statusText), this.panelX1 + 36, this.contentY2 - 12, this.statusColor, false);
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        block7: for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '\"': {
                    out.append("\\\"");
                    continue block7;
                }
                case '\\': {
                    out.append("\\\\");
                    continue block7;
                }
                case '\n': {
                    out.append("\\n");
                    continue block7;
                }
                case '\r': {
                    out.append("\\r");
                    continue block7;
                }
                case '\t': {
                    out.append("\\t");
                    continue block7;
                }
                default: {
                    if (c < ' ') {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int)c));
                        continue block7;
                    }
                    out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer(ModInfo.MOD_ID).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    private static String mcVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    public boolean isPauseScreen() {
        return false;
    }
}

