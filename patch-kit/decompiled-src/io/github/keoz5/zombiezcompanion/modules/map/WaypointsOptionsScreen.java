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
package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointManagerScreen;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;

public final class WaypointsOptionsScreen
extends ModuleOptionsScreen {
    private final WaypointsModule moduleRef;

    public WaypointsOptionsScreen(class_437 parent, WaypointsModule module, ConfigManager configManager) {
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
        this.addToggle(x, y, (class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.toggle.markers"), toggleW, () -> this.config().showWaypoints, v -> {
            this.config().showWaypoints = v;
        });
        this.addToggle(x + toggleW + toggleGap, y, (class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.toggle.hud"), toggleW, () -> this.config().showWaypointHud, v -> {
            this.config().showWaypointHud = v;
        });
        this.method_37063((class_364)new StyledButton(x, y + 42, optionW, 20, this.styleButtonLabel(), button -> {
            this.config().waypointMarkerStyle = (this.config().waypointMarkerStyle + 1) % 2;
            button.method_25355(this.styleButtonLabel());
        }, -266723542, -265932737, -854792));
        this.method_37063((class_364)new StyledButton(x, y + 70, optionW, 20, this.positionButtonLabel(), button -> {
            this.config().waypointHudPosition = (this.config().waypointHudPosition + 1) % 4;
            button.method_25355(this.positionButtonLabel());
        }, -266723542, -265932737, -854792));
        this.addToggle(x, y + 104, (class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.toggle.auto_death"), optionW, () -> this.config().autoDeathWaypoint, v -> {
            this.config().autoDeathWaypoint = v;
        });
        this.method_37063((class_364)new StyledButton(x, y + 138, optionW, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.open_manager"), button -> class_310.method_1551().method_1507((class_437)new WaypointManagerScreen(this, this.configManager)), -266723542, -265932737, -854792));
        this.addKeybindRow(x, y + 168, optionW, Keybinds.openWaypoints(), (class_2561)class_2561.method_43471((String)"key.zombiezcompanion.open_waypoints"));
        this.addKeybindRow(x, y + 194, optionW, Keybinds.clearGuide(), (class_2561)class_2561.method_43471((String)"key.zombiezcompanion.clear_guide"));
        this.addCrossLink(x, y + 224, optionW, "mini_map", (class_2561)class_2561.method_43471((String)"zombiezcompanion.crosslink.mini_map"));
    }

    private class_2561 styleButtonLabel() {
        String key = this.config().waypointMarkerStyle == 0 ? "zombiezcompanion.waypoint.style.beacon" : "zombiezcompanion.waypoint.style.arrow";
        return class_2561.method_43469((String)"zombiezcompanion.waypoint.style.label", (Object[])new Object[]{class_2561.method_43471((String)key)});
    }

    private void addToggle(int x, int y, class_2561 label, int w, BoolGetter getter, BoolSetter setter) {
        this.method_37063((class_364)new StyledButton(x, y, w, 20, WaypointsOptionsScreen.toggleLabel(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.set(next);
            button.method_25355(WaypointsOptionsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, getter.get() ? -14709924 : -12965328, getter.get() ? -14179731 : -11716288, -854792));
    }

    private class_2561 positionButtonLabel() {
        return class_2561.method_43469((String)"zombiezcompanion.waypoint.position.label", (Object[])new Object[]{WaypointsModule.positionLabel(this.config().waypointHudPosition)});
    }

    private static class_2561 toggleLabel(class_2561 label, boolean enabled) {
        return class_2561.method_43469((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, class_2561.method_43471((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private MapConfig config() {
        return this.moduleRef.config();
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        super.method_25394(ctx, mouseX, mouseY, delta);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
    }

    @Override
    protected void renderOptionsBackground(class_332 ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 282;
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

