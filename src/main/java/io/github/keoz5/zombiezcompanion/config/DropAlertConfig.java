package io.github.keoz5.zombiezcompanion.config;

import java.util.HashSet;
import java.util.Set;

public final class DropAlertConfig {
    public boolean common = true;
    public boolean uncommon = true;
    public boolean rare = true;
    public boolean epic = true;
    public boolean legendary = true;
    public boolean mythic = true;
    public boolean exalted = true;
    public boolean primal = true;
    public boolean items = true;
    public boolean food = false;
    public boolean gadgets = false;
    public Set<String> hiddenConsumables = new HashSet<String>();
    public int markerStyle = 0;
    public int soundMinRarity = 4;
    public float soundVolume = 0.6f;
}

