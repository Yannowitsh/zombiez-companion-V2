package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class WaypointDeleteConfirmScreen
extends Screen {
    private final Screen parent;
    private final ConfigManager configManager;
    private final String waypointId;
    private final boolean deleteAll;

    WaypointDeleteConfirmScreen(Screen parent, ConfigManager configManager, String waypointId) {
        super((Component)Component.translatable((String)"zombiezcompanion.waypoint.delete.title"));
        this.parent = parent;
        this.configManager = configManager;
        this.waypointId = waypointId;
        this.deleteAll = false;
    }

    WaypointDeleteConfirmScreen(Screen parent, ConfigManager configManager) {
        super((Component)Component.translatable((String)"zombiezcompanion.waypoint.delete.title_all"));
        this.parent = parent;
        this.configManager = configManager;
        this.waypointId = null;
        this.deleteAll = true;
    }

    protected void init() {
        int panelW = Math.max(220, Math.min(300, this.width - 16));
        int panelH = 118;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        int buttonW = (panelW - 54) / 2;
        this.addRenderableWidget(new StyledButton(panelX + 18, panelY + 78, buttonW, 20, (Component)Component.translatable((String)(this.deleteAll ? "zombiezcompanion.waypoint.delete.confirm_all" : "zombiezcompanion.waypoint.delete.confirm")), button -> this.deleteWaypoint(), -12965328, -11716288, -854792));
        this.addRenderableWidget(new StyledButton(panelX + 36 + buttonW, panelY + 78, buttonW, 20, (Component)Component.translatable((String)"zombiezcompanion.button.cancel"), button -> this.onClose(), -266723542, -265932737, -854792));
    }

    private void deleteWaypoint() {
        if (this.deleteAll) {
            this.configManager.get().map.waypoints.clear();
            if (this.isWaypointGuideTarget()) {
                this.configManager.get().map.guideTarget = null;
            }
        } else {
            MapConfig.Waypoint removed = this.findWaypoint();
            this.configManager.get().map.waypoints.removeIf(waypoint -> this.waypointId != null && this.waypointId.equals(waypoint.id));
            if (removed != null && this.isGuideTarget(removed)) {
                this.configManager.get().map.guideTarget = null;
            }
        }
        this.configManager.save();
        this.onClose();
    }

    //? if >= 26.1 {
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
        ctx.fill(0, 0, this.width, this.height, -1610612736);
        int panelW = Math.max(220, Math.min(300, this.width - 16));
        int panelH = 118;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        MapConfig.Waypoint waypoint = this.findWaypoint();
        String label = waypoint == null ? Component.translatable((String)"zombiezcompanion.waypoint.delete.placeholder").getString() : waypoint.label;
        ctx.fill(panelX + 2, panelY + 3, panelX + panelW + 2, panelY + panelH + 3, -1442840576);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, -183627755);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 2, -8874241);
        ctx.outline(panelX, panelY, panelW, panelH, -13880766);
        ctx.text(this.font, (Component)Component.translatable((String)(this.deleteAll ? "zombiezcompanion.waypoint.delete.prompt_all" : "zombiezcompanion.waypoint.delete.prompt")), panelX + 18, panelY + 14, -854792);
        ctx.text(this.font, (Component)(this.deleteAll ? Component.translatable((String)"zombiezcompanion.waypoint.delete.all_count", (Object[])new Object[]{this.configManager.get().map.waypoints.size()}) : Component.literal((String)label)), panelX + 18, panelY + 34, -1, false);
        ctx.text(this.font, (Component)Component.translatable((String)(this.deleteAll ? "zombiezcompanion.waypoint.delete.subtext_all" : "zombiezcompanion.waypoint.delete.subtext")), panelX + 18, panelY + 52, -8353376, false);
        //? if >= 26.1 {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        //?} else {
        /*super.render(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private MapConfig.Waypoint findWaypoint() {
        for (MapConfig.Waypoint waypoint : this.configManager.get().map.waypoints) {
            if (this.waypointId == null || !this.waypointId.equals(waypoint.id)) continue;
            return waypoint;
        }
        return null;
    }

    private boolean isGuideTarget(MapConfig.Waypoint waypoint) {
        MapConfig.GuideTarget target = this.configManager.get().map.guideTarget;
        return target != null && "Waypoint".equals(target.type) && Math.abs(target.x - waypoint.x) < 0.01 && Math.abs(target.z - waypoint.z) < 0.01;
    }

    private boolean isWaypointGuideTarget() {
        MapConfig.GuideTarget target = this.configManager.get().map.guideTarget;
        return target != null && "Waypoint".equals(target.type);
    }

    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    public boolean isPauseScreen() {
        return false;
    }
}

