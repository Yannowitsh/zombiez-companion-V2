package io.github.keoz5.zombiezcompanion.modules.mobsensor;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.config.MobSensorConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.hud.HudAnchor;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.ui.Colors;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Detects the new rig-based "MUTANT" mobs on ZombieZ (their readable name lives in a text_display,
 * not on any real mob) and highlights them to prioritise farming. Two independent, player-toggleable
 * displays: a screen-space frame drawn over each mutant (visible through walls and at any distance the
 * mutant is tracked) and a compact HUD counter with the nearest distance.
 */
public final class MobSensorModule
implements Module {
    public static final String ID = "mob_sensor";
    private static final String HUD_ELEMENT = "mutant_sensor";
    private static final String MUTANT_KEYWORD = "mutant";
    private static final int MUTANT_COLOR = 0xAA33FF;
    // ARGB default: the alpha byte controls the translucent frame fill (outline stays opaque).
    private static final int MUTANT_FRAME_DEFAULT = 0x33AA33FF;
    // The highlighted box in world space, relative to the mutant's name text_display (which floats a
    // bit above the mob's head): extend down to cover the body, slightly above the name.
    private static final double BOX_HALF_WIDTH = 0.7;
    private static final double BOX_TOP_OFFSET = 0.1;
    private static final double BOX_BOTTOM_OFFSET = 2.3;
    // Screen frame width:height ratio, from the world box proportions.
    private static final double FRAME_WH_RATIO = (2.0 * BOX_HALF_WIDTH) / (BOX_TOP_OFFSET + BOX_BOTTOM_OFFSET);
    private static final double FRAME_MIN_H = 12.0;
    private static final double FRAME_MIN_W = 8.0;

    private static final int MAX_TRACKED = 400;
    private ConfigManager configManager;
    // Matched entity boxes from the last tick scan; consumed by both HUD counter and frames.
    private List<Target> targets = List.of();
    private int mutantCount;
    private int nearestDistBlocks = -1;

    /** A highlighted entity: horizontal position + world Y for the frame top/bottom, and a center for distance. */
    private record Target(double x, double y, double z, double topY, double botY) {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Traqueur de mobs";
    }

    @Override
    public String description() {
        return Component.translatable((String)"zombiezcompanion.module.mob_sensor.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.EVENTS;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("mutant", "mob", "capteur", "traqueur", "tracker", "sensor", "esp", "contour", "cadre", "farm", "slot");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new MobSensorOptionsScreen(parent, this, this.configManager);
    }

    public static MobSensorModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module m : mm.modules()) {
            if (m instanceof MobSensorModule) {
                return (MobSensorModule)m;
            }
        }
        return null;
    }

    public MobSensorConfig config() {
        return this.configManager.get().mobSensor;
    }

    @Override
    public void onLeaveWorld() {
        this.resetCounts();
    }

    @Override
    public void onDisable() {
        this.resetCounts();
    }

    @Override
    public void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null || !ZombieZDetector.isOnZombieZ()) {
            this.resetCounts();
            return;
        }
        this.targets = this.collectTargets(client, this.range());
        this.mutantCount = this.targets.size();
        double bestSq = Double.MAX_VALUE;
        for (Target t : this.targets) {
            double sq = client.player.distanceToSqr(t.x(), t.y(), t.z());
            if (sq < bestSq) {
                bestSq = sq;
            }
        }
        this.nearestDistBlocks = this.targets.isEmpty() ? -1 : (int)Math.round(Math.sqrt(bestSq));
    }

    private double range() {
        return Math.max(32.0, Math.min(100.0, (double)this.config().detectionRange));
    }

    private void resetCounts() {
        this.targets = List.of();
        this.mutantCount = 0;
        this.nearestDistBlocks = -1;
    }

    /** Normalized, non-empty queries from the enabled track slots. */
    private List<String> enabledQueries() {
        ArrayList<String> qs = new ArrayList<String>();
        for (MobSensorConfig.Track t : this.config().tracks) {
            if (t == null || !t.enabled) continue;
            String q = MobSensorModule.stripDiacritics(t.query == null ? "" : t.query.trim()).toLowerCase(Locale.ROOT);
            if (!q.isEmpty()) {
                qs.add(q);
            }
        }
        return qs;
    }

    /**
     * Nearby entities matching any enabled track query. The query is matched (case/accent-insensitive)
     * against the entity's type id, custom name, display name, scoreboard tags, and — for the display
     * rigs used by ZombieZ mutants — the {@code text_display} text. Framing uses the entity bounding box
     * for real mobs, or the name-relative box for a {@code text_display} rig (whose name floats above).
     */
    private List<Target> collectTargets(Minecraft client, double range) {
        List<String> queries = this.enabledQueries();
        if (queries.isEmpty() || client.player == null || client.level == null) {
            return List.of();
        }
        AABB area = client.player.getBoundingBox().inflate(range);
        List<Entity> ents = client.level.getEntitiesOfClass(Entity.class, area, e -> !e.isRemoved() && e != client.player);
        ArrayList<Target> out = new ArrayList<Target>();
        for (Entity e : ents) {
            String hay = MobSensorModule.searchText(e);
            if (hay.isEmpty()) continue;
            boolean match = false;
            for (String q : queries) {
                if (hay.contains(q)) {
                    match = true;
                    break;
                }
            }
            if (!match) continue;
            double topY;
            double botY;
            double cy;
            if (e instanceof Display.TextDisplay) {
                double py = e.getY();
                topY = py + BOX_TOP_OFFSET;
                botY = py - BOX_BOTTOM_OFFSET;
                cy = py - 1.0;
            } else {
                AABB bb = e.getBoundingBox();
                topY = bb.maxY + 0.15;
                botY = bb.minY;
                cy = (bb.maxY + bb.minY) / 2.0;
            }
            out.add(new Target(e.getX(), cy, e.getZ(), topY, botY));
            if (out.size() >= MAX_TRACKED) break;
        }
        return out;
    }

    /** Full searchable text for an entity: type id + custom name + display name + tags + text_display text. */
    private static String searchText(Entity e) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(EntityType.getKey(e.getType())).append(' ');
        }
        catch (Exception exception) {
            // ignore
        }
        Component custom = e.getCustomName();
        if (custom != null) {
            sb.append(custom.getString()).append(' ');
        }
        try {
            sb.append(e.getName().getString()).append(' ');
        }
        catch (Exception exception) {
            // ignore
        }
        for (String tag : e.entityTags()) {
            sb.append(tag).append(' ');
        }
        if (e instanceof Display.TextDisplay td) {
            try {
                sb.append(td.textRenderState().text().getString()).append(' ');
            }
            catch (Exception exception) {
                // ignore
            }
        }
        return MobSensorModule.stripDiacritics(sb.toString()).toLowerCase(Locale.ROOT);
    }

    /** True when a text_display's text contains the mutant keyword (ignoring case, accents and pack glyphs). */
    public static boolean isMutantText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return MobSensorModule.stripDiacritics(text).toLowerCase(Locale.ROOT).contains(MUTANT_KEYWORD);
    }

    public static boolean isMutant(LivingEntity mob) {
        String name = MobSensorModule.detectionName(mob);
        if (name.isBlank()) {
            return false;
        }
        return MobSensorModule.stripDiacritics(name).toLowerCase(Locale.ROOT).contains(MUTANT_KEYWORD);
    }

    /**
     * Full text the client has for this mob: the team-decorated display name plus any custom name.
     * Kept for the {@code /zzc scanmobs} verdict column; the live sensor detects via text_displays.
     */
    public static String detectionName(LivingEntity mob) {
        StringBuilder sb = new StringBuilder();
        Component disp = mob.getDisplayName();
        if (disp != null) {
            sb.append(disp.getString());
        }
        Component custom = mob.getCustomName();
        if (custom != null) {
            sb.append(' ').append(custom.getString());
        }
        return sb.toString();
    }

    private static String stripDiacritics(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    // --- Rendering (screen-space) -------------------------------------------

    @Override
    public void onHudRender(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null || client.options.hideGui) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        if (this.config().outline && !this.targets.isEmpty()) {
            this.renderFrames(ctx, client);
        }
        if (this.config().hud && this.mutantCount > 0) {
            this.renderCounter(ctx, client);
        }
    }

    /**
     * Draws a screen-space frame over each mutant. The world box (under the nameplate) is projected to
     * screen using the camera basis, so the frame shows through walls and at any distance — the only
     * limit is whether the server still tracks the mutant's nameplate entity.
     */
    private void renderFrames(GuiGraphicsExtractor ctx, Minecraft client) {
        Camera camera = client.gameRenderer.getMainCamera();
        int screenW = ctx.guiWidth();
        int screenH = ctx.guiHeight();
        int base = Colors.get("mutant_frame", MUTANT_FRAME_DEFAULT);
        int frameCol = 0xFF000000 | (base & 0xFFFFFF);
        int fillCol = base;
        for (Target t : this.targets) {
            double[] top = MobSensorModule.project(camera, screenW, screenH, t.x(), t.topY(), t.z());
            double[] bot = MobSensorModule.project(camera, screenW, screenH, t.x(), t.botY(), t.z());
            if (top == null || bot == null) {
                continue;
            }
            double cx = (top[0] + bot[0]) / 2.0;
            double cy = (top[1] + bot[1]) / 2.0;
            double h = Math.max(FRAME_MIN_H, Math.abs(bot[1] - top[1]));
            double w = Math.max(FRAME_MIN_W, h * FRAME_WH_RATIO);
            int x1 = (int)Math.round(MobSensorModule.clamp(cx - w / 2.0, -5000.0, screenW + 5000.0));
            int x2 = (int)Math.round(MobSensorModule.clamp(cx + w / 2.0, -5000.0, screenW + 5000.0));
            int y1 = (int)Math.round(MobSensorModule.clamp(cy - h / 2.0, -5000.0, screenH + 5000.0));
            int y2 = (int)Math.round(MobSensorModule.clamp(cy + h / 2.0, -5000.0, screenH + 5000.0));
            ctx.fill(x1, y1, x2, y2, fillCol);
            // 2px frame: a dark backing outline for contrast, then the colored frame on top.
            ctx.outline(x1 - 1, y1 - 1, x2 - x1 + 2, y2 - y1 + 2, 0x80000000);
            ctx.outline(x1, y1, x2 - x1, y2 - y1, frameCol);
        }
    }

    /** Projects a world point to screen coordinates, or null if it is behind the camera. */
    private static double[] project(Camera camera, int screenW, int screenH, double wx, double wy, double wz) {
        Vec3 cam = camera.position();
        Vec3 to = new Vec3(wx - cam.x, wy - cam.y, wz - cam.z);
        Vec3 forward = Vec3.directionFromRotation((float)camera.xRot(), (float)camera.yaw()).normalize();
        Vec3 right = Vec3.directionFromRotation((float)0.0f, (float)(camera.yaw() + 90.0f)).normalize();
        Vec3 up = right.cross(forward).normalize();
        double depth = to.dot(forward);
        if (depth <= 0.05) {
            return null;
        }
        double xCam = to.dot(right);
        double yCam = to.dot(up);
        double vfov = Math.toRadians(MobSensorModule.clamp(io.github.keoz5.zombiezcompanion.compat.ZCCompat.cameraFov(camera), 12.0, 110.0));
        double aspect = (double)screenW / Math.max(1.0, (double)screenH);
        double halfH = Math.tan(vfov / 2.0) * depth;
        double halfW = halfH * aspect;
        double xNorm = xCam / Math.max(0.001, halfW);
        double yNorm = yCam / Math.max(0.001, halfH);
        double sx = (double)screenW / 2.0 + xNorm * (double)screenW / 2.0;
        double sy = (double)screenH / 2.0 - yNorm * (double)screenH / 2.0;
        return new double[]{sx, sy};
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // --- HUD counter --------------------------------------------------------

    private void renderCounter(GuiGraphicsExtractor ctx, Minecraft client) {
        MutableComponent line = Component.translatable((String)"zombiezcompanion.mob_sensor.hud", (Object[])new Object[]{this.mutantCount, this.nearestDistBlocks});
        Font tr = client.font;
        int baseW = tr.width((FormattedText)line) + 12;
        int baseH = 16;
        int screenW = ctx.guiWidth();
        int screenH = ctx.guiHeight();
        HudConfig hud = this.configManager.get().hud;
        double scale = HudAnchor.scale(hud, HUD_ELEMENT);
        int sw = (int)Math.round((double)baseW * scale);
        int sh = (int)Math.round((double)baseH * scale);
        int x = HudAnchor.resolveX(hud, HUD_ELEMENT, screenW, sw, 0.0);
        int y = HudAnchor.resolveY(hud, HUD_ELEMENT, screenH, sh, 0.54);
        HudElements.report(HUD_ELEMENT, x, y, sw, sh);
        int accent = 0xFF000000 | (Colors.get("mutant_frame", MUTANT_FRAME_DEFAULT) & 0xFFFFFF);
        ctx.pose().pushMatrix();
        io.github.keoz5.zombiezcompanion.compat.ZCPose.translate(ctx, (float)x, (float)y);
        if (scale != 1.0) {
            io.github.keoz5.zombiezcompanion.compat.ZCPose.scale(ctx, (float)scale, (float)scale);
        }
        ctx.fill(0, 0, baseW, baseH, -1442840576);
        ctx.fill(0, 0, baseW, 1, accent);
        ctx.text(tr, (Component)line, 6, 4, -1);
        ctx.pose().popMatrix();
    }
}
