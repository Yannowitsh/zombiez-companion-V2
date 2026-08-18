package io.github.keoz5.zombiezcompanion.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import io.github.keoz5.zombiezcompanion.config.AutoTextConfig;
import io.github.keoz5.zombiezcompanion.config.BrightnessConfig;
import io.github.keoz5.zombiezcompanion.config.CoordinatesConfig;
import io.github.keoz5.zombiezcompanion.config.DropAlertConfig;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.config.MiniEventsConfig;
import io.github.keoz5.zombiezcompanion.config.MobSensorConfig;
import io.github.keoz5.zombiezcompanion.config.ModConfig;
import io.github.keoz5.zombiezcompanion.config.PlayersConfig;
import io.github.keoz5.zombiezcompanion.config.SkullsConfig;
import io.github.keoz5.zombiezcompanion.config.StatsConfig;
import io.github.keoz5.zombiezcompanion.config.TelemetryConfig;
import io.github.keoz5.zombiezcompanion.config.ZoomConfig;
import io.github.keoz5.zombiezcompanion.log.Log;
import io.github.keoz5.zombiezcompanion.log.LogCategory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.UUID;

public final class ConfigManager {
    private static final String FILE_NAME = "config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_SCHEMA = 8;
    private final Path configDir;
    private final Path configFile;
    private ModConfig config;

    public ConfigManager(Path configDir) {
        this.configDir = configDir;
        this.configFile = configDir.resolve(FILE_NAME);
        this.config = this.load();
    }

    public ModConfig get() {
        return this.config;
    }

    private ModConfig load() {
        try {
            Files.createDirectories(this.configDir, new FileAttribute[0]);
        }
        catch (IOException e) {
            Log.error("Failed to create config dir " + String.valueOf(this.configDir), e);
        }
        if (!Files.exists(this.configFile, new LinkOption[0])) {
            Log.info("No config file, creating defaults at " + String.valueOf(this.configFile));
            ModConfig fresh = new ModConfig();
            this.saveInternal(fresh);
            return fresh;
        }
        try {
            String json = Files.readString(this.configFile);
            ModConfig parsed = (ModConfig)GSON.fromJson(json, ModConfig.class);
            if (parsed == null) {
                throw new JsonSyntaxException("Empty or null config");
            }
            ConfigManager.fillDefaults(parsed);
            Log.debug(LogCategory.CONFIG, "loaded schemaVersion=" + parsed.schemaVersion);
            return parsed;
        }
        catch (JsonSyntaxException | IOException e) {
            Log.error("Failed to read " + String.valueOf(this.configFile) + " \u2014 restoring defaults", e);
            this.backupCorruptFile();
            ModConfig fresh = new ModConfig();
            this.saveInternal(fresh);
            return fresh;
        }
    }

    public void save() {
        this.saveInternal(this.config);
    }

    private static void fillDefaults(ModConfig cfg) {
        int loadedSchemaVersion = cfg.schemaVersion;
        ConfigManager.ensureSubConfigs(cfg);
        ConfigManager.migrate(cfg, loadedSchemaVersion);
        ConfigManager.clampRanges(cfg);
        ConfigManager.normalizeAutoText(cfg.autoText);
        cfg.schemaVersion = 8;
    }

    private static void ensureSubConfigs(ModConfig cfg) {
        if (cfg.moduleEnabled == null) {
            cfg.moduleEnabled = new LinkedHashMap<String, Boolean>();
        }
        if (cfg.brightness == null) {
            cfg.brightness = new BrightnessConfig();
        }
        if (cfg.map == null) {
            cfg.map = new MapConfig();
        }
        if (cfg.autoText == null) {
            cfg.autoText = new AutoTextConfig();
        }
        if (cfg.zoom == null) {
            cfg.zoom = new ZoomConfig();
        }
        if (cfg.dropAlert == null) {
            cfg.dropAlert = new DropAlertConfig();
        }
        if (cfg.stats == null) {
            cfg.stats = new StatsConfig();
        }
        if (cfg.stats.dropsByRarity == null) {
            cfg.stats.dropsByRarity = new LinkedHashMap<String, Integer>();
        }
        if (cfg.miniEvents == null) {
            cfg.miniEvents = new MiniEventsConfig();
        }
        if (cfg.mobSensor == null) {
            cfg.mobSensor = new MobSensorConfig();
        }
        if (cfg.mobSensor.tracks == null || cfg.mobSensor.tracks.isEmpty()) {
            cfg.mobSensor.tracks = MobSensorConfig.defaultTracks();
        }
        while (cfg.mobSensor.tracks.size() < MobSensorConfig.SLOTS) {
            cfg.mobSensor.tracks.add(new MobSensorConfig.Track());
        }
        for (MobSensorConfig.Track track : cfg.mobSensor.tracks) {
            if (track != null && track.query == null) {
                track.query = "";
            }
        }
        if (cfg.skulls == null) {
            cfg.skulls = new SkullsConfig();
        }
        if (cfg.skulls.visited == null) {
            cfg.skulls.visited = new HashSet<String>();
        }
        if (cfg.telemetry == null) {
            cfg.telemetry = new TelemetryConfig();
        }
        if (cfg.players == null) {
            cfg.players = new PlayersConfig();
        }
        if (cfg.friends == null) {
            cfg.friends = new FriendsConfig();
        }
        if (cfg.friends.hidden == null) {
            cfg.friends.hidden = new ArrayList<String>();
        }
        if (cfg.coordinates == null) {
            cfg.coordinates = new CoordinatesConfig();
        }
        if (cfg.colors == null) {
            cfg.colors = new ColorsConfig();
        }
        if (cfg.colors.overrides == null) {
            cfg.colors.overrides = new java.util.LinkedHashMap<String, Integer>();
        }
        if (cfg.hud == null) {
            cfg.hud = new HudConfig();
        }
        if (cfg.hud.elements == null) {
            cfg.hud.elements = new LinkedHashMap<String, HudConfig.Pos>();
        }
        if (cfg.map.waypoints == null) {
            cfg.map.waypoints = new ArrayList<MapConfig.Waypoint>();
        }
        if (cfg.autoText.entries == null) {
            cfg.autoText.entries = new ArrayList<AutoTextConfig.Entry>();
        }
        if (cfg.autoText.text == null) {
            cfg.autoText.text = "";
        }
        if (cfg.map.guideTarget != null) {
            MapConfig.GuideTarget gt = cfg.map.guideTarget;
            if (gt.label == null || gt.label.isBlank()) {
                gt.label = "Target";
            }
            if (gt.type == null || gt.type.isBlank()) {
                gt.type = "target";
            }
            if (!(Double.isFinite(gt.x) && Double.isFinite(gt.y) && Double.isFinite(gt.z))) {
                cfg.map.guideTarget = null;
            }
        }
        for (MapConfig.Waypoint waypoint : cfg.map.waypoints) {
            if (waypoint.id != null && !waypoint.id.isBlank()) continue;
            waypoint.id = UUID.randomUUID().toString();
        }
    }

