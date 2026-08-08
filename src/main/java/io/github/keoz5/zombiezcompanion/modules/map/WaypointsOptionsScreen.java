package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointManagerScreen;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

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
        this.addToggle(x, y, (Text)Text.translatable((String)"zombiezcompanion.waypoint.toggle.markers"), toggleW, () -> this.config().showWaypoints, v -> {
            this.config().showWaypoints = v;
        });
        this.addToggle(x + toggleW + toggleGap, y, (Text)Text.translatable((String)"zombiezcompanion.waypoint.toggle.hud"), toggleW, () -> this.config().showWaypointHud, v -> {
            this.config().showWaypointHud = v;
        });
        this.addDrawableChild(new StyledButton(x, y + 42, optionW, 20, this.styleButtonLabel(), button -> {
            this.config().waypointMarkerStyle = (this.config().waypointMarkerStyle + 1) % 2;
            button.setMessage(this.styleButtonLabel());
        }, -266723542, -265932737, -854792));
        this.addDrawableChild(new StyledButton(x, y + 70, optionW, 20, this.positionButtonLabel(), button -> {
            this.config().waypointHudPosition = (this.config().waypointHudPosition + 1) % 4;
            button.setMessage(this.positionButtonLabel());
        }, -266723542, -265932737, -854792));
        this.addToggle(x, y + 104, (Text)Text.translatable((String)"zombiezcompanion.waypoint.toggle.auto_death"), optionW, () -> this.config().autoDeathWaypoint, v -> {
            this.config().autoDeathWaypoint = v;
        });
        this.addDrawableChild(new StyledButton(x, y + 138, optionW, 20, (Text)Text.translatable((String)"zombiezcompanion.waypoint.open_manager"), button -> MinecraftClient.getInstance().setScreen((Screen)new WaypointManagerScreen(this, this.configManager)), -266723542, -265932737, -854792));
        this.addKeybindRow(x, y + 168, optionW, Keybinds.openWaypoints(), (Text)Text.translatable((String)"key.zombiezcompanion.open_waypoints"));
        this.addKeybindRow(x, y + 194, optionW, Keybinds.clearGuide(), (Text)Text.translatable((String)"key.zombiezcompanion.clear_guide"));
        this.addCrossLink(x, y + 224, optionW, "mini_map", (Text)Text.translatable((String)"zombiezcompanion.crosslink.mini_map"));
    }

    private Text styleButtonLabel() {
        String key = this.config().waypointMarkerStyle == 0 ? "zombiezcompanion.waypoint.style.beacon" : "zombiezcompanion.waypoint.style.arrow";
        return Text.translatable((String)"zombiezcompanion.waypoint.style.label", (Object[])new Object[]{Text.translatable((String)key)});
    }

    private void addToggle(int x, int y, Text label, int w, BoolGetter getter, BoolSetter setter) {
        this.addDrawableChild(new StyledButton(x, y, w, 20, WaypointsOptionsScreen.toggleLabel(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.set(next);
            button.setMessage(WaypointsOptionsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, getter.get() ? -14709924 : -12965328, getter.get() ? -14179731 : -11716288, -854792));
    }

    private Text positionButtonLabel() {
        return Text.translatable((String)"zombiezcompanion.waypoint.position.label", (Object[])new Object[]{WaypointsModule.positionLabel(this.config().waypointHudPosition)});
    }

    private static Text toggleLabel(Text label, boolean enabled) {
        return Text.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, Text.translatable((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private MapConfig config() {
        return this.moduleRef.config();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.waypoint.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
    }

    @Override
    protected void renderOptionsBackground(DrawContext ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 282;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.drawBorder(x, y, w, h, -14736594);
    }

    private static interface BoolGetter {
        public boolean get();
    }

    private static interface BoolSetter {
        public void set(boolean var1);
    }
}

