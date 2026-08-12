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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        return Component.translatable((String)"zombiezcompanion.module.consumables.desc").getString();
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
    public Screen createOptionsScreen(Screen parent) {
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
    public void onChatMessage(Component message, boolean overlay) {
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
    public void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null || !ZombieZDetector.isOnZombieZ()) {
            this.flowers.clear();
            return;
        }
        long now = System.currentTimeMillis();
        this.flowers.removeIf(f -> now > f.expiresAt + 500L);
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null) {
            return;
        }
        if (client.options.hideGui) {
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

    private void renderLure(GuiGraphicsExtractor ctx, Minecraft client, long now) {
        Font tr = client.font;
        long remainMs = Math.max(0L, this.lureExpiresAt - now);
        String label = Component.translatable((String)"zombiezcompanion.consumables.lure.label", (Object[])new Object[]{ConsumablesModule.formatTime(remainMs)}).getString();
        this.drawBoxMulti(ctx, client, tr, "lure_timer", label, 1, -13261, 0.0, 0.42);
    }

    private void renderFlowers(GuiGraphicsExtractor ctx, Minecraft client, long now) {
        Font tr = client.font;
        StringBuilder sb = new StringBuilder();
        int lines = 0;
        for (FlowerState f : this.flowers) {
            long remainMs = Math.max(0L, f.expiresAt - now);
            if (sb.length() > 0) {
                sb.append('\n');
            }
            String s = f.rarity.isEmpty() ? Component.translatable((String)"zombiezcompanion.consumables.flower.line", (Object[])new Object[]{ConsumablesModule.formatTime(remainMs)}).getString() : Component.translatable((String)"zombiezcompanion.consumables.flower.line_rarity", (Object[])new Object[]{f.rarity, ConsumablesModule.formatTime(remainMs)}).getString();
            sb.append(s);
            ++lines;
        }
        this.drawBoxMulti(ctx, client, tr, "flower_timer", sb.toString(), lines, -21812, 0.0, 0.48);
    }

    private void drawBoxMulti(GuiGraphicsExtractor ctx, Minecraft client, Font tr, String elementId, String text, int lines, int accent, double defFx, double defFy) {
        int padding = 4;
        int lineH = 10;
        int maxWidth = 0;
        for (String line : text.split("\n")) {
            maxWidth = Math.max(maxWidth, tr.width(line));
        }
        int boxW = maxWidth + padding * 2;
        int boxH = lineH * lines + padding * 2;
        HudConfig hud = this.configManager.get().hud;
        double scale = HudAnchor.scale(hud, elementId);
        int scaledW = (int)Math.round((double)boxW * scale);
        int scaledH = (int)Math.round((double)boxH * scale);
        int screenW = ctx.guiWidth();
        int screenH = ctx.guiHeight();
        int boxX = HudAnchor.resolveX(hud, elementId, screenW, scaledW, defFx);
        int boxY = HudAnchor.resolveY(hud, elementId, screenH, scaledH, defFy);
        ctx.pose().pushMatrix();
        io.github.keoz5.zombiezcompanion.compat.ZCPose.translate(ctx, (float)boxX, (float)boxY);
        if (scale != 1.0) {
            io.github.keoz5.zombiezcompanion.compat.ZCPose.scale(ctx, (float)scale, (float)scale);
        }
        ctx.fill(0, 0, boxW, boxH, -1073741824);
        ctx.fill(0, 0, boxW, 1, accent);
        int y = padding;
        for (String line : text.split("\n")) {
            ctx.text(tr, (Component)Component.literal((String)line), padding, y, -854792);
            y += lineH;
        }
        ctx.pose().popMatrix();
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

