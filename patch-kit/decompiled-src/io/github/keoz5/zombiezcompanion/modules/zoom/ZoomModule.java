/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
 *  net.minecraft.class_2561
 *  net.minecraft.class_304
 *  net.minecraft.class_310
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.modules.zoom;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.ZoomConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.modules.zoom.ZoomOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.zoom.ZoomState;
import java.util.List;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_437;

public final class ZoomModule
implements Module {
    public static final String ID = "zoom";
    public static final double MIN_FACTOR = 2.0;
    public static final double MAX_FACTOR = 8.0;
    private static final String CATEGORY = "key.categories.zombiezcompanion";
    private ConfigManager configManager;
    private class_304 zoomKey;

    public class_304 zoomKey() {
        return this.zoomKey;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Zoom";
    }

    @Override
    public String description() {
        return class_2561.method_43471((String)"zombiezcompanion.module.zoom.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.COMFORT;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of(ID, "loupe", "agrandir", "longue vue", "fov");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
        this.zoomKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.zombiezcompanion.zoom", class_3675.class_307.field_1668, 67, CATEGORY));
    }

    @Override
    public class_437 createOptionsScreen(class_437 parent) {
        return new ZoomOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onDisable() {
        ZoomState.reset();
    }

    @Override
    public void onClientTick(class_310 client) {
        boolean zooming = client.field_1724 != null && client.field_1755 == null && this.zoomKey != null && this.zoomKey.method_1434();
        ZoomState.update(zooming, this.config().factor);
    }

    @Override
    public void onLeaveWorld() {
        ZoomState.reset();
    }

    public ZoomConfig config() {
        return this.configManager.get().zoom;
    }

    public void setFactor(double factor) {
        this.config().factor = Math.max(2.0, Math.min(8.0, factor));
    }

    public class_2561 keyLabel() {
        return this.zoomKey != null ? this.zoomKey.method_16007() : class_2561.method_43470((String)"C");
    }
}

