package io.github.keoz5.zombiezcompanion.modules.players;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.modules.telemetry.PresenceCache;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class ModUserTag {
    private static final String BADGE = "\u25c8 ";

    private ModUserTag() {
    }

    public static boolean enabled() {
        ConfigManager cm = ZombieZCompanionClient.configManager();
        return cm != null && cm.get().players.modUserNametag;
    }

    public static boolean matches(String label) {
        if (label == null || label.isEmpty()) {
            return false;
        }
        for (PresenceCache.Presence p : PresenceCache.presences()) {
            String n = p.name();
            if (n == null || n.isEmpty() || !label.contains(n)) continue;
            return true;
        }
        return false;
    }

    public static Component decorate(Component original) {
        return Component.literal((String)BADGE).withStyle(ChatFormatting.AQUA).append(original);
    }
}

