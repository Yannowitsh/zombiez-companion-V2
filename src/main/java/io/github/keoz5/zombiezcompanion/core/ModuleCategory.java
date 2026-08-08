package io.github.keoz5.zombiezcompanion.core;

import java.util.Locale;
import net.minecraft.text.Text;

public enum ModuleCategory {
    MAP,
    EVENTS,
    PROGRESSION,
    PLAYERS,
    COMFORT;


    public String displayName() {
        return Text.translatable((String)("zombiezcompanion.category." + this.name().toLowerCase(Locale.ROOT))).getString();
    }
}

