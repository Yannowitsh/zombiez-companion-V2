/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.modules.brightness;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.modules.brightness.BrightnessModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledSlider;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;

public final class BrightnessOptionsScreen
extends ModuleOptionsScreen {
    private final BrightnessModule moduleRef;

    public BrightnessOptionsScreen(class_437 parent, BrightnessModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int sliderW = Math.min(360, this.panelX2 - this.panelX1 - 72);
        int sliderH = 22;
        int sliderX = (this.panelX1 + this.panelX2) / 2 - sliderW / 2;
        int sliderY = this.contentY1 + 64;
        this.method_37063((class_364)new StyledSlider(sliderX, sliderY, sliderW, sliderH, this.moduleRef.config().gamma, 0.0, 15.0, this.moduleRef::setGamma, v -> class_2561.method_43469((String)"zombiezcompanion.brightness.slider", (Object[])new Object[]{BrightnessOptionsScreen.toPercent(v)})));
    }

    private static int toPercent(double gamma) {
        return (int)Math.round(gamma / 15.0 * 100.0);
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
        ctx.method_27534(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.brightness.desc"), cx, this.contentY1 + 22, -854792);
        ctx.method_27534(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.brightness.range_hint"), cx, this.contentY1 + 36, -8353376);
        ctx.method_27534(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.brightness.vanilla_hint"), cx, this.contentY1 + 104, -8353376);
    }

    @Override
    protected void renderOptionsBackground(class_332 ctx) {
        int w = Math.min(430, this.panelX2 - this.panelX1 - 48);
        int h = 132;
        int x = (this.panelX1 + this.panelX2) / 2 - w / 2;
        int y = this.contentY1 + 14;
        ctx.method_25294(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.method_25294(x, y, x + w, y + h, -267053025);
        ctx.method_25294(x, y, x + w, y + 2, -8874241);
        ctx.method_49601(x, y, w, h, -14736594);
    }
}

