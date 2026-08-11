package io.github.keoz5.zombiezcompanion.config;

import java.util.ArrayList;
import java.util.List;

public final class FriendsConfig {
    /** Master toggle: render friends in the world/HUD at all. */
    public boolean showFriends = true;
    /** Distance (blocks) at or above which a friend shows as a moving waypoint; below it, a compact HUD dot. */
    public int nearHudRange = 100;
    /** mcuuids of friends the local player has un-checked (default is visible). */
    public List<String> hidden = new ArrayList<String>();
}
