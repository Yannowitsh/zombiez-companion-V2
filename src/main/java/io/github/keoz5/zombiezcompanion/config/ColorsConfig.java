package io.github.keoz5.zombiezcompanion.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-element color overrides (RGB, no alpha), keyed by a stable element id (see
 * {@code ui.Colors.ELEMENTS}). Absent id = use the code default, so the file only stores changes.
 */
public final class ColorsConfig {
    public Map<String, Integer> overrides = new LinkedHashMap<String, Integer>();
}
