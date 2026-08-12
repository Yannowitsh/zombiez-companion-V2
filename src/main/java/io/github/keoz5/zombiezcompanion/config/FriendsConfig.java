package io.github.keoz5.zombiezcompanion.config;

import java.util.ArrayList;
import java.util.List;

public final class FriendsConfig {
    /** Master toggle: render friends in the world/HUD at all. */
    public boolean showFriends = true;
    /** Distance (blocks) at or above which a friend shows as the full labeled waypoint; below it, the near marker. */
    public int nearHudRange = 200;
    /** Global marker style: "auto" (waypoint far / compact near), "waypoint" (always), or "box" (framed near). */
    public String markerStyle = "auto";
    /** mcuuids of friends the local player has un-checked (default is visible). */
    public List<String> hidden = new ArrayList<String>();
}
