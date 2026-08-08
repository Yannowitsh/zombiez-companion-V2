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
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;

public final class ZoomModule
implements Module {
    public static final String ID = "zoom";
    public static final double MIN_FACTOR = 2.0;
    public static final double MAX_FACTOR = 8.0;
    private static final String CATEGORY = "key.categories.zombiezcompanion";
    private ConfigManager configManager;
    private KeyBinding zoomKey;

    public KeyBinding zoomKey() {
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
        return Text.translatable((String)"zombiezcompanion.module.zoom.desc").getString();
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
        this.zoomKey = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.zombiezcompanion.zoom", InputUtil.Type.KEYSYM, 67, CATEGORY));
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new ZoomOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onDisable() {
        ZoomState.reset();
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        boolean zooming = client.player != null && client.currentScreen == null && this.zoomKey != null && this.zoomKey.isPressed();
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

    public Text keyLabel() {
        return this.zoomKey != null ? this.zoomKey.getBoundKeyLocalizedText() : Text.literal((String)"C");
    }
}

