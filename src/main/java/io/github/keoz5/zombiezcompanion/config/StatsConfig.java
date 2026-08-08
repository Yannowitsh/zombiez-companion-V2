package io.github.keoz5.zombiezcompanion.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StatsConfig {
    public Map<String, Integer> dropsByRarity = new LinkedHashMap<String, Integer>();
    public int totalDrops = 0;
    public long firstSeen = 0L;
    public long recycledItems = 0L;
}

