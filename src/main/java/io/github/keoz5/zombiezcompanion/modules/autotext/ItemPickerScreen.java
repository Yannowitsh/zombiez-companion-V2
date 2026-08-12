package io.github.keoz5.zombiezcompanion.modules.autotext;

import io.github.keoz5.zombiezcompanion.config.AutoTextConfig;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Visual item browser for a preset's bar icon: a scrollable grid of every registered item with a search box.
 * Clicking an item writes its id into the preset and returns to the {@link PresetEditScreen}.
 */
public final class ItemPickerScreen
extends Screen {
    /** Cached once: every non-air item as (id, searchable text, render stack). Items don't change at runtime. */
    private static List<ItemEntry> ALL;
    private static final int CELL = 18;

    private final Screen parent;
    private final AutoTextConfig.Preset preset;
    private final List<ItemEntry> filtered = new ArrayList<ItemEntry>();
    private EditBox searchField;
    private int scrollRow;

    private int panelX1, panelY1, panelX2, panelY2, titleY2, footerY1;
    private int gridX1, gridY1, gridX2, gridY2, cols, visibleRows;

    public ItemPickerScreen(Screen parent, AutoTextConfig.Preset preset) {
        super((Component)Component.translatable((String)"zombiezcompanion.autotext.pick.title"));
        this.parent = parent;
        this.preset = preset;
    }

    @Override
    protected void init() {
        this.computeLayout();
        this.searchField = new EditBox(this.font, this.panelX1 + 12, this.panelY1 + 14, this.panelX2 - this.panelX1 - 24 - 26, 18, (Component)Component.literal((String)""));
        this.searchField.setMaxLength(64);
        this.searchField.setHint((Component)Component.translatable((String)"zombiezcompanion.autotext.pick.search"));
        this.searchField.setResponder(v -> this.applyFilter(v));
        this.addRenderableWidget(this.searchField);
        this.setInitialFocus((net.minecraft.client.gui.components.events.GuiEventListener)this.searchField);
        this.addRenderableWidget(new StyledButton(this.panelX2 - 12 - 22, this.panelY1 + 14, 22, 18, (Component)Component.literal((String)"X"), b -> this.onClose(), -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(this.panelX1 + 12, this.footerY1 + 7, 100, 20, (Component)Component.translatable((String)"zombiezcompanion.button.back"), b -> this.onClose(), -266723542, -265932737, -854792));
        this.applyFilter(this.searchField.getValue());
    }

    private void computeLayout() {
        int margin = Math.max(6, Math.min(28, Math.min(this.width, this.height) / 16));
        int panelW = Math.min(380, this.width - 2 * margin);
        int panelH = Math.min(340, this.height - 2 * margin);
        this.panelX1 = (this.width - panelW) / 2;
        this.panelY1 = (this.height - panelH) / 2;
        this.panelX2 = this.panelX1 + panelW;
        this.panelY2 = this.panelY1 + panelH;
        this.titleY2 = this.panelY1 + 40;
        this.footerY1 = this.panelY2 - 34;
        this.gridX1 = this.panelX1 + 12;
        this.gridY1 = this.titleY2 + 4;
        this.gridX2 = this.panelX2 - 12;
        this.gridY2 = this.footerY1 - 4;
        this.cols = Math.max(1, (this.gridX2 - this.gridX1) / CELL);
        this.visibleRows = Math.max(1, (this.gridY2 - this.gridY1) / CELL);
    }

    private void applyFilter(String query) {
        this.scrollRow = 0;
        this.filtered.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (ItemEntry e : ItemPickerScreen.allItems()) {
            if (q.isEmpty() || e.search.contains(q)) {
                this.filtered.add(e);
            }
        }
    }

    private int totalRows() {
        return (this.filtered.size() + this.cols - 1) / this.cols;
    }

    private int maxScrollRow() {
        return Math.max(0, this.totalRows() - this.visibleRows);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int next = this.scrollRow + (verticalAmount < 0.0 ? 1 : -1);
        this.scrollRow = Math.max(0, Math.min(this.maxScrollRow(), next));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            if (mx >= (double)this.gridX1 && mx < (double)(this.gridX1 + this.cols * CELL) && my >= (double)this.gridY1 && my < (double)(this.gridY1 + this.visibleRows * CELL)) {
                int col = (int)((mx - (double)this.gridX1) / (double)CELL);
                int row = this.scrollRow + (int)((my - (double)this.gridY1) / (double)CELL);
                int index = row * this.cols + col;
                if (index >= 0 && index < this.filtered.size()) {
                    this.preset.itemId = this.filtered.get(index).id;
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.parent);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, -872415232);
        ctx.fill(this.panelX1 + 3, this.panelY1 + 6, this.panelX2 + 3, this.panelY2 + 6, -1442840576);
        ctx.fill(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.fill(this.panelX1, this.footerY1, this.panelX2, this.footerY1 + 1, -14736594);
        ctx.outline(this.gridX1 - 1, this.gridY1 - 1, this.cols * CELL + 2, this.visibleRows * CELL + 2, -14736594);
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.autotext.pick.title"), this.panelX1 + 12, this.panelY1 + 36, -854792, false);

        ItemEntry hovered = null;
        int start = this.scrollRow * this.cols;
        for (int visible = 0; visible < this.visibleRows * this.cols; ++visible) {
            int index = start + visible;
            if (index >= this.filtered.size()) break;
            ItemEntry e = this.filtered.get(index);
            int col = visible % this.cols;
            int row = visible / this.cols;
            int sx = this.gridX1 + col * CELL;
            int sy = this.gridY1 + row * CELL;
            boolean hover = mouseX >= sx && mouseX < sx + CELL && mouseY >= sy && mouseY < sy + CELL;
            boolean selected = e.id.equals(this.preset.itemId);
            ctx.fill(sx, sy, sx + CELL - 1, sy + CELL - 1, hover ? 0x60FFFFFF : 0x30000000);
            if (selected) {
                ctx.outline(sx, sy, CELL - 1, CELL - 1, -8874241);
            }
            ctx.item(e.stack, sx + 1, sy + 1);
            if (hover) {
                hovered = e;
            }
        }

        // Footer: hovered item name + id, or a result count.
        if (hovered != null) {
            ctx.text(this.font, (Component)Component.literal((String)(hovered.name + " §7(" + hovered.id + "§7)")), this.panelX1 + 12 + 108, this.footerY1 + 12, -854792, false);
        } else {
            ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.autotext.pick.count", (Object[])new Object[]{Integer.valueOf(this.filtered.size())}), this.panelX1 + 12 + 108, this.footerY1 + 12, -8353376, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Builds (once) the searchable list of every non-air item, sorted by id. */
    private static synchronized List<ItemEntry> allItems() {
        if (ALL != null) {
            return ALL;
        }
        List<ItemEntry> list = new ArrayList<ItemEntry>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == null || item == Items.AIR) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                continue;
            }
            ItemStack stack = new ItemStack((net.minecraft.world.level.ItemLike)item);
            String name = stack.getHoverName().getString();
            String idStr = id.toString();
            list.add(new ItemEntry(idStr, name, (idStr + " " + name).toLowerCase(Locale.ROOT), stack));
        }
        list.sort((a, b) -> a.id.compareTo(b.id));
        ALL = list;
        return ALL;
    }

    private static final class ItemEntry {
        final String id;
        final String name;
        final String search;
        final ItemStack stack;

        ItemEntry(String id, String name, String search, ItemStack stack) {
            this.id = id;
            this.name = name;
            this.search = search;
            this.stack = stack;
        }
    }
}
