package io.github.keoz5.zombiezcompanion.command;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.log.Log;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.modules.mobsensor.MobSensorModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

public final class Commands {
    private Commands() {
    }

    public static void register(ConfigManager configManager, ModuleManager moduleManager) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(ClientCommands.literal("zzc")
            .then(ClientCommands.literal("debug").executes(ctx -> {
                boolean next;
                configManager.get().debugMode = next = !configManager.get().debugMode;
                configManager.save();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"zombiezcompanion.command.debug.state", (Object[])new Object[]{Commands.stateText(next)}));
                Log.info("Debug mode " + (next ? "ON" : "OFF"));
                return 1;
            }))
            .then(ClientCommands.literal("status").executes(ctx -> {
                StringBuilder sb = new StringBuilder();
                sb.append(Component.translatable((String)"zombiezcompanion.command.status.header").getString()).append('\n');
                for (Module m : moduleManager.modules()) {
                    sb.append(Component.translatable((String)"zombiezcompanion.command.status.module_line", (Object[])new Object[]{m.id(), Commands.stateText(moduleManager.isEnabled(m.id()))}).getString()).append('\n');
                }
                sb.append(Component.translatable((String)"zombiezcompanion.command.status.debug_line", (Object[])new Object[]{Commands.stateText(configManager.get().debugMode)}).getString());
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.literal((String)sb.toString()));
                return 1;
            }))
            .then(ClientCommands.literal("reload").executes(ctx -> {
                configManager.save();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.translatable((String)"zombiezcompanion.command.reload.saved"));
                return 1;
            }))
            .then(ClientCommands.literal("scanmobs").executes(ctx -> Commands.scanMobs((FabricClientCommandSource)ctx.getSource())))
            .then(ClientCommands.literal("scanlook").executes(ctx -> Commands.scanLook((FabricClientCommandSource)ctx.getSource())))
            .then(ClientCommands.literal("guidump").executes(ctx -> {
                GuiDump.arm();
                ((FabricClientCommandSource)ctx.getSource()).sendFeedback((Component)Component.literal((String)"§b[ZZC] guidump armé §7— ouvre le marchand, le contenu sera écrit dans §f~/zzc-guidump.txt"));
                return 1;
            }))
        ));
    }

    // Under Lunar Client (Ichor+Genesis) the mod's SLF4J logs do NOT reach latest.log, so the scan
    // report is written to a file in the user's home dir where it can be shared/read directly.
    private static final Path SCAN_FILE = Paths.get(System.getProperty("user.home"), "zzc-scan.txt");

    /**
     * Diagnostic dump of ALL nearby entities (not just living: armor stands, display/marker entities
     * too), appended to {@code ~/zzc-scan.txt}. Per entity: type, distance, display/custom name,
     * scoreboard tags, team, invisibility, vehicle/passengers; for living entities hp/size/glow +
     * mutant verdict; for armor stands marker/small + equipment (head/chest/hand). Used to find the
     * real "mutant" signal (e.g. a scoreboard tag, a team, or an armor-stand-based custom mob).
     */
    private static int scanMobs(FabricClientCommandSource src) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            src.sendError((Component)Component.literal("[ZZC] Pas en jeu."));
            return 0;
        }
        Player self = mc.player;
        double range = 100.0;
        List<Entity> ents = mc.level.getEntitiesOfClass(Entity.class, self.getBoundingBox().inflate(range), e -> !e.isRemoved() && e != self && !(e instanceof Player));
        ents.sort(Comparator.comparingDouble(e -> e.distanceToSqr(self)));
        String host = mc.getCurrentServer() != null && mc.getCurrentServer().ip != null ? mc.getCurrentServer().ip : "-";
        StringBuilder sb = new StringBuilder();
        sb.append("=== scanmobs @ ").append(java.time.LocalTime.now().withNano(0))
          .append(" host=").append(host).append(" zombiez=").append(ZombieZDetector.isOnZombieZ())
          .append(" pos=").append((int)self.getX()).append(',').append((int)self.getY()).append(',').append((int)self.getZ())
          .append(" entities=").append(ents.size()).append(" ===\n");
        // Type histogram: a quick overview that stays readable even when the list is huge.
        Map<String, Integer> hist = new TreeMap<String, Integer>();
        for (Entity e : ents) {
            hist.merge(EntityType.getKey(e.getType()).toString(), 1, Integer::sum);
        }
        sb.append("types:");
        for (Map.Entry<String, Integer> en : hist.entrySet()) {
            sb.append(' ').append(en.getKey()).append('=').append(en.getValue());
        }
        sb.append('\n');
        int shown = 0;
        for (Entity e : ents) {
            sb.append(Commands.describe(e, self)).append('\n');
            if (++shown >= 1000) {
                sb.append("… (capped at 1000)\n");
                break;
            }
        }
        return Commands.writeScan(src, sb, "scanmobs", ents.size());
    }

    /**
     * Dumps the entity the player is looking at (ray-picked up to 64 blocks) plus every entity within
     * 5 blocks of it, to {@code ~/zzc-scan.txt}. Aim at a mutant and run it: the picked hitbox is
     * usually the custom mob's {@code interaction}/base entity, and the neighborhood contains its
     * display-entity rig (model item_displays + the name text_display). Precise, no clutter.
     */
    private static int scanLook(FabricClientCommandSource src) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            src.sendError((Component)Component.literal("[ZZC] Pas en jeu."));
            return 0;
        }
        Player self = mc.player;
        double reach = 64.0;
        Vec3 eye = self.getEyePosition(1.0f);
        Vec3 look = self.getViewVector(1.0f);
        Vec3 dir = new Vec3(look.x * reach, look.y * reach, look.z * reach);
        Vec3 end = eye.add(dir.x, dir.y, dir.z);
        AABB searchBox = self.getBoundingBox().expandTowards(dir.x, dir.y, dir.z).inflate(1.0);
        List<Entity> cand = mc.level.getEntitiesOfClass(Entity.class, searchBox, e -> !e.isRemoved() && e != self);
        Entity target = null;
        double bestSq = Double.MAX_VALUE;
        for (Entity e : cand) {
            Optional<Vec3> clip = e.getBoundingBox().inflate(0.2).clip(eye, end);
            if (clip.isEmpty()) continue;
            double d = clip.get().distanceToSqr(eye);
            if (d < bestSq) {
                bestSq = d;
                target = e;
            }
        }
        if (target == null) {
            src.sendError((Component)Component.literal("§e[ZZC] scanlook: aucune entité visée (vise le mutant à moins de 64m)."));
            return 0;
        }
        Entity tgt = target;
        StringBuilder sb = new StringBuilder();
        sb.append("=== scanlook @ ").append(java.time.LocalTime.now().withNano(0))
          .append(" dist=").append((int)Math.round(Math.sqrt(bestSq))).append("m ===\n");
        sb.append("TARGET  ").append(Commands.describe(tgt, self)).append('\n');
        List<Entity> around = mc.level.getEntitiesOfClass(Entity.class, tgt.getBoundingBox().inflate(5.0), e -> !e.isRemoved() && e != self && e != tgt);
        around.sort(Comparator.comparingDouble(e -> e.distanceToSqr(tgt)));
        for (Entity e : around) {
            sb.append("  near  ").append(Commands.describe(e, self)).append('\n');
        }
        return Commands.writeScan(src, sb, "scanlook[" + EntityType.getKey(tgt.getType()) + "]", around.size() + 1);
    }

    private static int writeScan(FabricClientCommandSource src, CharSequence report, String label, int count) {
        try {
            Files.writeString(SCAN_FILE, report, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            src.sendError((Component)Component.literal("§c[ZZC] écriture scan échouée: " + ex.getMessage()));
            return 0;
        }
        src.sendFeedback((Component)Component.literal("§b[ZZC] " + label + " §7→ §f" + SCAN_FILE + " §7(" + count + ")"));
        return 1;
    }

    /** One diagnostic line for an entity: identity, tags, team, rig links and type-specific detail. */
    private static String describe(Entity e, Player self) {
        String disp = e.getDisplayName() != null ? e.getDisplayName().getString() : "";
        String custom = e.getCustomName() != null ? e.getCustomName().getString() : "";
        Set<String> tags = e.entityTags();
        String tagStr = tags.isEmpty() ? "-" : String.join(",", tags);
        Team team = e.getTeam();
        String teamStr = team != null ? team.getName() : "-";
        int dist = (int)Math.round(Math.sqrt(e.distanceToSqr(self)));
        StringBuilder sb = new StringBuilder();
        sb.append(dist).append("m | ").append(EntityType.getKey(e.getType()))
          .append(" | disp='").append(disp).append("' | custom='").append(custom).append("'")
          .append(" | tags=[").append(tagStr).append("] | team=").append(teamStr)
          .append(" | invis=").append(e.isInvisible());
        Entity veh = e.getVehicle();
        if (veh != null) {
            sb.append(" | veh=").append(EntityType.getKey(veh.getType()));
        }
        if (!e.getPassengers().isEmpty()) {
            sb.append(" | pass=").append(e.getPassengers().size());
        }
        if (e instanceof LivingEntity le) {
            sb.append(String.format(Locale.ROOT, " | hp=%.0f/%.0f | size=%.2fx%.2f | glow=%b | mutant=%b",
                    le.getHealth(), le.getMaxHealth(), le.getBbWidth(), le.getBbHeight(), le.isCurrentlyGlowing(), MobSensorModule.isMutant(le)));
        }
        if (e instanceof ArmorStand as) {
            sb.append(" | STAND marker=").append(as.isMarker()).append(" small=").append(as.isSmall())
              .append(" head='").append(itemDesc(as.getItemBySlot(EquipmentSlot.HEAD))).append("'")
              .append(" chest='").append(itemDesc(as.getItemBySlot(EquipmentSlot.CHEST))).append("'")
              .append(" hand='").append(itemDesc(as.getItemBySlot(EquipmentSlot.MAINHAND))).append("'");
        }
        // Custom mobs are often "rigged" from display entities: the visible name lives in a
        // text_display's text (NOT customName), and the model in item_displays. Extract both.
        if (e instanceof Display.TextDisplay td) {
            sb.append(" | TEXT='").append(td.textRenderState().text().getString().replace('\n', ' ')).append("'");
        }
        if (e instanceof Display.ItemDisplay id) {
            sb.append(" | ITEM=").append(itemDesc(id.getSlot(0).get()));
        }
        return sb.toString();
    }

    private static String itemDesc(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "-";
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return stack.getCount() + "x " + id + " '" + stack.getHoverName().getString() + "'";
    }

    private static String stateText(boolean enabled) {
        return Component.translatable((String)(enabled ? "zombiezcompanion.command.state.on" : "zombiezcompanion.command.state.off")).getString();
    }
}
