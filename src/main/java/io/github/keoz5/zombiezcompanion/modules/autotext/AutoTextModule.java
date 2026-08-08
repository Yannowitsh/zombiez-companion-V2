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
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;
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
        return Text.translatable((String)"zombiezcompanion.module.auto_text.desc").getString();
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
    public Screen createOptionsScreen(Screen parent) {
        return new AutoTextOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onDisable() {
        Arrays.fill(this.pressedLastTick, false);
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        int i;
        AutoTextConfig cfg = this.config();
        if (client.currentScreen != null || client.player == null || client.getNetworkHandler() == null || !ZombieZDetector.isOnZombieZ()) {
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
            boolean bl = pressed = GLFW.glfwGetKey((long)client.getWindow().getHandle(), (int)entry.keyCode) == 1;
            if (pressed && !this.pressedLastTick[i]) {
                this.sendConfiguredText(client, entry.text);
            }
            this.pressedLastTick[i] = pressed;
        }
        for (i = count; i < this.pressedLastTick.length; ++i) {
            this.pressedLastTick[i] = false;
        }
    }

    private void sendConfiguredText(MinecraftClient client, String rawText) {
        if (rawText == null) {
            return;
        }
        String text = rawText.trim();
        if (text.isEmpty() || client.getNetworkHandler() == null) {
            return;
        }
        if (text.startsWith("/")) {
            String command = text.substring(1).trim();
            if (!command.isEmpty()) {
                client.getNetworkHandler().sendChatCommand(command);
            }
        } else {
            client.getNetworkHandler().sendChatMessage(text);
        }
    }

    public AutoTextConfig config() {
        return this.configManager.get().autoText;
    }

    static String keyLabel(int keyCode) {
        if (keyCode == -1) {
            return Text.translatable((String)"zombiezcompanion.autotext.key.none").getString();
        }
        return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
    }
}

