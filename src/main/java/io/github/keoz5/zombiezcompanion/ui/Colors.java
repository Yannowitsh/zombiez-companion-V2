package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ColorsConfig;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import java.util.List;

/**
 * Central registry and accessor for player-configurable element colors (mutant frame, event pings…).
 * Colors are stored as ARGB overrides in {@link ColorsConfig}; absent = the code default returned here.
 * The alpha byte is honored where it makes sense (e.g. the mutant frame fill); line/beacon renders
 * force their own opacity. Ids match {@code MiniEventsModule.MiniEventType.key} for events, plus
 * "mutant_frame".
 */
public final class Colors {
    private Colors() {
    }

    public record Element(String id, String langKey, int def) {
    }

    // Defaults are ARGB. Events are opaque (their renders ignore alpha anyway); the mutant frame keeps
    // a low default alpha because that byte controls its translucent fill.
    public static final List<Element> ELEMENTS = List.of(
            new Element("mutant_frame", "zombiezcompanion.colors.mutant_frame", 0x33AA33FF),
            new Element("world_boss", "zombiezcompanion.colors.world_boss", 0xFF000000 | 0xFF3030),
            new Element("marchand", "zombiezcompanion.colors.marchand", 0xFF000000 | 16096779),
            new Element("faille", "zombiezcompanion.colors.faille", 0xFF000000 | 12616956),
            new Element("fuyeur", "zombiezcompanion.colors.fuyeur", 0xFF000000 | 16766720),
            new Element("colis", "zombiezcompanion.colors.colis", 0xFF000000 | 3528703),
            new Element("pinata", "zombiezcompanion.colors.pinata", 0xFF000000 | 15485081),
            new Element("bombe", "zombiezcompanion.colors.bombe", 0xFF000000 | 14427686),
            new Element("jackpot", "zombiezcompanion.colors.jackpot", 0xFF000000 | 1096065),
            new Element("assaut", "zombiezcompanion.colors.assaut", 0xFF000000 | 15381256));

    // Curated palette cycled through by the Colors screen (RGB). Includes every element default.
    public static final int[] PALETTE = {
            0xFF3030, 0xFF8800, 0xFFCB00, 0xFFE84D, 0x7CFC00, 0x10BC01, 0x00E5A0, 0x35DCFF,
            0x3B82F6, 0xAA33FF, 0xC086BC, 0xFF4FD8, 0xEC4E99, 0xF5A60B, 0xFFFFFF, 0x9AA0A6};

    private static ColorsConfig cfg() {
        ConfigManager cm = ZombieZCompanionClient.configManager();
        return cm != null ? cm.get().colors : null;
    }

    /** The element's ARGB color: the player's override if set, otherwise the built-in default. */
    public static int get(String id, int def) {
        ColorsConfig cc = Colors.cfg();
        Integer v = cc != null && cc.overrides != null ? cc.overrides.get(id) : null;
        return v != null ? v.intValue() : def;
    }

    public static void set(String id, int argb) {
        Colors.setNoSave(id, argb);
        ZombieZCompanionClient.configManager().save();
    }

    /** Live in-memory change (for smooth previewing while dragging); persist later with {@code save()}. */
    public static void setNoSave(String id, int argb) {
        ColorsConfig cc = Colors.cfg();
        if (cc != null) {
            cc.overrides.put(id, argb);
        }
    }

    public static void reset(String id) {
        ColorsConfig cc = Colors.cfg();
        if (cc == null) {
            return;
        }
        cc.overrides.remove(id);
        ZombieZCompanionClient.configManager().save();
    }
}
