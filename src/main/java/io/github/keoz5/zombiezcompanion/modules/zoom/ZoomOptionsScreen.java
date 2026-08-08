package io.github.keoz5.zombiezcompanion.modules.zoom;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.modules.zoom.ZoomModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledSlider;
import java.util.Locale;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

public final class ZoomOptionsScreen
extends ModuleOptionsScreen {
    private final ZoomModule moduleRef;

    public ZoomOptionsScreen(Screen parent, ZoomModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int sliderW = Math.min(360, this.panelX2 - this.panelX1 - 72);
        int sliderH = 22;
        int sliderX = (this.panelX1 + this.panelX2) / 2 - sliderW / 2;
        int sliderY = this.contentY1 + 64;
        this.addDrawableChild(new StyledSlider(sliderX, sliderY, sliderW, sliderH, this.moduleRef.config().factor, 2.0, 8.0, this.moduleRef::setFactor, v -> Text.translatable((String)"zombiezcompanion.zoom.slider.factor", (Object[])new Object[]{ZoomOptionsScreen.formatFactor(v)})));
        this.addKeybindRow(sliderX, sliderY + 60, sliderW, this.moduleRef.zoomKey(), (Text)Text.translatable((String)"key.zombiezcompanion.zoom"));
    }

    private static String formatFactor(double factor) {
        return String.format(Locale.ROOT, "%.1f", factor);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean result = super.mouseReleased(mouseX, mouseY, button);
        this.configManager.save();
        return result;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = (this.panelX1 + this.panelX2) / 2;
        ctx.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.zoom.desc"), cx, this.contentY1 + 22, -854792);
        ctx.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.zoom.key_hint", (Object[])new Object[]{this.moduleRef.keyLabel()}), cx, this.contentY1 + 36, -8353376);
        ctx.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.zoom.range_hint"), cx, this.contentY1 + 104, -8353376);
    }

    @Override
    protected void renderOptionsBackground(DrawContext ctx) {
        int w = Math.min(430, this.panelX2 - this.panelX1 - 48);
        int h = 170;
        int x = (this.panelX1 + this.panelX2) / 2 - w / 2;
        int y = this.contentY1 + 14;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.drawBorder(x, y, w, h, -14736594);
    }
}

