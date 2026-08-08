/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.UUID;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_364;
import net.minecraft.class_437;

final class WaypointEditScreen
extends class_437 {
    private static final int[] COLORS = new int[]{3528703, 16436245, 2278750, 0xEF4444, 16020150, 10980346, 16486972, 1357990, 6333946, 15235577, 0xFFFFFF, 9741240};
    private static final int COLOR_COLUMNS = 6;
    private final class_437 parent;
    private final ConfigManager configManager;
    private final double defaultX;
    private final double defaultY;
    private final double defaultZ;
    private final MapConfig.Waypoint editingWaypoint;
    private class_342 labelField;
    private class_342 xField;
    private class_342 yField;
    private class_342 zField;
    private int selectedColor = COLORS[0];
    private boolean invalidX;
    private boolean invalidY;
    private boolean invalidZ;
    private class_2561 validationError = class_2561.method_43473();

    WaypointEditScreen(class_437 parent, ConfigManager configManager, double x, double z) {
        this(parent, configManager, x, WaypointEditScreen.playerY(), z, null);
    }

    WaypointEditScreen(class_437 parent, ConfigManager configManager, double x, double y, double z) {
        this(parent, configManager, x, y, z, null);
    }

    WaypointEditScreen(class_437 parent, ConfigManager configManager, MapConfig.Waypoint waypoint) {
        this(parent, configManager, waypoint.x, waypoint.y, waypoint.z, waypoint);
    }

    private WaypointEditScreen(class_437 parent, ConfigManager configManager, double x, double y, double z, MapConfig.Waypoint editingWaypoint) {
        super((class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.edit.title"));
        this.parent = parent;
        this.configManager = configManager;
        this.defaultX = x;
        this.defaultY = y;
        this.defaultZ = z;
        this.editingWaypoint = editingWaypoint;
        if (editingWaypoint != null) {
            this.selectedColor = editingWaypoint.colorRgb;
        }
    }

    private static double playerY() {
        class_310 mc = class_310.method_1551();
        return mc.field_1724 != null ? mc.field_1724.method_23318() : 64.0;
    }

    protected void method_25426() {
        int panelW = Math.max(204, Math.min(320, this.field_22789 - 16));
        int panelH = 228;
        int panelX = Math.max(8, (this.field_22789 - panelW) / 2);
        int panelY = Math.max(8, (this.field_22790 - panelH) / 2);
        int fieldW = (panelW - 36 - 8 - 8) / 3;
        int fieldX = panelX + 18;
        int fieldY = panelY + 42;
        this.xField = this.coordField(fieldX, fieldY, fieldW, this.defaultX);
        this.yField = this.coordField(fieldX + fieldW + 8, fieldY, fieldW, this.defaultY);
        this.zField = this.coordField(fieldX + 2 * (fieldW + 8), fieldY, fieldW, this.defaultZ);
        this.method_37063((class_364)this.xField);
        this.method_37063((class_364)this.yField);
        this.method_37063((class_364)this.zField);
        this.labelField = new class_342(this.field_22793, panelX + 18, panelY + 86, panelW - 36, 20, (class_2561)class_2561.method_43473());
        this.labelField.method_1880(32);
        this.labelField.method_1852(this.editingWaypoint != null ? (this.editingWaypoint.label == null ? "" : this.editingWaypoint.label) : class_2561.method_43469((String)"zombiezcompanion.waypoint.edit.default_name", (Object[])new Object[]{this.configManager.get().map.waypoints.size() + 1}).getString());
        this.method_48265((class_364)this.labelField);
        this.method_37063((class_364)this.labelField);
        int colorX = panelX + 18;
        for (int i = 0; i < COLORS.length; ++i) {
            int color = COLORS[i];
            int col = i % 6;
            int row = i / 6;
            this.method_37063((class_364)new StyledButton(colorX + col * 28, panelY + 130 + row * 24, 22, 18, (class_2561)class_2561.method_43470((String)""), button -> {
                this.selectedColor = color;
            }, 0xFF000000 | color, 0xFF000000 | color, -1));
        }
        int buttonW = (panelW - 48) / 2;
        this.method_37063((class_364)new StyledButton(panelX + 18, panelY + 192, buttonW, 20, (class_2561)class_2561.method_43471((String)(this.editingWaypoint == null ? "zombiezcompanion.waypoint.edit.add" : "zombiezcompanion.waypoint.edit.save")), button -> this.saveWaypoint(), -11441921, -8874241, -854792));
        this.method_37063((class_364)new StyledButton(panelX + 30 + buttonW, panelY + 192, buttonW, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.button.cancel"), button -> this.method_25419(), -266723542, -265932737, -854792));
    }

    private class_342 coordField(int x, int y, int w, double defaultValue) {
        class_342 field = new class_342(this.field_22793, x, y, w, 20, (class_2561)class_2561.method_43473());
        field.method_1880(12);
        field.method_1852(Integer.toString((int)Math.round(defaultValue)));
        field.method_1863(value -> this.clearValidation());
        return field;
    }

    private Double parseCoord(class_342 field) {
        String text = field.method_1882().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(text.replace(',', '.'));
            return Double.isFinite(value) ? Double.valueOf(value) : null;
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private void clearValidation() {
        this.invalidX = false;
        this.invalidY = false;
        this.invalidZ = false;
        this.validationError = class_2561.method_43473();
    }

    private void saveWaypoint() {
        String trimmed;
        boolean updateGuideTarget;
        this.clearValidation();
        class_310 mc = class_310.method_1551();
        double playerX = mc.field_1724 != null ? mc.field_1724.method_23317() : this.defaultX;
        double playerY = mc.field_1724 != null ? mc.field_1724.method_23318() : this.defaultY;
        double playerZ = mc.field_1724 != null ? mc.field_1724.method_23321() : this.defaultZ;
        Double parsedX = this.parseCoord(this.xField);
        Double parsedY = this.parseCoord(this.yField);
        Double parsedZ = this.parseCoord(this.zField);
        this.invalidX = parsedX == null && !this.xField.method_1882().trim().isEmpty();
        this.invalidY = parsedY == null && !this.yField.method_1882().trim().isEmpty();
        boolean bl = this.invalidZ = parsedZ == null && !this.zField.method_1882().trim().isEmpty();
        if (this.invalidX || this.invalidY || this.invalidZ) {
            this.validationError = class_2561.method_43471((String)"zombiezcompanion.waypoint.edit.error.invalid_coords");
            return;
        }
        MapConfig.Waypoint waypoint = this.editingWaypoint == null ? new MapConfig.Waypoint() : this.editingWaypoint;
        boolean bl2 = updateGuideTarget = this.editingWaypoint != null && this.isGuideTarget(waypoint);
        if (waypoint.id == null || waypoint.id.isBlank()) {
            waypoint.id = UUID.randomUUID().toString();
        }
        if (this.editingWaypoint == null) {
            waypoint.createdAt = System.currentTimeMillis();
        }
        waypoint.label = (trimmed = this.labelField.method_1882().trim()).isEmpty() ? class_2561.method_43471((String)"zombiezcompanion.waypoint.edit.default_label").getString() : trimmed;
        waypoint.x = parsedX != null ? parsedX : playerX;
        waypoint.y = parsedY != null ? parsedY : playerY;
        waypoint.z = parsedZ != null ? parsedZ : playerZ;
        waypoint.colorRgb = this.selectedColor;
        this.configManager.get().map.showWaypoints = true;
        if (this.editingWaypoint == null) {
            this.configManager.get().map.waypoints.add(waypoint);
        }
        if (updateGuideTarget) {
            MapConfig.GuideTarget target = this.configManager.get().map.guideTarget;
            target.label = waypoint.label;
            target.x = waypoint.x;
            target.y = waypoint.y;
            target.z = waypoint.z;
            target.colorRgb = waypoint.colorRgb;
        }
        this.configManager.save();
        this.method_25419();
    }

    private boolean isGuideTarget(MapConfig.Waypoint waypoint) {
        MapConfig.GuideTarget target = this.configManager.get().map.guideTarget;
        return target != null && "Waypoint".equals(target.type) && Math.abs(target.x - waypoint.x) < 0.01 && Math.abs(target.z - waypoint.z) < 0.01;
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, -1610612736);
        int panelW = Math.max(204, Math.min(320, this.field_22789 - 16));
        int panelH = 228;
        int panelX = Math.max(8, (this.field_22789 - panelW) / 2);
        int panelY = Math.max(8, (this.field_22790 - panelH) / 2);
        ctx.method_25294(panelX + 2, panelY + 3, panelX + panelW + 2, panelY + panelH + 3, -1442840576);
        ctx.method_25294(panelX, panelY, panelX + panelW, panelY + panelH, -183627755);
        ctx.method_25294(panelX, panelY, panelX + panelW, panelY + 2, -8874241);
        ctx.method_49601(panelX, panelY, panelW, panelH, -13880766);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)(this.editingWaypoint == null ? "zombiezcompanion.waypoint.edit.title" : "zombiezcompanion.waypoint.edit.title_edit")), panelX + 18, panelY + 14, -854792);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.edit.field.coords"), panelX + 18, panelY + 30, -8353376, false);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.edit.field.name"), panelX + 18, panelY + 74, -8353376, false);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.waypoint.edit.field.color"), panelX + 18, panelY + 118, -8353376, false);
        int selectedIndex = this.colorIndex();
        if (selectedIndex >= 0) {
            int selectedX = panelX + 18 + selectedIndex % 6 * 28;
            int selectedY = panelY + 128 + selectedIndex / 6 * 24;
            ctx.method_49601(selectedX - 2, selectedY, 26, 22, -8874241);
        }
        super.method_25394(ctx, mouseX, mouseY, delta);
        this.drawInvalidBorder(ctx, this.xField, this.invalidX);
        this.drawInvalidBorder(ctx, this.yField, this.invalidY);
        this.drawInvalidBorder(ctx, this.zField, this.invalidZ);
        if (!this.validationError.getString().isEmpty()) {
            ctx.method_51439(this.field_22793, this.validationError, panelX + 18, panelY + 64, -42394, false);
        }
    }

    private void drawInvalidBorder(class_332 ctx, class_342 field, boolean invalid) {
        if (!invalid) {
            return;
        }
        ctx.method_49601(field.method_46426() - 1, field.method_46427() - 1, field.method_25368() + 2, field.method_25364() + 2, -42394);
    }

    private int colorIndex() {
        for (int i = 0; i < COLORS.length; ++i) {
            if (COLORS[i] != this.selectedColor) continue;
            return i;
        }
        return -1;
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

