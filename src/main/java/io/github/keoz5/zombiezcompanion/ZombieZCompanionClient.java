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
import io.github.keoz5.zombiezcompanion.modules.brightness.BrightnessModule;
import io.github.keoz5.zombiezcompanion.modules.consumables.ConsumablesModule;
import io.github.keoz5.zombiezcompanion.modules.coordinates.CoordinatesModule;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropAlertModule;
import io.github.keoz5.zombiezcompanion.modules.map.MiniMapModule;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointManagerScreen;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapData;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapScreen;
import io.github.keoz5.zombiezcompanion.modules.minievents.MiniEventsModule;
import io.github.keoz5.zombiezcompanion.modules.players.PlayersModule;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsManagerScreen;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsModule;
import io.github.keoz5.zombiezcompanion.modules.stats.StatsModule;
import io.github.keoz5.zombiezcompanion.modules.telemetry.TelemetryModule;
import io.github.keoz5.zombiezcompanion.modules.zoom.ZoomModule;
import io.github.keoz5.zombiezcompanion.ui.ConfigScreen;
import io.github.keoz5.zombiezcompanion.ui.StatsScreen;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class ZombieZCompanionClient
implements ClientModInitializer {
    private static ConfigManager configManager;
    private static ModuleManager moduleManager;
    private static EventBus eventBus;

    public void onInitializeClient() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("zombiezcompanion");
        Log.bindDebugFlag(() -> configManager != null && ZombieZCompanionClient.configManager.get().debugMode);
        configManager = new ConfigManager(configDir);
        eventBus = new EventBus();
        moduleManager = new ModuleManager(configManager, new ModuleContext(configManager, eventBus));
        ZombieZCompanionClient.registerHudElements();
        ZombieZCompanionClient.registerModules(moduleManager);
        this.registerFabricHooks();
        Commands.register(configManager, moduleManager);
        Keybinds.register(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen((Screen)new ConfigScreen(null, configManager, moduleManager));
        }, () -> MinecraftClient.getInstance().setScreen((Screen)new ZombieZMapScreen(configManager)), () -> {
            if (moduleManager.isEnabled("waypoints")) {
                MinecraftClient.getInstance().setScreen((Screen)new WaypointManagerScreen(null, configManager));
            }
        }, () -> MinecraftClient.getInstance().setScreen((Screen)new StatsScreen(null, configManager)), () -> {
            if (ZombieZCompanionClient.configManager.get().map.guideTarget != null) {
                ZombieZCompanionClient.configManager.get().map.guideTarget = null;
                configManager.save();
            }
        }, () -> {
            SkullsModule sm = SkullsModule.get();
            if (sm != null && moduleManager.isEnabled("skulls")) {
                MinecraftClient.getInstance().setScreen((Screen)new SkullsManagerScreen(null, configManager, sm));
            }
        }, ZombieZCompanionClient::tpToEventRefuge);
        moduleManager.startEnabledModules();
        configManager.save();
        Log.info("ZombieZ Companion initialized \u2014 " + moduleManager.modules().size() + " module(s), debug=" + ZombieZCompanionClient.configManager.get().debugMode);
    }

    private static void registerHudElements() {
        HudElements.register("mini_map", "zombiezcompanion.hud.element.mini_map", 80, 80, 1.0, 1.0, true);
        HudElements.register("coordinates", "zombiezcompanion.hud.element.coordinates", 96, 24, 0.0, 0.0, true);
        HudElements.register("mini_events_toast", "zombiezcompanion.hud.element.mini_events_toast", 220, 32, 0.5, 0.08, true);
        HudElements.register("drop_notifications", "zombiezcompanion.hud.element.drop_notifications", 220, 25, 0.0, 0.5, true);
        HudElements.register("marchand_timer", "zombiezcompanion.hud.element.marchand_timer", 150, 16, 0.0, 0.3, true);
        HudElements.register("world_boss_timer", "zombiezcompanion.hud.element.world_boss_timer", 150, 16, 0.0, 0.36, true);
        HudElements.register("lure_timer", "zombiezcompanion.hud.element.lure_timer", 110, 18, 0.0, 0.42, true);
        HudElements.register("flower_timer", "zombiezcompanion.hud.element.flower_timer", 150, 18, 0.0, 0.48, true);
    }

    private static void registerModules(ModuleManager mm) {
        mm.register(new BrightnessModule());
        mm.register(new MiniMapModule());
        mm.register(new WaypointsModule());
        mm.register(new AutoTextModule());
        mm.register(new ZoomModule());
        mm.register(new DropAlertModule());
        mm.register(new MiniEventsModule());
        mm.register(new StatsModule());
        mm.register(new SkullsModule());
        mm.register(new TelemetryModule());
        mm.register(new PlayersModule());
        mm.register(new CoordinatesModule());
        mm.register(new ConsumablesModule());
    }

    private void registerFabricHooks() {
        ClientTickEvents.END_CLIENT_TICK.register(moduleManager::onClientTick);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> moduleManager.onHudRender(drawContext, tickCounter.getTickDelta(true)));
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) {
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
                mc.getNetworkHandler().sendChatCommand("refuge tp w2");
                return;
            }
            ZombieZMapData.Refuge r = ZombieZMapData.nearestRefuge(tgt[0], tgt[1]);
            if (r == null) {
                return;
            }
            int n = Math.max(0, r.order() - 1);
            Log.debug(LogCategory.EVENT, "quick-tp: active event on map 1 -> refuge tp " + n);
            mc.getNetworkHandler().sendChatCommand("refuge tp " + n);
            return;
        }
        // 2) No active event: route by the player's current dimension.
        String playerDim = mc.world != null ? mc.world.getRegistryKey().getValue().toString() : null;
        if (ZombieZMapData.DIM_MAP2.equals(playerDim)) {
            Log.debug(LogCategory.EVENT, "quick-tp: player on map 2 -> refuge tp w2");
            mc.getNetworkHandler().sendChatCommand("refuge tp w2");
            return;
        }
        ZombieZMapData.Refuge r = ZombieZMapData.nearestRefuge(mc.player.getX(), mc.player.getZ());
        if (r == null) {
            return;
        }
        int n = Math.max(0, r.order() - 1);
        Log.debug(LogCategory.EVENT, "quick-tp: player on map 1 -> refuge tp " + n);
        mc.getNetworkHandler().sendChatCommand("refuge tp " + n);
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

