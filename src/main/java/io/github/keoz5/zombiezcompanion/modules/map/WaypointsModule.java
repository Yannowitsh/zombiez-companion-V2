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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.math.RotationAxis;

public final class WaypointsModule
implements Module {
    public static final String ID = "waypoints";
    public static final RenderLayer BEACON_LINES_BEHIND = BeaconLayer.build("behind", true);
    public static final RenderLayer BEACON_LINES_FRONT = BeaconLayer.build("front", false);
    private static final int DEATH_WAYPOINT_COLOR = 0xEF4444;
    private static final int DEATH_CONFIRM_TICKS = 8;
    private ConfigManager configManager;
    private boolean wasAlive = true;
    private int deadTicks;
    private Vec3d lastLivePos;

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
        return Text.translatable((String)"zombiezcompanion.module.waypoints.desc").getString();
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        MapConfig cfg = this.config();
        if (cfg.waypointMarkerStyle != 0) {
            return;
        }
        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        TextRenderer tr = client.textRenderer;
        Frustum frustum = ctx.frustum();
        String currentDim = WaypointsModule.currentDimensionId(client);
        matrices.push();
        for (MapConfig.Waypoint wp : cfg.waypoints) {
            if (!wp.visible || !WaypointsModule.isInDimension(wp, currentDim) || WaypointsModule.isSameGuideTarget(cfg.guideTarget, wp) || !WaypointsModule.isBeaconVisible(frustum, wp.x, wp.y, wp.z)) continue;
            WaypointsModule.drawBeacon(matrices, immediate, camera, cam, tr, wp.x, wp.y, wp.z, wp.label, 0xFF000000 | wp.colorRgb);
        }
        if (cfg.guideTarget != null && !"Waypoint".equals(cfg.guideTarget.type)) {
            MapConfig.GuideTarget t = cfg.guideTarget;
            if (WaypointsModule.isBeaconVisible(frustum, t.x, t.y, t.z)) {
                WaypointsModule.drawBeacon(matrices, immediate, camera, cam, tr, t.x, t.y, t.z, t.label, 0xFF000000 | t.colorRgb);
            }
        }
        matrices.pop();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth((float)4.0f);
        immediate.draw();
        RenderSystem.lineWidth((float)1.0f);
        RenderSystem.enableDepthTest();
    }

    public static boolean isBeaconVisible(Frustum frustum, double wx, double wy, double wz) {
        return frustum.isVisible(new Box(wx - 0.5, wy, wz - 0.5, wx + 0.5, wy + 4.0, wz + 0.5));
    }

    /** Registry id of the player's current dimension (e.g. "minecraft:overworld"), or null. */
    public static String currentDimensionId(MinecraftClient client) {
        return client != null && client.world != null
                ? client.world.getRegistryKey().getValue().toString()
                : null;
    }

    /**
     * Whether a waypoint should be shown in the current dimension. A null waypoint dimension
     * (created before dimension support, or when the dimension is unknown) is treated as
     * visible everywhere for backward compatibility.
     */
    public static boolean isInDimension(MapConfig.Waypoint wp, String currentDim) {
        return wp.dimension == null || wp.dimension.equals(currentDim);
    }

    private static boolean isSameGuideTarget(MapConfig.GuideTarget target, MapConfig.Waypoint waypoint) {
        return target != null && "Waypoint".equals(target.type) && Math.abs(target.x - waypoint.x) < 0.01 && Math.abs(target.z - waypoint.z) < 0.01;
    }

    public static void drawBeacon(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Camera camera, Vec3d cam, TextRenderer tr, double wx, double wy, double wz, String label, int color) {
        double dx = wx - cam.x;
        double dy = wy - cam.y;
        double dz = wz - cam.z;
        double labelY = dy + 2.35;
        double dist = Math.sqrt(dx * dx + labelY * labelY + dz * dz);
        String text = label + " [" + (int)Math.round(dist) + "m]";
        WaypointsModule.drawBeaconShape(matrices, immediate, BEACON_LINES_BEHIND, dx, dy, dz, color, 0.35f);
        WaypointsModule.drawBeaconShape(matrices, immediate, BEACON_LINES_FRONT, dx, dy, dz, color, 0.95f);
        WaypointsModule.drawWorldText(matrices, immediate, camera, tr, "+", color, dx, dy + 1.05, dz, (float)Math.min(0.18, Math.max(0.045, dist * 0.0038)), -1879048192);
        WaypointsModule.drawWorldText(matrices, immediate, camera, tr, text, color, dx, labelY, dz, (float)Math.min(0.15, Math.max(0.033, dist * 0.0032)), -1610612736);
    }

    private static void drawBeaconShape(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, RenderLayer layer, double dx, double dy, double dz, int color, float alpha) {
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
        VertexConsumer lines = immediate.getBuffer(layer);
        MatrixStack.Entry entry = matrices.peek();
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

    private static void drawWorldText(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Camera camera, TextRenderer tr, String text, int color, double x, double y, double z, float scale, int bgColor) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(camera.getRotation());
        matrices.scale(-scale, -scale, scale);
        int w = tr.getWidth(text);
        tr.draw(text, (float)(-w) / 2.0f, -4.0f, color, false, matrices.peek().getPositionMatrix(), (VertexConsumerProvider)immediate, TextRenderer.TextLayerType.SEE_THROUGH, bgColor, 0xF000F0);
        tr.draw(text, (float)(-w) / 2.0f, -4.0f, color, false, matrices.peek().getPositionMatrix(), (VertexConsumerProvider)immediate, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        matrices.pop();
    }

    private static void drawLine(VertexConsumer lines, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        lines.vertex(entry, x1, y1, z1).color(r, g, b, a).normal(entry, 0.0f, 1.0f, 0.0f);
        lines.vertex(entry, x2, y2, z2).color(r, g, b, a).normal(entry, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new WaypointsOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        boolean alive;
        if (client.player == null) {
            this.wasAlive = true;
            this.deadTicks = 0;
            this.lastLivePos = null;
            return;
        }
        boolean bl = alive = client.player.getHealth() > 0.0f && !client.player.isRemoved();
        if (alive) {
            this.wasAlive = true;
            this.deadTicks = 0;
            this.lastLivePos = client.player.getPos();
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

    private void createDeathWaypoint(Vec3d pos) {
        MapConfig.Waypoint wp = new MapConfig.Waypoint();
        wp.id = UUID.randomUUID().toString();
        wp.label = "Mort " + new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date());
        wp.x = pos.x;
        wp.y = pos.y;
        wp.z = pos.z;
        wp.colorRgb = 0xEF4444;
        wp.createdAt = System.currentTimeMillis();
        wp.visible = true;
        wp.dimension = WaypointsModule.currentDimensionId(MinecraftClient.getInstance());
        this.config().waypoints.add(wp);
        this.configManager.save();
    }

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        double relative;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.currentScreen != null) {
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
        double dx = target.x - client.player.getX();
        double dz = target.z - client.player.getZ();
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        for (relative = bearing - (double)client.player.getYaw(); relative > 180.0; relative -= 360.0) {
        }
        while (relative < -180.0) {
            relative += 360.0;
        }
        this.renderGuide(ctx, target, Math.hypot(dx, dz), relative);
    }

    private void renderHudBeacons(DrawContext ctx, MinecraftClient client, MapConfig cfg, float tickDelta) {
        String currentDim = WaypointsModule.currentDimensionId(client);
        for (MapConfig.Waypoint wp : cfg.waypoints) {
            if (!wp.visible || !WaypointsModule.isInDimension(wp, currentDim) || WaypointsModule.isSameGuideTarget(cfg.guideTarget, wp)) continue;
            WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, wp.x, wp.y, wp.z, wp.label, 0xFF000000 | wp.colorRgb);
        }
        if (cfg.guideTarget != null && !"Waypoint".equals(cfg.guideTarget.type)) {
            MapConfig.GuideTarget target = cfg.guideTarget;
            WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, target.x, target.y, target.z, target.label, 0xFF000000 | target.colorRgb);
        }
    }

    public static void renderScreenBeacon(DrawContext ctx, MinecraftClient client, double wx, double wy, double wz, String label, int color) {
        WaypointsModule.renderScreenBeacon(ctx, client, 1.0f, wx, wy, wz, label, color);
    }

    public static void renderScreenBeacon(DrawContext ctx, MinecraftClient client, float tickDelta, double wx, double wy, double wz, String label, int color) {
        double yNorm;
        double xNorm;
        if (client == null || client.player == null || client.gameRenderer == null) {
            return;
        }
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();
        Vec3d toTarget = new Vec3d(wx - cam.x, wy + 1.45 - cam.y, wz - cam.z);
        double distance = toTarget.length();
        if (distance < 0.35) {
            return;
        }
        int screenW = ctx.getScaledWindowWidth();
        int screenH = ctx.getScaledWindowHeight();
        Vec3d forward = Vec3d.fromPolar((float)camera.getPitch(), (float)camera.getYaw()).normalize();
        Vec3d right = Vec3d.fromPolar((float)0.0f, (float)(camera.getYaw() + 90.0f)).normalize();
        Vec3d up = right.crossProduct(forward).normalize();
        double depth = toTarget.dotProduct(forward);
        double xCamera = toTarget.dotProduct(right);
        double yCamera = toTarget.dotProduct(up);
        double verticalFov = Math.toRadians(WaypointsModule.clamp(((GameRendererAccessor)client.gameRenderer).zombiezcompanion$invokeGetFov(camera, tickDelta, true), 12.0, 110.0));
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
        TextRenderer tr = client.textRenderer;
        String safeLabel = label == null || label.isBlank() ? "Rep\u00e8re" : label;
        String text = safeLabel + " [" + (int)Math.round(distance) + "m]";
        int markerR = 6;
        WaypointsModule.drawHudDiamond(ctx, x, y, markerR + 2, -872415232);
        WaypointsModule.drawHudDiamond(ctx, x, y, markerR, color);
        ctx.fill(x - 1, y - markerR - 5, x + 2, y - markerR, color);
        ctx.fill(x - 1, y + markerR, x + 2, y + markerR + 6, color);
        ctx.fill(x - 1, y - 1, x + 2, y + 2, -1);
        int textW = Math.min(320, tr.getWidth(text) + 12);
        int boxX = (int)WaypointsModule.clamp((double)x - (double)textW / 2.0, 6.0, screenW - textW - 6);
        int boxY = (int)WaypointsModule.clamp(y - markerR - 22, 6.0, screenH - 18);
        ctx.fill(boxX + 1, boxY + 1, boxX + textW + 1, boxY + 17, -1442840576);
        ctx.fill(boxX, boxY, boxX + textW, boxY + 16, -804647918);
        ctx.drawBorder(boxX, boxY, textW, 16, color);
        ctx.drawTextWithShadow(tr, tr.trimToWidth(text, textW - 8), boxX + 5, boxY + 5, -1);
    }

    private static void drawHudDiamond(DrawContext ctx, int x, int y, int radius, int color) {
        for (int dy = -radius; dy <= radius; ++dy) {
            int half = radius - Math.abs(dy);
            ctx.fill(x - half, y + dy, x + half + 1, y + dy + 1, color);
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

    private void renderGuide(DrawContext ctx, GuidePoint target, double distance, double relativeAngle) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        MutableText label = Text.translatable((String)"zombiezcompanion.waypoint.guide.label", (Object[])new Object[]{target.label, (int)Math.round(distance)});
        int boxW = Math.max(100, textRenderer.getWidth((StringVisitable)label) + 34);
        int boxH = 22;
        int[] pos = this.guidePosition(ctx, boxW, boxH);
        int x = pos[0];
        int y = pos[1];
        ctx.fill(x + 2, y + 2, x + boxW + 2, y + boxH + 2, -1442840576);
        ctx.fill(x, y, x + boxW, y + boxH, -183627755);
        ctx.drawBorder(x, y, boxW, boxH, -8874241);
        ctx.getMatrices().push();
        ctx.getMatrices().translate((float)(x + 12), (float)(y + 11), 0.0f);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)relativeAngle));
        this.drawGuideArrow(ctx, 0xFF000000 | target.colorRgb);
        ctx.getMatrices().pop();
        ctx.drawTextWithShadow(textRenderer, (Text)label, x + 26, y + 7, -1);
    }

    private void drawGuideArrow(DrawContext ctx, int color) {
        int shadow = -872415232;
        ctx.fill(-1, -7, 2, 7, shadow);
        ctx.fill(-4, -5, 5, -2, shadow);
        ctx.fill(-6, -2, 7, 1, shadow);
        ctx.fill(-1, -6, 2, 5, color);
        ctx.fill(-3, -5, 4, -2, color);
        ctx.fill(-5, -2, 6, 1, color);
        ctx.fill(-1, 3, 2, 6, -1);
    }

    private int[] guidePosition(DrawContext ctx, int boxW, int boxH) {
        int y;
        int x;
        MapConfig cfg = this.config();
        int windowW = ctx.getScaledWindowWidth();
        int windowH = ctx.getScaledWindowHeight();
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
        return Text.translatable((String)key).getString();
    }

    private record GuidePoint(String label, double x, double z, int colorRgb) {
    }

    private static final class BeaconLayer
    extends RenderLayer {
        private BeaconLayer(String name, VertexFormat fmt, VertexFormat.DrawMode mode, int size, boolean crumbling, boolean translucent, Runnable startAction, Runnable endAction) {
            super(name, fmt, mode, size, crumbling, translucent, startAction, endAction);
        }

        static RenderLayer build(String suffix, boolean throughWall) {
            return RenderLayer.of((String)("zombiezcompanion:beacon_" + suffix), (VertexFormat)VertexFormats.LINES, (VertexFormat.DrawMode)VertexFormat.DrawMode.DEBUG_LINES, (int)1536, (boolean)false, (boolean)false, (RenderLayer.MultiPhaseParameters)RenderLayer.MultiPhaseParameters.builder().program(LINES_PROGRAM).lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(throughWall ? 2.5 : 4.0))).layering(VIEW_OFFSET_Z_LAYERING).transparency(TRANSLUCENT_TRANSPARENCY).target(ITEM_ENTITY_TARGET).writeMaskState(ALL_MASK).cull(DISABLE_CULLING).depthTest(throughWall ? ALWAYS_DEPTH_TEST : LEQUAL_DEPTH_TEST).build(false));
        }
    }
}

