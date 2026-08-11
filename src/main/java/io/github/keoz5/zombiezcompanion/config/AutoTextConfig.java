package io.github.keoz5.zombiezcompanion.config;

import java.util.ArrayList;
import java.util.List;

public final class AutoTextConfig {
    public static final int UNBOUND_KEY = -1;
    /** Hard cap on presets. A new empty slot is offered until this is reached. */
    public static final int MAX_PRESETS = 100;

    /** Unified preset list: each entry can fire on a keybind and/or appear as a clickable icon in the chat bar. */
    public List<Preset> presets = new ArrayList<Preset>();

    // --- Clickable chat bar settings (rendered when the chat is open) ---
    public boolean barEnabled = true;
    public boolean barOnlyWhenChatOpen = true;
    public String barOrientation = "horizontal"; // "horizontal" | "vertical"
    public int barIconSize = 18;
    public boolean barShowBackground = true;

    // --- Legacy fields (migrated into presets on first load, then ignored) ---
    public List<Entry> entries;
    public String text = "";
    public int keyCode = -1;

    /** A single preset: a message or /command, optionally bound to a key and/or shown in the chat bar. */
    public static final class Preset {
        public String id = "";
        public String name = "";
        public String text = "";
        /** Item shown as the bar icon (registry id, e.g. "minecraft:paper"). */
        public String itemId = "minecraft:paper";
        public int keyCode = -1;
        public boolean showInBar = true;
        public int color = 0xFFFFFFFF;
        public int backgroundColor = 0x80000000;
    }

    /** Legacy pre-rework entry (text + keybind). Kept only so old configs can be migrated. */
    public static final class Entry {
        public String text = "";
        public int keyCode = -1;
    }
}
