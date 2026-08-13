package io.github.keoz5.zombiezcompanion.modules.minievents;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Configurable spawn-alert sounds. Uses built-in vanilla sounds only (no bundled assets), played by id via
 * {@link SoundEvent#createVariableRangeEvent} so there is no registry lookup or {@code Holder} typing to worry
 * about across MC versions. The options screen cycles through {@link #IDS}.
 */
public final class SpawnSounds {
    /** The "no sound" sentinel — the first entry the picker offers, disables the alert. */
    public static final String NONE = "";

    /** Curated, varied vanilla alert sounds selectable in the options (World Boss / Marchand / Monarque). */
    public static final String[] IDS = {
        "minecraft:block.note_block.pling",
        "minecraft:block.note_block.chime",
        "minecraft:block.note_block.bell",
        "minecraft:block.note_block.harp",
        "minecraft:block.note_block.flute",
        "minecraft:block.note_block.bit",
        "minecraft:block.note_block.cow_bell",
        "minecraft:block.note_block.didgeridoo",
        "minecraft:block.amethyst_block.chime",
        "minecraft:block.bell.resonate",
        "minecraft:block.beacon.activate",
        "minecraft:block.beacon.power_select",
        "minecraft:block.conduit.activate",
        "minecraft:block.enchantment_table.use",
        "minecraft:block.respawn_anchor.charge",
        "minecraft:block.end_portal.spawn",
        "minecraft:block.anvil.land",
        "minecraft:item.trident.thunder",
        "minecraft:item.goat_horn.sound.1",
        "minecraft:item.goat_horn.sound.4",
        "minecraft:item.goat_horn.sound.6",
        "minecraft:entity.experience_orb.pickup",
        "minecraft:entity.player.levelup",
        "minecraft:entity.villager.yes",
        "minecraft:entity.evoker.prepare_summon",
        "minecraft:entity.warden.sonic_boom",
        "minecraft:entity.wither.shoot",
        "minecraft:entity.ender_dragon.flap",
        "minecraft:entity.lightning_bolt.thunder",
        "minecraft:entity.firework_rocket.blast",
        "minecraft:ui.button.click",
        "minecraft:ui.toast.in",
    };

    private SpawnSounds() {
    }

    /** Plays a vanilla sound by id at {@code volume} (0..1). No-op if disabled/blank/silent. */
    public static void play(String id, float volume) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || id == null || id.isBlank() || volume <= 0.0f) {
            return;
        }
        Identifier rl = Identifier.tryParse((String)id);
        if (rl == null) {
            return;
        }
        SoundEvent se = SoundEvent.createVariableRangeEvent(rl);
        mc.player.playSound(se, Math.min(1.0f, volume), 1.0f);
    }

    /** The next sound id in the list (wraps), used by the options selector. */
    public static String next(String id) {
        int i = SpawnSounds.indexOf(id);
        return IDS[(i + 1) % IDS.length];
    }

    /** A short, language-neutral label for a sound id (its last two dot segments, e.g. "raid.horn"). */
    public static String label(String id) {
        if (id == null || id.isBlank()) {
            return "?";
        }
        String s = id.contains(":") ? id.substring(id.indexOf(58) + 1) : id;
        String[] parts = s.split("\\.");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "." + parts[parts.length - 1];
        }
        return s;
    }

    private static int indexOf(String id) {
        for (int i = 0; i < IDS.length; ++i) {
            if (IDS[i].equals(id)) {
                return i;
            }
        }
        return 0;
    }
}
