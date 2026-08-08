package io.github.keoz5.zombiezcompanion.keybind;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class Keybinds {
    private static final String CATEGORY = "key.categories.zombiezcompanion";
    private static KeyBinding openMenu;
    private static KeyBinding openMap;
    private static KeyBinding openWaypoints;
    private static KeyBinding openStats;
    private static KeyBinding clearGuide;
    private static KeyBinding openSkulls;
    private static KeyBinding tpRefuge;

    private Keybinds() {
    }

    public static void register(Runnable onMenuPressed, Runnable onMapPressed, Runnable onWaypointsPressed, Runnable onStatsPressed, Runnable onClearGuidePressed, Runnable onSkullsPressed, Runnable onTpRefugePressed) {
        openMenu = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.zombiezcompanion.open_menu", InputUtil.Type.KEYSYM, 344, CATEGORY));
        openMap = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.zombiezcompanion.open_map", InputUtil.Type.KEYSYM, 77, CATEGORY));
        openWaypoints = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.zombiezcompanion.open_waypoints", InputUtil.Type.KEYSYM, 78, CATEGORY));
        openStats = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.zombiezcompanion.open_stats", InputUtil.Type.KEYSYM, 74, CATEGORY));
        clearGuide = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.zombiezcompanion.clear_guide", InputUtil.Type.KEYSYM, 71, CATEGORY));
        openSkulls = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.zombiezcompanion.open_skulls", InputUtil.Type.KEYSYM, 75, CATEGORY));
        tpRefuge = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.zombiezcompanion.tp_refuge", InputUtil.Type.KEYSYM, -1, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) {
                if (client.currentScreen != null) continue;
                onMenuPressed.run();
            }
            while (openMap.wasPressed()) {
                if (client.currentScreen != null) continue;
                onMapPressed.run();
            }
            while (openWaypoints.wasPressed()) {
                if (client.currentScreen != null) continue;
                onWaypointsPressed.run();
            }
            while (openStats.wasPressed()) {
                if (client.currentScreen != null) continue;
                onStatsPressed.run();
            }
            while (clearGuide.wasPressed()) {
                if (client.currentScreen != null) continue;
                onClearGuidePressed.run();
            }
            while (openSkulls.wasPressed()) {
                if (client.currentScreen != null) continue;
                onSkullsPressed.run();
            }
            while (tpRefuge.wasPressed()) {
                if (client.currentScreen != null) continue;
                onTpRefugePressed.run();
            }
        });
    }

    public static boolean matchesClearGuide(int keyCode, int scanCode) {
        return clearGuide != null && clearGuide.matchesKey(keyCode, scanCode);
    }

    public static KeyBinding openMenu() {
        return openMenu;
    }

    public static KeyBinding openMap() {
        return openMap;
    }

    public static KeyBinding openWaypoints() {
        return openWaypoints;
    }

    public static KeyBinding openStats() {
        return openStats;
    }

    public static KeyBinding clearGuide() {
        return clearGuide;
    }

    public static KeyBinding openSkulls() {
        return openSkulls;
    }

    public static KeyBinding tpRefuge() {
        return tpRefuge;
    }

    public static List<KeyBinding> all() {
        ArrayList<KeyBinding> list = new ArrayList<KeyBinding>();
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

