package io.github.keoz5.zombiezcompanion.modules.consumables;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.ConsumablesConfig;
import io.github.keoz5.zombiezcompanion.modules.consumables.ConsumablesModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConsumablesOptionsScreen
extends ModuleOptionsScreen {
    private final ConsumablesModule moduleRef;

    public ConsumablesOptionsScreen(Screen parent, ConsumablesModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 30;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        this.addToggle(x, y, optionW, (Component)Component.translatable((String)"zombiezcompanion.consumables.toggle.lure"), () -> this.config().lureTimer, v -> {
            this.config().lureTimer = v;
        });
        this.addToggle(x, y + 28, optionW, (Component)Component.translatable((String)"zombiezcompanion.consumables.toggle.flower"), () -> this.config().flowerTimer, v -> {
            this.config().flowerTimer = v;
        });
    }

    private ConsumablesConfig config() {
        return this.moduleRef.config();
    }

    private void addToggle(int x, int y, int w, Component label, BoolGetter getter, BoolSetter setter) {
        this.addRenderableWidget(new StyledButton(x, y, w, 20, ConsumablesOptionsScreen.toggleLabel(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.set(next);
            button.setMessage(ConsumablesOptionsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, getter.get() ? -14709924 : -12965328, getter.get() ? -14179731 : -11716288, -854792));
    }

    private static Component toggleLabel(Component label, boolean enabled) {
        return Component.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, Component.translatable((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    @Override
    //? if >= 26.1 {
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
        //? if >= 26.1 {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        //?} else {
        /*super.render(ctx, mouseX, mouseY, delta);
        *///?}
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.consumables.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.consumables.options.hint"), this.panelX1 + 36, this.contentY1 + 76, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(GuiGraphicsExtractor ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 96;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.outline(x, y, w, h, -14736594);
    }

    private static interface BoolGetter {
        public boolean get();
    }

    private static interface BoolSetter {
        public void set(boolean var1);
    }
}

