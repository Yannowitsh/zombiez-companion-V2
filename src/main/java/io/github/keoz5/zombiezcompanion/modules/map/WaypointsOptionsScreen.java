package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointManagerScreen;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class WaypointsOptionsScreen
extends ModuleOptionsScreen {
    private final WaypointsModule moduleRef;

    public WaypointsOptionsScreen(Screen parent, WaypointsModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 34;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        int toggleGap = 18;
        int toggleW = Math.max(100, (optionW - toggleGap) / 2);
        this.addToggle(x, y, (Component)Component.translatable((String)"zombiezcompanion.waypoint.toggle.markers"), toggleW, () -> this.config().showWaypoints, v -> {
            this.config().showWaypoints = v;
        });
        this.addToggle(x + toggleW + toggleGap, y, (Component)Component.translatable((String)"zombiezcompanion.waypoint.toggle.hud"), toggleW, () -> this.config().showWaypointHud, v -> {
            this.config().showWaypointHud = v;
        });
        this.addRenderableWidget(new StyledButton(x, y + 42, optionW, 20, this.styleButtonLabel(), button -> {
            this.config().waypointMarkerStyle = (this.config().waypointMarkerStyle + 1) % 2;
            button.setMessage(this.styleButtonLabel());
        }, -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(x, y + 70, optionW, 20, this.positionButtonLabel(), button -> {
            this.config().waypointHudPosition = (this.config().waypointHudPosition + 1) % 4;
            button.setMessage(this.positionButtonLabel());
        }, -266723542, -265932737, -854792));
        this.addToggle(x, y + 104, (Component)Component.translatable((String)"zombiezcompanion.waypoint.toggle.auto_death"), optionW, () -> this.config().autoDeathWaypoint, v -> {
            this.config().autoDeathWaypoint = v;
        });
        this.addRenderableWidget(new StyledButton(x, y + 138, optionW, 20, (Component)Component.translatable((String)"zombiezcompanion.waypoint.open_manager"), button -> Minecraft.getInstance().setScreen((Screen)new WaypointManagerScreen(this, this.configManager)), -266723542, -265932737, -854792));
        this.addKeybindRow(x, y + 168, optionW, Keybinds.openWaypoints(), (Component)Component.translatable((String)"key.zombiezcompanion.open_waypoints"));
        this.addKeybindRow(x, y + 194, optionW, Keybinds.clearGuide(), (Component)Component.translatable((String)"key.zombiezcompanion.clear_guide"));
        this.addCrossLink(x, y + 224, optionW, "mini_map", (Component)Component.translatable((String)"zombiezcompanion.crosslink.mini_map"));
    }

    private Component styleButtonLabel() {
        String key = this.config().waypointMarkerStyle == 0 ? "zombiezcompanion.waypoint.style.beacon" : "zombiezcompanion.waypoint.style.arrow";
        return Component.translatable((String)"zombiezcompanion.waypoint.style.label", (Object[])new Object[]{Component.translatable((String)key)});
    }

    private void addToggle(int x, int y, Component label, int w, BoolGetter getter, BoolSetter setter) {
        this.addRenderableWidget(new StyledButton(x, y, w, 20, WaypointsOptionsScreen.toggleLabel(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.set(next);
            button.setMessage(WaypointsOptionsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, getter.get() ? -14709924 : -12965328, getter.get() ? -14179731 : -11716288, -854792));
    }

    private Component positionButtonLabel() {
        return Component.translatable((String)"zombiezcompanion.waypoint.position.label", (Object[])new Object[]{WaypointsModule.positionLabel(this.config().waypointHudPosition)});
    }

    private static Component toggleLabel(Component label, boolean enabled) {
        return Component.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, Component.translatable((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private MapConfig config() {
        return this.moduleRef.config();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.waypoint.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
    }

    @Override
    protected void renderOptionsBackground(GuiGraphicsExtractor ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 282;
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

