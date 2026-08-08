/*
 * Decompiled with CFR 0.152.
 */
package io.github.keoz5.zombiezcompanion.config;

import java.util.ArrayList;
import java.util.List;

public final class AutoTextConfig {
    public static final int UNBOUND_KEY = -1;
    public static final int MAX_ENTRIES = 5;
    public List<Entry> entries = new ArrayList<Entry>();
    public String text = "";
    public int keyCode = -1;

    public static final class Entry {
        public String text = "";
        public int keyCode = -1;
    }
}

