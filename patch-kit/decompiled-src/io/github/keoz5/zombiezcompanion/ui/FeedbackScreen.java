/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 *  net.minecraft.class_7529
 */
package io.github.keoz5.zombiezcompanion.ui;

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
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;
import net.minecraft.class_7529;

public final class FeedbackScreen
extends class_437 {
    private static final String ENDPOINT = "https://zombiez-companion-api.keoz5.workers.dev/feedback";
    private static final int MAX_LEN = 1500;
    private final class_437 parent;
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
    private class_7529 editor;
    private StyledButton sendBtn;
    private String category = "suggestion";
    private String statusText = "";
    private int statusColor = -8353376;
    private boolean sending;

    public FeedbackScreen(class_437 parent, ConfigManager configManager) {
        super((class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.title"));
        this.parent = parent;
        this.configManager = configManager;
    }

    protected void method_25426() {
        int margin = Math.max(4, Math.min(28, Math.min(this.field_22789, this.field_22790) / 16));
        int panelW = Math.min(560, this.field_22789 - 2 * margin);
        int panelH = Math.min(420, this.field_22790 - 2 * margin);
        this.panelX1 = (this.field_22789 - panelW) / 2;
        this.panelY1 = (this.field_22790 - panelH) / 2;
        this.panelX2 = this.panelX1 + panelW;
        this.panelY2 = this.panelY1 + panelH;
        this.titleY1 = this.panelY1;
        this.titleY2 = this.titleY1 + 42;
        this.footerY2 = this.panelY2;
        this.footerY1 = this.footerY2 - 34;
        this.contentY1 = this.titleY2;
        this.contentY2 = this.footerY1;
        this.method_37063((class_364)new StyledButton(this.panelX2 - 12 - 22, this.titleY1 + 10, 22, 22, (class_2561)class_2561.method_43470((String)"X"), b -> this.method_25419(), -266723542, -265932737, -854792));
        int catY = this.contentY1 + 28;
        int catW = 120;
        int catGap = 8;
        int catX = this.panelX1 + 36;
        this.addCategory(catX, catY, catW, "bug", (class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.cat.bug"));
        this.addCategory(catX + catW + catGap, catY, catW, "suggestion", (class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.cat.suggestion"));
        this.addCategory(catX + 2 * (catW + catGap), catY, catW, "autre", (class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.cat.other"));
        int editorY = catY + 32;
        int editorW = this.panelX2 - this.panelX1 - 72;
        int editorH = this.contentY2 - editorY - 24;
        this.editor = new class_7529(this.field_22793, this.panelX1 + 36, editorY, editorW, editorH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.placeholder"), (class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.placeholder"));
        this.editor.method_44402(1500);
        this.method_37063((class_364)this.editor);
        int btnH = 20;
        int btnY = this.footerY1 + (34 - btnH) / 2;
        this.method_37063((class_364)new StyledButton(this.panelX1 + 12, btnY, 100, btnH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.button.back"), b -> this.method_25419(), -266723542, -265932737, -854792));
        this.sendBtn = new StyledButton(this.panelX2 - 12 - 120, btnY, 120, btnH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.send"), b -> this.send(), -11441921, -8874241, -854792);
        this.method_37063((class_364)this.sendBtn);
    }

    private void addCategory(int x, int y, int w, String key, class_2561 label) {
        this.method_37063((class_364)new StyledButton(x, y, w, 22, this.catLabel(key, label), btn -> {
            this.category = key;
            this.rebuild();
        }, this.isActive(key) ? -11441921 : -266723542, this.isActive(key) ? -8874241 : -265932737, -854792));
    }

    private boolean isActive(String key) {
        return key.equals(this.category);
    }

    private class_2561 catLabel(String key, class_2561 base) {
        return base;
    }

    private void rebuild() {
        this.method_37067();
        this.method_25426();
    }

    private void send() {
        if (this.sending) {
            return;
        }
        if (this.editor == null) {
            return;
        }
        String msg = this.editor.method_44405().trim();
        if (msg.isEmpty()) {
            this.statusText = class_2561.method_43471((String)"zombiezcompanion.feedback.empty").getString();
            this.statusColor = -32640;
            return;
        }
        this.sending = true;
        this.sendBtn.field_22763 = false;
        this.statusText = class_2561.method_43471((String)"zombiezcompanion.feedback.sending").getString();
        this.statusColor = -8353376;
        String uuid = this.configManager.get().telemetry.uuid;
        class_310 mc = class_310.method_1551();
        String name = mc.field_1724 != null ? mc.field_1724.method_7334().getName() : "?";
        String modV = FeedbackScreen.modVersion();
        String mcV = FeedbackScreen.mcVersion();
        String locale = mc.field_1690 != null ? mc.field_1690.field_1883 : "fr_fr";
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
        class_310.method_1551().execute(() -> {
            this.sending = false;
            if (code == 200) {
                this.statusText = class_2561.method_43471((String)"zombiezcompanion.feedback.sent").getString();
                this.statusColor = -8323200;
                if (this.editor != null) {
                    this.editor.method_44400("");
                }
            } else if (code == 429) {
                this.statusText = class_2561.method_43471((String)"zombiezcompanion.feedback.rate_limited").getString();
                this.statusColor = -32640;
            } else {
                this.statusText = class_2561.method_43469((String)"zombiezcompanion.feedback.error", (Object[])new Object[]{code}).getString();
                this.statusColor = -32640;
            }
            if (this.sendBtn != null) {
                this.sendBtn.field_22763 = true;
            }
        });
    }

    private void onError() {
        class_310.method_1551().execute(() -> {
            this.sending = false;
            this.statusText = class_2561.method_43471((String)"zombiezcompanion.feedback.network_error").getString();
            this.statusColor = -32640;
            if (this.sendBtn != null) {
                this.sendBtn.field_22763 = true;
            }
        });
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, -872415232);
        ctx.method_25294(this.panelX1 + 3, this.panelY1 + 6, this.panelX2 + 3, this.panelY2 + 6, -1442840576);
        ctx.method_25294(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.method_25294(this.panelX1, this.titleY1, this.panelX2, this.titleY2, -183232737);
        ctx.method_25294(this.panelX1, this.contentY1, this.panelX2, this.contentY2, -183825134);
        ctx.method_25294(this.panelX1, this.footerY1, this.panelX2, this.footerY2, -183232737);
        ctx.method_25294(this.panelX1, this.titleY1, this.panelX2, this.titleY1 + 2, -8874241);
        ctx.method_25294(this.panelX1, this.titleY1 + 2, this.panelX2, this.titleY1 + 3, 0x33FFFFFF);
        ctx.method_25294(this.panelX1, this.titleY2 - 1, this.panelX2, this.titleY2, -14736594);
        ctx.method_25294(this.panelX1, this.footerY1, this.panelX2, this.footerY1 + 1, -14736594);
        ctx.method_25294(this.panelX1, this.footerY1 + 1, this.panelX2, this.footerY1 + 2, 0x33FFFFFF);
        ctx.method_25294(this.panelX1 + 1, this.panelY1, this.panelX2 - 1, this.panelY1 + 1, -13880766);
        ctx.method_25294(this.panelX1 + 1, this.panelY2 - 1, this.panelX2 - 1, this.panelY2, -13880766);
        ctx.method_25294(this.panelX1, this.panelY1 + 1, this.panelX1 + 1, this.panelY2 - 1, -13880766);
        ctx.method_25294(this.panelX2 - 1, this.panelY1 + 1, this.panelX2, this.panelY2 - 1, -13880766);
        ctx.method_25294(this.panelX1, this.panelY1 - 1, this.panelX2, this.panelY1, 1148753663);
        ctx.method_25294(this.panelX1 - 1, this.panelY1, this.panelX1, this.panelY2, 1148753663);
        ctx.method_25294(this.panelX2, this.panelY1, this.panelX2 + 1, this.panelY2, 1148753663);
        super.method_25394(ctx, mouseX, mouseY, delta);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.title"), this.panelX1 + 18, this.titleY1 + 10, -854792, true);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.subtitle"), this.panelX1 + 18, this.titleY1 + 23, -8353376, false);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.category"), this.panelX1 + 36, this.contentY1 + 14, -8874241, false);
        int len = this.editor == null ? 0 : this.editor.method_44405().length();
        String counter = len + " / 1500";
        int cw = this.field_22793.method_1727(counter);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43470((String)counter), this.panelX2 - 36 - cw, this.contentY2 - 12, len > 1500 ? -32640 : -8353376, false);
        if (!this.statusText.isEmpty()) {
            ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43470((String)this.statusText), this.panelX1 + 36, this.contentY2 - 12, this.statusColor, false);
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
        return FabricLoader.getInstance().getModContainer("zombiezcompanion").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    private static String mcVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    public void method_25419() {
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    public boolean method_25421() {
        return false;
    }
}

