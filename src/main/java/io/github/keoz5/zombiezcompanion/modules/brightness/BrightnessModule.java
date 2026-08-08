package io.github.keoz5.zombiezcompanion.modules.brightness;

import io.github.keoz5.zombiezcompanion.config.BrightnessConfig;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.log.Log;
import io.github.keoz5.zombiezcompanion.log.LogCategory;
import io.github.keoz5.zombiezcompanion.modules.brightness.BrightnessOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.brightness.BrightnessOverride;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.Screen;

public final class BrightnessModule
implements Module {
    public static final String ID = "brightness";
    public static final double GAMMA_MIN = 0.0;
    public static final double GAMMA_MAX = 15.0;
    private ConfigManager configManager;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Luminosit\u00e9";
    }

    @Override
    public String description() {
        return Text.translatable((String)"zombiezcompanion.module.brightness.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.COMFORT;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("luminosit\u00e9", "gamma", "fullbright", "full bright", "\u00e9clairage", "lumi\u00e8re", "sombre", ID, "light", "night vision");
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
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
    }

    @Override
    public void onEnable() {
        BrightnessOverride.enable(this.clampedGamma());
        Log.debug(LogCategory.MODULE, "brightness on, target=" + this.config().gamma);
    }

    @Override
    public void onDisable() {
        BrightnessOverride.disable();
        Log.debug(LogCategory.MODULE, "brightness off");
    }

    public void setGamma(double newGamma) {
        double clamped;
        this.config().gamma = clamped = BrightnessModule.clamp(newGamma);
        if (BrightnessOverride.isActive()) {
            BrightnessOverride.setTarget(clamped);
        }
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new BrightnessOptionsScreen(parent, this, this.configManager);
    }

    public BrightnessConfig config() {
        return this.configManager.get().brightness;
    }

    private double clampedGamma() {
        return BrightnessModule.clamp(this.config().gamma);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(15.0, v));
    }
}

