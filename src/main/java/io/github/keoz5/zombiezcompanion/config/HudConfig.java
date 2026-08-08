package io.github.keoz5.zombiezcompanion.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HudConfig {
    public Map<String, Pos> elements = new LinkedHashMap<String, Pos>();

    public static final class Pos {
        public double fx = -1.0;
        public double fy = -1.0;
        public double scale = 1.0;
    }
}

