package io.github.keoz5.zombiezcompanion.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.log.Log;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class Commands {
    private Commands() {
    }

    public static void register(ConfigManager configManager, ModuleManager moduleManager) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal((String)"zzc").then(ClientCommandManager.literal((String)"debug").executes(ctx -> {
            boolean next;
            configManager.get().debugMode = next = !configManager.get().debugMode;
            configManager.save();
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Text)Text.translatable((String)"zombiezcompanion.command.debug.state", (Object[])new Object[]{Commands.stateText(next)}));
            Log.info("Debug mode " + (next ? "ON" : "OFF"));
            return 1;
        }))).then(ClientCommandManager.literal((String)"status").executes(ctx -> {
            StringBuilder sb = new StringBuilder();
            sb.append(Text.translatable((String)"zombiezcompanion.command.status.header").getString()).append('\n');
            for (Module m : moduleManager.modules()) {
                sb.append(Text.translatable((String)"zombiezcompanion.command.status.module_line", (Object[])new Object[]{m.id(), Commands.stateText(moduleManager.isEnabled(m.id()))}).getString()).append('\n');
            }
            sb.append(Text.translatable((String)"zombiezcompanion.command.status.debug_line", (Object[])new Object[]{Commands.stateText(configManager.get().debugMode)}).getString());
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Text)Text.literal((String)sb.toString()));
            return 1;
        }))).then(ClientCommandManager.literal((String)"reload").executes(ctx -> {
            configManager.save();
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Text)Text.translatable((String)"zombiezcompanion.command.reload.saved"));
            return 1;
        }))));
    }

    private static String stateText(boolean enabled) {
        return Text.translatable((String)(enabled ? "zombiezcompanion.command.state.on" : "zombiezcompanion.command.state.off")).getString();
    }
}

