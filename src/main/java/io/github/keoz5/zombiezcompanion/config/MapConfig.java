package io.github.keoz5.zombiezcompanion.config;

import java.util.ArrayList;
import java.util.List;

public final class MapConfig {
    public boolean showZones = true;
    public boolean showRefuges = true;
    public boolean showCranes = true;
    public boolean showBosses = true;
    public boolean showWaypoints = true;
    public boolean showWaypointHud = true;
    public int waypointHudPosition = 0;
    public int waypointMarkerStyle = 0;
    public boolean autoDeathWaypoint = true;
    public boolean showModUsers = true;
    public int miniMapSize = 80;
    public int miniMapPeekSize = 200;
    public double miniMapZoom = 0.25;
    public int miniMapCorner = 3;
    public int miniMapOffsetY = 0;
    public GuideTarget guideTarget = null;
    public List<Waypoint> waypoints = new ArrayList<Waypoint>();

    public static final class GuideTarget {
        public String label = "Target";
        public String type = "target";
        public double x;
        public double y = 64.0;
        public double z;
        public int colorRgb = 7902975;
    }

    public static final class Waypoint {
        public String id = "";
        public String label = "Rep\u00e8re";
        public double x;
        public double y = 64.0;
        public double z;
        public int colorRgb = 7902975;
        public long createdAt = 0L;
        public boolean visible = true;
        /**
         * Registry id of the dimension this waypoint belongs to (e.g. "minecraft:overworld",
         * "minecraft:world2"). Nullable: waypoints saved before dimension support existed have
         * no value and are treated as visible in every dimension (backward compatibility).
         */
        public String dimension = null;
    }
}

