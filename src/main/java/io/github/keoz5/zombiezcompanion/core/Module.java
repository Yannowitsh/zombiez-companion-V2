package io.github.keoz5.zombiezcompanion.core;

import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public interface Module {
    public String id();

    default public String displayName() {
        return this.id();
    }

    default public String description() {
        return "";
    }

    default public List<String> searchKeywords() {
        return List.of();
    }

    default public ModuleCategory category() {
        return ModuleCategory.COMFORT;
    }

    default public boolean defaultEnabled() {
        return true;
    }

    default public boolean hasOptions() {
        return false;
    }

    default public boolean hidden() {
        return false;
    }

    default public Screen createOptionsScreen(Screen parent) {
        return null;
    }

    default public void onRegister(ModuleContext ctx) {
    }

    default public void onEnable() {
    }

    default public void onDisable() {
    }

    default public void onClientTick(Minecraft client) {
    }

    default public void onChatMessage(Component message, boolean overlay) {
    }

    default public void onHudRender(GuiGraphicsExtractor drawContext, float tickDelta) {
    }

    default public void onJoinWorld() {
    }

    default public void onLeaveWorld() {
    }
}

