package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.UUID;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

final class WaypointEditScreen
extends Screen {
    private static final int[] COLORS = new int[]{3528703, 16436245, 2278750, 0xEF4444, 16020150, 10980346, 16486972, 1357990, 6333946, 15235577, 0xFFFFFF, 9741240};
    private static final int COLOR_COLUMNS = 6;
    private final Screen parent;
    private final ConfigManager configManager;
    private final double defaultX;
    private final double defaultY;
    private final double defaultZ;
    private final MapConfig.Waypoint editingWaypoint;
    private TextFieldWidget labelField;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private int selectedColor = COLORS[0];
    private boolean invalidX;
    private boolean invalidY;
    private boolean invalidZ;
    private Text validationError = Text.empty();

    WaypointEditScreen(Screen parent, ConfigManager configManager, double x, double z) {
        this(parent, configManager, x, WaypointEditScreen.playerY(), z, null);
    }

    WaypointEditScreen(Screen parent, ConfigManager configManager, double x, double y, double z) {
        this(parent, configManager, x, y, z, null);
    }

    WaypointEditScreen(Screen parent, ConfigManager configManager, MapConfig.Waypoint waypoint) {
        this(parent, configManager, waypoint.x, waypoint.y, waypoint.z, waypoint);
    }

    private WaypointEditScreen(Screen parent, ConfigManager configManager, double x, double y, double z, MapConfig.Waypoint editingWaypoint) {
        super((Text)Text.translatable((String)"zombiezcompanion.waypoint.edit.title"));
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
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null ? mc.player.getY() : 64.0;
    }

    protected void init() {
        int panelW = Math.max(204, Math.min(320, this.width - 16));
        int panelH = 228;
        int panelX = Math.max(8, (this.width - panelW) / 2);
        int panelY = Math.max(8, (this.height - panelH) / 2);
        int fieldW = (panelW - 36 - 8 - 8) / 3;
        int fieldX = panelX + 18;
        int fieldY = panelY + 42;
        this.xField = this.coordField(fieldX, fieldY, fieldW, this.defaultX);
        this.yField = this.coordField(fieldX + fieldW + 8, fieldY, fieldW, this.defaultY);
        this.zField = this.coordField(fieldX + 2 * (fieldW + 8), fieldY, fieldW, this.defaultZ);
        this.addDrawableChild(this.xField);
        this.addDrawableChild(this.yField);
        this.addDrawableChild(this.zField);
        this.labelField = new TextFieldWidget(this.textRenderer, panelX + 18, panelY + 86, panelW - 36, 20, (Text)Text.empty());
        this.labelField.setMaxLength(32);
        this.labelField.setText(this.editingWaypoint != null ? (this.editingWaypoint.label == null ? "" : this.editingWaypoint.label) : Text.translatable((String)"zombiezcompanion.waypoint.edit.default_name", (Object[])new Object[]{this.configManager.get().map.waypoints.size() + 1}).getString());
        this.setInitialFocus((Element)this.labelField);
        this.addDrawableChild(this.labelField);
        int colorX = panelX + 18;
        for (int i = 0; i < COLORS.length; ++i) {
            int color = COLORS[i];
            int col = i % 6;
            int row = i / 6;
            this.addDrawableChild(new StyledButton(colorX + col * 28, panelY + 130 + row * 24, 22, 18, (Text)Text.literal((String)""), button -> {
                this.selectedColor = color;
            }, 0xFF000000 | color, 0xFF000000 | color, -1));
        }
        int buttonW = (panelW - 48) / 2;
        this.addDrawableChild(new StyledButton(panelX + 18, panelY + 192, buttonW, 20, (Text)Text.translatable((String)(this.editingWaypoint == null ? "zombiezcompanion.waypoint.edit.add" : "zombiezcompanion.waypoint.edit.save")), button -> this.saveWaypoint(), -11441921, -8874241, -854792));
        this.addDrawableChild(new StyledButton(panelX + 30 + buttonW, panelY + 192, buttonW, 20, (Text)Text.translatable((String)"zombiezcompanion.button.cancel"), button -> this.close(), -266723542, -265932737, -854792));
    }

    private TextFieldWidget coordField(int x, int y, int w, double defaultValue) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, w, 20, (Text)Text.empty());
        field.setMaxLength(12);
        field.setText(Integer.toString((int)Math.round(defaultValue)));
        field.setChangedListener(value -> this.clearValidation());
        return field;
    }

    private Double parseCoord(TextFieldWidget field) {
        String text = field.getText().trim();
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
        this.validationError = Text.empty();
    }

    private void saveWaypoint() {
        String trimmed;
        boolean updateGuideTarget;
        this.clearValidation();
        MinecraftClient mc = MinecraftClient.getInstance();
        double playerX = mc.player != null ? mc.player.getX() : this.defaultX;
        double playerY = mc.player != null ? mc.player.getY() : this.defaultY;
        double playerZ = mc.player != null ? mc.player.getZ() : this.defaultZ;
        Double parsedX = this.parseCoord(this.xField);
        Double parsedY = this.parseCoord(this.yField);
        Double parsedZ = this.parseCoord(this.zField);
        this.invalidX = parsedX == null && !this.xField.getText().trim().isEmpty();
        this.invalidY = parsedY == null && !this.yField.getText().trim().isEmpty();
        boolean bl = this.invalidZ = parsedZ == null && !this.zField.getText().trim().isEmpty();
        if (this.invalidX || this.invalidY || this.invalidZ) {
            this.validationError = Text.translatable((String)"zombiezcompanion.waypoint.edit.error.invalid_coords");
            return;
        }
        MapConfig.Waypoint waypoint = this.editingWaypoint == null ? new MapConfig.Waypoint() : this.editingWaypoint;
        boolean bl2 = updateGuideTarget = this.editingWaypoint != null && this.isGuideTarget(waypoint);
        if (waypoint.id == null || waypoint.id.isBlank()) {
            waypoint.id = UUID.randomUUID().toString();
        }
        if (this.editingWaypoint == null) {
            waypoint.createdAt = System.currentTimeMillis();
            // Tag a manually-placed waypoint with the player's current dimension so it is only
            // shown on the map/world it belongs to (map 1 overworld vs. map 2 world2).
            waypoint.dimension = mc.world != null ? mc.world.getRegistryKey().getValue().toString() : null;
        }
        waypoint.label = (trimmed = this.labelField.getText().trim()).isEmpty() ? Text.translatable((String)"zombiezcompanion.waypoint.edit.default_label").getString() : trimmed;
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
        this.close();
    }

    private boolean isGuideTarget(MapConfig.Waypoint waypoint) {
        MapConfig.GuideTarget target = this.configManager.get().map.guideTarget;
        return target != null && "Waypoint".equals(target.type) && Math.abs(target.x - waypoint.x) < 0.01 && Math.abs(target.z - waypoint.z) < 0.01;
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, -1610612736);
        int panelW = Math.max(204, Math.min(320, this.width - 16));
        int panelH = 228;
        int panelX = Math.max(8, (this.width - panelW) / 2);
        int panelY = Math.max(8, (this.height - panelH) / 2);
        ctx.fill(panelX + 2, panelY + 3, panelX + panelW + 2, panelY + panelH + 3, -1442840576);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, -183627755);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 2, -8874241);
        ctx.drawBorder(panelX, panelY, panelW, panelH, -13880766);
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.translatable((String)(this.editingWaypoint == null ? "zombiezcompanion.waypoint.edit.title" : "zombiezcompanion.waypoint.edit.title_edit")), panelX + 18, panelY + 14, -854792);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.waypoint.edit.field.coords"), panelX + 18, panelY + 30, -8353376, false);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.waypoint.edit.field.name"), panelX + 18, panelY + 74, -8353376, false);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.waypoint.edit.field.color"), panelX + 18, panelY + 118, -8353376, false);
        int selectedIndex = this.colorIndex();
        if (selectedIndex >= 0) {
            int selectedX = panelX + 18 + selectedIndex % 6 * 28;
            int selectedY = panelY + 128 + selectedIndex / 6 * 24;
            ctx.drawBorder(selectedX - 2, selectedY, 26, 22, -8874241);
        }
        super.render(ctx, mouseX, mouseY, delta);
        this.drawInvalidBorder(ctx, this.xField, this.invalidX);
        this.drawInvalidBorder(ctx, this.yField, this.invalidY);
        this.drawInvalidBorder(ctx, this.zField, this.invalidZ);
        if (!this.validationError.getString().isEmpty()) {
            ctx.drawText(this.textRenderer, this.validationError, panelX + 18, panelY + 64, -42394, false);
        }
    }

    private void drawInvalidBorder(DrawContext ctx, TextFieldWidget field, boolean invalid) {
        if (!invalid) {
            return;
        }
        ctx.drawBorder(field.getX() - 1, field.getY() - 1, field.getWidth() + 2, field.getHeight() + 2, -42394);
    }

    private int colorIndex() {
        for (int i = 0; i < COLORS.length; ++i) {
            if (COLORS[i] != this.selectedColor) continue;
            return i;
        }
        return -1;
    }

    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    public boolean shouldPause() {
        return false;
    }
}

