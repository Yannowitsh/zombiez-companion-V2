package io.github.keoz5.zombiezcompanion.modules.stats;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.StatsConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.log.Log;
import io.github.keoz5.zombiezcompanion.log.LogCategory;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropClassifier;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropRarity;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.modules.stats.StatsOptionsScreen;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;

public final class StatsModule
implements Module {
    public static final String ID = "stats";
    private static final Pattern PROFIL_PATTERN = Pattern.compile("Niv(?:eau)?\\.?\\s*(\\d+).*?(\\d+(?:[.,]\\d+)?)\\s*%");
    private static final Pattern RODEUR_PATTERN = Pattern.compile("R[\u00f4o]deur\\s*(\\d+).*?(\\d+(?:[.,]\\d+)?)\\s*%");
    private static final Pattern KILLS_PATTERN = Pattern.compile("Kills?\\s*:?\\s*([\\d.,]+[KMkm]?)");
    private static final Pattern STREAK_PATTERN = Pattern.compile("Streak\\s*:?\\s*([\\d.,]+[KMkm]?)");
    private static final Pattern POINTS_PATTERN = Pattern.compile("Points?\\s*:?\\s*([\\d.,]+[KMkm]?)");
    private static final Pattern GEMMES_PATTERN = Pattern.compile("Gemmes?\\s*:?\\s*([\\d.,]+[KMkm]?)");
    private static final Pattern FRAGMENT_PATTERN = Pattern.compile("\\+\\s*(\\d+)\\s*fragments?\\s+de\\s+noyau");
    private static final Pattern RECYCLE_PATTERN = Pattern.compile("recyclage\\s*:?\\s*(\\d+)\\s*items?");
    private final LevelHistory profil = new LevelHistory();
    private final LevelHistory rodeur = new LevelHistory();
    private final CombatStats combat = new CombatStats();
    private final EconomyStats economy = new EconomyStats();
    private long lastReadMs;
    private long lastDebugDumpMs;
    private long lastScanMs;
    private final Set<UUID> seenItemUuids = new HashSet<UUID>();
    private static final double SCAN_RADIUS_SQ = 256.0;
    private ConfigManager configManager;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Stats";
    }

    @Override
    public String description() {
        return Text.translatable((String)"zombiezcompanion.module.stats.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.PROGRESSION;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of(ID, "statistiques", "xp", "niveau", "level", "kills", "points", "gemmes", "fragments", "combat", "\u00e9conomie", "eco", "drops", "streak", "r\u00f4deur", "profil");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new StatsOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastReadMs >= 1000L) {
            this.lastReadMs = now;
            this.readSidebar(client, now);
        }
        if (now - this.lastScanMs >= 250L) {
            this.lastScanMs = now;
            this.scanGroundItems(client);
        }
    }

    @Override
    public void onLeaveWorld() {
        this.profil.reset();
        this.rodeur.reset();
        this.combat.reset();
        this.economy.reset();
        this.lastReadMs = 0L;
        this.lastScanMs = 0L;
        this.seenItemUuids.clear();
    }

    @Override
    public void onDisable() {
    }

    public static void onLocalPickup(ItemStack stack, int amount) {
    }

    private void scanGroundItems(MinecraftClient client) {
        if (this.configManager == null || client.player == null || client.world == null) {
            return;
        }
        for (Entity e : client.world.getEntities()) {
            String name;
            ItemStack stack;
            UUID id;
            if (!(e instanceof ItemEntity)) continue;
            ItemEntity item = (ItemEntity)e;
            if (e.squaredDistanceTo((Entity)client.player) > 256.0 || !this.seenItemUuids.add(id = e.getUuid()) || (stack = item.getStack()) == null || stack.isEmpty() || DropClassifier.isGadget(name = stack.getName().getString()) || DropClassifier.isFood(name)) continue;
            this.trackStat(DropClassifier.rarityOf(stack), stack.getCount());
        }
        if (this.seenItemUuids.size() > 5000) {
            this.seenItemUuids.clear();
        }
    }

    private void trackStat(DropRarity rarity, int count) {
        if (count <= 0) {
            return;
        }
        StatsConfig stats = this.configManager.get().stats;
        if (stats.firstSeen == 0L) {
            stats.firstSeen = System.currentTimeMillis();
        }
        stats.dropsByRarity.merge(rarity.key, count, Integer::sum);
        stats.totalDrops += count;
    }

    private void readSidebar(MinecraftClient client, long now) {
        Scoreboard sb = client.world.getScoreboard();
        if (sb == null) {
            return;
        }
        StringBuilder dump = new StringBuilder();
        boolean anyMatch = false;
        ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (obj != null) {
            String title = obj.getDisplayName().getString();
            dump.append("\n  [title] ").append(title);
            if (this.tryParse(title, now)) {
                anyMatch = true;
            }
            for (ScoreboardEntry e : sb.getScoreboardEntries(obj)) {
                if (e.name() == null) continue;
                Team team = sb.getScoreHolderTeam(e.owner());
                String line = (team != null ? Team.decorateName((AbstractTeam)team, (Text)e.name()) : e.name().copy()).getString();
                dump.append("\n  ").append(line);
                if (!this.tryParse(line, now)) continue;
                anyMatch = true;
            }
        } else {
            dump.append("\n  [no SIDEBAR objective]");
        }
        if (now - this.lastDebugDumpMs > 10000L) {
            this.lastDebugDumpMs = now;
            Log.debug(LogCategory.MODULE, "stats sidebar (match=" + anyMatch + "):" + String.valueOf(dump));
        }
    }

    private boolean tryParse(String rawLine, long now) {
        if (rawLine == null || rawLine.isEmpty()) {
            return false;
        }
        String line = rawLine.replaceAll("\u00a7.", "");
        Matcher m = PROFIL_PATTERN.matcher(line);
        if (m.find()) {
            try {
                int lvl = Integer.parseInt(m.group(1));
                double pct = Double.parseDouble(m.group(2).replace(',', '.'));
                this.profil.add(new LevelSample(now, lvl, pct));
                return true;
            }
            catch (NumberFormatException lvl) {
                // empty catch block
            }
        }
        if ((m = RODEUR_PATTERN.matcher(line)).find()) {
            try {
                int lvl = Integer.parseInt(m.group(1));
                double pct = Double.parseDouble(m.group(2).replace(',', '.'));
                this.rodeur.add(new LevelSample(now, lvl, pct));
                return true;
            }
            catch (NumberFormatException lvl) {
                // empty catch block
            }
        }
        boolean any = false;
        m = KILLS_PATTERN.matcher(line);
        if (m.find()) {
            this.combat.setKills(StatsModule.parseAbbrev(m.group(1)), now);
            any = true;
        }
        if ((m = STREAK_PATTERN.matcher(line)).find()) {
            this.combat.setStreak((long)StatsModule.parseAbbrev(m.group(1)));
            any = true;
        }
        if ((m = POINTS_PATTERN.matcher(line)).find()) {
            this.economy.setPoints(StatsModule.parseAbbrev(m.group(1)));
            any = true;
        }
        if ((m = GEMMES_PATTERN.matcher(line)).find()) {
            this.economy.setGemmes(StatsModule.parseAbbrev(m.group(1)));
            any = true;
        }
        return any;
    }

    @Override
    public void onChatMessage(Text message, boolean overlay) {
        Matcher r;
        if (message == null) {
            return;
        }
        String txt = message.getString();
        if (txt == null) {
            return;
        }
        String low = txt.toLowerCase(Locale.ROOT);
        Matcher m = FRAGMENT_PATTERN.matcher(low);
        if (m.find()) {
            try {
                this.economy.addFragments(Integer.parseInt(m.group(1)));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        if ((r = RECYCLE_PATTERN.matcher(low)).find()) {
            try {
                StatsConfig stats = this.configManager.get().stats;
                stats.recycledItems += (long)Integer.parseInt(r.group(1));
                if (stats.firstSeen == 0L) {
                    stats.firstSeen = System.currentTimeMillis();
                }
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
    }

    private static double parseAbbrev(String raw) {
        int last;
        if (raw == null) {
            return 0.0;
        }
        String s = raw.trim().replace(",", ".");
        double mult = 1.0;
        int n = last = s.isEmpty() ? 32 : (int)Character.toUpperCase(s.charAt(s.length() - 1));
        if (last == 75) {
            mult = 1000.0;
            s = s.substring(0, s.length() - 1);
        } else if (last == 77) {
            mult = 1000000.0;
            s = s.substring(0, s.length() - 1);
        }
        try {
            return Double.parseDouble(s) * mult;
        }
        catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public LevelHistory profil() {
        return this.profil;
    }

    public LevelHistory rodeur() {
        return this.rodeur;
    }

    public CombatStats combat() {
        return this.combat;
    }

    public EconomyStats economy() {
        return this.economy;
    }

    public int profilLevel() {
        LevelSample s = this.profil.latest();
        return s != null ? s.level() : -1;
    }

    public int rodeurLevel() {
        LevelSample s = this.rodeur.latest();
        return s != null ? s.level() : -1;
    }

    public long killsTotal() {
        return (long)this.combat.kills();
    }

    public long pointsTotal() {
        return (long)this.economy.points();
    }

    public static StatsModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module m : mm.modules()) {
            if (!(m instanceof StatsModule)) continue;
            StatsModule s = (StatsModule)m;
            return s;
        }
        return null;
    }

    public static final class LevelHistory {
        private static final long WINDOW_MS = 1800000L;
        private static final long MIN_SPAN_MS = 30000L;
        private final ArrayDeque<LevelSample> samples = new ArrayDeque();
        private LevelSample sessionStart;
        private LevelSample latest;

        public void add(LevelSample s) {
            if (this.sessionStart == null) {
                this.sessionStart = s;
            }
            if (this.latest != null && this.latest.level == s.level && Math.abs(this.latest.pct - s.pct) < 0.001) {
                this.latest = new LevelSample(s.timestamp, this.latest.level, this.latest.pct);
                return;
            }
            this.samples.addLast(s);
            this.latest = s;
            long cutoff = s.timestamp - 1800000L;
            while (!this.samples.isEmpty() && this.samples.peekFirst().timestamp < cutoff) {
                this.samples.pollFirst();
            }
        }

        public LevelSample latest() {
            return this.latest;
        }

        public LevelSample sessionStart() {
            return this.sessionStart;
        }

        public double progressPerHour() {
            if (this.samples.size() < 2) {
                return 0.0;
            }
            LevelSample first = this.samples.peekFirst();
            LevelSample last = this.samples.peekLast();
            long span = last.timestamp - first.timestamp;
            if (span < 30000L) {
                return 0.0;
            }
            double delta = last.progress() - first.progress();
            if (delta <= 0.0) {
                return 0.0;
            }
            return delta / ((double)span / 3600000.0);
        }

        public long etaNextLevelMs() {
            double rate = this.progressPerHour();
            if (rate <= 0.0 || this.latest == null) {
                return -1L;
            }
            double remaining = 1.0 - this.latest.pct / 100.0;
            return (long)(remaining / rate * 3600000.0);
        }

        public double sessionDeltaLevels() {
            if (this.sessionStart == null || this.latest == null) {
                return 0.0;
            }
            return this.latest.progress() - this.sessionStart.progress();
        }

        public long sessionDurationMs() {
            if (this.sessionStart == null || this.latest == null) {
                return 0L;
            }
            return this.latest.timestamp - this.sessionStart.timestamp;
        }

        public List<LevelSample> samplesSnapshot() {
            return new ArrayList<LevelSample>(this.samples);
        }

        public void reset() {
            this.samples.clear();
            this.sessionStart = null;
            this.latest = null;
        }
    }

    public static final class CombatStats {
        private double kills;
        private double sessionStartKills = -1.0;
        private long sessionStartMs;
        private long lastMs;
        private long streak;
        private long streakRecord;

        public void setKills(double value, long now) {
            if (this.sessionStartKills < 0.0) {
                this.sessionStartKills = value;
                this.sessionStartMs = now;
            }
            this.kills = value;
            this.lastMs = now;
        }

        public void setStreak(long value) {
            this.streak = value;
            if (value > this.streakRecord) {
                this.streakRecord = value;
            }
        }

        public double kills() {
            return this.kills;
        }

        public long streak() {
            return this.streak;
        }

        public long streakRecord() {
            return this.streakRecord;
        }

        public double sessionKills() {
            return this.sessionStartKills < 0.0 ? 0.0 : this.kills - this.sessionStartKills;
        }

        public long sessionDurationMs() {
            return this.sessionStartMs == 0L ? 0L : this.lastMs - this.sessionStartMs;
        }

        public double killsPerHour() {
            long span = this.sessionDurationMs();
            if (span < 30000L) {
                return 0.0;
            }
            double delta = this.sessionKills();
            return delta <= 0.0 ? 0.0 : delta / ((double)span / 3600000.0);
        }

        public boolean hasData() {
            return this.sessionStartKills >= 0.0;
        }

        public void reset() {
            this.kills = 0.0;
            this.sessionStartKills = -1.0;
            this.sessionStartMs = 0L;
            this.lastMs = 0L;
            this.streak = 0L;
            this.streakRecord = 0L;
        }
    }

    public static final class EconomyStats {
        private double points;
        private double gemmes;
        private double sessionStartPoints = -1.0;
        private double sessionStartGemmes = -1.0;
        private long sessionStartMs;
        private long lastMs;
        private long fragments;

        public void setPoints(double v) {
            long now = System.currentTimeMillis();
            if (this.sessionStartPoints < 0.0) {
                this.sessionStartPoints = v;
                this.sessionStartMs = now;
            }
            this.points = v;
            this.lastMs = now;
        }

        public void setGemmes(double v) {
            if (this.sessionStartGemmes < 0.0) {
                this.sessionStartGemmes = v;
            }
            this.gemmes = v;
        }

        public void addFragments(int n) {
            if (n > 0) {
                this.fragments += (long)n;
            }
        }

        public double points() {
            return this.points;
        }

        public double gemmes() {
            return this.gemmes;
        }

        public long fragments() {
            return this.fragments;
        }

        public double sessionPoints() {
            return this.sessionStartPoints < 0.0 ? 0.0 : this.points - this.sessionStartPoints;
        }

        public double sessionGemmes() {
            return this.sessionStartGemmes < 0.0 ? 0.0 : this.gemmes - this.sessionStartGemmes;
        }

        public long sessionDurationMs() {
            return this.sessionStartMs == 0L ? 0L : this.lastMs - this.sessionStartMs;
        }

        public double pointsPerHour() {
            long span = this.sessionDurationMs();
            if (span < 30000L) {
                return 0.0;
            }
            double delta = this.sessionPoints();
            return delta <= 0.0 ? 0.0 : delta / ((double)span / 3600000.0);
        }

        public boolean hasData() {
            return this.sessionStartPoints >= 0.0 || this.sessionStartGemmes >= 0.0 || this.fragments > 0L;
        }

        public void reset() {
            this.points = 0.0;
            this.gemmes = 0.0;
            this.sessionStartPoints = -1.0;
            this.sessionStartGemmes = -1.0;
            this.sessionStartMs = 0L;
            this.lastMs = 0L;
            this.fragments = 0L;
        }
    }

    public record LevelSample(long timestamp, int level, double pct) {
        public double progress() {
            return (double)this.level + this.pct / 100.0;
        }
    }
}

