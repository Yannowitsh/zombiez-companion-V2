package io.github.keoz5.zombiezcompanion.modules.minievents;

import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A simple scrollable list to pick a spawn-alert sound. The first row is "Aucun" (disables the alert);
 * the rest are {@link SpawnSounds#IDS}. Clicking a row previews it (plays it once) and applies it live, so
 * closing keeps the last-clicked choice.
 */
public final class SoundPickerScreen
extends Screen {
    private static final int ROW = 16;

    private final Screen parent;
    private final Consumer<String> setter;
    private final float previewVolume;
    private final String[] entries;
    private String selected;
    private int scrollRow;
    private int panelX1, panelY1, panelX2, panelY2, listY1, listY2, visibleRows;

    public SoundPickerScreen(Screen parent, String current, Consumer<String> setter, float previewVolume) {
        super((Component)Component.translatable((String)"zombiezcompanion.mini_events.sound.pick.title"));
        this.parent = parent;
        this.setter = setter;
        this.previewVolume = previewVolume;
        this.selected = current == null ? "" : current;
        this.entries = new String[SpawnSounds.IDS.length + 1];
        this.entries[0] = "";
        System.arraycopy(SpawnSounds.IDS, 0, this.entries, 1, SpawnSounds.IDS.length);
    }

    @Override
    protected void init() {
        int w = Math.min(300, this.width - 40);
        int h = Math.min(320, this.height - 40);
        this.panelX1 = (this.width - w) / 2;
        this.panelY1 = (this.height - h) / 2;
        this.panelX2 = this.panelX1 + w;
        this.panelY2 = this.panelY1 + h;
        this.listY1 = this.panelY1 + 34;
        this.listY2 = this.panelY2 - 32;
        this.visibleRows = Math.max(1, (this.listY2 - this.listY1) / ROW);
        int bw = 100;
        this.addRenderableWidget(new StyledButton((this.panelX1 + this.panelX2) / 2 - bw / 2, this.panelY2 - 26, bw, 20, (Component)Component.translatable((String)"zombiezcompanion.mini_events.sound.pick.validate"), b -> this.onClose(), -266723542, -265932737, -854792));
    }

    private int maxScroll() {
        return Math.max(0, this.entries.length - this.visibleRows);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollRow = Math.max(0, Math.min(this.maxScroll(), this.scrollRow + (verticalAmount < 0.0 ? 1 : -1)));
        return true;
    }

    // Suppress the vanilla screen background: its blur/darken (rendered by super) would wash out the panel.
    //? if >= 26.1 {
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void renderBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
    }

    //? if >= 26.1 {
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        double mx = event.x();
        double my = event.y();
    //?} else {
    /*public boolean mouseClicked(double mx, double my, int button) {
    *///?}
        if (button == 0 && mx >= (double)this.panelX1 && mx < (double)this.panelX2 && my >= (double)this.listY1 && my < (double)this.listY2) {
            int row = this.scrollRow + (int)((my - (double)this.listY1) / (double)ROW);
            if (row >= 0 && row < this.entries.length) {
                this.selected = this.entries[row];
                this.setter.accept(this.selected);
                if (!this.selected.isEmpty()) {
                    SpawnSounds.play(this.selected, this.previewVolume);
                }
                return true;
            }
        }
        //? if >= 26.1 {
        return super.mouseClicked(event, doubleClick);
        //?} else {
        /*return super.mouseClicked(mx, my, button);
        *///?}
    }

    @Override
    //? if >= 26.1 {
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
        ctx.fill(0, 0, this.width, this.height, -872415232);
        ctx.fill(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.outline(this.panelX1, this.panelY1, this.panelX2 - this.panelX1, this.panelY2 - this.panelY1, -13880766);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.mini_events.sound.pick.title"), this.panelX1 + 12, this.panelY1 + 12, -854792, false);
        int start = this.scrollRow;
        for (int i = 0; i < this.visibleRows; ++i) {
            int idx = start + i;
            if (idx >= this.entries.length) break;
            String id = this.entries[idx];
            int ry = this.listY1 + i * ROW;
            boolean hover = mouseX >= this.panelX1 && mouseX < this.panelX2 && mouseY >= ry && mouseY < ry + ROW;
            boolean sel = id.equals(this.selected);
            if (sel) {
                ctx.fill(this.panelX1 + 4, ry, this.panelX2 - 4, ry + ROW, 0x6033FF9E);
            } else if (hover) {
                ctx.fill(this.panelX1 + 4, ry, this.panelX2 - 4, ry + ROW, 0x40FFFFFF);
            }
            String label = id.isEmpty()
                ? Component.translatable((String)"zombiezcompanion.mini_events.sound.none").getString()
                : SpawnSounds.label(id);
            ctx.text(this.font, (Component)Component.literal((String)label), this.panelX1 + 12, ry + 4, sel ? -8331542 : -3092272, false);
        }
        // Scrollbar indicator (only when the list overflows) so it's clear more sounds are below.
        if (this.entries.length > this.visibleRows) {
            int tx2 = this.panelX2 - 4;
            int tx1 = tx2 - 3;
            int th = this.listY2 - this.listY1;
            ctx.fill(tx1, this.listY1, tx2, this.listY2, 0x40FFFFFF);
            int thumbH = Math.max(12, th * this.visibleRows / this.entries.length);
            int ms = this.maxScroll();
            int thumbY = this.listY1 + (ms <= 0 ? 0 : (th - thumbH) * this.scrollRow / ms);
            ctx.fill(tx1, thumbY, tx2, thumbY + thumbH, -1442840321);
        }
        // Render the "Valider" button widget.
        //? if >= 26.1 {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        //?} else {
        /*super.render(ctx, mouseX, mouseY, delta);
        *///?}
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
