package io.github.keoz5.zombiezcompanion.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class Keybinds {
    // Key categories are now identified by an Identifier (was a translation-key String).
    //? if >= 26.1 {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("zombiezcompanion", "main"));
    //?} else {
    /*private static final String CATEGORY = "key.categories.zombiezcompanion.main";
    *///?}
    private static KeyMapping openMenu;
    private static KeyMapping openMap;
    private static KeyMapping openWaypoints;
    private static KeyMapping openStats;
    private static KeyMapping clearGuide;
    private static KeyMapping openSkulls;
    private static KeyMapping tpRefuge;
    private static KeyMapping pingGroup;

    private Keybinds() {
    }

    public static void register(Runnable onMenuPressed, Runnable onMapPressed, Runnable onWaypointsPressed, Runnable onStatsPressed, Runnable onClearGuidePressed, Runnable onSkullsPressed, Runnable onTpRefugePressed, Runnable onPingPressed) {
        openMenu = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.zombiezcompanion.open_menu", InputConstants.Type.KEYSYM, 344, CATEGORY));
        openMap = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.zombiezcompanion.open_map", InputConstants.Type.KEYSYM, 77, CATEGORY));
        openWaypoints = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.zombiezcompanion.open_waypoints", InputConstants.Type.KEYSYM, 78, CATEGORY));
        openStats = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.zombiezcompanion.open_stats", InputConstants.Type.KEYSYM, 74, CATEGORY));
        clearGuide = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.zombiezcompanion.clear_guide", InputConstants.Type.KEYSYM, 71, CATEGORY));
        openSkulls = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.zombiezcompanion.open_skulls", InputConstants.Type.KEYSYM, 75, CATEGORY));
        tpRefuge = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.zombiezcompanion.tp_refuge", InputConstants.Type.KEYSYM, -1, CATEGORY));
        pingGroup = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.zombiezcompanion.ping_group", InputConstants.Type.KEYSYM, -1, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.screen != null) continue;
                onMenuPressed.run();
            }
            while (openMap.consumeClick()) {
                if (client.screen != null) continue;
                onMapPressed.run();
            }
            while (openWaypoints.consumeClick()) {
                if (client.screen != null) continue;
                onWaypointsPressed.run();
            }
            while (openStats.consumeClick()) {
                if (client.screen != null) continue;
                onStatsPressed.run();
            }
            while (clearGuide.consumeClick()) {
                if (client.screen != null) continue;
                onClearGuidePressed.run();
            }
            while (openSkulls.consumeClick()) {
                if (client.screen != null) continue;
                onSkullsPressed.run();
            }
            while (tpRefuge.consumeClick()) {
                if (client.screen != null) continue;
                onTpRefugePressed.run();
            }
            while (pingGroup.consumeClick()) {
                if (client.screen != null) continue;
                onPingPressed.run();
            }
        });
    }

    public static boolean matchesClearGuide(int keyCode, int scanCode) {
        //? if >= 26.1 {
        return clearGuide != null && clearGuide.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0));
        //?} else {
        /*return clearGuide != null && clearGuide.matches(keyCode, scanCode);
        *///?}
    }

    public static KeyMapping openMenu() {
        return openMenu;
    }

    public static KeyMapping openMap() {
        return openMap;
    }

    public static KeyMapping openWaypoints() {
        return openWaypoints;
    }

    public static KeyMapping openStats() {
        return openStats;
    }

    public static KeyMapping clearGuide() {
        return clearGuide;
    }

    public static KeyMapping openSkulls() {
        return openSkulls;
    }

    public static KeyMapping tpRefuge() {
        return tpRefuge;
    }

    public static KeyMapping pingGroup() {
        return pingGroup;
    }

    /**
     * Whether the ping key is physically held right now, polled straight from GLFW so it keeps working
     * while a screen (the ping wheel) is open — unlike {@code consumeClick}/{@code isDown}. Handles both
     * keyboard and mouse bindings.
     */
    public static boolean pingKeyHeld() {
        if (pingGroup == null) {
            return false;
        }
        InputConstants.Key key = InputConstants.getKey(pingGroup.saveString());
        if (key == null) {
            return false;
        }
        int value = key.getValue();
        if (value < 0) {
            return false;
        }
        long handle = Minecraft.getInstance().getWindow().handle();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, value) == 1;
        }
        return GLFW.glfwGetKey(handle, value) == 1;
    }

    public static List<KeyMapping> all() {
        ArrayList<KeyMapping> list = new ArrayList<KeyMapping>();
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
        if (pingGroup != null) {
            list.add(pingGroup);
        }
        return list;
    }
}

