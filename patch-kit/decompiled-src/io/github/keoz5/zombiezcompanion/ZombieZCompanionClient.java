/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
 *  net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion;

import io.github.keoz5.zombiezcompanion.command.Commands;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.event.EventBus;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.log.Log;
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
import net.minecraft.class_310;
import net.minecraft.class_437;

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
            class_310 client = class_310.method_1551();
            client.method_1507((class_437)new ConfigScreen(null, configManager, moduleManager));
        }, () -> class_310.method_1551().method_1507((class_437)new ZombieZMapScreen(configManager)), () -> {
            if (moduleManager.isEnabled("waypoints")) {
                class_310.method_1551().method_1507((class_437)new WaypointManagerScreen(null, configManager));
            }
        }, () -> class_310.method_1551().method_1507((class_437)new StatsScreen(null, configManager)), () -> {
            if (ZombieZCompanionClient.configManager.get().map.guideTarget != null) {
                ZombieZCompanionClient.configManager.get().map.guideTarget = null;
                configManager.save();
            }
        }, () -> {
            SkullsModule sm = SkullsModule.get();
            if (sm != null && moduleManager.isEnabled("skulls")) {
                class_310.method_1551().method_1507((class_437)new SkullsManagerScreen(null, configManager, sm));
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
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> moduleManager.onHudRender(drawContext, tickCounter.method_60637(true)));
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
        ZombieZMapData.Refuge r;
        double[] tgt;
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.method_1562() == null) {
            return;
        }
        double x = mc.field_1724.method_23317();
        double z = mc.field_1724.method_23321();
        MiniEventsModule me = MiniEventsModule.get();
        if (me != null && (tgt = me.activeEventTarget()) != null) {
            x = tgt[0];
            z = tgt[1];
        }
        if ((r = ZombieZMapData.nearestRefuge(x, z)) == null) {
            return;
        }
        int n = Math.max(0, r.order() - 1);
        mc.method_1562().method_45730("refuge tp " + n);
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

