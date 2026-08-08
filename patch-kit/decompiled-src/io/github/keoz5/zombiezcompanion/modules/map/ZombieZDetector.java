/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_642
 */
package io.github.keoz5.zombiezcompanion.modules.map;

import net.minecraft.class_310;
import net.minecraft.class_642;

public final class ZombieZDetector {
    private static final String SERVER_HOST = "rinaorc.com";

    private ZombieZDetector() {
    }

    public static boolean isOnZombieZ() {
        class_310 client = class_310.method_1551();
        if (client.field_1687 == null || client.field_1724 == null) {
            return false;
        }
        class_642 info = client.method_1558();
        if (info == null || info.field_3761 == null) {
            return false;
        }
        String host = info.field_3761.toLowerCase().split(":", 2)[0];
        return host.equals(SERVER_HOST) || host.endsWith(".rinaorc.com");
    }
}

