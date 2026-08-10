package io.github.keoz5.zombiezcompanion.modules.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ZombieZDetector {
    private static final String SERVER_HOST = "rinaorc.com";

    private ZombieZDetector() {
    }

    public static boolean isOnZombieZ() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return false;
        }
        ServerData info = client.getCurrentServer();
        if (info == null || info.ip == null) {
            return false;
        }
        String host = info.ip.toLowerCase().split(":", 2)[0];
        return host.equals(SERVER_HOST) || host.endsWith(".rinaorc.com");
    }
}