    private static void migrate(ModConfig cfg, int fromVersion) {
        if (fromVersion < 5) {
            ConfigManager.migrateToV5(cfg);
        }
        if (fromVersion < 6) {
            ConfigManager.migrateToV6(cfg);
        }
        if (fromVersion < 7) {
            ConfigManager.migrateToV7(cfg);
        }
        ConfigManager.purgeOrphanWaypoints(cfg);
    }

    private static void migrateToV5(ModConfig cfg) {
        if (cfg.autoText.entries.isEmpty() && !cfg.autoText.text.isBlank()) {
            AutoTextConfig.Entry legacy = new AutoTextConfig.Entry();
            legacy.text = cfg.autoText.text;
            legacy.keyCode = cfg.autoText.keyCode;
            cfg.autoText.entries.add(legacy);
        }
    }

    private static void migrateToV6(ModConfig cfg) {
        if (cfg.autoText.entries.isEmpty()) {
            cfg.autoText.text = "";
        }
    }

    private static void migrateToV7(ModConfig cfg) {
        for (MapConfig.Waypoint waypoint : cfg.map.waypoints) {
            if (waypoint == null) continue;
            waypoint.visible = true;
        }
    }

    private static void purgeOrphanWaypoints(ModConfig cfg) {
        long now = System.currentTimeMillis();
        long marchandTtl = 600000L;
        cfg.map.waypoints.removeIf(w -> {
            if (w == null || w.id == null) {
                return true;
            }
            if (w.id.startsWith("marchand-")) {
                return w.createdAt <= 0L || now - w.createdAt > marchandTtl;
            }
            return false;
        });
    }

    private static void clampRanges(ModConfig cfg) {
        if (cfg.map.waypointHudPosition < 0 || cfg.map.waypointHudPosition > 3) {
            cfg.map.waypointHudPosition = 0;
        }
        if (cfg.map.waypointMarkerStyle < 0 || cfg.map.waypointMarkerStyle > 1) {
            cfg.map.waypointMarkerStyle = 0;
        }
        if (cfg.dropAlert.soundMinRarity < -1 || cfg.dropAlert.soundMinRarity > 6) {
            cfg.dropAlert.soundMinRarity = 4;
        }
        if (cfg.dropAlert.soundVolume < 0.0f || cfg.dropAlert.soundVolume > 1.0f) {
            cfg.dropAlert.soundVolume = 0.6f;
        }
        if (cfg.dropAlert.markerStyle < 0 || cfg.dropAlert.markerStyle > 1) {
            cfg.dropAlert.markerStyle = 0;
        }
        if (!Double.isFinite(cfg.zoom.factor)) {
            cfg.zoom.factor = 2.0;
        }
        cfg.zoom.factor = Math.max(2.0, Math.min(8.0, cfg.zoom.factor));
    }

    private static void normalizeAutoText(AutoTextConfig cfg) {
        for (int i = cfg.entries.size() - 1; i >= 0; --i) {
            AutoTextConfig.Entry entry = cfg.entries.get(i);
            if (entry == null) {
                cfg.entries.remove(i);
                continue;
            }
            if (entry.text != null) continue;
            entry.text = "";
        }
        while (cfg.entries.size() > 5) {
            cfg.entries.remove(cfg.entries.size() - 1);
        }
    }

    private void saveInternal(ModConfig cfg) {
        try {
            Files.createDirectories(this.configDir, new FileAttribute[0]);
            Path tmp = this.configDir.resolve("config.json.tmp");
            Files.writeString(tmp, (CharSequence)GSON.toJson((Object)cfg), new OpenOption[0]);
            Files.move(tmp, this.configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Log.debug(LogCategory.CONFIG, "saved");
        }
        catch (IOException e) {
            Log.error("Failed to save config to " + String.valueOf(this.configFile), e);
        }
    }

    private void backupCorruptFile() {
        try {
            Path bak = this.configDir.resolve("config.json.bak");
            Files.move(this.configFile, bak, StandardCopyOption.REPLACE_EXISTING);
            Log.warn("Corrupt config moved to " + String.valueOf(bak));
        }
        catch (IOException e) {
            Log.error("Failed to back up corrupt config", e);
        }
    }
}

