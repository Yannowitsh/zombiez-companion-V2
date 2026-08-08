/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 */
package io.github.keoz5.zombiezcompanion.core;

import java.util.Locale;
import net.minecraft.class_2561;

public enum ModuleCategory {
    MAP,
    EVENTS,
    PROGRESSION,
    PLAYERS,
    COMFORT;


    public String displayName() {
        return class_2561.method_43471((String)("zombiezcompanion.category." + this.name().toLowerCase(Locale.ROOT))).getString();
    }
}

