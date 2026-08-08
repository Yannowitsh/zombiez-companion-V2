/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
 *  net.minecraft.class_304
 *  net.minecraft.class_3675$class_307
 */
package io.github.keoz5.zombiezcompanion.keybind;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_304;
import net.minecraft.class_3675;

public final class Keybinds {
    private static final String CATEGORY = "key.categories.zombiezcompanion";
    private static class_304 openMenu;
    private static class_304 openMap;
    private static class_304 openWaypoints;
    private static class_304 openStats;
    private static class_304 clearGuide;
    private static class_304 openSkulls;
    private static class_304 tpRefuge;

    private Keybinds() {
    }

    public static void register(Runnable onMenuPressed, Runnable onMapPressed, Runnable onWaypointsPressed, Runnable onStatsPressed, Runnable onClearGuidePressed, Runnable onSkullsPressed, Runnable onTpRefugePressed) {
        openMenu = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.zombiezcompanion.open_menu", class_3675.class_307.field_1668, 344, CATEGORY));
        openMap = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.zombiezcompanion.open_map", class_3675.class_307.field_1668, 77, CATEGORY));
        openWaypoints = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.zombiezcompanion.open_waypoints", class_3675.class_307.field_1668, 78, CATEGORY));
        openStats = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.zombiezcompanion.open_stats", class_3675.class_307.field_1668, 74, CATEGORY));
        clearGuide = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.zombiezcompanion.clear_guide", class_3675.class_307.field_1668, 71, CATEGORY));
        openSkulls = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.zombiezcompanion.open_skulls", class_3675.class_307.field_1668, 75, CATEGORY));
        tpRefuge = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.zombiezcompanion.tp_refuge", class_3675.class_307.field_1668, -1, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.method_1436()) {
                if (client.field_1755 != null) continue;
                onMenuPressed.run();
            }
            while (openMap.method_1436()) {
                if (client.field_1755 != null) continue;
                onMapPressed.run();
            }
            while (openWaypoints.method_1436()) {
                if (client.field_1755 != null) continue;
                onWaypointsPressed.run();
            }
            while (openStats.method_1436()) {
                if (client.field_1755 != null) continue;
                onStatsPressed.run();
            }
            while (clearGuide.method_1436()) {
                if (client.field_1755 != null) continue;
                onClearGuidePressed.run();
            }
            while (openSkulls.method_1436()) {
                if (client.field_1755 != null) continue;
                onSkullsPressed.run();
            }
            while (tpRefuge.method_1436()) {
                if (client.field_1755 != null) continue;
                onTpRefugePressed.run();
            }
        });
    }

    public static boolean matchesClearGuide(int keyCode, int scanCode) {
        return clearGuide != null && clearGuide.method_1417(keyCode, scanCode);
    }

    public static class_304 openMenu() {
        return openMenu;
    }

    public static class_304 openMap() {
        return openMap;
    }

    public static class_304 openWaypoints() {
        return openWaypoints;
    }

    public static class_304 openStats() {
        return openStats;
    }

    public static class_304 clearGuide() {
        return clearGuide;
    }

    public static class_304 openSkulls() {
        return openSkulls;
    }

    public static class_304 tpRefuge() {
        return tpRefuge;
    }

    public static List<class_304> all() {
        ArrayList<class_304> list = new ArrayList<class_304>();
        if (openMenu != null) {
            list.add(openMenu);
        }
        if (openMap != null) {
            list.add(openMap);
        }
        if (openWaypoints != null) {
            list.add(openWaypoints);
        }
        if (openStats != null) {
            list.add(openStats);
        }
        if (clearGuide != null) {
            list.add(clearGuide);
        }
        if (openSkulls != null) {
            list.add(openSkulls);
        }
        if (tpRefuge != null) {
            list.add(tpRefuge);
        }
        return list;
    }
}

