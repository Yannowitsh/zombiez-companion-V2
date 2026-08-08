/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.modules.skulls;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.SkullsConfig;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsManagerScreen;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;

public final class SkullsOptionsScreen
extends ModuleOptionsScreen {
    private final SkullsModule moduleRef;

    public SkullsOptionsScreen(class_437 parent, SkullsModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 30;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        this.method_37063((class_364)new StyledButton(x, y, optionW, 22, (class_2561)class_2561.method_43471((String)"zombiezcompanion.skulls.open"), btn -> class_310.method_1551().method_1507((class_437)new SkullsManagerScreen(this, this.configManager, this.moduleRef)), -11441921, -8874241, -854792));
        this.method_37063((class_364)new StyledButton(x, y + 32, optionW, 20, this.hideToggleLabel(), btn -> {
            SkullsConfig cfg = this.config();
            cfg.hideVisitedBeacons = !cfg.hideVisitedBeacons;
            this.configManager.save();
            btn.method_25355(this.hideToggleLabel());
            ((StyledButton)btn).setColors(cfg.hideVisitedBeacons ? -14709924 : -12965328, cfg.hideVisitedBeacons ? -14179731 : -11716288);
        }, this.config().hideVisitedBeacons ? -14709924 : -12965328, this.config().hideVisitedBeacons ? -14179731 : -11716288, -854792));
        int halfW = (optionW - 8) / 2;
        this.method_37063((class_364)new StyledButton(x, y + 60, halfW, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.skulls.remove_all_beacons"), btn -> this.moduleRef.removeAllSkullWaypoints(), -12965328, -11716288, -854792));
        this.method_37063((class_364)new StyledButton(x + halfW + 8, y + 60, halfW, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.skulls.reset_progress"), btn -> this.moduleRef.resetAllVisited(), -12965328, -11716288, -854792));
        this.addKeybindRow(x, y + 92, optionW, Keybinds.openSkulls(), (class_2561)class_2561.method_43471((String)"key.zombiezcompanion.open_skulls"));
    }

    private class_2561 hideToggleLabel() {
        return class_2561.method_43469((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{class_2561.method_43471((String)"zombiezcompanion.skulls.toggle.hide_visited"), class_2561.method_43471((String)(this.config().hideVisitedBeacons ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private SkullsConfig config() {
        return this.moduleRef.config();
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        super.method_25394(ctx, mouseX, mouseY, delta);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.skulls.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43469((String)"zombiezcompanion.skulls.options.hint", (Object[])new Object[]{this.moduleRef.totalVisited(), this.moduleRef.totalSkulls()}), this.panelX1 + 36, this.contentY1 + 144, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(class_332 ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 154;
        ctx.method_25294(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.method_25294(x, y, x + w, y + h, -267053025);
        ctx.method_25294(x, y, x + w, y + 2, -8874241);
        ctx.method_49601(x, y, w, h, -14736594);
    }
}

