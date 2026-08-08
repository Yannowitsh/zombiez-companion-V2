/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.core;

import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;

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

    default public class_437 createOptionsScreen(class_437 parent) {
        return null;
    }

    default public void onRegister(ModuleContext ctx) {
    }

    default public void onEnable() {
    }

    default public void onDisable() {
    }

    default public void onClientTick(class_310 client) {
    }

    default public void onChatMessage(class_2561 message, boolean overlay) {
    }

    default public void onHudRender(class_332 drawContext, float tickDelta) {
    }

    default public void onJoinWorld() {
    }

    default public void onLeaveWorld() {
    }
}

