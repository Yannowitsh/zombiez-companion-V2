package io.github.keoz5.zombiezcompanion.core;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum ModuleCategory {
    MAP,
    EVENTS,
    PROGRESSION,
    PLAYERS,
    COMFORT;


    public String displayName() {
        return Component.translatable((String)("zombiezcompanion.category." + this.name().toLowerCase(Locale.ROOT))).getString();
    }
}

