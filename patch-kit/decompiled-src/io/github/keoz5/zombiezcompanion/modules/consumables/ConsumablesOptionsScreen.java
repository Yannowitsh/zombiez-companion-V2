/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.modules.consumables;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.ConsumablesConfig;
import io.github.keoz5.zombiezcompanion.modules.consumables.ConsumablesModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;

public final class ConsumablesOptionsScreen
extends ModuleOptionsScreen {
    private final ConsumablesModule moduleRef;

    public ConsumablesOptionsScreen(class_437 parent, ConsumablesModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 30;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        this.addToggle(x, y, optionW, (class_2561)class_2561.method_43471((String)"zombiezcompanion.consumables.toggle.lure"), () -> this.config().lureTimer, v -> {
            this.config().lureTimer = v;
        });
        this.addToggle(x, y + 28, optionW, (class_2561)class_2561.method_43471((String)"zombiezcompanion.consumables.toggle.flower"), () -> this.config().flowerTimer, v -> {
            this.config().flowerTimer = v;
        });
    }

    private ConsumablesConfig config() {
        return this.moduleRef.config();
    }

    private void addToggle(int x, int y, int w, class_2561 label, BoolGetter getter, BoolSetter setter) {
        this.method_37063((class_364)new StyledButton(x, y, w, 20, ConsumablesOptionsScreen.toggleLabel(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.set(next);
            button.method_25355(ConsumablesOptionsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, getter.get() ? -14709924 : -12965328, getter.get() ? -14179731 : -11716288, -854792));
    }

    private static class_2561 toggleLabel(class_2561 label, boolean enabled) {
        return class_2561.method_43469((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, class_2561.method_43471((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        super.method_25394(ctx, mouseX, mouseY, delta);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.consumables.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.consumables.options.hint"), this.panelX1 + 36, this.contentY1 + 76, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(class_332 ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 96;
        ctx.method_25294(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.method_25294(x, y, x + w, y + h, -267053025);
        ctx.method_25294(x, y, x + w, y + 2, -8874241);
        ctx.method_49601(x, y, w, h, -14736594);
    }

    private static interface BoolGetter {
        public boolean get();
    }

    private static interface BoolSetter {
        public void set(boolean var1);
    }
}

