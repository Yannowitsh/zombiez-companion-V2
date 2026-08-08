/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_437
 *  org.lwjgl.glfw.GLFW
 */
package io.github.keoz5.zombiezcompanion.modules.autotext;

import io.github.keoz5.zombiezcompanion.config.AutoTextConfig;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.modules.autotext.AutoTextOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import java.util.Arrays;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

public final class AutoTextModule
implements Module {
    public static final String ID = "auto_text";
    private ConfigManager configManager;
    private final boolean[] pressedLastTick = new boolean[5];

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Textes auto";
    }

    @Override
    public String description() {
        return class_2561.method_43471((String)"zombiezcompanion.module.auto_text.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.COMFORT;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("auto", "texte", "commande", "macro", "raccourci", "message", "chat", "autotext");
    }

    @Override
    public boolean defaultEnabled() {
        return false;
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
    public class_437 createOptionsScreen(class_437 parent) {
        return new AutoTextOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onDisable() {
        Arrays.fill(this.pressedLastTick, false);
    }

    @Override
    public void onClientTick(class_310 client) {
        int i;
        AutoTextConfig cfg = this.config();
        if (client.field_1755 != null || client.field_1724 == null || client.method_1562() == null || !ZombieZDetector.isOnZombieZ()) {
            Arrays.fill(this.pressedLastTick, false);
            return;
        }
        List<AutoTextConfig.Entry> entries = cfg.entries;
        if (entries == null || entries.isEmpty()) {
            Arrays.fill(this.pressedLastTick, false);
            return;
        }
        int count = Math.min(entries.size(), 5);
        for (i = 0; i < count; ++i) {
            boolean pressed;
            AutoTextConfig.Entry entry = entries.get(i);
            if (entry == null || entry.keyCode == -1) {
                this.pressedLastTick[i] = false;
                continue;
            }
            boolean bl = pressed = GLFW.glfwGetKey((long)client.method_22683().method_4490(), (int)entry.keyCode) == 1;
            if (pressed && !this.pressedLastTick[i]) {
                this.sendConfiguredText(client, entry.text);
            }
            this.pressedLastTick[i] = pressed;
        }
        for (i = count; i < this.pressedLastTick.length; ++i) {
            this.pressedLastTick[i] = false;
        }
    }

    private void sendConfiguredText(class_310 client, String rawText) {
        if (rawText == null) {
            return;
        }
        String text = rawText.trim();
        if (text.isEmpty() || client.method_1562() == null) {
            return;
        }
        if (text.startsWith("/")) {
            String command = text.substring(1).trim();
            if (!command.isEmpty()) {
                client.method_1562().method_45730(command);
            }
        } else {
            client.method_1562().method_45729(text);
        }
    }

    public AutoTextConfig config() {
        return this.configManager.get().autoText;
    }

    static String keyLabel(int keyCode) {
        if (keyCode == -1) {
            return class_2561.method_43471((String)"zombiezcompanion.autotext.key.none").getString();
        }
        return class_3675.class_307.field_1668.method_1447(keyCode).method_27445().getString();
    }
}

