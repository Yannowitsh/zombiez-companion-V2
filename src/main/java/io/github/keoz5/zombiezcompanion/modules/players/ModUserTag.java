package io.github.keoz5.zombiezcompanion.modules.players;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.modules.telemetry.PresenceCache;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;

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

    public static Text decorate(Text original) {
        return Text.literal((String)BADGE).formatted(Formatting.AQUA).append(original);
    }
}

