package io.github.keoz5.zombiezcompanion.config;

import java.util.ArrayList;
import java.util.List;

public final class MiniEventsConfig {
    public boolean fuyeur = true;
    public boolean colis = true;
    public boolean faille = true;
    public boolean pinata = true;
    public boolean bombe = true;
    public boolean jackpot = true;
    public boolean marchand = true;
    public boolean assaut = true;
    public boolean worldBoss = true;
    public int detectionRange = 100;
    public boolean marchandTimer = true;
    public boolean worldBossTimer = true;
    public List<Long> marchandSpawns = new ArrayList<Long>();
    public List<Long> worldBossSpawns = new ArrayList<Long>();
    // Configurable spawn sound alerts. "" = no sound (default); otherwise a vanilla id from SpawnSounds.IDS.
    public String worldBossSoundId = "";
    public String marchandSoundId = "";
    public int spawnSoundVolume = 100;
    // Le Monarque Damné: fixed 1h respawn boss, HUD countdown synced online. "" = no 1-min alert sound.
    public boolean monarchTimer = true;
    public String monarchSoundId = "";
}

