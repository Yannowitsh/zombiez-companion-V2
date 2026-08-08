package io.github.keoz5.zombiezcompanion.modules.map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class ZombieZDetector {
    private static final String SERVER_HOST = "rinaorc.com";

    private ZombieZDetector() {
    }

    public static boolean isOnZombieZ() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return false;
        }
        ServerInfo info = client.getCurrentServerEntry();
        if (info == null || info.address == null) {
            return false;
        }
        String host = info.address.toLowerCase().split(":", 2)[0];
        return host.equals(SERVER_HOST) || host.endsWith(".rinaorc.com");
    }
}

