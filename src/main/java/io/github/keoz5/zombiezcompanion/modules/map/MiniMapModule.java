package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.hud.HudAnchor;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.modules.map.MiniMapOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapData;
import io.github.keoz5.zombiezcompanion.modules.telemetry.PresenceCache;
import java.util.List;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.RotationAxis;

public final class MiniMapModule
implements Module {
    public static final String ID = "mini_map";
    public static final int PEEK_MIN = 120;
    public static final int PEEK_MAX = 400;
    private ConfigManager configManager;
    private KeyBinding peekKey;

    public KeyBinding peekKey() {
        return this.peekKey;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Mini-map";
    }

    @Override
    public String description() {
        return Text.translatable((String)"zombiezcompanion.module.mini_map.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.MAP;
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
        return List.of("minimap", "mini-map", "carte", "map", "radar", "taille", "zoom");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
        this.peekKey = KeyBindingHelper.registerKeyBinding((KeyBinding)new KeyBinding("key.zombiezcompanion.minimap_peek", InputUtil.Type.KEYSYM, 79, "key.categories.zombiezcompanion"));
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new MiniMapOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        int y;
        int x;
        boolean peeking;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.currentScreen != null) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        MapConfig cfg = this.configManager.get().map;
        int size = Math.max(80, Math.min(260, cfg.miniMapSize));
        boolean bl = peeking = this.peekKey != null && this.peekKey.isPressed();
        if (peeking) {
            size = Math.max(size, Math.min(400, Math.max(120, cfg.miniMapPeekSize)));
        }
        int screenW = ctx.getScaledWindowWidth();
        int screenH = ctx.getScaledWindowHeight();
        HudConfig hud = this.configManager.get().hud;
        if (HudAnchor.hasCustom(hud, ID)) {
            x = HudAnchor.resolveX(hud, ID, screenW, size, 1.0);
            y = HudAnchor.resolveY(hud, ID, screenH, size, 1.0);
        } else {
            int margin = 8;
            x = switch (cfg.miniMapCorner) {
                case 0, 2 -> margin;
                default -> screenW - size - margin;
            };
            y = switch (cfg.miniMapCorner) {
                case 2, 3 -> screenH - size - margin;
                default -> margin;
            };
            int offset = Math.max(-200, Math.min(200, cfg.miniMapOffsetY));
            y = Math.max(0, Math.min(screenH - size, y + offset));
        }
        this.renderMiniMap(ctx, client, cfg, x, y, size);
        HudElements.report(ID, x, y, size, size);
    }

    private void renderMiniMap(DrawContext ctx, MinecraftClient client, MapConfig cfg, int x, int y, int size) {
        double zoom = Math.max(0.25, Math.min(2.0, cfg.miniMapZoom));
        double centerX = ZombieZMapData.mapX(client.player.getX());
        double centerY = ZombieZMapData.mapY(client.player.getZ());
        double viewX = MiniMapModule.clamp(centerX - (double)size / (2.0 * zoom), 0.0, Math.max(0.0, 1242.0 - (double)size / zoom));
        double viewY = MiniMapModule.clamp(centerY - (double)size / (2.0 * zoom), 0.0, Math.max(0.0, 10432.0 - (double)size / zoom));
        ctx.fill(x + 1, y + 2, x + size + 2, y + size + 3, -1442840576);
        ctx.fill(x - 1, y - 1, x + size + 1, y + size + 1, -15658216);
        ctx.drawBorder(x - 1, y - 1, size + 2, size + 2, -8874241);
        ctx.enableScissor(x, y, x + size, y + size);
        this.renderTiles(ctx, x, y, size, viewX, viewY, zoom);
        this.renderMiniMarkers(ctx, cfg, x, y, size, viewX, viewY, zoom);
        this.renderMiniPresences(ctx, x, y, size, viewX, viewY, zoom);
        int px = (int)Math.round((double)x + (centerX - viewX) * zoom);
        int py = (int)Math.round((double)y + (centerY - viewY) * zoom);
        MiniMapModule.drawHeadingArrow(ctx, px, py, client.player.getYaw());
        this.renderMiniWaypoints(ctx, client, cfg, x, y, size, viewX, viewY, zoom);
        ctx.disableScissor();
    }

    private void renderMiniPresences(DrawContext ctx, int screenX, int screenY, int size, double viewX, double viewY, double zoom) {
        if (!ZombieZCompanionClient.configManager().get().map.showModUsers) {
            return;
        }
        List<PresenceCache.Presence> list = PresenceCache.presences();
        if (list.isEmpty()) {
            return;
        }
        for (PresenceCache.Presence p : list) {
            int px = (int)Math.round((double)screenX + (ZombieZMapData.mapX(p.x()) - viewX) * zoom);
            int py = (int)Math.round((double)screenY + (ZombieZMapData.mapY(p.z()) - viewY) * zoom);
            if (px < screenX - 4 || px > screenX + size + 4 || py < screenY - 4 || py > screenY + size + 4) continue;
            ctx.fill(px - 2, py - 2, px + 3, py + 3, -587202560);
            ctx.fill(px - 1, py - 1, px + 2, py + 2, -13058568);
        }
    }

    private void renderTiles(DrawContext ctx, int screenX, int screenY, int size, double viewX, double viewY, double zoom) {
        int startCx = Math.max(0, (int)Math.floor(viewX / 128.0));
        int endCx = Math.min(9, (int)Math.floor((viewX + (double)size / zoom) / 128.0));
        int startCy = Math.max(0, (int)Math.floor(viewY / 128.0));
        int endCy = Math.min(81, (int)Math.floor((viewY + (double)size / zoom) / 128.0));
        int drawSize = (int)Math.ceil(128.0 * zoom) + 1;
        for (int cy = startCy; cy <= endCy; ++cy) {
            for (int cx = startCx; cx <= endCx; ++cx) {
                int x = (int)Math.floor((double)screenX + ((double)(cx * 128) - viewX) * zoom);
                int y = (int)Math.floor((double)screenY + ((double)(cy * 128) - viewY) * zoom);
                ctx.drawTexture(RenderLayer::getGuiTextured, ZombieZMapData.tileId(cx, cy), x, y, 0.0f, 0.0f, drawSize, drawSize, 128, 128, 128, 128);
            }
        }
    }

    private void renderMiniMarkers(DrawContext ctx, MapConfig cfg, int screenX, int screenY, int size, double viewX, double viewY, double zoom) {
        MinecraftClient client = MinecraftClient.getInstance();
        ZombieZMapData.Zone playerZone = this.playerZone(client);
        if (cfg.showZones) {
            for (ZombieZMapData.Zone zone : ZombieZMapData.ZONES) {
                int labelY;
                if (zone.placeholder() || zone.zMax() <= zone.zMin()) continue;
                int yMin = (int)Math.round((double)screenY + (ZombieZMapData.mapY(zone.zMin()) - viewY) * zoom);
                int yMax = (int)Math.round((double)screenY + (ZombieZMapData.mapY(zone.zMax()) - viewY) * zoom);
                int color = 0xAA000000 | ZombieZMapData.zoneColor(zone.num());
                if (yMin >= screenY && yMin <= screenY + size) {
                    ctx.fill(screenX, yMin, screenX + size, yMin + 1, color);
                }
                if (yMax >= screenY && yMax <= screenY + size) {
                    ctx.fill(screenX, yMax, screenX + size, yMax + 1, color);
                }
                if (!(zoom >= 0.7) || (labelY = (yMin + yMax) / 2 - 4) < screenY + 2 || labelY > screenY + size - 10) continue;
                MinecraftClient mc = MinecraftClient.getInstance();
                String label = "Z" + zone.num();
                int textColor = playerZone != null && playerZone.num() == zone.num() ? -1 : -855638017;
                ctx.drawTextWithShadow(mc.textRenderer, (Text)Text.literal((String)label), screenX + 3, labelY, textColor);
            }
        }
        if (cfg.showRefuges) {
            int fill = 1143842406;
            int border = -13254793;
            for (ZombieZMapData.Refuge refuge : ZombieZMapData.REFUGES) {
                int x1 = (int)Math.round((double)screenX + (ZombieZMapData.mapX(refuge.xMin()) - viewX) * zoom);
                int x2 = (int)Math.round((double)screenX + (ZombieZMapData.mapX(refuge.xMax()) - viewX) * zoom);
                int y1 = (int)Math.round((double)screenY + (ZombieZMapData.mapY(refuge.zMin()) - viewY) * zoom);
                int y2 = (int)Math.round((double)screenY + (ZombieZMapData.mapY(refuge.zMax()) - viewY) * zoom);
                int lx1 = Math.max(screenX, Math.min(screenX + size, x1));
                int lx2 = Math.max(screenX, Math.min(screenX + size, x2));
                int ly1 = Math.max(screenY, Math.min(screenY + size, y1));
                int ly2 = Math.max(screenY, Math.min(screenY + size, y2));
                if (lx2 <= lx1 || ly2 <= ly1) continue;
                ctx.fill(lx1, ly1, lx2, ly2, fill);
                if (y1 >= screenY && y1 <= screenY + size) {
                    ctx.fill(lx1, y1, lx2, y1 + 1, border);
                }
                if (y2 >= screenY && y2 <= screenY + size) {
                    ctx.fill(lx1, y2 - 1, lx2, y2, border);
                }
                if (x1 >= screenX && x1 <= screenX + size) {
                    ctx.fill(x1, ly1, x1 + 1, ly2, border);
                }
                if (x2 < screenX || x2 > screenX + size) continue;
                ctx.fill(x2 - 1, ly1, x2, ly2, border);
            }
        }
        if (cfg.showCranes || this.isGuideType(cfg, "Crane")) {
            for (Record record : ZombieZMapData.ZONES) {
                for (ZombieZMapData.Point skull : ((ZombieZMapData.Zone)record).skulls()) {
                    boolean guided = this.isGuideTarget(cfg, "Crane", skull.x(), skull.z());
                    if ((!cfg.showCranes || !this.isSameZone(playerZone, (ZombieZMapData.Zone)record)) && !guided) continue;
                    this.drawMiniSmartMarker(ctx, screenX, screenY, size, viewX, viewY, zoom, skull.x(), skull.z(), skull.colorRgb(), guided ? 3 : 1);
                }
            }
        }
        if (cfg.showBosses || this.isGuideType(cfg, "Boss")) {
            for (Record record : ZombieZMapData.BOSSES) {
                boolean guided = this.isGuideTarget(cfg, "Boss", ((ZombieZMapData.Boss)record).x(), ((ZombieZMapData.Boss)record).z());
                if ((!cfg.showBosses || playerZone == null || ((ZombieZMapData.Boss)record).zone() != playerZone.num()) && !guided) continue;
                this.drawMiniSmartMarker(ctx, screenX, screenY, size, viewX, viewY, zoom, ((ZombieZMapData.Boss)record).x(), ((ZombieZMapData.Boss)record).z(), 0xEF4444, guided ? 4 : 2);
            }
        }
    }

    private void renderMiniWaypoints(DrawContext ctx, MinecraftClient client, MapConfig cfg, int screenX, int screenY, int size, double viewX, double viewY, double zoom) {
        if (!WaypointsModule.isEnabled()) {
            return;
        }
        if (!cfg.showWaypoints && !this.isGuideType(cfg, "Waypoint")) {
            return;
        }
        for (MapConfig.Waypoint waypoint : cfg.waypoints) {
            if (!waypoint.visible) continue;
            boolean guided = this.isGuideTarget(cfg, "Waypoint", waypoint.x, waypoint.z);
            if (!cfg.showWaypoints && !guided) continue;
            this.drawMiniSmartMarker(ctx, screenX, screenY, size, viewX, viewY, zoom, waypoint.x, waypoint.z, waypoint.colorRgb, guided ? 5 : 3);
        }
    }

    private void drawMiniMarker(DrawContext ctx, int screenX, int screenY, int size, double viewX, double viewY, double zoom, double worldX, double worldZ, int rgb, int radius) {
        int x = (int)Math.round((double)screenX + (ZombieZMapData.mapX(worldX) - viewX) * zoom);
        int y = (int)Math.round((double)screenY + (ZombieZMapData.mapY(worldZ) - viewY) * zoom);
        if (x < screenX || x > screenX + size || y < screenY || y > screenY + size) {
            return;
        }
        ctx.fill(x - radius - 1, y - radius - 1, x + radius + 2, y + radius + 2, -1442840576);
        ctx.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, 0xFF000000 | rgb & 0xFFFFFF);
    }

    private void drawMiniSmartMarker(DrawContext ctx, int screenX, int screenY, int size, double viewX, double viewY, double zoom, double worldX, double worldZ, int rgb, int radius) {
        double rawX = (double)screenX + (ZombieZMapData.mapX(worldX) - viewX) * zoom;
        double rawY = (double)screenY + (ZombieZMapData.mapY(worldZ) - viewY) * zoom;
        if (rawX >= (double)screenX && rawX <= (double)(screenX + size) && rawY >= (double)screenY && rawY <= (double)(screenY + size)) {
            this.drawMiniMarker(ctx, screenX, screenY, size, viewX, viewY, zoom, worldX, worldZ, rgb, radius);
            return;
        }
        int edgePad = 6;
        int x = (int)Math.round(MiniMapModule.clamp(rawX, screenX + edgePad, screenX + size - edgePad));
        int y = (int)Math.round(MiniMapModule.clamp(rawY, screenY + edgePad, screenY + size - edgePad));
        int color = 0xFF000000 | rgb & 0xFFFFFF;
        this.drawMiniDiamond(ctx, x, y, radius + 3, -805306368);
        this.drawMiniDiamond(ctx, x, y, radius + 1, color);
        ctx.fill(x - 1, y - 1, x + 2, y + 2, -1);
    }

    private void drawMiniDiamond(DrawContext ctx, int x, int y, int radius, int color) {
        for (int dy = -radius; dy <= radius; ++dy) {
            int half = radius - Math.abs(dy);
            ctx.fill(x - half, y + dy, x + half + 1, y + dy + 1, color);
        }
    }

    private static void drawHeadingArrow(DrawContext ctx, int px, int py, float yaw) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate((float)px, (float)py, 0.0f);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw + 180.0f));
        ctx.fill(-1, -5, 2, 4, -872415232);
        ctx.fill(-3, -3, 4, -1, -872415232);
        ctx.fill(0, -4, 1, 3, -1);
        ctx.fill(-2, -2, 3, -1, -1);
        ctx.fill(0, -3, 1, 2, -8874241);
        ctx.getMatrices().pop();
    }

    public MapConfig config() {
        return this.configManager.get().map;
    }

    private ZombieZMapData.Zone playerZone(MinecraftClient client) {
        if (client == null || client.player == null) {
            return null;
        }
        return this.zoneForZ(client.player.getZ());
    }

    private ZombieZMapData.Zone zoneForZ(double z) {
        for (ZombieZMapData.Zone zone : ZombieZMapData.ZONES) {
            if (zone.placeholder() || zone.zMax() <= zone.zMin() || !(z >= (double)zone.zMin()) || !(z <= (double)zone.zMax())) continue;
            return zone;
        }
        return null;
    }

    private boolean isSameZone(ZombieZMapData.Zone a, ZombieZMapData.Zone b) {
        return a != null && b != null && a.num() == b.num();
    }

    private boolean isInZone(ZombieZMapData.Zone zone, double z) {
        return zone != null && z >= (double)zone.zMin() && z <= (double)zone.zMax();
    }

    private boolean isGuideType(MapConfig cfg, String type) {
        return cfg.guideTarget != null && type.equals(cfg.guideTarget.type);
    }

    private boolean isGuideTarget(MapConfig cfg, String type, double x, double z) {
        return cfg.guideTarget != null && type.equals(cfg.guideTarget.type) && Math.abs(cfg.guideTarget.x - x) < 0.01 && Math.abs(cfg.guideTarget.z - z) < 0.01;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

