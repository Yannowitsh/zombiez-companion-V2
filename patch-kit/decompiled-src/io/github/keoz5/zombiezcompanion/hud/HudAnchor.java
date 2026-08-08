/*
 * Decompiled with CFR 0.152.
 */
package io.github.keoz5.zombiezcompanion.hud;

import io.github.keoz5.zombiezcompanion.config.HudConfig;
import java.util.LinkedHashMap;

public final class HudAnchor {
    public static final double MIN_SCALE = 0.5;
    public static final double MAX_SCALE = 3.0;

    private HudAnchor() {
    }

    public static int resolveX(HudConfig cfg, String id, int screenW, int w, double defFx) {
        double fx = HudAnchor.fraction(cfg, id, true, defFx);
        int free = Math.max(0, screenW - w);
        return HudAnchor.clamp((int)Math.round(fx * (double)free), 0, free);
    }

    public static int resolveY(HudConfig cfg, String id, int screenH, int h, double defFy) {
        double fy = HudAnchor.fraction(cfg, id, false, defFy);
        int free = Math.max(0, screenH - h);
        return HudAnchor.clamp((int)Math.round(fy * (double)free), 0, free);
    }

    private static double fraction(HudConfig cfg, String id, boolean horizontal, double def) {
        HudConfig.Pos pos;
        if (cfg != null && cfg.elements != null && (pos = cfg.elements.get(id)) != null) {
            double v;
            double d = v = horizontal ? pos.fx : pos.fy;
            if (v >= 0.0) {
                return Math.min(1.0, v);
            }
        }
        return def;
    }

    public static void setPosition(HudConfig cfg, String id, int x, int y, int w, int h, int screenW, int screenH) {
        if (cfg.elements == null) {
            cfg.elements = new LinkedHashMap<String, HudConfig.Pos>();
        }
        HudConfig.Pos pos = cfg.elements.computeIfAbsent(id, k -> new HudConfig.Pos());
        int freeX = Math.max(1, screenW - w);
        int freeY = Math.max(1, screenH - h);
        pos.fx = HudAnchor.clampD((double)x / (double)freeX, 0.0, 1.0);
        pos.fy = HudAnchor.clampD((double)y / (double)freeY, 0.0, 1.0);
    }

    public static double scale(HudConfig cfg, String id) {
        HudConfig.Pos p;
        if (cfg != null && cfg.elements != null && (p = cfg.elements.get(id)) != null && p.scale > 0.0) {
            return HudAnchor.clampD(p.scale, 0.5, 3.0);
        }
        return 1.0;
    }

    public static void setScale(HudConfig cfg, String id, double scale) {
        if (cfg.elements == null) {
            cfg.elements = new LinkedHashMap<String, HudConfig.Pos>();
        }
        HudConfig.Pos pos = cfg.elements.computeIfAbsent(id, k -> new HudConfig.Pos());
        pos.scale = HudAnchor.clampD(scale, 0.5, 3.0);
    }

    public static boolean hasCustom(HudConfig cfg, String id) {
        if (cfg == null || cfg.elements == null) {
            return false;
        }
        HudConfig.Pos p = cfg.elements.get(id);
        return p != null && p.fx >= 0.0 && p.fy >= 0.0;
    }

    public static void reset(HudConfig cfg, String id) {
        if (cfg.elements != null) {
            cfg.elements.remove(id);
        }
    }

    public static void resetAll(HudConfig cfg) {
        if (cfg.elements != null) {
            cfg.elements.clear();
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clampD(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}

