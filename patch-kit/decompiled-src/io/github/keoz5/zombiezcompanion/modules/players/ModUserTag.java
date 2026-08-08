/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 */
package io.github.keoz5.zombiezcompanion.modules.players;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.modules.telemetry.PresenceCache;
import net.minecraft.class_124;
import net.minecraft.class_2561;

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

    public static class_2561 decorate(class_2561 original) {
        return class_2561.method_43470((String)BADGE).method_27692(class_124.field_1075).method_10852(original);
    }
}

