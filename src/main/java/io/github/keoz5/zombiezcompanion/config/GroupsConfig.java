package io.github.keoz5.zombiezcompanion.config;

/** Client-side options for the Groups module. Group membership itself lives on the backend. */
public final class GroupsConfig {
    /** Master toggle: render group members in the world/HUD. */
    public boolean showGroup = true;
    /** Distance (blocks) at or above which a member shows as the full labeled waypoint; below it, the near marker. */
    public int nearHudRange = 200;
    /** Marker style: "auto" (waypoint far / tracker near), "waypoint" (always), or "box" (framed near). */
    public String markerStyle = "auto";
    /** When enabled, replay the chief's refuge/dungeon actions (wired in later phases). */
    public boolean followChief = false;
    /** When enabled, auto-accept group dungeon entries after a short countdown (wired in a later phase). */
    public boolean dungeonAuto = false;
}
