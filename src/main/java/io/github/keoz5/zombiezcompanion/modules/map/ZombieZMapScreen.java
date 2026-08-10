package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.config.PlayersConfig;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointDeleteConfirmScreen;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointEditScreen;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapData;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsModule;
import io.github.keoz5.zombiezcompanion.modules.telemetry.PresenceCache;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;

public final class ZombieZMapScreen
extends Screen {
    private static final double MIN_ZOOM = 0.12;
    private static final double MAX_ZOOM = 3.0;
    private static final double INITIAL_ZOOM_MULTIPLIER = 2.3;
    private static final int VIEW_MARGIN = 8;
    private static final int LEFT_PANEL_W = 132;
    private static final int CONTROL_X = 8;
    private static final int SEARCH_Y = 38;
    private static final int SEARCH_H = 20;
    private static final int CONTROL_Y = 72;
    private static final int CONTROL_W = 124;
    private static final int CONTROL_H = 20;
    private static final int CONTROL_GAP = 5;
    private static final int BG = -301463026;
    private static final int PANEL_BG = -183627755;
    private static final int PANEL_MUTED_BORDER = -13880766;
    private static final double CLICK_DRAG_THRESHOLD = 4.0;
    private static final double WAYPOINT_HIT_RADIUS = 5.0;
    private static final double BOSS_HIT_RADIUS = 6.0;
    private static final double SKULL_HIT_RADIUS = 5.0;
    private static final double PLAYER_HIT_RADIUS = 5.0;
    private final ConfigManager configManager;
    private final Double initialWorldX;
    private final Double initialWorldZ;
    private double zoom = 1.0;
    private double viewX;
    private double viewY;
    private boolean initializedView;
    private boolean waypointClickCandidate;
    private boolean waypointClickDragged;
    private double waypointClickStartX;
    private double waypointClickStartY;
    private EditBox searchField;
    private String searchText = "";
    private SearchMatch searchMatch;

    public ZombieZMapScreen(ConfigManager configManager) {
        super((Component)Component.translatable((String)"zombiezcompanion.map.title"));
        this.configManager = configManager;
        this.initialWorldX = null;
        this.initialWorldZ = null;
    }

    public ZombieZMapScreen(ConfigManager configManager, double initialWorldX, double initialWorldZ) {
        super((Component)Component.translatable((String)"zombiezcompanion.map.title"));
        this.configManager = configManager;
        this.initialWorldX = initialWorldX;
        this.initialWorldZ = initialWorldZ;
    }

    protected void init() {
        if (!this.initializedView) {
            this.zoom = ZombieZMapScreen.clamp(this.fitZoom() * 2.3, 0.12, 3.0);
            if (this.initialWorldX != null && this.initialWorldZ != null) {
                this.centerOn(ZombieZMapData.mapX(this.initialWorldX), ZombieZMapData.mapY(this.initialWorldZ));
            } else {
                this.centerOnPlayerOrSpawn();
            }
            this.initializedView = true;
        }
        this.clampView();
        this.searchField = new EditBox(this.font, 10, 38, 120, 20, (Component)Component.translatable((String)"zombiezcompanion.map.search.placeholder"));
        this.searchField.setMaxLength(40);
        this.searchField.setHint((Component)Component.translatable((String)"zombiezcompanion.map.search.placeholder"));
        this.searchField.setValue(this.searchText);
        this.searchField.setResponder(s -> {
            this.searchText = s;
            this.searchMatch = this.computeSearchMatch((String)s);
        });
        this.addRenderableWidget(this.searchField);
        this.searchMatch = this.computeSearchMatch(this.searchText);
    }

    private SearchMatch computeSearchMatch(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String needle = query.toLowerCase(Locale.ROOT).trim();
        for (MapConfig.Waypoint wp : this.config().waypoints) {
            if (wp.label == null || !wp.label.toLowerCase(Locale.ROOT).contains(needle)) continue;
            return new SearchMatch(wp.label, wp.x, wp.z, wp.colorRgb);
        }
        for (ZombieZMapData.Refuge refuge : ZombieZMapData.REFUGES) {
            if (!refuge.name().toLowerCase(Locale.ROOT).contains(needle)) continue;
            double cx = (double)(refuge.xMin() + refuge.xMax()) / 2.0;
            double cz = (double)(refuge.zMin() + refuge.zMax()) / 2.0;
            return new SearchMatch(refuge.name(), cx, cz, 3522423);
        }
        for (Record record : ZombieZMapData.BOSSES) {
            if (!((ZombieZMapData.Boss)record).name().toLowerCase(Locale.ROOT).contains(needle)) continue;
            return new SearchMatch(((ZombieZMapData.Boss)record).name(), ((ZombieZMapData.Boss)record).x(), ((ZombieZMapData.Boss)record).z(), 0xEF4444);
        }
        for (Record record : ZombieZMapData.ZONES) {
            if (!((ZombieZMapData.Zone)record).name().toLowerCase(Locale.ROOT).contains(needle) && !("z" + ((ZombieZMapData.Zone)record).num()).equals(needle) && !String.valueOf(((ZombieZMapData.Zone)record).num()).equals(needle)) continue;
            double cz = (double)(((ZombieZMapData.Zone)record).zMin() + ((ZombieZMapData.Zone)record).zMax()) / 2.0;
            return new SearchMatch("Zone " + ((ZombieZMapData.Zone)record).num() + " - " + ((ZombieZMapData.Zone)record).name(), 621.0, cz, 7902975);
        }
        for (Record record : ZombieZMapData.ZONES) {
            for (ZombieZMapData.Point skull : ((ZombieZMapData.Zone)record).skulls()) {
                if (!skull.label().toLowerCase(Locale.ROOT).contains(needle)) continue;
                return new SearchMatch(skull.label(), skull.x(), skull.z(), skull.colorRgb());
            }
        }
        return null;
    }

    private void jumpToSearchMatch() {
        if (this.searchMatch == null) {
            return;
        }
        double targetZoom = Math.min(3.0, Math.max(this.zoom, 1.4));
        this.zoom = ZombieZMapScreen.clamp(targetZoom, 0.12, 3.0);
        this.centerOn(ZombieZMapData.mapX(this.searchMatch.x), ZombieZMapData.mapY(this.searchMatch.z));
    }

    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, -301463026);
        this.renderBackdrop(ctx);
        int clipX1 = (int)Math.max((double)this.viewportLeft(), Math.floor(this.mapLeft()));
        int clipY1 = (int)Math.max((double)this.viewportTop(), Math.floor(this.mapTop()));
        int clipX2 = (int)Math.min((double)this.viewportRight(), Math.ceil(this.mapRight()));
        int clipY2 = (int)Math.min((double)this.viewportBottom(), Math.ceil(this.mapBottom()));
        if (clipX2 > clipX1 && clipY2 > clipY1) {
            ctx.enableScissor(clipX1, clipY1, clipX2, clipY2);
            this.renderTiles(ctx);
            this.renderLayers(ctx);
            this.renderPresences(ctx);
            this.renderPlayerMarker(ctx);
            this.renderSkullBeacons(ctx);
            if (this.config().showWaypoints && WaypointsModule.isEnabled()) {
                this.renderWaypoints(ctx);
            }
            ctx.disableScissor();
        }
        this.renderChrome(ctx, mouseX, mouseY);
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        this.renderHoverInfo(ctx, mouseX, mouseY);
        this.renderOffServerOverlay(ctx);
    }

    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    }

    private void renderOffServerOverlay(GuiGraphicsExtractor ctx) {
        if (ZombieZDetector.isOnZombieZ()) {
            return;
        }
        int boxW = 300;
        int boxH = 64;
        int bx = (this.width - boxW) / 2;
        int by = (this.height - boxH) / 2;
        ctx.fill(bx + 2, by + 4, bx + boxW + 2, by + boxH + 4, -1442840576);
        ctx.fill(bx, by, bx + boxW, by + boxH, -183627755);
        ctx.fill(bx, by, bx + boxW, by + 2, -8874241);
        ctx.outline(bx, by, boxW, boxH, -8874241);
        ctx.centeredText(this.font, (Component)Component.translatable((String)"zombiezcompanion.map.off_server.title"), bx + boxW / 2, by + 14, -854792);
        ctx.centeredText(this.font, (Component)Component.translatable((String)"zombiezcompanion.map.off_server.hint"), bx + boxW / 2, by + 30, -8353376);
        ctx.centeredText(this.font, (Component)Component.translatable((String)"zombiezcompanion.map.off_server.host"), bx + boxW / 2, by + 46, -8874241);
    }

    private void renderTiles(GuiGraphicsExtractor ctx) {
        int startCx = Math.max(0, (int)Math.floor(this.screenToMapX(this.viewportLeft()) / 128.0));
        int endCx = Math.min(9, (int)Math.floor(this.screenToMapX(this.viewportRight()) / 128.0));
        int startCy = Math.max(0, (int)Math.floor(this.screenToMapY(this.viewportTop()) / 128.0));
        int endCy = Math.min(81, (int)Math.floor(this.screenToMapY(this.viewportBottom()) / 128.0));
        int drawSize = (int)Math.ceil(128.0 * this.zoom) + 1;
        for (int cy = startCy; cy <= endCy; ++cy) {
            for (int cx = startCx; cx <= endCx; ++cx) {
                int x = this.screenX(cx * 128);
                int y = this.screenY(cy * 128);
                ctx.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ZombieZMapData.tileId(cx, cy), x, y, 0.0f, 0.0f, drawSize, drawSize, 128, 128, 128, 128);
            }
        }
    }

    private void renderLayers(GuiGraphicsExtractor ctx) {
        MapConfig cfg = this.config();
        if (cfg.showZones) {
            this.renderZones(ctx);
        }
        if (cfg.showRefuges) {
            this.renderRefuges(ctx);
        }
        if (cfg.showCranes) {
            this.renderCranes(ctx);
        }
        if (cfg.showBosses) {
            this.renderBosses(ctx);
        }
    }

    private void renderRefuges(GuiGraphicsExtractor ctx) {
        int fill = 1143842406;
        int border = -13254793;
        for (ZombieZMapData.Refuge refuge : ZombieZMapData.REFUGES) {
            int x1 = this.screenX(ZombieZMapData.mapX(refuge.xMin()));
            int x2 = this.screenX(ZombieZMapData.mapX(refuge.xMax()));
            int y1 = this.screenY(ZombieZMapData.mapY(refuge.zMin()));
            int y2 = this.screenY(ZombieZMapData.mapY(refuge.zMax()));
            if (x2 < this.viewportLeft() || x1 > this.viewportRight() || y2 < this.viewportTop() || y1 > this.viewportBottom()) continue;
            ctx.fill(x1, y1, x2, y2, fill);
            ctx.fill(x1, y1, x2, y1 + 1, border);
            ctx.fill(x1, y2 - 1, x2, y2, border);
            ctx.fill(x1, y1, x1 + 1, y2, border);
            ctx.fill(x2 - 1, y1, x2, y2, border);
            if (!(this.zoom >= 0.55)) continue;
            MutableComponent label = Component.literal((String)refuge.name());
            int labelX = (x1 + x2) / 2 - this.font.width((FormattedText)label) / 2;
            int labelY = (y1 + y2) / 2 - 4;
            ctx.text(this.font, (Component)label, labelX, labelY, -1638420);
        }
    }

    private void renderZones(GuiGraphicsExtractor ctx) {
        int left = (int)Math.max((double)this.viewportLeft(), Math.floor(this.mapLeft()));
        int right = (int)Math.min((double)this.viewportRight(), Math.ceil(this.mapRight()));
        for (ZombieZMapData.Zone zone : ZombieZMapData.ZONES) {
            int labelY;
            if (zone.placeholder() || zone.zMax() <= zone.zMin()) continue;
            int color = 0x66000000 | ZombieZMapData.zoneColor(zone.num());
            int yMin = this.screenY(ZombieZMapData.mapY(zone.zMin()));
            int yMax = this.screenY(ZombieZMapData.mapY(zone.zMax()));
            if (yMin >= 0 && yMin <= this.height) {
                ctx.fill(left, yMin, right, yMin + 1, color);
            }
            if (yMax >= 0 && yMax <= this.height) {
                ctx.fill(left, yMax, right, yMax + 1, color);
            }
            if (!(this.zoom >= 0.45) || (labelY = (yMin + yMax) / 2) < this.viewportTop() + 4 || labelY > this.viewportBottom() - 12) continue;
            MutableComponent label = Component.translatable((String)"zombiezcompanion.map.zone_label", (Object[])new Object[]{zone.num(), zone.name()});
            ctx.text(this.font, (Component)label, left + 6, labelY, -570425345);
        }
    }

    private void renderCranes(GuiGraphicsExtractor ctx) {
        for (ZombieZMapData.Zone zone : ZombieZMapData.ZONES) {
            for (ZombieZMapData.Point skull : zone.skulls()) {
                SkullHit hit = new SkullHit(zone, skull);
                this.drawMarker(ctx, skull.x(), skull.z(), skull.colorRgb(), 2, false, this.zoom >= 1.15 ? skull.label() : null, this.isSameGuideTarget(this.targetFromSkull(hit)));
            }
        }
    }

    private void renderBosses(GuiGraphicsExtractor ctx) {
        for (ZombieZMapData.Boss boss : ZombieZMapData.BOSSES) {
            this.drawMarker(ctx, boss.x(), boss.z(), 0xEF4444, 4, true, this.zoom >= 0.55 ? boss.name() : null, this.isSameGuideTarget(this.targetFromBoss(boss)));
        }
    }

    private void renderWaypoints(GuiGraphicsExtractor ctx) {
        for (MapConfig.Waypoint waypoint : this.config().waypoints) {
            if (SkullsModule.isSkullWaypointId(waypoint.id)) continue;
            int rgb = waypoint.visible ? waypoint.colorRgb : ZombieZMapScreen.dimColor(waypoint.colorRgb);
            String label = waypoint.visible && this.zoom >= 0.45 ? waypoint.label : null;
            int radius = waypoint.visible ? 4 : 3;
            this.drawMarker(ctx, waypoint.x, waypoint.z, rgb, radius, true, label, waypoint.visible && this.isSameGuideTarget(this.targetFromWaypoint(waypoint)));
            if (!waypoint.visible) continue;
            this.drawWaypointEdgeMarker(ctx, waypoint);
        }
    }

    private void renderSkullBeacons(GuiGraphicsExtractor ctx) {
        for (MapConfig.Waypoint waypoint : this.config().waypoints) {
            if (!SkullsModule.isSkullWaypointId(waypoint.id)) continue;
            this.drawMarker(ctx, waypoint.x, waypoint.z, waypoint.colorRgb, 5, true, this.zoom >= 0.45 ? waypoint.label : null, this.isSameGuideTarget(this.targetFromWaypoint(waypoint)));
        }
    }

    private static int dimColor(int rgb) {
        int r = (rgb >> 16 & 0xFF) / 3;
        int g = (rgb >> 8 & 0xFF) / 3;
        int b = (rgb & 0xFF) / 3;
        return r << 16 | g << 8 | b;
    }

    private void drawWaypointEdgeMarker(GuiGraphicsExtractor ctx, MapConfig.Waypoint waypoint) {
        boolean below;
        int x = this.screenX(ZombieZMapData.mapX(waypoint.x));
        int y = this.screenY(ZombieZMapData.mapY(waypoint.z));
        boolean above = y < this.viewportTop();
        boolean bl = below = y > this.viewportBottom();
        if (!above && !below) {
            return;
        }
        int edgeY = above ? this.viewportTop() + 10 : this.viewportBottom() - 10;
        int edgeX = (int)ZombieZMapScreen.clamp(x, this.viewportLeft() + 10, this.viewportRight() - 10);
        boolean selected = this.isSameGuideTarget(this.targetFromWaypoint(waypoint));
        int radius = selected ? 7 + this.selectedPulse() : 6;
        int color = 0xFF000000 | waypoint.colorRgb & 0xFFFFFF;
        this.drawMarkerShape(ctx, edgeX, edgeY, radius + 2, true, -587202560);
        this.drawMarkerShape(ctx, edgeX, edgeY, radius, true, selected ? -8874241 : color);
        ctx.fill(edgeX - 1, edgeY - 1, edgeX + 2, edgeY + 2, -1);
        if (this.zoom >= 0.45 || selected) {
            String label = this.font.plainSubstrByWidth(waypoint.label == null ? "Rep\u00e8re" : waypoint.label, 96);
            int labelX = (int)ZombieZMapScreen.clamp(edgeX + 8, this.viewportLeft() + 4, this.viewportRight() - this.font.width(label) - 4);
            int labelY = above ? edgeY + 8 : edgeY - 16;
            ctx.text(this.font, label, labelX, labelY, selected ? -8874241 : -1);
        }
    }

    private void drawMarker(GuiGraphicsExtractor ctx, double worldX, double worldZ, int rgb, int radius, boolean diamond, String label, boolean selected) {
        double mapX = ZombieZMapData.mapX(worldX);
        double mapY = ZombieZMapData.mapY(worldZ);
        int x = this.screenX(mapX);
        int y = this.screenY(mapY);
        int r = Math.max(2, (int)Math.round((double)radius * Math.max(0.55, this.zoom * 0.82)));
        if (selected) {
            r += 2 + this.selectedPulse();
        }
        if (x < this.viewportLeft() - r || x > this.viewportRight() + r || y < this.viewportTop() - r || y > this.viewportBottom() + r) {
            return;
        }
        int color = 0xFF000000 | rgb & 0xFFFFFF;
        ctx.fill(x - r - 1, y - r - 1, x + r + 2, y + r + 2, selected ? -587202560 : -1157627904);
        if (selected) {
            this.drawMarkerShape(ctx, x, y, r, diamond, -8874241);
            r = Math.max(2, r - 2);
        }
        this.drawMarkerShape(ctx, x, y, r, diamond, color);
        ctx.fill(x - 1, y - 1, x + 2, y + 2, selected ? -8874241 : -1);
        if (label != null && !label.isBlank()) {
            ctx.text(this.font, label, x + r + 4, y - 4, selected ? -8874241 : -1);
        }
    }

    private int selectedPulse() {
        double phase = (double)(System.currentTimeMillis() % 900L) / 900.0 * Math.PI * 2.0;
        return (int)Math.round((Math.sin(phase) + 1.0) * 1.5);
    }

    private void drawMarkerShape(GuiGraphicsExtractor ctx, int x, int y, int r, boolean diamond, int color) {
        if (diamond) {
            ctx.fill(x - 1, y - r, x + 2, y + r + 1, color);
            ctx.fill(x - r, y - 1, x + r + 1, y + 2, color);
        } else {
            ctx.fill(x - r, y - r, x + r + 1, y + r + 1, color);
        }
    }

    private void renderPlayerMarker(GuiGraphicsExtractor ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        int x = this.screenX(ZombieZMapData.mapX(client.player.getX()));
        int y = this.screenY(ZombieZMapData.mapY(client.player.getZ()));
        ZombieZMapScreen.drawPlayerDot(ctx, x, y, 0.78f);
    }

    private void renderPresences(GuiGraphicsExtractor ctx) {
        if (!this.config().showModUsers) {
            return;
        }
        List<PresenceCache.Presence> list = PresenceCache.presences();
        if (list.isEmpty()) {
            return;
        }
        PlayersConfig pcfg = this.configManager.get().players;
        for (PresenceCache.Presence p : list) {
            int x = this.screenX(ZombieZMapData.mapX(p.x()));
            int y = this.screenY(ZombieZMapData.mapY(p.z()));
            if (x < this.viewportLeft() - 8 || x > this.viewportRight() + 8 || y < this.viewportTop() - 8 || y > this.viewportBottom() + 8) continue;
            ctx.fill(x - 4, y - 4, x + 5, y + 5, -587202560);
            ctx.fill(x - 3, y - 3, x + 4, y + 4, -13058568);
            ctx.fill(x - 1, y - 1, x + 2, y + 2, -1);
            int textY = y - 4;
            if (pcfg.showNames && this.zoom >= 0.55) {
                ctx.text(this.font, (Component)Component.literal((String)p.name()), x + 6, textY, -3149825);
                textY += 10;
            }
            if (!pcfg.showCoords || !(this.zoom >= 0.55)) continue;
            String coords = "x " + (int)Math.round(p.x()) + "  z " + (int)Math.round(p.z());
            ctx.text(this.font, (Component)Component.literal((String)coords), x + 6, textY, -6699044);
        }
    }

    static void drawPlayerDot(GuiGraphicsExtractor ctx, int x, int y, float scale) {
        ctx.pose().pushMatrix();
        ctx.pose().translate((float)x, (float)y);
        ctx.pose().scale(scale, scale);
        ctx.fill(-5, -5, 6, 6, -587202560);
        ctx.fill(-4, -1, 5, 2, -8874241);
        ctx.fill(-1, -4, 2, 5, -8874241);
        ctx.fill(-3, -3, 4, 4, -1);
        ctx.fill(-2, -2, 3, 3, -8874241);
        ctx.fill(-1, -1, 2, 2, -15658216);
        ctx.pose().popMatrix();
    }

    private void renderBackdrop(GuiGraphicsExtractor ctx) {
        int mapLeft = (int)Math.max((double)this.viewportLeft(), Math.floor(this.mapLeft()));
        int mapTop = (int)Math.max((double)this.viewportTop(), Math.floor(this.mapTop()));
        int mapRight = (int)Math.min((double)this.viewportRight(), Math.ceil(this.mapRight()));
        int mapBottom = (int)Math.min((double)this.viewportBottom(), Math.ceil(this.mapBottom()));
        ctx.fill(mapLeft + 3, mapTop + 4, mapRight + 3, mapBottom + 4, -1442840576);
        ctx.fill(mapLeft - 3, mapTop - 3, mapRight + 3, mapBottom + 3, -2012739054);
        ctx.fill(mapLeft - 3, mapTop - 3, mapRight + 3, mapTop - 1, -8874241);
        ctx.outline(mapLeft - 3, mapTop - 3, mapRight - mapLeft + 6, mapBottom - mapTop + 6, -8874241);
    }

    private void renderChrome(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int mapX = (int)Math.floor(ZombieZMapScreen.clamp(this.screenToMapX(mouseX), 0.0, 1242.0));
        int mapZ = (int)Math.floor(ZombieZMapScreen.clamp(ZombieZMapData.worldZ(this.screenToMapY(mouseY)), 0.0, 10400.0));
        MutableComponent status = Component.translatable((String)"zombiezcompanion.map.status", (Object[])new Object[]{mapX, mapZ, Math.round(this.zoom * 100.0)});
        int statusWidth = this.font.width((FormattedText)status);
        int statusX = Math.max(8, this.width - statusWidth - 18);
        ctx.fill(8, 8, 132, 30, -183627755);
        ctx.outline(8, 8, 124, 22, -13880766);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.map.title"), 16, 14, -854792);
        ctx.fill(statusX - 8, 8, this.width - 8, 30, -183627755);
        ctx.outline(statusX - 8, 8, this.width - statusX, 22, -13880766);
        ctx.text(this.font, (Component)status, statusX, 14, -854792);
        if (!this.searchText.isBlank()) {
            int badgeY = 60;
            if (this.searchMatch != null) {
                String preview = this.font.plainSubstrByWidth("> " + this.searchMatch.label(), 120);
                ctx.text(this.font, preview, 10, badgeY, -8874241, false);
            } else {
                ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.map.search.no_match"), 10, badgeY, -8353376, false);
            }
        }
        MapConfig cfg = this.config();
        int y = 72;
        this.drawLayerButton(ctx, y, (Component)Component.translatable((String)"zombiezcompanion.map.layer.zones"), cfg.showZones);
        this.drawLayerButton(ctx, y += 25, (Component)Component.translatable((String)"zombiezcompanion.map.layer.refuges"), cfg.showRefuges);
        this.drawLayerButton(ctx, y += 25, (Component)Component.translatable((String)"zombiezcompanion.map.layer.cranes"), cfg.showCranes);
        this.drawLayerButton(ctx, y += 25, (Component)Component.translatable((String)"zombiezcompanion.map.layer.bosses"), cfg.showBosses);
        this.drawLayerButton(ctx, y += 25, (Component)Component.translatable((String)"zombiezcompanion.map.layer.waypoints"), cfg.showWaypoints);
        this.drawLayerButton(ctx, y += 25, (Component)Component.translatable((String)"zombiezcompanion.map.layer.mod_users"), cfg.showModUsers);
        this.renderActiveGuidePanel(ctx, y + 20 + 10);
    }

    private void renderActiveGuidePanel(GuiGraphicsExtractor ctx, int y) {
        MapConfig.GuideTarget target = this.config().guideTarget;
        if (target == null) {
            return;
        }
        int h = 44;
        ctx.fill(8, y, 132, y + h, -586544110);
        ctx.fill(8, y, 132, y + 2, -8874241);
        ctx.outline(8, y, 124, h, -8874241);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.map.guide.active"), 16, y + 7, -8874241, false);
        String label = this.font.plainSubstrByWidth(target.label, 108);
        ctx.text(this.font, label, 16, y + 20, -854792);
        String hint = this.font.plainSubstrByWidth(Component.translatable((String)"zombiezcompanion.map.guide.click_again").getString(), 108);
        ctx.text(this.font, hint, 16, y + 32, -8353376, false);
    }

    private void drawLayerButton(GuiGraphicsExtractor ctx, int y, Component label, boolean enabled) {
        int bg = enabled ? -14867392 : -266723542;
        int border = enabled ? -8874241 : -14736594;
        ctx.fill(8, y, 132, y + 20, bg);
        ctx.outline(8, y, 124, 20, border);
        ctx.text(this.font, label, 16, y + 6, enabled ? -854792 : -8353376, false);
        MutableComponent state = Component.translatable((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"));
        int stateW = this.font.width((FormattedText)state);
        ctx.text(this.font, (Component)state, 132 - stateW - 8, y + 6, enabled ? -8874241 : -12235684, false);
    }

    private void renderHoverInfo(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        if (!this.isInsideMap(mouseX, mouseY)) {
            return;
        }
        MapConfig.Waypoint waypoint = this.findNearestWaypointAtScreen(mouseX, mouseY);
        if (waypoint != null) {
            String actionKey = waypoint.visible ? "zombiezcompanion.map.tooltip.action.hide" : "zombiezcompanion.map.tooltip.action.show";
            this.renderTooltipBox(ctx, mouseX, mouseY, waypoint.label, Component.translatable((String)"zombiezcompanion.coord.xz", (Object[])new Object[]{(int)Math.round(waypoint.x), (int)Math.round(waypoint.z)}).getString(), Component.translatable((String)actionKey).getString(), Component.translatable((String)"zombiezcompanion.map.tooltip.action.middle_delete").getString());
            return;
        }
        ZombieZMapData.Boss boss = this.findNearestBossAtScreen(mouseX, mouseY);
        if (boss != null) {
            MapGuideTarget target = this.targetFromBoss(boss);
            this.renderTooltipBox(ctx, mouseX, mouseY, boss.name(), Component.translatable((String)"zombiezcompanion.map.tooltip.boss_zone", (Object[])new Object[]{boss.zone()}).getString(), Component.translatable((String)"zombiezcompanion.coord.xyz", (Object[])new Object[]{(int)Math.round(boss.x()), (int)Math.round(boss.y()), (int)Math.round(boss.z())}).getString(), boss.respawn().isBlank() ? "" : Component.translatable((String)"zombiezcompanion.map.tooltip.boss_respawn", (Object[])new Object[]{boss.respawn()}).getString(), this.guideActionLabel(target));
            return;
        }
        SkullHit skull = this.findNearestSkullAtScreen(mouseX, mouseY);
        if (skull != null) {
            MapGuideTarget target = this.targetFromSkull(skull);
            this.renderTooltipBox(ctx, mouseX, mouseY, skull.point.label(), Component.translatable((String)"zombiezcompanion.map.tooltip.skull_zone", (Object[])new Object[]{skull.zone.num(), skull.zone.name()}).getString(), Component.translatable((String)"zombiezcompanion.coord.xyz", (Object[])new Object[]{(int)Math.round(skull.point.x()), (int)Math.round(skull.point.y()), (int)Math.round(skull.point.z())}).getString(), this.guideActionLabel(target));
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && this.isHoveringPlayer(mouseX, mouseY)) {
            this.renderTooltipBox(ctx, mouseX, mouseY, Component.translatable((String)"zombiezcompanion.map.tooltip.player").getString(), Component.translatable((String)"zombiezcompanion.coord.xyz", (Object[])new Object[]{(int)Math.round(mc.player.getX()), (int)Math.round(mc.player.getY()), (int)Math.round(mc.player.getZ())}).getString(), Component.translatable((String)"zombiezcompanion.map.tooltip.player_yaw", (Object[])new Object[]{Math.round(mc.player.getYRot())}).getString());
            return;
        }
        ZombieZMapData.Zone zone = this.findHoveredZoneLine(mouseY);
        if (zone != null) {
            this.renderTooltipBox(ctx, mouseX, mouseY, Component.translatable((String)"zombiezcompanion.map.tooltip.zone_title", (Object[])new Object[]{zone.num()}).getString(), zone.name(), Component.translatable((String)"zombiezcompanion.map.tooltip.zone_range", (Object[])new Object[]{zone.zMin(), zone.zMax()}).getString());
        }
    }

    private void renderTooltipBox(GuiGraphicsExtractor ctx, int mouseX, int mouseY, String title, String ... lines) {
        int textWidth = this.font.width(title);
        int visibleLines = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            textWidth = Math.max(textWidth, this.font.width(line));
            ++visibleLines;
        }
        int w = textWidth + 14;
        int h = 18 + visibleLines * 12 + 6;
        int x = Math.min(this.width - w - 8, mouseX + 12);
        int y = Math.min(this.height - h - 8, mouseY + 12);
        ctx.fill(x + 2, y + 2, x + w + 2, y + h + 2, -1442840576);
        ctx.fill(x, y, x + w, y + h, -183627755);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.outline(x, y, w, h, -8874241);
        ctx.text(this.font, title, x + 7, y + 6, -854792);
        int lineY = y + 18;
        int index = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            ctx.text(this.font, line, x + 7, lineY, index == 0 ? -854792 : -8353376, false);
            lineY += 12;
            ++index;
        }
    }

    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y(); int button = event.button();
        if (button == 0 && this.config().guideTarget != null && this.isOverActiveGuidePanel(mouseX, mouseY)) {
            this.clearGuideTarget();
            return true;
        }
        if (button == 0 && this.handleLayerButtonClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && this.isInsideMap(mouseX, mouseY) && this.minecraft != null) {
            this.waypointClickCandidate = true;
            this.waypointClickDragged = false;
            this.waypointClickStartX = mouseX;
            this.waypointClickStartY = mouseY;
            return true;
        }
        if (button == 2 && this.isInsideMap(mouseX, mouseY)) {
            MapConfig.Waypoint waypoint = this.findNearestWaypointAtScreen(mouseX, mouseY);
            if (waypoint != null && this.minecraft != null) {
                this.minecraft.setScreen((Screen)new WaypointDeleteConfirmScreen(this, this.configManager, waypoint.id));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleLayerButtonClick(double mouseX, double mouseY) {
        if (mouseX < 8.0 || mouseX > 132.0) {
            return false;
        }
        int y = 72;
        if (this.isControlRow(mouseY, y)) {
            return this.toggle(() -> {
                this.config().showZones = !this.config().showZones;
            });
        }
        if (this.isControlRow(mouseY, y += 25)) {
            return this.toggle(() -> {
                this.config().showRefuges = !this.config().showRefuges;
            });
        }
        if (this.isControlRow(mouseY, y += 25)) {
            return this.toggle(() -> {
                this.config().showCranes = !this.config().showCranes;
            });
        }
        if (this.isControlRow(mouseY, y += 25)) {
            return this.toggle(() -> {
                this.config().showBosses = !this.config().showBosses;
            });
        }
        if (this.isControlRow(mouseY, y += 25)) {
            return this.toggle(() -> {
                this.config().showWaypoints = !this.config().showWaypoints;
            });
        }
        if (this.isControlRow(mouseY, y += 25)) {
            return this.toggle(() -> {
                this.config().showModUsers = !this.config().showModUsers;
            });
        }
        return false;
    }

    private boolean isControlRow(double mouseY, int y) {
        return mouseY >= (double)y && mouseY <= (double)(y + 20);
    }

    private int activeGuidePanelY() {
        return 227;
    }

    private boolean isOverActiveGuidePanel(double mouseX, double mouseY) {
        int panelY = this.activeGuidePanelY();
        return mouseX >= 8.0 && mouseX <= 132.0 && mouseY >= (double)panelY && mouseY <= (double)(panelY + 44);
    }

    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x(), mouseY = event.y(); int button = event.button();
        if (button == 0 && this.waypointClickCandidate) {
            double dragDistance = Math.hypot(mouseX - this.waypointClickStartX, mouseY - this.waypointClickStartY);
            boolean isClick = !this.waypointClickDragged && dragDistance <= 4.0;
            this.waypointClickCandidate = false;
            this.waypointClickDragged = false;
            if (isClick && this.isInsideMap(mouseX, mouseY) && this.minecraft != null) {
                double mapX = ZombieZMapScreen.clamp(this.screenToMapX(mouseX), 0.0, 1242.0);
                double worldZ = ZombieZMapData.worldZ(ZombieZMapScreen.clamp(this.screenToMapY(mouseY), 0.0, 10432.0));
                MapConfig.Waypoint waypoint = this.findNearestWaypointAtScreen(mouseX, mouseY);
                if (waypoint != null) {
                    waypoint.visible = !waypoint.visible;
                    this.configManager.save();
                } else {
                    MapGuideTarget target = this.findNearestGuideTargetAtScreen(mouseX, mouseY);
                    if (target != null) {
                        this.selectGuideTarget(target);
                    } else if (WaypointsModule.isEnabled()) {
                        this.minecraft.setScreen((Screen)new WaypointEditScreen(this, this.configManager, mapX, worldZ));
                    }
                }
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    private MapConfig.Waypoint findNearestWaypointAtScreen(double mouseX, double mouseY) {
        if (!this.config().showWaypoints || !WaypointsModule.isEnabled()) {
            return null;
        }
        MapConfig.Waypoint best = null;
        double bestDist = 5.0;
        for (MapConfig.Waypoint waypoint : this.config().waypoints) {
            double y;
            double x = this.screenX(ZombieZMapData.mapX(waypoint.x));
            double dist = Math.hypot(x - mouseX, (y = (double)this.screenY(ZombieZMapData.mapY(waypoint.z))) - mouseY);
            if (!(dist < bestDist)) continue;
            bestDist = dist;
            best = waypoint;
        }
        return best;
    }

    private MapGuideTarget findNearestGuideTargetAtScreen(double mouseX, double mouseY) {
        double skullDist;
        ZombieZMapData.Boss boss = this.findNearestBossAtScreen(mouseX, mouseY);
        SkullHit skull = this.findNearestSkullAtScreen(mouseX, mouseY);
        if (boss == null && skull == null) {
            return null;
        }
        if (boss == null) {
            return this.targetFromSkull(skull);
        }
        if (skull == null) {
            return this.targetFromBoss(boss);
        }
        double bossDist = Math.hypot((double)this.screenX(ZombieZMapData.mapX(boss.x())) - mouseX, (double)this.screenY(ZombieZMapData.mapY(boss.z())) - mouseY);
        return bossDist <= (skullDist = Math.hypot((double)this.screenX(ZombieZMapData.mapX(skull.point.x())) - mouseX, (double)this.screenY(ZombieZMapData.mapY(skull.point.z())) - mouseY)) ? this.targetFromBoss(boss) : this.targetFromSkull(skull);
    }

    private MapGuideTarget targetFromBoss(ZombieZMapData.Boss boss) {
        return new MapGuideTarget(boss.name(), "Boss", boss.x(), boss.y(), boss.z(), 0xEF4444);
    }

    private MapGuideTarget targetFromSkull(SkullHit skull) {
        return new MapGuideTarget(skull.point.label(), "Crane", skull.point.x(), skull.point.y(), skull.point.z(), skull.point.colorRgb());
    }

    private MapGuideTarget targetFromWaypoint(MapConfig.Waypoint waypoint) {
        return new MapGuideTarget(waypoint.label, "Waypoint", waypoint.x, waypoint.y, waypoint.z, waypoint.colorRgb);
    }

    private void selectGuideTarget(MapGuideTarget target) {
        if (this.isSameGuideTarget(target)) {
            this.config().guideTarget = null;
            this.configManager.save();
            return;
        }
        MapConfig.GuideTarget guideTarget = new MapConfig.GuideTarget();
        guideTarget.label = target.label;
        guideTarget.type = target.type;
        guideTarget.x = target.x;
        guideTarget.y = target.y;
        guideTarget.z = target.z;
        guideTarget.colorRgb = target.colorRgb;
        this.config().guideTarget = guideTarget;
        this.configManager.save();
    }

    private String guideActionLabel(MapGuideTarget target) {
        return Component.translatable((String)(this.isSameGuideTarget(target) ? "zombiezcompanion.map.tooltip.action.unguide" : "zombiezcompanion.map.tooltip.action.guide")).getString();
    }

    private boolean isSameGuideTarget(MapGuideTarget target) {
        MapConfig.GuideTarget current = this.config().guideTarget;
        return current != null && target.type.equals(current.type) && Math.abs(current.x - target.x) < 0.01 && Math.abs(current.z - target.z) < 0.01;
    }

    private ZombieZMapData.Boss findNearestBossAtScreen(double mouseX, double mouseY) {
        if (!this.config().showBosses) {
            return null;
        }
        ZombieZMapData.Boss best = null;
        double bestDist = 6.0;
        for (ZombieZMapData.Boss boss : ZombieZMapData.BOSSES) {
            double y;
            double x = this.screenX(ZombieZMapData.mapX(boss.x()));
            double dist = Math.hypot(x - mouseX, (y = (double)this.screenY(ZombieZMapData.mapY(boss.z()))) - mouseY);
            if (!(dist < bestDist)) continue;
            bestDist = dist;
            best = boss;
        }
        return best;
    }

    private SkullHit findNearestSkullAtScreen(double mouseX, double mouseY) {
        if (!this.config().showCranes) {
            return null;
        }
        SkullHit best = null;
        double bestDist = 5.0;
        for (ZombieZMapData.Zone zone : ZombieZMapData.ZONES) {
            for (ZombieZMapData.Point skull : zone.skulls()) {
                double y;
                double x = this.screenX(ZombieZMapData.mapX(skull.x()));
                double dist = Math.hypot(x - mouseX, (y = (double)this.screenY(ZombieZMapData.mapY(skull.z()))) - mouseY);
                if (!(dist < bestDist)) continue;
                bestDist = dist;
                best = new SkullHit(zone, skull);
            }
        }
        return best;
    }

    private ZombieZMapData.Zone findHoveredZoneLine(double screenY) {
        if (!this.config().showZones) {
            return null;
        }
        for (ZombieZMapData.Zone zone : ZombieZMapData.ZONES) {
            if (zone.placeholder() || zone.zMax() <= zone.zMin()) continue;
            int yMin = this.screenY(ZombieZMapData.mapY(zone.zMin()));
            int yMax = this.screenY(ZombieZMapData.mapY(zone.zMax()));
            if (!(Math.abs(screenY - (double)yMin) <= 5.0) && !(Math.abs(screenY - (double)yMax) <= 5.0)) continue;
            return zone;
        }
        return null;
    }

    private boolean isHoveringPlayer(double mouseX, double mouseY) {
        int playerY;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        int playerX = this.screenX(ZombieZMapData.mapX(mc.player.getX()));
        return Math.hypot((double)playerX - mouseX, (double)(playerY = this.screenY(ZombieZMapData.mapY(mc.player.getZ()))) - mouseY) <= 5.0;
    }

    private void centerOnPlayerOrSpawn() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            this.centerOn(ZombieZMapData.mapX(client.player.getX()), ZombieZMapData.mapY(client.player.getZ()));
        } else {
            this.centerOn(621.0, 5216.0);
        }
    }

    private void centerOn(double mapX, double mapY) {
        this.viewX = mapX - this.visibleMapWidth() / 2.0;
        this.viewY = mapY - this.visibleMapHeight() / 2.0;
        this.clampView();
    }

    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {
        double mouseX = event.x(), mouseY = event.y(); int button = event.button();
        if (button != 0) {
            return super.mouseDragged(event, deltaX, deltaY);
        }
        if (this.waypointClickCandidate && Math.hypot(mouseX - this.waypointClickStartX, mouseY - this.waypointClickStartY) > 4.0) {
            this.waypointClickDragged = true;
        }
        if (this.isMapWiderThanScreen()) {
            this.viewX -= deltaX / this.zoom;
        }
        if (this.isMapTallerThanScreen()) {
            this.viewY -= deltaY / this.zoom;
        }
        this.clampView();
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0.0) {
            return false;
        }
        double anchorScreenX = ZombieZMapScreen.clamp(mouseX, Math.max(this.mapLeft(), (double)this.viewportLeft()), Math.min(this.mapRight(), (double)this.viewportRight()));
        double anchorScreenY = ZombieZMapScreen.clamp(mouseY, Math.max(this.mapTop(), (double)this.viewportTop()), Math.min(this.mapBottom(), (double)this.viewportBottom()));
        double mapXBefore = ZombieZMapScreen.clamp(this.screenToMapX(anchorScreenX), 0.0, 1242.0);
        double mapYBefore = ZombieZMapScreen.clamp(this.screenToMapY(anchorScreenY), 0.0, 10432.0);
        double factor = verticalAmount > 0.0 ? 1.18 : 0.8474576271186441;
        this.zoom = ZombieZMapScreen.clamp(this.zoom * factor, 0.12, 3.0);
        this.viewX = mapXBefore - (anchorScreenX - this.mapOffsetX()) / this.zoom;
        this.viewY = mapYBefore - (anchorScreenY - this.mapOffsetY()) / this.zoom;
        this.clampView();
        return true;
    }

    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
        if (this.searchField != null && this.searchField.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                this.jumpToSearchMatch();
                return true;
            }
            if (keyCode == 256) {
                this.searchField.setValue("");
                this.searchField.setFocused(false);
                return true;
            }
            return super.keyPressed(event);
        }
        if (keyCode == 77 || keyCode == 256) {
            this.onClose();
            return true;
        }
        if (keyCode == 67) {
            this.centerOnPlayerOrSpawn();
            return true;
        }
        if (Keybinds.matchesClearGuide(keyCode, scanCode)) {
            this.clearGuideTarget();
            return true;
        }
        if (keyCode == 61 || keyCode == 334) {
            this.zoomAroundCenter(1.18);
            return true;
        }
        if (keyCode == 45 || keyCode == 333) {
            this.zoomAroundCenter(0.8474576271186441);
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean toggle(Runnable action) {
        action.run();
        this.configManager.save();
        return true;
    }

    private void clearGuideTarget() {
        if (this.config().guideTarget != null) {
            this.config().guideTarget = null;
            this.configManager.save();
        }
    }

    public boolean isPauseScreen() {
        return false;
    }

    private MapConfig config() {
        return this.configManager.get().map;
    }

    private boolean isInsideMap(double screenX, double screenY) {
        return screenX >= Math.max(this.mapLeft(), (double)this.viewportLeft()) && screenX <= Math.min(this.mapRight(), (double)this.viewportRight()) && screenY >= Math.max(this.mapTop(), (double)this.viewportTop()) && screenY <= Math.min(this.mapBottom(), (double)this.viewportBottom());
    }

    private int screenX(double mapX) {
        return (int)Math.floor(this.mapOffsetX() + (mapX - this.viewX) * this.zoom);
    }

    private int screenY(double mapY) {
        return (int)Math.floor(this.mapOffsetY() + (mapY - this.viewY) * this.zoom);
    }

    private void clampView() {
        double maxX = Math.max(0.0, 1242.0 - this.visibleMapWidth());
        double maxY = Math.max(0.0, 10432.0 - this.visibleMapHeight());
        this.viewX = ZombieZMapScreen.clamp(this.viewX, 0.0, maxX);
        this.viewY = ZombieZMapScreen.clamp(this.viewY, 0.0, maxY);
    }

    private void zoomAroundCenter(double factor) {
        double centerX = this.screenToMapX((double)this.width / 2.0);
        double centerY = this.screenToMapY((double)this.height / 2.0);
        this.zoom = ZombieZMapScreen.clamp(this.zoom * factor, 0.12, 3.0);
        this.centerOn(centerX, centerY);
    }

    private double fitZoom() {
        return ZombieZMapScreen.clamp(Math.min(((double)this.viewportWidth() - 16.0) / 1242.0, ((double)this.viewportHeight() - 16.0) / 10432.0), 0.12, 1.0);
    }

    private double mapOffsetX() {
        return this.isMapWiderThanScreen() ? (double)this.viewportLeft() : (double)this.viewportLeft() + ((double)this.viewportWidth() - 1242.0 * this.zoom) / 2.0;
    }

    private double mapOffsetY() {
        return this.isMapTallerThanScreen() ? (double)this.viewportTop() : (double)this.viewportTop() + ((double)this.viewportHeight() - 10432.0 * this.zoom) / 2.0;
    }

    private double mapLeft() {
        return this.mapOffsetX() - this.viewX * this.zoom;
    }

    private double mapRight() {
        return this.mapOffsetX() + (1242.0 - this.viewX) * this.zoom;
    }

    private double mapTop() {
        return this.mapOffsetY() - this.viewY * this.zoom;
    }

    private double mapBottom() {
        return this.mapOffsetY() + (10432.0 - this.viewY) * this.zoom;
    }

    private double screenToMapX(double screenX) {
        return this.viewX + (screenX - this.mapOffsetX()) / this.zoom;
    }

    private double screenToMapY(double screenY) {
        return this.viewY + (screenY - this.mapOffsetY()) / this.zoom;
    }

    private double visibleMapWidth() {
        return Math.min(1242.0, (double)this.viewportWidth() / this.zoom);
    }

    private double visibleMapHeight() {
        return Math.min(10432.0, (double)this.viewportHeight() / this.zoom);
    }

    private boolean isMapWiderThanScreen() {
        return 1242.0 * this.zoom > (double)this.viewportWidth();
    }

    private boolean isMapTallerThanScreen() {
        return 10432.0 * this.zoom > (double)this.viewportHeight();
    }

    private int viewportLeft() {
        return 148;
    }

    private int viewportRight() {
        return Math.max(this.viewportLeft() + 1, this.width - 8);
    }

    private int viewportTop() {
        return 8;
    }

    private int viewportBottom() {
        return Math.max(this.viewportTop() + 1, this.height - 8);
    }

    private int viewportWidth() {
        return this.viewportRight() - this.viewportLeft();
    }

    private int viewportHeight() {
        return this.viewportBottom() - this.viewportTop();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SearchMatch(String label, double x, double z, int colorRgb) {
    }

    private record SkullHit(ZombieZMapData.Zone zone, ZombieZMapData.Point point) {
    }

    private record MapGuideTarget(String label, String type, double x, double y, double z, int colorRgb) {
    }
}

