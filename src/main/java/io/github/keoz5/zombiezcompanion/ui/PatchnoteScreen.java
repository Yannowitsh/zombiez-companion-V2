package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.net.HttpClients;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import io.github.keoz5.zombiezcompanion.update.UpdateChecker;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Small popup showing the mod's recent release notes, pulled from the "Notes de version" section of the
 * README on GitHub (same source already kept up to date per release — see release-changelog-workflow).
 */
public final class PatchnoteScreen extends Screen {
    private static final String RAW_BASE = "https://raw.githubusercontent.com/Yannowitsh/zombiez-companion-V2";
    private static final String SECTION_HEADER = "## Notes de version";

    // Cached across screen opens within the session; null until the first successful fetch.
    private static volatile String cachedText;
    private static volatile boolean fetchFailed;
    private static volatile boolean fetching;

    private final Screen parent;
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
    private MultiLineEditBox textBox;
    private String appliedText;

    public PatchnoteScreen(Screen parent) {
        super((Component) Component.translatable((String) "zombiezcompanion.patchnote.title"));
        this.parent = parent;
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
        this.addRenderableWidget(new StyledButton(this.panelX2 - 12 - 22, this.titleY1 + 10, 22, 22, (Component) Component.literal((String) "X"), b -> this.onClose(), -266723542, -265932737, -854792));

        int editorY = this.contentY1 + 8;
        int editorW = this.panelX2 - this.panelX1 - 72;
        int editorH = this.contentY2 - editorY - 8;
        String text = this.currentText();
        //? if >= 26.1 {
        this.textBox = new MultiLineEditBox.Builder().setX(this.panelX1 + 36).setY(editorY)
                .build(this.font, editorW, editorH, (Component) Component.literal((String) ""));
        //?} else {
        /*this.textBox = new MultiLineEditBox(this.font, this.panelX1 + 36, editorY, editorW, editorH,
                (Component) Component.literal((String) ""), (Component) Component.literal((String) ""));
        *///?}
        this.textBox.setValue(text);
        this.appliedText = text;
        this.addRenderableWidget(this.textBox);

        int btnH = 20;
        int btnY = this.footerY1 + (34 - btnH) / 2;
        this.addRenderableWidget(new StyledButton(this.panelX1 + 12, btnY, 100, btnH, (Component) Component.translatable((String) "zombiezcompanion.button.back"), b -> this.onClose(), -266723542, -265932737, -854792));

        if (cachedText == null && !fetching) {
            PatchnoteScreen.fetchAsync();
        }
    }

    @Override
    public void tick() {
        super.tick();
        String text = this.currentText();
        if (this.textBox != null && !text.equals(this.appliedText)) {
            this.appliedText = text;
            this.textBox.setValue(text);
        }
    }

    private String currentText() {
        if (cachedText != null) {
            return cachedText;
        }
        return fetchFailed
                ? Component.translatable((String) "zombiezcompanion.patchnote.error").getString()
                : Component.translatable((String) "zombiezcompanion.patchnote.loading").getString();
    }

    private static void fetchAsync() {
        fetching = true;
        fetchFailed = false;
        String url = RAW_BASE + "/" + UpdateChecker.branch() + "/README.md";
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(8L)).GET().build();
            HttpClients.SHARED.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, err) -> {
                fetching = false;
                String extracted = err == null && resp != null && resp.statusCode() == 200 ? PatchnoteScreen.extract(resp.body()) : null;
                if (extracted != null) {
                    cachedText = extracted;
                } else {
                    fetchFailed = true;
                }
            });
        } catch (Exception e) {
            fetching = false;
            fetchFailed = true;
        }
    }

    /** Pulls the "## Notes de version" section out of the README (up to the next top-level heading), and
     *  strips markdown bold markers for a slightly cleaner plain-text read. */
    private static String extract(String md) {
        if (md == null) {
            return null;
        }
        int start = md.indexOf(SECTION_HEADER);
        if (start < 0) {
            return null;
        }
        int bodyStart = start + SECTION_HEADER.length();
        int end = md.indexOf("\n## ", bodyStart);
        String body = (end > 0 ? md.substring(bodyStart, end) : md.substring(bodyStart)).trim();
        if (body.isEmpty()) {
            return null;
        }
        return body.replace("**", "");
    }

    //? if >= 26.1 {
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
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
        //? if >= 26.1 {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        //?} else {
        /*super.render(ctx, mouseX, mouseY, delta);
        *///?}
        ctx.text(this.font, (Component) Component.translatable((String) "zombiezcompanion.patchnote.title"), this.panelX1 + 18, this.titleY1 + 10, -854792, true);
        ctx.text(this.font, (Component) Component.translatable((String) "zombiezcompanion.patchnote.subtitle"), this.panelX1 + 18, this.titleY1 + 23, -8353376, false);
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
