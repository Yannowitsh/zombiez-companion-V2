/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.modules.consumables;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.ConsumablesConfig;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.hud.HudAnchor;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.modules.consumables.ConsumablesOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;

public final class ConsumablesModule
implements Module {
    public static final String ID = "consumables";
    private static final long LURE_DURATION_MS = 30000L;
    private static final Pattern LURE_DEPLOY = Pattern.compile("leurre\\s+deploye");
    private static final Pattern FLOWER_DEPLOY = Pattern.compile("fleur\\s+d['' ]?infestation\\s+posee\\s*\\(\\s*([^,]+?)\\s*,\\s*(\\d+)\\s*s");
    private ConfigManager configManager;
    private long lureExpiresAt;
    private final List<FlowerState> flowers = new ArrayList<FlowerState>();
    private int flowerIdSeq;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Consommables";
    }

    @Override
    public String description() {
        return class_2561.method_43471((String)"zombiezcompanion.module.consumables.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.COMFORT;
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
        return List.of("leurre", "fleur", "infestation", "timer", "consommables", "consumable", "compte \u00e0 rebours", "hud");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
    }

    @Override
    public class_437 createOptionsScreen(class_437 parent) {
        return new ConsumablesOptionsScreen(parent, this, this.configManager);
    }

    public ConsumablesConfig config() {
        return this.configManager.get().consumables;
    }

    @Override
    public void onLeaveWorld() {
        this.lureExpiresAt = 0L;
        this.flowers.clear();
    }

    @Override
    public void onChatMessage(class_2561 message, boolean overlay) {
        Matcher m;
        if (message == null) {
            return;
        }
        String raw = message.getString();
        if (raw == null) {
            return;
        }
        String ascii = ConsumablesModule.stripDiacritics(raw).toLowerCase(Locale.ROOT);
        if (this.config().lureTimer && LURE_DEPLOY.matcher(ascii).find()) {
            this.lureExpiresAt = System.currentTimeMillis() + 30000L;
        }
        if (this.config().flowerTimer && (m = FLOWER_DEPLOY.matcher(ascii)).find()) {
            try {
                String rarity = ConsumablesModule.capitalize(m.group(1).trim());
                int seconds = Integer.parseInt(m.group(2));
                long now = System.currentTimeMillis();
                this.flowers.add(new FlowerState(++this.flowerIdSeq, rarity, now + (long)seconds * 1000L));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
    }

    @Override
    public void onClientTick(class_310 client) {
        if (client.field_1724 == null || client.field_1687 == null || !ZombieZDetector.isOnZombieZ()) {
            this.flowers.clear();
            return;
        }
        long now = System.currentTimeMillis();
        this.flowers.removeIf(f -> now > f.expiresAt + 500L);
    }

    @Override
    public void onHudRender(class_332 ctx, float tickDelta) {
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null || client.field_1755 != null) {
            return;
        }
        if (client.field_1690.field_1842) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.lureExpiresAt > now && this.config().lureTimer) {
            this.renderLure(ctx, client, now);
        }
        if (!this.flowers.isEmpty() && this.config().flowerTimer) {
            this.renderFlowers(ctx, client, now);
        }
    }

    private void renderLure(class_332 ctx, class_310 client, long now) {
        class_327 tr = client.field_1772;
        long remainMs = Math.max(0L, this.lureExpiresAt - now);
        String label = class_2561.method_43469((String)"zombiezcompanion.consumables.lure.label", (Object[])new Object[]{ConsumablesModule.formatTime(remainMs)}).getString();
        this.drawBoxMulti(ctx, client, tr, "lure_timer", label, 1, -13261, 0.0, 0.42);
    }

    private void renderFlowers(class_332 ctx, class_310 client, long now) {
        class_327 tr = client.field_1772;
        StringBuilder sb = new StringBuilder();
        int lines = 0;
        for (FlowerState f : this.flowers) {
            long remainMs = Math.max(0L, f.expiresAt - now);
            if (sb.length() > 0) {
                sb.append('\n');
            }
            String s = f.rarity.isEmpty() ? class_2561.method_43469((String)"zombiezcompanion.consumables.flower.line", (Object[])new Object[]{ConsumablesModule.formatTime(remainMs)}).getString() : class_2561.method_43469((String)"zombiezcompanion.consumables.flower.line_rarity", (Object[])new Object[]{f.rarity, ConsumablesModule.formatTime(remainMs)}).getString();
            sb.append(s);
            ++lines;
        }
        this.drawBoxMulti(ctx, client, tr, "flower_timer", sb.toString(), lines, -21812, 0.0, 0.48);
    }

    private void drawBoxMulti(class_332 ctx, class_310 client, class_327 tr, String elementId, String text, int lines, int accent, double defFx, double defFy) {
        int padding = 4;
        int lineH = 10;
        int maxWidth = 0;
        for (String line : text.split("\n")) {
            maxWidth = Math.max(maxWidth, tr.method_1727(line));
        }
        int boxW = maxWidth + padding * 2;
        int boxH = lineH * lines + padding * 2;
        HudConfig hud = this.configManager.get().hud;
        double scale = HudAnchor.scale(hud, elementId);
        int scaledW = (int)Math.round((double)boxW * scale);
        int scaledH = (int)Math.round((double)boxH * scale);
        int screenW = ctx.method_51421();
        int screenH = ctx.method_51443();
        int boxX = HudAnchor.resolveX(hud, elementId, screenW, scaledW, defFx);
        int boxY = HudAnchor.resolveY(hud, elementId, screenH, scaledH, defFy);
        ctx.method_51448().method_22903();
        ctx.method_51448().method_46416((float)boxX, (float)boxY, 0.0f);
        if (scale != 1.0) {
            ctx.method_51448().method_22905((float)scale, (float)scale, 1.0f);
        }
        ctx.method_25294(0, 0, boxW, boxH, -1073741824);
        ctx.method_25294(0, 0, boxW, 1, accent);
        int y = padding;
        for (String line : text.split("\n")) {
            ctx.method_27535(tr, (class_2561)class_2561.method_43470((String)line), padding, y, -854792);
            y += lineH;
        }
        ctx.method_51448().method_22909();
        HudElements.report(elementId, boxX, boxY, scaledW, scaledH);
    }

    private static String stripDiacritics(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replaceAll("\u00a7.", "");
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length());
        boolean cap = true;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                cap = true;
                out.append(c);
                continue;
            }
            out.append(cap ? Character.toUpperCase(c) : Character.toLowerCase(c));
            cap = false;
        }
        return out.toString();
    }

    private static String formatTime(long ms) {
        long s = Math.max(0L, ms / 1000L);
        return s + "s";
    }

    public static ConsumablesModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module m : mm.modules()) {
            if (!(m instanceof ConsumablesModule)) continue;
            ConsumablesModule c = (ConsumablesModule)m;
            return c;
        }
        return null;
    }

    private record FlowerState(int id, String rarity, long expiresAt) {
    }
}

