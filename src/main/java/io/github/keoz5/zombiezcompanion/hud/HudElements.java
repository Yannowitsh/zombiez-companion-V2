package io.github.keoz5.zombiezcompanion.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudElements {
    public static final String COORDINATES = "coordinates";
    public static final String MINI_EVENTS_TOAST = "mini_events_toast";
    public static final String DROP_NOTIFICATIONS = "drop_notifications";
    public static final String MARCHAND_TIMER = "marchand_timer";
    public static final String WORLD_BOSS_TIMER = "world_boss_timer";
    public static final String LURE_TIMER = "lure_timer";
    public static final String FLOWER_TIMER = "flower_timer";
    private static final Map<String, Element> ELEMENTS = new LinkedHashMap<String, Element>();

    private HudElements() {
    }

    public static void register(String id, String labelKey, int defaultW, int defaultH, double defaultFx, double defaultFy, boolean scalable) {
        ELEMENTS.computeIfAbsent(id, k -> new Element(id, labelKey, defaultW, defaultH, defaultFx, defaultFy, scalable));
    }

    public static void report(String id, int x, int y, int w, int h) {
        Element e = ELEMENTS.get(id);
        if (e == null) {
            return;
        }
        e.x = x;
        e.y = y;
        e.w = w;
        e.h = h;
        e.reportedAt = System.currentTimeMillis();
    }

    public static List<Element> all() {
        return new ArrayList<Element>(ELEMENTS.values());
    }

    public static Element get(String id) {
        return ELEMENTS.get(id);
    }

    public static final class Element {
        public final String id;
        public final String labelKey;
        public final int defaultW;
        public final int defaultH;
        public final double defaultFx;
        public final double defaultFy;
        public final boolean scalable;
        public int x;
        public int y;
        public int w;
        public int h;
        public long reportedAt;

        Element(String id, String labelKey, int dw, int dh, double dfx, double dfy, boolean scalable) {
            this.id = id;
            this.labelKey = labelKey;
            this.defaultW = dw;
            this.defaultH = dh;
            this.defaultFx = dfx;
            this.defaultFy = dfy;
            this.scalable = scalable;
            this.w = dw;
            this.h = dh;
        }
    }
}

