package io.github.keoz5.zombiezcompanion.config;

import java.util.ArrayList;
import java.util.List;

public final class MobSensorConfig {
    public boolean outline = true;
    public boolean hud = true;
    public int detectionRange = 64;
    /** User-defined search slots. Any nearby entity matching an enabled slot's query is highlighted. */
    public List<Track> tracks = MobSensorConfig.defaultTracks();

    public static final int SLOTS = 5;

    public static List<Track> defaultTracks() {
        ArrayList<Track> l = new ArrayList<Track>();
        l.add(new Track("mutant", true));
        for (int i = 1; i < SLOTS; ++i) {
            l.add(new Track());
        }
        return l;
    }

    public static final class Track {
        public String query = "";
        public boolean enabled = false;

        public Track() {
        }

        public Track(String query, boolean enabled) {
            this.query = query;
            this.enabled = enabled;
        }
    }
}
