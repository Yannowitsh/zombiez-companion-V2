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
package io.github.keoz5.zombiezcompanion.modules.stats;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.stats.StatsModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.StatsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;

public final class StatsOptionsScreen
extends ModuleOptionsScreen {
    public StatsOptionsScreen(class_437 parent, StatsModule module, ConfigManager configManager) {
        super(parent, module, configManager);
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 30;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        this.method_37063((class_364)new StyledButton(x, y, optionW, 22, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.open"), btn -> class_310.method_1551().method_1507((class_437)new StatsScreen(this, this.configManager)), -11441921, -8874241, -854792));
        this.addKeybindRow(x, y + 36, optionW, Keybinds.openStats(), (class_2561)class_2561.method_43471((String)"key.zombiezcompanion.open_stats"));
        this.addCrossLink(x, y + 96, optionW, "drop_alert", (class_2561)class_2561.method_43471((String)"zombiezcompanion.crosslink.drop_alert"));
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        super.method_25394(ctx, mouseX, mouseY, delta);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.options.hint"), this.panelX1 + 36, this.contentY1 + 100, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(class_332 ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 148;
        ctx.method_25294(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.method_25294(x, y, x + w, y + h, -267053025);
        ctx.method_25294(x, y, x + w, y + 2, -8874241);
        int divY = this.contentY1 + 30 + 88;
        ctx.method_25294(x + 12, divY, x + w - 12, divY + 1, -14736594);
        ctx.method_49601(x, y, w, h, -14736594);
    }
}

