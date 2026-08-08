/*
 * Decompiled with CFR 0.152.
 */
package io.github.keoz5.zombiezcompanion.modules.dropalert;

public enum DropRarity {
    COMMON("common", 0xFFFFFF),
    UNCOMMON("uncommon", 0x55FF55),
    RARE("rare", 0x5555FF),
    EPIC("epic", 0xAA00AA),
    LEGENDARY("legendary", 0xFFAA00),
    MYTHIC("mythic", 0xFF55FF),
    EXALTED("exalted", 0xFF5555),
    PRIMAL("primal", 0xAA0000);

    public final String key;
    public final int colorRgb;

    private DropRarity(String key, int colorRgb) {
        this.key = key;
        this.colorRgb = colorRgb;
    }
}

