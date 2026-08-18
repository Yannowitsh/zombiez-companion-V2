package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointDeleteConfirmScreen;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointEditScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class WaypointManagerScreen
extends Screen {
    private static final int BG = -872415232;
    private static final int PANEL = -183627755;
    private static final int BAND = -183232737;
    private static final int ROW = -267053025;
    private static final int ROW_HOVER = -266526160;
    private static final int BORDER = -13880766;
    private static final int TEXT = -854792;
    private static final int MUTED = -8353376;
    private static final int ROW_STEP = 34;
    private final Screen parent;
    private final ConfigManager configManager;
    private final List<StyledButton> rowButtons = new ArrayList<StyledButton>();
    private int scrollOffset;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private SortMode sortMode = SortMode.DISTANCE;
    private StyledButton sortButton;
    private StyledButton clearAllButton;

    public WaypointManagerScreen(Screen parent, ConfigManager configManager) {
        super((Component)Component.translatable((String)"zombiezcompanion.waypoint.manager.title"));
        this.parent = parent;
        this.configManager = configManager;
    }

    protected void init() {
        this.panelW = Math.min(640, this.width - 48);
        this.panelH = Math.min(360, this.height - 48);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        int closeW = 100;
        this.sortButton = new StyledButton(this.panelX + 150, this.panelY + 9, 128, 20, this.sortLabel(), button -> this.cycleSort(), -266723542, -265932737, -854792);
        this.addRenderableWidget(this.sortButton);
        this.addRenderableWidget(new StyledButton(this.panelX + this.panelW - 36, this.panelY + 8, 22, 22, (Component)Component.literal((String)"X"), button -> this.onClose(), -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(this.panelX + this.panelW - closeW - 14, this.panelY + this.panelH - 32, closeW, 20, (Component)Component.translatable((String)"zombiezcompanion.button.close"), button -> this.onClose(), -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(this.panelX + 14, this.panelY + this.panelH - 32, 100, 20, (Component)Component.translatable((String)"zombiezcompanion.waypoint.manager.create"), button -> this.createWaypointAtPlayer(), -14867392, -11441921, -854792));
        this.clearAllButton = new StyledButton(this.panelX + 122, this.panelY + this.panelH - 32, 106, 20, (Component)Component.translatable((String)"zombiezcompanion.waypoint.manager.delete_all"), button -> this.confirmDeleteAll(), -12965328, -11716288, -854792);
        this.addRenderableWidget(this.clearAllButton);
        this.rebuildRows();
    }

    private void rebuildRows() {
        int index;
        for (StyledButton button2 : this.rowButtons) {
            this.removeWidget((GuiEventListener)button2);
        }
        this.rowButtons.clear();
        List<MapConfig.Waypoint> waypoints = this.visibleWaypoints();
        int maxRows = this.maxRows();
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, Math.max(0, waypoints.size() - maxRows)));
        int startY = this.panelY + 54;
        if (this.clearAllButton != null) {
            boolean bl = this.clearAllButton.active = !this.waypoints().isEmpty();
        }
        if (this.sortButton != null) {
            this.sortButton.setMessage(this.sortLabel());
        }
        for (int visibleIndex = 0; visibleIndex < maxRows && (index = this.scrollOffset + visibleIndex) < waypoints.size(); ++visibleIndex) {
            MapConfig.Waypoint waypoint = waypoints.get(index);
            int y = startY + visibleIndex * 34;
            StyledButton visibilityButton = new StyledButton(this.panelX + this.panelW - 206, y + 6, 66, 18, this.visibilityLabel(waypoint), button -> this.toggleWaypointVisible(waypoint), waypoint.visible ? -14867392 : -266723542, waypoint.visible ? -11441921 : -265932737, -854792);
            StyledButton editButton = new StyledButton(this.panelX + this.panelW - 134, y + 6, 54, 18, (Component)Component.translatable((String)"zombiezcompanion.waypoint.manager.row.edit"), button -> this.editWaypoint(waypoint), -266723542, -265932737, -854792);
            StyledButton deleteButton = new StyledButton(this.panelX + this.panelW - 74, y + 6, 58, 18, (Component)Component.translatable((String)"zombiezcompanion.waypoint.manager.row.delete"), button -> this.confirmDelete(waypoint), -12965328, -11716288, -854792);
            this.rowButtons.add(visibilityButton);
            this.rowButtons.add(editButton);
            this.rowButtons.add(deleteButton);
            this.addRenderableWidget(visibilityButton);
            this.addRenderableWidget(editButton);
            this.addRenderableWidget(deleteButton);
        }
    }

    private void cycleSort() {
        this.sortMode = switch (this.sortMode.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> SortMode.NAME;
            case 1 -> SortMode.CREATED;
            case 2 -> SortMode.DISTANCE;
        };
        this.scrollOffset = 0;
        this.rebuildRows();
    }

    private Component sortLabel() {
        String key = switch (this.sortMode.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> "zombiezcompanion.waypoint.manager.sort.distance";
            case 1 -> "zombiezcompanion.waypoint.manager.sort.name";
            case 2 -> "zombiezcompanion.waypoint.manager.sort.created";
        };
        return Component.translatable((String)key);
    }

    private Component visibilityLabel(MapConfig.Waypoint waypoint) {
        return Component.translatable((String)(waypoint.visible ? "zombiezcompanion.waypoint.manager.row.visible" : "zombiezcompanion.waypoint.manager.row.hidden"));
    }

    private void toggleWaypointVisible(MapConfig.Waypoint waypoint) {
        boolean bl = waypoint.visible = !waypoint.visible;
        if (!waypoint.visible && this.isGuideTarget(waypoint)) {
            this.configManager.get().map.guideTarget = null;
        }
        this.configManager.save();
        this.rebuildRows();
    }

    private void editWaypoint(MapConfig.Waypoint waypoint) {
        if (this.minecraft != null) {
            this.minecraft.setScreen((Screen)new WaypointEditScreen(this, this.configManager, waypoint));
        }
    }

    private void confirmDelete(MapConfig.Waypoint waypoint) {
        if (this.minecraft != null) {
            this.minecraft.setScreen((Screen)new WaypointDeleteConfirmScreen(this, this.configManager, waypoint.id));
        }
    }

    private void confirmDeleteAll() {
        if (!this.waypoints().isEmpty() && this.minecraft != null) {
            this.minecraft.setScreen((Screen)new WaypointDeleteConfirmScreen(this, this.configManager));
        }
    }

    private void createWaypointAtPlayer() {
        Minecraft mc = Minecraft.getInstance();
        double x = 0.0;
        double z = 0.0;
        if (mc.player != null) {
            x = mc.player.getX();
            z = mc.player.getZ();
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen((Screen)new WaypointEditScreen(this, this.configManager, x, z));
        }
    }

    //? if >= 26.1 {
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
        ctx.fill(0, 0, this.width, this.height, -872415232);
        ctx.fill(this.panelX + 2, this.panelY + 3, this.panelX + this.panelW + 2, this.panelY + this.panelH + 3, -1442840576);
        ctx.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, -183627755);
        ctx.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 38, -183232737);
        ctx.fill(this.panelX, this.panelY + this.panelH - 44, this.panelX + this.panelW, this.panelY + this.panelH, -183232737);
        ctx.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 2, -8874241);
        ctx.outline(this.panelX, this.panelY, this.panelW, this.panelH, -13880766);
        this.renderRows(ctx, mouseX, mouseY);
        //? if >= 26.1 {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        //?} else {
        /*super.render(ctx, mouseX, mouseY, delta);
        *///?}
        this.renderForeground(ctx);
    }

    private void renderRows(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int index;
        List<MapConfig.Waypoint> waypoints = this.visibleWaypoints();
        int maxRows = this.maxRows();
        int startY = this.panelY + 54;
        for (int visibleIndex = 0; visibleIndex < maxRows && (index = this.scrollOffset + visibleIndex) < waypoints.size(); ++visibleIndex) {
            int y = startY + visibleIndex * 34;
            boolean hovered = mouseX >= this.panelX + 14 && mouseX <= this.panelX + this.panelW - 14 && mouseY >= y && mouseY <= y + 30;
            MapConfig.Waypoint waypoint = waypoints.get(index);
            boolean selected = this.isGuideTarget(waypoint);
            int rowBg = waypoint.visible ? (hovered ? -266526160 : -267053025) : -1441721316;
            ctx.fill(this.panelX + 14, y, this.panelX + this.panelW - 14, y + 30, rowBg);
            if (selected) {
                ctx.fill(this.panelX + 14, y, this.panelX + 17, y + 30, -8874241);
            }
            ctx.fill(this.panelX + 15, y + 1, this.panelX + this.panelW - 15, y + 3, 0x22FFFFFF);
            ctx.outline(this.panelX + 14, y, this.panelW - 28, 30, hovered || selected ? -8874241 : -14736594);
        }
    }

    private void renderForeground(GuiGraphicsExtractor ctx) {
        int index;
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.waypoint.manager.header"), this.panelX + 14, this.panelY + 14, -854792);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.waypoint.manager.count", (Object[])new Object[]{this.waypoints().size()}), this.panelX + this.panelW - 118, this.panelY + 15, -8353376, false);
        if (this.waypoints().isEmpty()) {
            ctx.centeredText(this.font, (Component)Component.translatable((String)"zombiezcompanion.waypoint.manager.empty.primary"), this.panelX + this.panelW / 2, this.panelY + this.panelH / 2 - 8, -8353376);
            ctx.centeredText(this.font, (Component)Component.translatable((String)"zombiezcompanion.waypoint.manager.empty.hint"), this.panelX + this.panelW / 2, this.panelY + this.panelH / 2 + 8, -9735552);
            return;
        }
        List<MapConfig.Waypoint> visible = this.visibleWaypoints();
        int maxRows = this.maxRows();
        int startY = this.panelY + 54;
        for (int visibleIndex = 0; visibleIndex < maxRows && (index = this.scrollOffset + visibleIndex) < visible.size(); ++visibleIndex) {
            MapConfig.Waypoint waypoint = visible.get(index);
            int y = startY + visibleIndex * 34;
            ctx.fill(this.panelX + 24, y + 9, this.panelX + 34, y + 19, waypoint.visible ? 0xFF000000 | waypoint.colorRgb & 0xFFFFFF : -12959925);
            int textMaxWidth = Math.max(80, this.panelW - 312);
            String label = this.font.plainSubstrByWidth(waypoint.label == null ? "Rep\u00e8re" : waypoint.label, textMaxWidth);
            ctx.text(this.font, label, this.panelX + 42, y + 5, waypoint.visible ? -854792 : -12235684);
            Component distance = this.waypointDistanceLabel(waypoint);
            String detail = Component.translatable((String)"zombiezcompanion.coord.xz", (Object[])new Object[]{(int)Math.round(waypoint.x), (int)Math.round(waypoint.z)}).getString() + "  |  " + distance.getString();
            ctx.text(this.font, (Component)Component.literal((String)this.font.plainSubstrByWidth(detail, textMaxWidth)), this.panelX + 42, y + 17, -8353376, false);
        }
        if (visible.size() > maxRows) {
            int from = this.scrollOffset + 1;
            int to = Math.min(this.scrollOffset + maxRows, visible.size());
            ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.waypoint.manager.scroll_hint", (Object[])new Object[]{from, to, visible.size()}), this.panelX + 124, this.panelY + this.panelH - 26, -8353376, false);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxOffset = Math.max(0, this.visibleWaypoints().size() - this.maxRows());
        if (maxOffset <= 0) {
            return false;
        }
        int next = this.scrollOffset + (verticalAmount < 0.0 ? 1 : -1);
        this.scrollOffset = Math.max(0, Math.min(maxOffset, next));
        this.rebuildRows();
        return true;
    }

    //? if >= 26.1 {
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    //?} else {
    /*public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    *///?}
        if (keyCode == 256 || keyCode == 78) {
            this.onClose();
            return true;
        }
        //? if >= 26.1 {
        return super.keyPressed(event);
        //?} else {
        /*return super.keyPressed(keyCode, scanCode, modifiers);
        *///?}
    }

    private int maxRows() {
        return Math.max(1, (this.panelH - 104) / 34);
    }

    private List<MapConfig.Waypoint> waypoints() {
        return this.configManager.get().map.waypoints;
    }

    private List<MapConfig.Waypoint> visibleWaypoints() {
        ArrayList<MapConfig.Waypoint> sorted = new ArrayList<MapConfig.Waypoint>(this.waypoints());
        switch (this.sortMode.ordinal()) {
            case 1: {
                sorted.sort(Comparator.comparing(waypoint -> waypoint.label == null ? "" : waypoint.label.toLowerCase(Locale.ROOT)));
                break;
            }
            case 2: {
                sorted.sort(Comparator.comparingLong((MapConfig.Waypoint w) -> w.createdAt).reversed());
                break;
            }
            case 0: {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) break;
                sorted.sort(Comparator.comparingDouble(waypoint -> Math.hypot(waypoint.x - mc.player.getX(), waypoint.z - mc.player.getZ())));
            }
        }
        return sorted;
    }

    private Component waypointDistanceLabel(MapConfig.Waypoint waypoint) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return Component.translatable((String)"zombiezcompanion.waypoint.manager.distance.unknown");
        }
        int distance = (int)Math.round(Math.hypot(waypoint.x - mc.player.getX(), waypoint.z - mc.player.getZ()));
        return Component.translatable((String)"zombiezcompanion.waypoint.manager.distance", (Object[])new Object[]{distance});
    }

    private boolean isGuideTarget(MapConfig.Waypoint waypoint) {
        MapConfig.GuideTarget target = this.configManager.get().map.guideTarget;
        return target != null && "Waypoint".equals(target.type) && Math.abs(target.x - waypoint.x) < 0.01 && Math.abs(target.z - waypoint.z) < 0.01;
    }

    public void onClose() {
        this.configManager.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static enum SortMode {
        DISTANCE,
        NAME,
        CREATED;

    }
}

