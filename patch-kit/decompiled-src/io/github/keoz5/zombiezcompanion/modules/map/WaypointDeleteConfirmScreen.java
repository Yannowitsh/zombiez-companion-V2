/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;

final class WaypointDeleteConfirmScreen
extends class_437 {
    private final class_437 parent;
    private final ConfigManager configManager;
    private final String waypointId;
    private final boolean deleteAll;

    WaypointDeleteConfirmScreen(class_437 parent, ConfigManager configManager, String waypointId) {
        super((class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.delete.title"));
        this.parent = parent;
        this.configManager = configManager;
        this.waypointId = waypointId;
        this.deleteAll = false;
    }

    WaypointDeleteConfirmScreen(class_437 parent, ConfigManager configManager) {
        super((class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.delete.title_all"));
        this.parent = parent;
        this.configManager = configManager;
        this.waypointId = null;
        this.deleteAll = true;
    }

    protected void method_25426() {
        int panelW = Math.max(220, Math.min(300, this.field_22789 - 16));
        int panelH = 118;
        int panelX = (this.field_22789 - panelW) / 2;
        int panelY = (this.field_22790 - panelH) / 2;
        int buttonW = (panelW - 54) / 2;
        this.method_37063((class_364)new StyledButton(panelX + 18, panelY + 78, buttonW, 20, (class_2561)class_2561.method_43471((String)(this.deleteAll ? "zombiezcompanion.waypoint.delete.confirm_all" : "zombiezcompanion.waypoint.delete.confirm")), button -> this.deleteWaypoint(), -12965328, -11716288, -854792));
        this.method_37063((class_364)new StyledButton(panelX + 36 + buttonW, panelY + 78, buttonW, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.button.cancel"), button -> this.method_25419(), -266723542, -265932737, -854792));
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
        this.method_25419();
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, -1610612736);
        int panelW = Math.max(220, Math.min(300, this.field_22789 - 16));
        int panelH = 118;
        int panelX = (this.field_22789 - panelW) / 2;
        int panelY = (this.field_22790 - panelH) / 2;
        MapConfig.Waypoint waypoint = this.findWaypoint();
        String label = waypoint == null ? class_2561.method_43471((String)"zombiezcompanion.waypoint.delete.placeholder").getString() : waypoint.label;
        ctx.method_25294(panelX + 2, panelY + 3, panelX + panelW + 2, panelY + panelH + 3, -1442840576);
        ctx.method_25294(panelX, panelY, panelX + panelW, panelY + panelH, -183627755);
        ctx.method_25294(panelX, panelY, panelX + panelW, panelY + 2, -8874241);
        ctx.method_49601(panelX, panelY, panelW, panelH, -13880766);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)(this.deleteAll ? "zombiezcompanion.waypoint.delete.prompt_all" : "zombiezcompanion.waypoint.delete.prompt")), panelX + 18, panelY + 14, -854792);
        ctx.method_51439(this.field_22793, (class_2561)(this.deleteAll ? class_2561.method_43469((String)"zombiezcompanion.waypoint.delete.all_count", (Object[])new Object[]{this.configManager.get().map.waypoints.size()}) : class_2561.method_43470((String)label)), panelX + 18, panelY + 34, -1, false);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)(this.deleteAll ? "zombiezcompanion.waypoint.delete.subtext_all" : "zombiezcompanion.waypoint.delete.subtext")), panelX + 18, panelY + 52, -8353376, false);
        super.method_25394(ctx, mouseX, mouseY, delta);
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

    public void method_25419() {
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    public boolean method_25421() {
        return false;
    }
}

