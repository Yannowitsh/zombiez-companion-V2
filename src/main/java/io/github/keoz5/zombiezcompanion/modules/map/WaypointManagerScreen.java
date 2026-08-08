package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointDeleteConfirmScreen;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointEditScreen;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

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
        super((Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.title"));
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
        this.addDrawableChild(this.sortButton);
        this.addDrawableChild(new StyledButton(this.panelX + this.panelW - 36, this.panelY + 8, 22, 22, (Text)Text.literal((String)"X"), button -> this.close(), -266723542, -265932737, -854792));
        this.addDrawableChild(new StyledButton(this.panelX + this.panelW - closeW - 14, this.panelY + this.panelH - 32, closeW, 20, (Text)Text.translatable((String)"zombiezcompanion.button.close"), button -> this.close(), -266723542, -265932737, -854792));
        this.addDrawableChild(new StyledButton(this.panelX + 14, this.panelY + this.panelH - 32, 100, 20, (Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.create"), button -> this.createWaypointAtPlayer(), -14867392, -11441921, -854792));
        this.clearAllButton = new StyledButton(this.panelX + 122, this.panelY + this.panelH - 32, 106, 20, (Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.delete_all"), button -> this.confirmDeleteAll(), -12965328, -11716288, -854792);
        this.addDrawableChild(this.clearAllButton);
        this.rebuildRows();
    }

    private void rebuildRows() {
        int index;
        for (StyledButton button2 : this.rowButtons) {
            this.remove((Element)button2);
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
            StyledButton visibilityButton = new StyledButton(this.panelX + this.panelW - 262, y + 6, 66, 18, this.visibilityLabel(waypoint), button -> this.toggleWaypointVisible(waypoint), waypoint.visible ? -14867392 : -266723542, waypoint.visible ? -11441921 : -265932737, -854792);
            StyledButton mapButton = new StyledButton(this.panelX + this.panelW - 190, y + 6, 50, 18, (Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.row.map"), button -> this.openOnMap(waypoint), -14867392, -11441921, -854792);
            StyledButton editButton = new StyledButton(this.panelX + this.panelW - 134, y + 6, 54, 18, (Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.row.edit"), button -> this.editWaypoint(waypoint), -266723542, -265932737, -854792);
            StyledButton deleteButton = new StyledButton(this.panelX + this.panelW - 74, y + 6, 58, 18, (Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.row.delete"), button -> this.confirmDelete(waypoint), -12965328, -11716288, -854792);
            this.rowButtons.add(visibilityButton);
            this.rowButtons.add(mapButton);
            this.rowButtons.add(editButton);
            this.rowButtons.add(deleteButton);
            this.addDrawableChild(visibilityButton);
            this.addDrawableChild(mapButton);
            this.addDrawableChild(editButton);
            this.addDrawableChild(deleteButton);
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

    private Text sortLabel() {
        String key = switch (this.sortMode.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> "zombiezcompanion.waypoint.manager.sort.distance";
            case 1 -> "zombiezcompanion.waypoint.manager.sort.name";
            case 2 -> "zombiezcompanion.waypoint.manager.sort.created";
        };
        return Text.translatable((String)key);
    }

    private Text visibilityLabel(MapConfig.Waypoint waypoint) {
        return Text.translatable((String)(waypoint.visible ? "zombiezcompanion.waypoint.manager.row.visible" : "zombiezcompanion.waypoint.manager.row.hidden"));
    }

    private void toggleWaypointVisible(MapConfig.Waypoint waypoint) {
        boolean bl = waypoint.visible = !waypoint.visible;
        if (!waypoint.visible && this.isGuideTarget(waypoint)) {
            this.configManager.get().map.guideTarget = null;
        }
        this.configManager.save();
        this.rebuildRows();
    }

    private void openOnMap(MapConfig.Waypoint waypoint) {
        if (this.client != null) {
            this.client.setScreen((Screen)new ZombieZMapScreen(this.configManager, waypoint.x, waypoint.z));
        }
    }

    private void editWaypoint(MapConfig.Waypoint waypoint) {
        if (this.client != null) {
            this.client.setScreen((Screen)new WaypointEditScreen(this, this.configManager, waypoint));
        }
    }

    private void confirmDelete(MapConfig.Waypoint waypoint) {
        if (this.client != null) {
            this.client.setScreen((Screen)new WaypointDeleteConfirmScreen(this, this.configManager, waypoint.id));
        }
    }

    private void confirmDeleteAll() {
        if (!this.waypoints().isEmpty() && this.client != null) {
            this.client.setScreen((Screen)new WaypointDeleteConfirmScreen(this, this.configManager));
        }
    }

    private void createWaypointAtPlayer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        double x = 0.0;
        double z = 0.0;
        if (mc.player != null) {
            x = mc.player.getX();
            z = mc.player.getZ();
        }
        if (this.client != null) {
            this.client.setScreen((Screen)new WaypointEditScreen(this, this.configManager, x, z));
        }
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, -872415232);
        ctx.fill(this.panelX + 2, this.panelY + 3, this.panelX + this.panelW + 2, this.panelY + this.panelH + 3, -1442840576);
        ctx.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, -183627755);
        ctx.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 38, -183232737);
        ctx.fill(this.panelX, this.panelY + this.panelH - 44, this.panelX + this.panelW, this.panelY + this.panelH, -183232737);
        ctx.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 2, -8874241);
        ctx.drawBorder(this.panelX, this.panelY, this.panelW, this.panelH, -13880766);
        this.renderRows(ctx, mouseX, mouseY);
        super.render(ctx, mouseX, mouseY, delta);
        this.renderForeground(ctx);
    }

    private void renderRows(DrawContext ctx, int mouseX, int mouseY) {
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
            ctx.drawBorder(this.panelX + 14, y, this.panelW - 28, 30, hovered || selected ? -8874241 : -14736594);
        }
    }

    private void renderForeground(DrawContext ctx) {
        int index;
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.header"), this.panelX + 14, this.panelY + 14, -854792);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.count", (Object[])new Object[]{this.waypoints().size()}), this.panelX + this.panelW - 118, this.panelY + 15, -8353376, false);
        if (this.waypoints().isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.empty.primary"), this.panelX + this.panelW / 2, this.panelY + this.panelH / 2 - 8, -8353376);
            ctx.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.empty.hint"), this.panelX + this.panelW / 2, this.panelY + this.panelH / 2 + 8, -9735552);
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
            String label = this.textRenderer.trimToWidth(waypoint.label == null ? "Rep\u00e8re" : waypoint.label, textMaxWidth);
            ctx.drawTextWithShadow(this.textRenderer, label, this.panelX + 42, y + 5, waypoint.visible ? -854792 : -12235684);
            Text distance = this.waypointDistanceLabel(waypoint);
            String detail = Text.translatable((String)"zombiezcompanion.coord.xz", (Object[])new Object[]{(int)Math.round(waypoint.x), (int)Math.round(waypoint.z)}).getString() + "  |  " + distance.getString();
            ctx.drawText(this.textRenderer, (Text)Text.literal((String)this.textRenderer.trimToWidth(detail, textMaxWidth)), this.panelX + 42, y + 17, -8353376, false);
        }
        if (visible.size() > maxRows) {
            int from = this.scrollOffset + 1;
            int to = Math.min(this.scrollOffset + maxRows, visible.size());
            ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.waypoint.manager.scroll_hint", (Object[])new Object[]{from, to, visible.size()}), this.panelX + 124, this.panelY + this.panelH - 26, -8353376, false);
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

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 78) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null) break;
                sorted.sort(Comparator.comparingDouble(waypoint -> Math.hypot(waypoint.x - mc.player.getX(), waypoint.z - mc.player.getZ())));
            }
        }
        return sorted;
    }

    private Text waypointDistanceLabel(MapConfig.Waypoint waypoint) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return Text.translatable((String)"zombiezcompanion.waypoint.manager.distance.unknown");
        }
        int distance = (int)Math.round(Math.hypot(waypoint.x - mc.player.getX(), waypoint.z - mc.player.getZ()));
        return Text.translatable((String)"zombiezcompanion.waypoint.manager.distance", (Object[])new Object[]{distance});
    }

    private boolean isGuideTarget(MapConfig.Waypoint waypoint) {
        MapConfig.GuideTarget target = this.configManager.get().map.guideTarget;
        return target != null && "Waypoint".equals(target.type) && Math.abs(target.x - waypoint.x) < 0.01 && Math.abs(target.z - waypoint.z) < 0.01;
    }

    public void close() {
        this.configManager.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    public boolean shouldPause() {
        return false;
    }

    private static enum SortMode {
        DISTANCE,
        NAME,
        CREATED;

    }
}

