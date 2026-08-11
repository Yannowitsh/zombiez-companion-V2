package io.github.keoz5.zombiezcompanion;

import io.github.keoz5.zombiezcompanion.command.Commands;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.event.EventBus;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.log.Log;
import io.github.keoz5.zombiezcompanion.log.LogCategory;
import io.github.keoz5.zombiezcompanion.modules.autotext.AutoTextModule;
import io.github.keoz5.zombiezcompanion.modules.consumables.ConsumablesModule;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropAlertModule;
import io.github.keoz5.zombiezcompanion.modules.friends.FriendsModule;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointManagerScreen;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapData;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapScreen;
import io.github.keoz5.zombiezcompanion.modules.minievents.MiniEventsModule;
import io.github.keoz5.zombiezcompanion.modules.mobsensor.MobSensorModule;
import io.github.keoz5.zombiezcompanion.modules.players.PlayersModule;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsManagerScreen;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsModule;
import io.github.keoz5.zombiezcompanion.modules.stats.StatsModule;
import io.github.keoz5.zombiezcompanion.modules.telemetry.TelemetryModule;
import io.github.keoz5.zombiezcompanion.ui.ConfigScreen;
import io.github.keoz5.zombiezcompanion.ui.StatsScreen;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ZombieZCompanionClient
implements ClientModInitializer {
    private static ConfigManager configManager;
    private static ModuleManager moduleManager;
    private static EventBus eventBus;

    public void onInitializeClient() {
        Path configDir = ZombieZCompanionClient.resolveConfigDir();
        Log.bindDebugFlag(() -> configManager != null && ZombieZCompanionClient.configManager.get().debugMode);
        configManager = new ConfigManager(configDir);
        eventBus = new EventBus();
        moduleManager = new ModuleManager(configManager, new ModuleContext(configManager, eventBus));
        ZombieZCompanionClient.registerHudElements();
        ZombieZCompanionClient.registerModules(moduleManager);
        this.registerFabricHooks();
        Commands.register(configManager, moduleManager);
        Keybinds.register(() -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen((Screen)new ConfigScreen(null, configManager, moduleManager));
        }, () -> Minecraft.getInstance().setScreen((Screen)new ZombieZMapScreen(configManager)), () -> {
            if (moduleManager.isEnabled("waypoints")) {
                Minecraft.getInstance().setScreen((Screen)new WaypointManagerScreen(null, configManager));
            }
        }, () -> Minecraft.getInstance().setScreen((Screen)new StatsScreen(null, configManager)), () -> {
            if (ZombieZCompanionClient.configManager.get().map.guideTarget != null) {
                ZombieZCompanionClient.configManager.get().map.guideTarget = null;
                configManager.save();
            }
        }, () -> {
            SkullsModule sm = SkullsModule.get();
            if (sm != null && moduleManager.isEnabled("skulls")) {
                Minecraft.getInstance().setScreen((Screen)new SkullsManagerScreen(null, configManager, sm));
            }
        }, ZombieZCompanionClient::tpToEventRefuge);
        moduleManager.startEnabledModules();
        configManager.save();
        Log.info("ZombieZ Companion initialized \u2014 " + moduleManager.modules().size() + " module(s), debug=" + ZombieZCompanionClient.configManager.get().debugMode);
    }

    private static void registerHudElements() {
        HudElements.register("mini_events_toast", "zombiezcompanion.hud.element.mini_events_toast", 220, 32, 0.5, 0.08, true);
        HudElements.register("drop_notifications", "zombiezcompanion.hud.element.drop_notifications", 220, 25, 0.0, 0.5, true);
        HudElements.register("marchand_timer", "zombiezcompanion.hud.element.marchand_timer", 150, 16, 0.0, 0.3, true);
        HudElements.register("world_boss_timer", "zombiezcompanion.hud.element.world_boss_timer", 150, 16, 0.0, 0.36, true);
        HudElements.register("lure_timer", "zombiezcompanion.hud.element.lure_timer", 110, 18, 0.0, 0.42, true);
        HudElements.register("flower_timer", "zombiezcompanion.hud.element.flower_timer", 150, 18, 0.0, 0.48, true);
        HudElements.register("mutant_sensor", "zombiezcompanion.hud.element.mutant_sensor", 140, 16, 0.0, 0.54, true);
    }

    /**
     * Config lives in a per-brand folder. On first launch of V2, if the new folder does not
     * exist yet, seed it from the legacy "zombiezcompanion" folder so existing settings and
     * waypoints carry over. Afterwards all reads/writes go to the V2 folder, leaving the old
     * one untouched as a backup.
     */
    private static Path resolveConfigDir() {
        Path base = FabricLoader.getInstance().getConfigDir();
        Path newDir = base.resolve("zombiezcompanionv2");
        Path oldDir = base.resolve("zombiezcompanion");
        try {
            if (!Files.exists(newDir) && Files.isDirectory(oldDir)) {
                Files.createDirectories(newDir);
                try (java.util.stream.Stream<Path> files = Files.list(oldDir)) {
                    files.filter(Files::isRegularFile).forEach(src -> {
                        try {
                            Files.copy(src, newDir.resolve(src.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
                        } catch (java.io.IOException e) {
                            Log.error("Config migration: failed to copy " + String.valueOf(src), e);
                        }
                    });
                }
                Log.info("Imported existing config 'zombiezcompanion' -> 'zombiezcompanionv2'.");
            }
        } catch (java.io.IOException e) {
            Log.error("Config directory migration failed", e);
        }
        return newDir;
    }

    private static void registerModules(ModuleManager mm) {
        mm.register(new WaypointsModule());
        mm.register(new AutoTextModule());
        mm.register(new DropAlertModule());
        mm.register(new MiniEventsModule());
        mm.register(new MobSensorModule());
        mm.register(new StatsModule());
        mm.register(new SkullsModule());
        mm.register(new TelemetryModule());
        mm.register(new PlayersModule());
        mm.register(new FriendsModule());
        mm.register(new ConsumablesModule());
    }

    private void registerFabricHooks() {
        ClientTickEvents.END_CLIENT_TICK.register(moduleManager::onClientTick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("zombiezcompanion", "hud"), (drawContext, deltaTracker) -> moduleManager.onHudRender(drawContext, deltaTracker.getGameTimeDeltaPartialTick(true)));
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> moduleManager.onChatMessage(message, false));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                moduleManager.onChatMessage(message, true);
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> moduleManager.onJoinWorld());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            moduleManager.onLeaveWorld();
            configManager.save();
        });
    }

    private static void tpToEventRefuge() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        MiniEventsModule me = MiniEventsModule.get();
        double[] tgt = me != null ? me.activeEventTarget() : null;
        // 1) An event (World Boss / Marchand) is active: route to the event's map.
        if (tgt != null) {
            String eventDim = me.activeEventDimension();
            if (ZombieZMapData.DIM_MAP2.equals(eventDim)) {
                // Map 2 has a single refuge reached via the dedicated server command.
                Log.debug(LogCategory.EVENT, "quick-tp: active event on map 2 -> refuge tp w2");
                mc.getConnection().sendCommand("refuge tp w2");
                return;
            }
            ZombieZMapData.Refuge r = ZombieZMapData.nearestRefuge(tgt[0], tgt[1]);
            if (r == null) {
                return;
            }
            int n = Math.max(0, r.order() - 1);
            Log.debug(LogCategory.EVENT, "quick-tp: active event on map 1 -> refuge tp " + n);
            mc.getConnection().sendCommand("refuge tp " + n);
            return;
        }
        // 2) No active event: route by the player's current dimension.
        String playerDim = mc.level != null ? mc.level.dimension().identifier().toString() : null;
        if (ZombieZMapData.DIM_MAP2.equals(playerDim)) {
            Log.debug(LogCategory.EVENT, "quick-tp: player on map 2 -> refuge tp w2");
            mc.getConnection().sendCommand("refuge tp w2");
            return;
        }
        ZombieZMapData.Refuge r = ZombieZMapData.nearestRefuge(mc.player.getX(), mc.player.getZ());
        if (r == null) {
            return;
        }
        int n = Math.max(0, r.order() - 1);
        Log.debug(LogCategory.EVENT, "quick-tp: player on map 1 -> refuge tp " + n);
        mc.getConnection().sendCommand("refuge tp " + n);
    }

    /** Teleport toward an arbitrary world position via the nearest refuge (map 1) or the map-2 refuge. */
    public static void quickTpTo(double x, double z, String dim) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        if (ZombieZMapData.DIM_MAP2.equals(dim)) {
            Log.debug(LogCategory.EVENT, "quick-tp: friend on map 2 -> refuge tp w2");
            mc.getConnection().sendCommand("refuge tp w2");
            return;
        }
        ZombieZMapData.Refuge r = ZombieZMapData.nearestRefuge(x, z);
        if (r == null) {
            return;
        }
        int n = Math.max(0, r.order() - 1);
        Log.debug(LogCategory.EVENT, "quick-tp: friend on map 1 -> refuge tp " + n);
        mc.getConnection().sendCommand("refuge tp " + n);
    }

    public static ConfigManager configManager() {
        return configManager;
    }

    public static ModuleManager moduleManager() {
        return moduleManager;
    }

    public static EventBus eventBus() {
        return eventBus;
    }
}

