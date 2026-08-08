/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
 *  net.minecraft.class_1921
 *  net.minecraft.class_1921$class_4688
 *  net.minecraft.class_238
 *  net.minecraft.class_243
 *  net.minecraft.class_2561
 *  net.minecraft.class_290
 *  net.minecraft.class_293
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_327$class_6415
 *  net.minecraft.class_332
 *  net.minecraft.class_4184
 *  net.minecraft.class_437
 *  net.minecraft.class_4587
 *  net.minecraft.class_4587$class_4665
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_4604
 *  net.minecraft.class_4668$class_4677
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 *  net.minecraft.class_7833
 */
package io.github.keoz5.zombiezcompanion.modules.map;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.mixin.GameRendererAccessor;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.class_1921;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4184;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4604;
import net.minecraft.class_4668;
import net.minecraft.class_5250;
import net.minecraft.class_5348;
import net.minecraft.class_7833;

public final class WaypointsModule
implements Module {
    public static final String ID = "waypoints";
    public static final class_1921 BEACON_LINES_BEHIND = BeaconLayer.build("behind", true);
    public static final class_1921 BEACON_LINES_FRONT = BeaconLayer.build("front", false);
    private static final int DEATH_WAYPOINT_COLOR = 0xEF4444;
    private static final int DEATH_CONFIRM_TICKS = 8;
    private ConfigManager configManager;
    private boolean wasAlive = true;
    private int deadTicks;
    private class_243 lastLivePos;

    public static boolean isEnabled() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        return mm != null && mm.isEnabled(ID);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Waypoints";
    }

    @Override
    public String description() {
        return class_2561.method_43471((String)"zombiezcompanion.module.waypoints.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.MAP;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("waypoint", "point", "rep\u00e8re", "balise", "marqueur", "navigation", "gps", "refuge");
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
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
        WorldRenderEvents.LAST.register(this::renderBeacons);
    }

    private void renderBeacons(WorldRenderContext ctx) {
        if (!WaypointsModule.isEnabled()) {
            return;
        }
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null || client.field_1687 == null) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        MapConfig cfg = this.config();
        if (cfg.waypointMarkerStyle != 0) {
            return;
        }
        class_4184 camera = ctx.camera();
        class_243 cam = camera.method_19326();
        class_4587 matrices = ctx.matrixStack();
        class_4597.class_4598 immediate = client.method_22940().method_23000();
        class_327 tr = client.field_1772;
        class_4604 frustum = ctx.frustum();
        matrices.method_22903();
        for (MapConfig.Waypoint wp : cfg.waypoints) {
            if (!wp.visible || WaypointsModule.isSameGuideTarget(cfg.guideTarget, wp) || !WaypointsModule.isBeaconVisible(frustum, wp.x, wp.y, wp.z)) continue;
            WaypointsModule.drawBeacon(matrices, immediate, camera, cam, tr, wp.x, wp.y, wp.z, wp.label, 0xFF000000 | wp.colorRgb);
        }
        if (cfg.guideTarget != null && !"Waypoint".equals(cfg.guideTarget.type)) {
            MapConfig.GuideTarget t = cfg.guideTarget;
            if (WaypointsModule.isBeaconVisible(frustum, t.x, t.y, t.z)) {
                WaypointsModule.drawBeacon(matrices, immediate, camera, cam, tr, t.x, t.y, t.z, t.label, 0xFF000000 | t.colorRgb);
            }
        }
        matrices.method_22909();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth((float)4.0f);
        immediate.method_22993();
        RenderSystem.lineWidth((float)1.0f);
        RenderSystem.enableDepthTest();
    }

    public static boolean isBeaconVisible(class_4604 frustum, double wx, double wy, double wz) {
        return frustum.method_23093(new class_238(wx - 0.5, wy, wz - 0.5, wx + 0.5, wy + 4.0, wz + 0.5));
    }

    private static boolean isSameGuideTarget(MapConfig.GuideTarget target, MapConfig.Waypoint waypoint) {
        return target != null && "Waypoint".equals(target.type) && Math.abs(target.x - waypoint.x) < 0.01 && Math.abs(target.z - waypoint.z) < 0.01;
    }

    public static void drawBeacon(class_4587 matrices, class_4597.class_4598 immediate, class_4184 camera, class_243 cam, class_327 tr, double wx, double wy, double wz, String label, int color) {
        double dx = wx - cam.field_1352;
        double dy = wy - cam.field_1351;
        double dz = wz - cam.field_1350;
        double labelY = dy + 2.35;
        double dist = Math.sqrt(dx * dx + labelY * labelY + dz * dz);
        String text = label + " [" + (int)Math.round(dist) + "m]";
        WaypointsModule.drawBeaconShape(matrices, immediate, BEACON_LINES_BEHIND, dx, dy, dz, color, 0.35f);
        WaypointsModule.drawBeaconShape(matrices, immediate, BEACON_LINES_FRONT, dx, dy, dz, color, 0.95f);
        WaypointsModule.drawWorldText(matrices, immediate, camera, tr, "+", color, dx, dy + 1.05, dz, (float)Math.min(0.18, Math.max(0.045, dist * 0.0038)), -1879048192);
        WaypointsModule.drawWorldText(matrices, immediate, camera, tr, text, color, dx, labelY, dz, (float)Math.min(0.15, Math.max(0.033, dist * 0.0032)), -1610612736);
    }

    private static void drawBeaconShape(class_4587 matrices, class_4597.class_4598 immediate, class_1921 layer, double dx, double dy, double dz, int color, float alpha) {
        float cx = (float)dx;
        float by = (float)dy + 0.12f;
        float cz = (float)dz;
        float height = 3.45f;
        float pillarHalf = 0.09f;
        float ringHalf = 0.42f;
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        float a = alpha;
        class_4588 lines = immediate.getBuffer(layer);
        class_4587.class_4665 entry = matrices.method_23760();
        float xMin = cx - pillarHalf;
        float xMax = cx + pillarHalf;
        float zMin = cz - pillarHalf;
        float zMax = cz + pillarHalf;
        float yTop = by + height;
        WaypointsModule.drawLine(lines, entry, xMin, by, zMin, xMin, yTop, zMin, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMax, by, zMin, xMax, yTop, zMin, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMax, by, zMax, xMax, yTop, zMax, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMin, by, zMax, xMin, yTop, zMax, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMin, by, zMin, xMax, by, zMin, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMax, by, zMin, xMax, by, zMax, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMax, by, zMax, xMin, by, zMax, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMin, by, zMax, xMin, by, zMin, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMin, yTop, zMin, xMax, yTop, zMin, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMax, yTop, zMin, xMax, yTop, zMax, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMax, yTop, zMax, xMin, yTop, zMax, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, xMin, yTop, zMax, xMin, yTop, zMin, r, g, b, a);
        float baseY = by + 0.18f;
        WaypointsModule.drawLine(lines, entry, cx - ringHalf, baseY, cz, cx + ringHalf, baseY, cz, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, cx, baseY, cz - ringHalf, cx, baseY, cz + ringHalf, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, cx - ringHalf, yTop, cz, cx + ringHalf, yTop, cz, r, g, b, a);
        WaypointsModule.drawLine(lines, entry, cx, yTop, cz - ringHalf, cx, yTop, cz + ringHalf, r, g, b, a);
    }

    private static void drawWorldText(class_4587 matrices, class_4597.class_4598 immediate, class_4184 camera, class_327 tr, String text, int color, double x, double y, double z, float scale, int bgColor) {
        matrices.method_22903();
        matrices.method_22904(x, y, z);
        matrices.method_22907(camera.method_23767());
        matrices.method_22905(-scale, -scale, scale);
        int w = tr.method_1727(text);
        tr.method_27521(text, (float)(-w) / 2.0f, -4.0f, color, false, matrices.method_23760().method_23761(), (class_4597)immediate, class_327.class_6415.field_33994, bgColor, 0xF000F0);
        tr.method_27521(text, (float)(-w) / 2.0f, -4.0f, color, false, matrices.method_23760().method_23761(), (class_4597)immediate, class_327.class_6415.field_33993, 0, 0xF000F0);
        matrices.method_22909();
    }

    private static void drawLine(class_4588 lines, class_4587.class_4665 entry, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        lines.method_56824(entry, x1, y1, z1).method_22915(r, g, b, a).method_60831(entry, 0.0f, 1.0f, 0.0f);
        lines.method_56824(entry, x2, y2, z2).method_22915(r, g, b, a).method_60831(entry, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public class_437 createOptionsScreen(class_437 parent) {
        return new WaypointsOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onClientTick(class_310 client) {
        boolean alive;
        if (client.field_1724 == null) {
            this.wasAlive = true;
            this.deadTicks = 0;
            this.lastLivePos = null;
            return;
        }
        boolean bl = alive = client.field_1724.method_6032() > 0.0f && !client.field_1724.method_31481();
        if (alive) {
            this.wasAlive = true;
            this.deadTicks = 0;
            this.lastLivePos = client.field_1724.method_19538();
            return;
        }
        if (!this.wasAlive) {
            return;
        }
        ++this.deadTicks;
        if (this.deadTicks < 8) {
            return;
        }
        this.wasAlive = false;
        this.deadTicks = 0;
        MapConfig cfg = this.config();
        if (cfg.autoDeathWaypoint && this.lastLivePos != null && ZombieZDetector.isOnZombieZ()) {
            this.createDeathWaypoint(this.lastLivePos);
        }
    }

    @Override
    public void onLeaveWorld() {
        this.wasAlive = true;
        this.deadTicks = 0;
        this.lastLivePos = null;
    }

    private void createDeathWaypoint(class_243 pos) {
        MapConfig.Waypoint wp = new MapConfig.Waypoint();
        wp.id = UUID.randomUUID().toString();
        wp.label = "Mort " + new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date());
        wp.x = pos.field_1352;
        wp.y = pos.field_1351;
        wp.z = pos.field_1350;
        wp.colorRgb = 0xEF4444;
        wp.createdAt = System.currentTimeMillis();
        wp.visible = true;
        this.config().waypoints.add(wp);
        this.configManager.save();
    }

    @Override
    public void onHudRender(class_332 ctx, float tickDelta) {
        double relative;
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null || client.field_1687 == null || client.field_1755 != null) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        MapConfig cfg = this.config();
        if (!cfg.showWaypointHud) {
            return;
        }
        if (cfg.waypointMarkerStyle == 0) {
            this.renderHudBeacons(ctx, client, cfg, tickDelta);
            return;
        }
        GuidePoint target = this.customGuideTarget(cfg);
        if (target == null) {
            return;
        }
        double dx = target.x - client.field_1724.method_23317();
        double dz = target.z - client.field_1724.method_23321();
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        for (relative = bearing - (double)client.field_1724.method_36454(); relative > 180.0; relative -= 360.0) {
        }
        while (relative < -180.0) {
            relative += 360.0;
        }
        this.renderGuide(ctx, target, Math.hypot(dx, dz), relative);
    }

    private void renderHudBeacons(class_332 ctx, class_310 client, MapConfig cfg, float tickDelta) {
        for (MapConfig.Waypoint wp : cfg.waypoints) {
            if (!wp.visible || WaypointsModule.isSameGuideTarget(cfg.guideTarget, wp)) continue;
            WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, wp.x, wp.y, wp.z, wp.label, 0xFF000000 | wp.colorRgb);
        }
        if (cfg.guideTarget != null && !"Waypoint".equals(cfg.guideTarget.type)) {
            MapConfig.GuideTarget target = cfg.guideTarget;
            WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, target.x, target.y, target.z, target.label, 0xFF000000 | target.colorRgb);
        }
    }

    public static void renderScreenBeacon(class_332 ctx, class_310 client, double wx, double wy, double wz, String label, int color) {
        WaypointsModule.renderScreenBeacon(ctx, client, 1.0f, wx, wy, wz, label, color);
    }

    public static void renderScreenBeacon(class_332 ctx, class_310 client, float tickDelta, double wx, double wy, double wz, String label, int color) {
        double yNorm;
        double xNorm;
        if (client == null || client.field_1724 == null || client.field_1773 == null) {
            return;
        }
        class_4184 camera = client.field_1773.method_19418();
        class_243 cam = camera.method_19326();
        class_243 toTarget = new class_243(wx - cam.field_1352, wy + 1.45 - cam.field_1351, wz - cam.field_1350);
        double distance = toTarget.method_1033();
        if (distance < 0.35) {
            return;
        }
        int screenW = ctx.method_51421();
        int screenH = ctx.method_51443();
        class_243 forward = class_243.method_1030((float)camera.method_19329(), (float)camera.method_19330()).method_1029();
        class_243 right = class_243.method_1030((float)0.0f, (float)(camera.method_19330() + 90.0f)).method_1029();
        class_243 up = right.method_1036(forward).method_1029();
        double depth = toTarget.method_1026(forward);
        double xCamera = toTarget.method_1026(right);
        double yCamera = toTarget.method_1026(up);
        double verticalFov = Math.toRadians(WaypointsModule.clamp(((GameRendererAccessor)client.field_1773).zombiezcompanion$invokeGetFov(camera, tickDelta, true), 12.0, 110.0));
        double aspect = (double)screenW / Math.max(1.0, (double)screenH);
        if (depth > 0.05) {
            double halfHeight = Math.tan(verticalFov / 2.0) * depth;
            double halfWidth = halfHeight * aspect;
            xNorm = xCamera / Math.max(0.001, halfWidth);
            yNorm = yCamera / Math.max(0.001, halfHeight);
        } else {
            double absX = Math.abs(xCamera);
            xNorm = absX < 0.001 ? 0.0 : Math.signum(xCamera) * Math.min(1.0, 0.4 + absX / Math.max(1.0, absX + 8.0));
            yNorm = -1.0;
        }
        double edgeScale = Math.max(1.0, Math.max(Math.abs(xNorm), Math.abs(yNorm)));
        xNorm /= edgeScale;
        yNorm /= edgeScale;
        xNorm = WaypointsModule.clamp(xNorm, -0.96, 0.96);
        yNorm = WaypointsModule.clamp(yNorm, -0.9, 0.9);
        int x = (int)Math.round((double)screenW / 2.0 + xNorm * (double)screenW / 2.0);
        int y = (int)Math.round((double)screenH / 2.0 - yNorm * (double)screenH / 2.0);
        int margin = 14;
        x = (int)WaypointsModule.clamp(x, margin, screenW - margin);
        y = (int)WaypointsModule.clamp(y, margin, screenH - margin);
        class_327 tr = client.field_1772;
        String safeLabel = label == null || label.isBlank() ? "Rep\u00e8re" : label;
        String text = safeLabel + " [" + (int)Math.round(distance) + "m]";
        int markerR = 6;
        WaypointsModule.drawHudDiamond(ctx, x, y, markerR + 2, -872415232);
        WaypointsModule.drawHudDiamond(ctx, x, y, markerR, color);
        ctx.method_25294(x - 1, y - markerR - 5, x + 2, y - markerR, color);
        ctx.method_25294(x - 1, y + markerR, x + 2, y + markerR + 6, color);
        ctx.method_25294(x - 1, y - 1, x + 2, y + 2, -1);
        int textW = Math.min(320, tr.method_1727(text) + 12);
        int boxX = (int)WaypointsModule.clamp((double)x - (double)textW / 2.0, 6.0, screenW - textW - 6);
        int boxY = (int)WaypointsModule.clamp(y - markerR - 22, 6.0, screenH - 18);
        ctx.method_25294(boxX + 1, boxY + 1, boxX + textW + 1, boxY + 17, -1442840576);
        ctx.method_25294(boxX, boxY, boxX + textW, boxY + 16, -804647918);
        ctx.method_49601(boxX, boxY, textW, 16, color);
        ctx.method_25303(tr, tr.method_27523(text, textW - 8), boxX + 5, boxY + 5, -1);
    }

    private static void drawHudDiamond(class_332 ctx, int x, int y, int radius, int color) {
        for (int dy = -radius; dy <= radius; ++dy) {
            int half = radius - Math.abs(dy);
            ctx.method_25294(x - half, y + dy, x + half + 1, y + dy + 1, color);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private GuidePoint customGuideTarget(MapConfig cfg) {
        if (cfg.guideTarget == null) {
            return null;
        }
        return new GuidePoint(cfg.guideTarget.label, cfg.guideTarget.x, cfg.guideTarget.z, cfg.guideTarget.colorRgb);
    }

    private void renderGuide(class_332 ctx, GuidePoint target, double distance, double relativeAngle) {
        class_327 textRenderer = class_310.method_1551().field_1772;
        class_5250 label = class_2561.method_43469((String)"zombiezcompanion.waypoint.guide.label", (Object[])new Object[]{target.label, (int)Math.round(distance)});
        int boxW = Math.max(100, textRenderer.method_27525((class_5348)label) + 34);
        int boxH = 22;
        int[] pos = this.guidePosition(ctx, boxW, boxH);
        int x = pos[0];
        int y = pos[1];
        ctx.method_25294(x + 2, y + 2, x + boxW + 2, y + boxH + 2, -1442840576);
        ctx.method_25294(x, y, x + boxW, y + boxH, -183627755);
        ctx.method_49601(x, y, boxW, boxH, -8874241);
        ctx.method_51448().method_22903();
        ctx.method_51448().method_46416((float)(x + 12), (float)(y + 11), 0.0f);
        ctx.method_51448().method_22907(class_7833.field_40718.rotationDegrees((float)relativeAngle));
        this.drawGuideArrow(ctx, 0xFF000000 | target.colorRgb);
        ctx.method_51448().method_22909();
        ctx.method_27535(textRenderer, (class_2561)label, x + 26, y + 7, -1);
    }

    private void drawGuideArrow(class_332 ctx, int color) {
        int shadow = -872415232;
        ctx.method_25294(-1, -7, 2, 7, shadow);
        ctx.method_25294(-4, -5, 5, -2, shadow);
        ctx.method_25294(-6, -2, 7, 1, shadow);
        ctx.method_25294(-1, -6, 2, 5, color);
        ctx.method_25294(-3, -5, 4, -2, color);
        ctx.method_25294(-5, -2, 6, 1, color);
        ctx.method_25294(-1, 3, 2, 6, -1);
    }

    private int[] guidePosition(class_332 ctx, int boxW, int boxH) {
        int y;
        int x;
        MapConfig cfg = this.config();
        int windowW = ctx.method_51421();
        int windowH = ctx.method_51443();
        int miniSize = Math.max(80, Math.min(260, cfg.miniMapSize));
        int margin = 8;
        int miniX = switch (cfg.miniMapCorner) {
            case 0, 2 -> margin;
            default -> windowW - miniSize - margin;
        };
        int miniY = switch (cfg.miniMapCorner) {
            case 2, 3 -> windowH - miniSize - margin;
            default -> margin;
        };
        int miniOffset = Math.max(-200, Math.min(200, cfg.miniMapOffsetY));
        miniY = Math.max(0, Math.min(windowH - miniSize, miniY + miniOffset));
        switch (cfg.waypointHudPosition) {
            case 1: {
                x = miniX - boxW - 8;
                y = miniY + miniSize / 2 - boxH / 2;
                if (x >= margin) break;
                x = miniX + miniSize + 8;
                break;
            }
            case 2: {
                x = (windowW - boxW) / 2;
                y = 18;
                break;
            }
            case 3: {
                x = (windowW - boxW) / 2;
                y = windowH - boxH - 42;
                break;
            }
            default: {
                x = miniX + miniSize - boxW;
                y = miniY - boxH - 8;
                if (y >= margin) break;
                y = miniY + miniSize + 8;
            }
        }
        x = Math.max(margin, Math.min(windowW - boxW - margin, x));
        y = Math.max(margin, Math.min(windowH - boxH - margin, y));
        return new int[]{x, y};
    }

    public MapConfig config() {
        return this.configManager.get().map;
    }

    static String positionLabel(int position) {
        String key = switch (position) {
            case 1 -> "zombiezcompanion.waypoint.position.left_minimap";
            case 2 -> "zombiezcompanion.waypoint.position.top_center";
            case 3 -> "zombiezcompanion.waypoint.position.bottom_center";
            default -> "zombiezcompanion.waypoint.position.above_minimap";
        };
        return class_2561.method_43471((String)key).getString();
    }

    private record GuidePoint(String label, double x, double z, int colorRgb) {
    }

    private static final class BeaconLayer
    extends class_1921 {
        private BeaconLayer(String name, class_293 fmt, class_293.class_5596 mode, int size, boolean crumbling, boolean translucent, Runnable startAction, Runnable endAction) {
            super(name, fmt, mode, size, crumbling, translucent, startAction, endAction);
        }

        static class_1921 build(String suffix, boolean throughWall) {
            return class_1921.method_24049((String)("zombiezcompanion:beacon_" + suffix), (class_293)class_290.field_29337, (class_293.class_5596)class_293.class_5596.field_29344, (int)1536, (boolean)false, (boolean)false, (class_1921.class_4688)class_1921.class_4688.method_23598().method_34578(field_29433).method_23609(new class_4668.class_4677(OptionalDouble.of(throughWall ? 2.5 : 4.0))).method_23607(field_22241).method_23615(field_21370).method_23610(field_25643).method_23616(field_21349).method_23603(field_21345).method_23604(throughWall ? field_21346 : field_21348).method_23617(false));
        }
    }
}

