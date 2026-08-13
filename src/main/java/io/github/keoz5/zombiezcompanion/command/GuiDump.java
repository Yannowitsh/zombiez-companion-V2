package io.github.keoz5.zombiezcompanion.command;

import io.github.keoz5.zombiezcompanion.log.Log;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropClassifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Debug-only helper to map an unknown server container GUI (here: the traveling merchant's buy screen).
 * A client command cannot run while a container is open (chat closes it), so this arms a one-shot capture
 * instead: after {@link #arm()}, the next container screen that the server has populated is dumped —
 * title, menu class, and every non-empty slot (index, item id, count, hover name, rarity, full component
 * map) — to {@code ~/zzc-guidump.txt}. We read that dump to name the exact offer slots, then build the
 * real capture. Not wired to any user-facing feature.
 */
public final class GuiDump {
    private static final Path FILE = Paths.get(System.getProperty("user.home"), "zzc-guidump.txt");
    private static volatile boolean armed;

    private GuiDump() {
    }

    /** Arm a one-shot dump of the next populated container GUI. */
    public static void arm() {
        armed = true;
    }

    public static boolean armed() {
        return armed;
    }

    /** Ticked every client tick; when armed and a populated container screen is open, dump it once. */
    public static void tick(Minecraft mc) {
        if (!armed || !(mc.screen instanceof AbstractContainerScreen<?> cs)) {
            return;
        }
        List<Slot> slots = cs.getMenu().slots;
        boolean populated = false;
        for (Slot s : slots) {
            if (!s.getItem().isEmpty()) {
                populated = true;
                break;
            }
        }
        if (!populated) {
            return; // slots arrive a tick or two after the screen opens — wait for content
        }
        armed = false;
        dump(mc, cs);
    }

    private static void dump(Minecraft mc, AbstractContainerScreen<?> cs) {
        List<Slot> slots = cs.getMenu().slots;
        StringBuilder sb = new StringBuilder();
        sb.append("=== guidump @ ").append(java.time.LocalTime.now().withNano(0))
          .append(" title='").append(cs.getTitle() == null ? "" : cs.getTitle().getString()).append("'")
          .append(" menu=").append(cs.getMenu().getClass().getName())
          .append(" slots=").append(slots.size()).append(" ===\n");
        for (int i = 0; i < slots.size(); ++i) {
            ItemStack st = slots.get(i).getItem();
            if (st.isEmpty()) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
            sb.append('[').append(i).append("] ").append(st.getCount()).append("x ").append(id)
              .append(" '").append(st.getHoverName().getString()).append("'")
              .append(" rarity=").append(DropClassifier.rarityOf(st))
              .append(" comp=").append(st.getComponents())
              .append('\n');
        }
        try {
            Files.writeString(FILE, sb, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Log.info("guidump written to " + FILE);
            if (mc.gui != null) {
                mc.gui.getChat().addClientSystemMessage((Component) Component.literal("§b[ZZC] GUI dump §7→ §f" + FILE));
            }
        } catch (IOException e) {
            Log.error("guidump write failed", e);
        }
    }
}
