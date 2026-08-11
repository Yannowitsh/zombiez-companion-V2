package io.github.keoz5.zombiezcompanion.config;

import io.github.keoz5.zombiezcompanion.config.AutoTextConfig;
import io.github.keoz5.zombiezcompanion.config.BrightnessConfig;
import io.github.keoz5.zombiezcompanion.config.ColorsConfig;
import io.github.keoz5.zombiezcompanion.config.ConsumablesConfig;
import io.github.keoz5.zombiezcompanion.config.CoordinatesConfig;
import io.github.keoz5.zombiezcompanion.config.DropAlertConfig;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.config.MiniEventsConfig;
import io.github.keoz5.zombiezcompanion.config.MobSensorConfig;
import io.github.keoz5.zombiezcompanion.config.PlayersConfig;
import io.github.keoz5.zombiezcompanion.config.SkullsConfig;
import io.github.keoz5.zombiezcompanion.config.StatsConfig;
import io.github.keoz5.zombiezcompanion.config.TelemetryConfig;
import io.github.keoz5.zombiezcompanion.config.ZoomConfig;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModConfig {
    public int schemaVersion = 7;
    public boolean debugMode = false;
    public Map<String, Boolean> moduleEnabled = new LinkedHashMap<String, Boolean>();
    public BrightnessConfig brightness = new BrightnessConfig();
    public MapConfig map = new MapConfig();
    public AutoTextConfig autoText = new AutoTextConfig();
    public ZoomConfig zoom = new ZoomConfig();
    public DropAlertConfig dropAlert = new DropAlertConfig();
    public StatsConfig stats = new StatsConfig();
    public MiniEventsConfig miniEvents = new MiniEventsConfig();
    public MobSensorConfig mobSensor = new MobSensorConfig();
    public SkullsConfig skulls = new SkullsConfig();
    public TelemetryConfig telemetry = new TelemetryConfig();
    public PlayersConfig players = new PlayersConfig();
    public CoordinatesConfig coordinates = new CoordinatesConfig();
    public ConsumablesConfig consumables = new ConsumablesConfig();
    public ColorsConfig colors = new ColorsConfig();
    public HudConfig hud = new HudConfig();
}

