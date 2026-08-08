/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.modules.zoom;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.modules.zoom.ZoomModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledSlider;
import java.util.Locale;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;

public final class ZoomOptionsScreen
extends ModuleOptionsScreen {
    private final ZoomModule moduleRef;

    public ZoomOptionsScreen(class_437 parent, ZoomModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int sliderW = Math.min(360, this.panelX2 - this.panelX1 - 72);
        int sliderH = 22;
        int sliderX = (this.panelX1 + this.panelX2) / 2 - sliderW / 2;
        int sliderY = this.contentY1 + 64;
        this.method_37063((class_364)new StyledSlider(sliderX, sliderY, sliderW, sliderH, this.moduleRef.config().factor, 2.0, 8.0, this.moduleRef::setFactor, v -> class_2561.method_43469((String)"zombiezcompanion.zoom.slider.factor", (Object[])new Object[]{ZoomOptionsScreen.formatFactor(v)})));
        this.addKeybindRow(sliderX, sliderY + 60, sliderW, this.moduleRef.zoomKey(), (class_2561)class_2561.method_43471((String)"key.zombiezcompanion.zoom"));
    }

    private static String formatFactor(double factor) {
        return String.format(Locale.ROOT, "%.1f", factor);
    }

    public boolean method_25406(double mouseX, double mouseY, int button) {
        boolean result = super.method_25406(mouseX, mouseY, button);
        this.configManager.save();
        return result;
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        super.method_25394(ctx, mouseX, mouseY, delta);
        int cx = (this.panelX1 + this.panelX2) / 2;
        ctx.method_27534(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.zoom.desc"), cx, this.contentY1 + 22, -854792);
        ctx.method_27534(this.field_22793, (class_2561)class_2561.method_43469((String)"zombiezcompanion.zoom.key_hint", (Object[])new Object[]{this.moduleRef.keyLabel()}), cx, this.contentY1 + 36, -8353376);
        ctx.method_27534(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.zoom.range_hint"), cx, this.contentY1 + 104, -8353376);
    }

    @Override
    protected void renderOptionsBackground(class_332 ctx) {
        int w = Math.min(430, this.panelX2 - this.panelX1 - 48);
        int h = 170;
        int x = (this.panelX1 + this.panelX2) / 2 - w / 2;
        int y = this.contentY1 + 14;
        ctx.method_25294(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.method_25294(x, y, x + w, y + h, -267053025);
        ctx.method_25294(x, y, x + w, y + 2, -8874241);
        ctx.method_49601(x, y, w, h, -14736594);
    }
}

