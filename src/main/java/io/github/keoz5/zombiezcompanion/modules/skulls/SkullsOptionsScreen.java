package io.github.keoz5.zombiezcompanion.modules.skulls;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.SkullsConfig;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsManagerScreen;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SkullsOptionsScreen
extends ModuleOptionsScreen {
    private final SkullsModule moduleRef;

    public SkullsOptionsScreen(Screen parent, SkullsModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 30;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        this.addRenderableWidget(new StyledButton(x, y, optionW, 22, (Component)Component.translatable((String)"zombiezcompanion.skulls.open"), btn -> Minecraft.getInstance().setScreen((Screen)new SkullsManagerScreen(this, this.configManager, this.moduleRef)), -11441921, -8874241, -854792));
        this.addRenderableWidget(new StyledButton(x, y + 32, optionW, 20, this.hideToggleLabel(), btn -> {
            SkullsConfig cfg = this.config();
            cfg.hideVisitedBeacons = !cfg.hideVisitedBeacons;
            this.configManager.save();
            btn.setMessage(this.hideToggleLabel());
            ((StyledButton)btn).setColors(cfg.hideVisitedBeacons ? -14709924 : -12965328, cfg.hideVisitedBeacons ? -14179731 : -11716288);
        }, this.config().hideVisitedBeacons ? -14709924 : -12965328, this.config().hideVisitedBeacons ? -14179731 : -11716288, -854792));
        int halfW = (optionW - 8) / 2;
        this.addRenderableWidget(new StyledButton(x, y + 60, halfW, 20, (Component)Component.translatable((String)"zombiezcompanion.skulls.remove_all_beacons"), btn -> this.moduleRef.removeAllSkullWaypoints(), -12965328, -11716288, -854792));
        this.addRenderableWidget(new StyledButton(x + halfW + 8, y + 60, halfW, 20, (Component)Component.translatable((String)"zombiezcompanion.skulls.reset_progress"), btn -> this.moduleRef.resetAllVisited(), -12965328, -11716288, -854792));
        this.addKeybindRow(x, y + 92, optionW, Keybinds.openSkulls(), (Component)Component.translatable((String)"key.zombiezcompanion.open_skulls"));
    }

    private Component hideToggleLabel() {
        return Component.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{Component.translatable((String)"zombiezcompanion.skulls.toggle.hide_visited"), Component.translatable((String)(this.config().hideVisitedBeacons ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private SkullsConfig config() {
        return this.moduleRef.config();
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
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.skulls.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.skulls.options.hint", (Object[])new Object[]{this.moduleRef.totalVisited(), this.moduleRef.totalSkulls()}), this.panelX1 + 36, this.contentY1 + 144, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(GuiGraphicsExtractor ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 154;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.outline(x, y, w, h, -14736594);
    }
}

