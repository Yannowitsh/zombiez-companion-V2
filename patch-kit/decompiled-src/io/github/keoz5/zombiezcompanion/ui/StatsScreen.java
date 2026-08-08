/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 *  net.minecraft.class_5348
 */
package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.StatsConfig;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropRarity;
import io.github.keoz5.zombiezcompanion.modules.stats.StatsModule;
import io.github.keoz5.zombiezcompanion.ui.widget.CategoryTabButton;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;
import net.minecraft.class_5348;

public final class StatsScreen
extends class_437 {
    private final class_437 parent;
    private final ConfigManager configManager;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private Tab activeTab = Tab.ITEMS;

    public StatsScreen(class_437 parent, ConfigManager configManager) {
        super((class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.title"));
        this.parent = parent;
        this.configManager = configManager;
    }

    protected void method_25426() {
        int margin = Math.max(4, Math.min(24, Math.min(this.field_22789, this.field_22790) / 16));
        this.panelW = Math.min(500, this.field_22789 - 2 * margin);
        this.panelH = Math.min(440, this.field_22790 - 2 * margin);
        this.panelW = Math.max(this.panelW, 320);
        this.panelH = Math.max(this.panelH, 360);
        this.panelX = (this.field_22789 - this.panelW) / 2;
        this.panelY = (this.field_22790 - this.panelH) / 2;
        this.method_37063((class_364)new StyledButton(this.panelX + this.panelW - 36, this.panelY + 8, 22, 22, (class_2561)class_2561.method_43470((String)"X"), btn -> this.method_25419(), -266723542, -265932737, -854792));
        int tabsY = this.panelY + 36;
        int tabH = 22;
        int tabsX = this.panelX + 16;
        int gap = 6;
        int tabW = (this.panelW - 32 - 3 * gap) / 4;
        this.addTabButton(tabsX, tabsY, tabW, tabH, "zombiezcompanion.stats.tab.items", Tab.ITEMS);
        this.addTabButton(tabsX + (tabW + gap), tabsY, tabW, tabH, "zombiezcompanion.stats.tab.xp", Tab.XP);
        this.addTabButton(tabsX + 2 * (tabW + gap), tabsY, tabW, tabH, "zombiezcompanion.stats.tab.combat", Tab.COMBAT);
        this.addTabButton(tabsX + 3 * (tabW + gap), tabsY, tabW, tabH, "zombiezcompanion.stats.tab.economy", Tab.ECONOMY);
        this.method_37063((class_364)new StyledButton(this.panelX + this.panelW - 124, this.panelY + this.panelH - 32, 108, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.button.close"), btn -> this.method_25419(), -266723542, -265932737, -854792));
        this.method_37063((class_364)new StyledButton(this.panelX + 16, this.panelY + this.panelH - 32, 110, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.reset"), btn -> this.resetActiveTab(), -12965328, -11716288, -854792));
    }

    private void addTabButton(int x, int y, int w, int h, String key, Tab tab) {
        this.method_37063((class_364)new CategoryTabButton(x, y, w, h, (class_2561)class_2561.method_43471((String)key), btn -> {
            this.activeTab = tab;
        }, () -> this.activeTab == tab));
    }

    private void resetActiveTab() {
        StatsModule m = StatsModule.get();
        switch (this.activeTab.ordinal()) {
            case 0: {
                StatsConfig stats = this.configManager.get().stats;
                stats.dropsByRarity.clear();
                stats.totalDrops = 0;
                stats.recycledItems = 0L;
                stats.firstSeen = 0L;
                this.configManager.save();
                break;
            }
            case 1: {
                if (m == null) break;
                m.profil().reset();
                m.rodeur().reset();
                break;
            }
            case 2: {
                if (m == null) break;
                m.combat().reset();
                break;
            }
            case 3: {
                if (m == null) break;
                m.economy().reset();
            }
        }
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, -872415232);
        ctx.method_25294(this.panelX + 2, this.panelY + 4, this.panelX + this.panelW + 2, this.panelY + this.panelH + 4, -1442840576);
        ctx.method_25294(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, -183627755);
        ctx.method_25294(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 2, -8874241);
        ctx.method_49601(this.panelX, this.panelY, this.panelW, this.panelH, -13880766);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.title"), this.panelX + 16, this.panelY + 14, -854792);
        int contentTop = this.panelY + 66;
        int contentBottom = this.panelY + this.panelH - 40;
        int cx = this.panelX + 16;
        int cw = this.panelW - 32;
        switch (this.activeTab.ordinal()) {
            case 0: {
                this.renderItemsTab(ctx, cx, contentTop, cw, contentBottom);
                break;
            }
            case 1: {
                this.renderXpTab(ctx, cx, contentTop, cw, contentBottom);
                break;
            }
            case 2: {
                this.renderCombatTab(ctx, cx, contentTop, cw, contentBottom);
                break;
            }
            case 3: {
                this.renderEconomyTab(ctx, cx, contentTop, cw, contentBottom);
            }
        }
        super.method_25394(ctx, mouseX, mouseY, delta);
    }

    private void renderItemsTab(class_332 ctx, int x, int top, int w, int bottom) {
        StatsConfig stats = this.configManager.get().stats;
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43469((String)"zombiezcompanion.stats.total", (Object[])new Object[]{stats.totalDrops}), x, top, -8874241);
        if (stats.recycledItems > 0L) {
            String rec = class_2561.method_43469((String)"zombiezcompanion.stats.recycled", (Object[])new Object[]{stats.recycledItems}).getString();
            ctx.method_25303(this.field_22793, rec, x + w - this.field_22793.method_1727(rec), top, -8353376);
        }
        int rowsTop = top + 18;
        if (stats.firstSeen > 0L) {
            String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).format(new Date(stats.firstSeen));
            ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43469((String)"zombiezcompanion.stats.since", (Object[])new Object[]{date}), x, top + 12, -8353376);
            String elapsed = StatsScreen.formatElapsed(System.currentTimeMillis() - stats.firstSeen);
            double rate = (double)stats.totalDrops / Math.max(1.0, (double)(System.currentTimeMillis() - stats.firstSeen) / 3600000.0);
            String rateStr = String.format(Locale.ROOT, "%.1f", rate);
            String elapsedLine = class_2561.method_43469((String)"zombiezcompanion.stats.elapsed", (Object[])new Object[]{elapsed, rateStr}).getString();
            int ew = this.field_22793.method_1727(elapsedLine);
            ctx.method_25303(this.field_22793, elapsedLine, x + w - ew, top + 12, -8353376);
            rowsTop = top + 30;
        }
        int available = bottom - rowsTop;
        int rowH = 18;
        int gap = 2;
        DropRarity[] rarities = DropRarity.values();
        int stepNeeded = rarities.length * (rowH + gap);
        if (stepNeeded > available) {
            rowH = Math.max(12, (available - rarities.length * gap) / rarities.length);
        }
        double hours = stats.firstSeen > 0L ? Math.max(2.777777777777778E-4, (double)(System.currentTimeMillis() - stats.firstSeen) / 3600000.0) : 1.0;
        int y = rowsTop;
        for (DropRarity rarity : rarities) {
            if (y + rowH > bottom) break;
            int count = stats.dropsByRarity.getOrDefault(rarity.key, 0);
            int color = 0xFF000000 | rarity.colorRgb;
            ctx.method_25294(x, y, x + w, y + rowH, -267053025);
            ctx.method_25294(x, y, x + 4, y + rowH, color);
            ctx.method_25294(x, y, x + w, y + 1, 0x22FFFFFF);
            int textY = y + (rowH - 8) / 2;
            ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)("zombiezcompanion.drop_alert.rarity." + rarity.key)), x + 12, textY, color);
            String rateStr = String.format(Locale.ROOT, "%.1f/h", (double)count / hours);
            int rw = this.field_22793.method_1727(rateStr);
            ctx.method_25303(this.field_22793, rateStr, x + w - rw - 12, textY, -8353376);
            String countStr = String.valueOf(count);
            int cw = this.field_22793.method_1727(countStr);
            ctx.method_25303(this.field_22793, countStr, x + w - rw - cw - 24, textY, -854792);
            y += rowH + gap;
        }
    }

    private void renderXpTab(class_332 ctx, int x, int top, int w, int bottom) {
        StatsModule tracker = StatsModule.get();
        if (tracker == null) {
            ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.xp.no_data"), x, top + 8, -8353376);
            return;
        }
        int rowH = 44;
        this.renderXpRow(ctx, x, top, w, rowH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.xp.profil"), tracker.profil(), -10496);
        this.renderXpRow(ctx, x, top + rowH + 8, w, rowH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.xp.rodeur"), tracker.rodeur(), -8874241);
        int sparkY = top + 2 * (rowH + 8) + 4;
        int sparkH = Math.max(40, bottom - sparkY);
        if (sparkH >= 40) {
            ctx.method_25294(x + 2, sparkY + 3, x + w + 2, sparkY + sparkH + 3, -1442840576);
            ctx.method_25294(x, sparkY, x + w, sparkY + sparkH, -267053025);
            ctx.method_25294(x, sparkY, x + w, sparkY + 1, 0x22FFFFFF);
            ctx.method_49601(x, sparkY, w, sparkH, -14736594);
            ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.xp.graph"), x + 10, sparkY + 6, -8353376);
            this.drawXpSparkline(ctx, x + 10, sparkY + 20, w - 20, sparkH - 28, tracker.profil().samplesSnapshot());
        }
    }

    private void drawXpSparkline(class_332 ctx, int x, int y, int w, int h, List<StatsModule.LevelSample> samples) {
        if (samples == null || samples.size() < 2) {
            ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.xp.graph_wait"), x, y + h / 2 - 4, -12235684);
            return;
        }
        long minTs = samples.get(0).timestamp();
        long maxTs = samples.get(samples.size() - 1).timestamp();
        double minP = Double.MAX_VALUE;
        double maxP = -1.7976931348623157E308;
        for (StatsModule.LevelSample s : samples) {
            minP = Math.min(minP, s.progress());
            maxP = Math.max(maxP, s.progress());
        }
        double spanTs = Math.max(1L, maxTs - minTs);
        double spanP = Math.max(1.0E-6, maxP - minP);
        int prevX = -1;
        int prevY = -1;
        for (StatsModule.LevelSample s : samples) {
            int px = x + (int)Math.round((double)(s.timestamp() - minTs) / spanTs * (double)w);
            int py = y + h - (int)Math.round((s.progress() - minP) / spanP * (double)h);
            if (prevX >= 0) {
                int steps = Math.max(1, Math.abs(px - prevX));
                for (int i = 0; i <= steps; ++i) {
                    int ix = prevX + (px - prevX) * i / steps;
                    int iy = prevY + (py - prevY) * i / steps;
                    ctx.method_25294(ix, iy, ix + 1, iy + 1, -8874241);
                }
            }
            prevX = px;
            prevY = py;
        }
        ctx.method_25294(prevX - 1, prevY - 1, prevX + 2, prevY + 2, -10496);
    }

    private void renderCombatTab(class_332 ctx, int x, int top, int w, int bottom) {
        StatsModule.CombatStats c;
        StatsModule m = StatsModule.get();
        StatsModule.CombatStats combatStats = c = m != null ? m.combat() : null;
        if (c == null || !c.hasData()) {
            ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.combat.no_data"), x, top + 8, -8353376);
            return;
        }
        int y = top;
        y = this.statLine(ctx, x, y, w, "zombiezcompanion.stats.combat.kills", StatsScreen.formatNum(c.kills()), -1096636);
        y = this.statLine(ctx, x, y, w, "zombiezcompanion.stats.combat.kills_session", "+" + StatsScreen.formatNum(c.sessionKills()) + " / " + StatsScreen.formatElapsed(c.sessionDurationMs()), -1096636);
        double kph = c.killsPerHour();
        y = this.statLine(ctx, x, y, w, "zombiezcompanion.stats.combat.kills_per_hour", (String)(kph > 0.0 ? StatsScreen.formatNum(kph) + "/h" : "\u2014"), -1096636);
        y = this.statLine(ctx, x, y, w, "zombiezcompanion.stats.combat.streak", String.valueOf(c.streak()), -680437);
        this.statLine(ctx, x, y, w, "zombiezcompanion.stats.combat.streak_record", String.valueOf(c.streakRecord()), -680437);
    }

    private void renderEconomyTab(class_332 ctx, int x, int top, int w, int bottom) {
        StatsModule.EconomyStats e;
        StatsModule m = StatsModule.get();
        StatsModule.EconomyStats economyStats = e = m != null ? m.economy() : null;
        if (e == null || !e.hasData()) {
            ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.economy.no_data"), x, top + 8, -8353376);
            return;
        }
        int y = top;
        int gold = -10934;
        y = this.statLine(ctx, x, y, w, "zombiezcompanion.stats.economy.points", StatsScreen.formatNum(e.points()), gold);
        y = this.statLine(ctx, x, y, w, "zombiezcompanion.stats.economy.points_session", "+" + StatsScreen.formatNum(e.sessionPoints()) + " / " + StatsScreen.formatElapsed(e.sessionDurationMs()), gold);
        double pph = e.pointsPerHour();
        y = this.statLine(ctx, x, y, w, "zombiezcompanion.stats.economy.points_per_hour", (String)(pph > 0.0 ? StatsScreen.formatNum(pph) + "/h" : "\u2014"), gold);
        y = this.statLine(ctx, x, y, w, "zombiezcompanion.stats.economy.gemmes", StatsScreen.formatNum(e.gemmes()) + "  (+" + StatsScreen.formatNum(e.sessionGemmes()) + ")", -1292135);
        this.statLine(ctx, x, y, w, "zombiezcompanion.stats.economy.fragments", String.valueOf(e.fragments()), -13248513);
    }

    private int statLine(class_332 ctx, int x, int y, int w, String labelKey, String value, int accent) {
        int h = 26;
        ctx.method_25294(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.method_25294(x, y, x + w, y + h, -267053025);
        ctx.method_25294(x, y, x + 4, y + h, accent);
        ctx.method_25294(x, y, x + w, y + 1, 0x22FFFFFF);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)labelKey), x + 12, y + 9, -8353376);
        int vw = this.field_22793.method_1727(value);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43470((String)value), x + w - vw - 12, y + 9, -854792);
        return y + h + 6;
    }

    private static String formatNum(double v) {
        double a = Math.abs(v);
        if (a >= 1000000.0) {
            return String.format(Locale.ROOT, "%.2fM", v / 1000000.0);
        }
        if (a >= 1000.0) {
            return String.format(Locale.ROOT, "%.2fK", v / 1000.0);
        }
        return String.format(Locale.ROOT, "%.0f", v);
    }

    private void renderXpRow(class_332 ctx, int x, int y, int w, int h, class_2561 label, StatsModule.LevelHistory hist, int accent) {
        String deltaStr;
        String etaStr;
        String rateStr;
        String levelStr;
        ctx.method_25294(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.method_25294(x, y, x + w, y + h, -267053025);
        ctx.method_25294(x, y, x + 4, y + h, accent);
        ctx.method_25294(x, y, x + w, y + 1, 0x22FFFFFF);
        ctx.method_49601(x, y, w, h, -14736594);
        StatsModule.LevelSample s = hist.latest();
        if (s == null) {
            levelStr = class_2561.method_43471((String)"zombiezcompanion.stats.xp.waiting").getString();
            rateStr = "\u2014";
            etaStr = "\u2014";
            deltaStr = "\u2014";
        } else {
            levelStr = String.format(Locale.ROOT, "Niv. %d  %.2f%%", s.level(), s.pct());
            double rate = hist.progressPerHour();
            rateStr = rate > 0.0 ? String.format(Locale.ROOT, "%.2f lvl/h", rate) : "\u2014";
            long eta = hist.etaNextLevelMs();
            etaStr = eta > 0L ? StatsScreen.formatElapsed(eta) : "\u2014";
            double dLvl = hist.sessionDeltaLevels();
            long dur = hist.sessionDurationMs();
            deltaStr = dur > 0L ? String.format(Locale.ROOT, "+%.2f / %s", dLvl, StatsScreen.formatElapsed(dur)) : "\u2014";
        }
        ctx.method_27535(this.field_22793, label, x + 12, y + 6, accent);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43470((String)levelStr), x + 12, y + 24, -854792);
        int rightX = x + w - 12;
        int statsSpan = (int)((double)(w - 24) * 0.58);
        int statW = statsSpan / 3;
        this.drawStat(ctx, rightX - 2 * statW, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.xp.rate"), rateStr, y);
        this.drawStat(ctx, rightX - statW, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.xp.eta"), etaStr, y);
        this.drawStat(ctx, rightX, (class_2561)class_2561.method_43471((String)"zombiezcompanion.stats.xp.session"), deltaStr, y);
    }

    private void drawStat(class_332 ctx, int rightX, class_2561 label, String value, int y) {
        int lw = this.field_22793.method_27525((class_5348)label);
        int vw = this.field_22793.method_1727(value);
        ctx.method_51439(this.field_22793, label, rightX - lw, y + 6, -8353376, false);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43470((String)value), rightX - vw, y + 24, -854792);
    }

    private static String formatElapsed(long ms) {
        long s = Math.max(0L, ms / 1000L);
        long h = s / 3600L;
        long m = s % 3600L / 60L;
        if (h > 0L) {
            return h + "h " + m + "m";
        }
        if (m > 0L) {
            return m + "m";
        }
        return s + "s";
    }

    public void method_25420(class_332 ctx, int mouseX, int mouseY, float delta) {
    }

    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.method_25419();
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    public void method_25419() {
        this.configManager.save();
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    public boolean method_25421() {
        return false;
    }

    public static enum Tab {
        ITEMS,
        XP,
        COMBAT,
        ECONOMY;

    }
}

