package io.github.keoz5.zombiezcompanion.modules.groups;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Radial "ping wheel": opened by holding the ping key and released to place a categorized ping. The whole
 * open/close/commit lifecycle is driven by {@link GroupsModule} (a raw GLFW poll of the key), so this screen
 * only renders the four sectors (up=danger, down=loot, left=help, right=enemy) and reports the sector the
 * cursor points at. The camera is frozen while it is open, so the aim/raycast at commit is the one from when
 * it opened.
 */
public final class PingWheelScreen
extends Screen {
    /** Cursor distance (px) from the centre below which no category is selected (a generic ping). Kept
     *  small so a slight nudge toward a direction already validates that category (works at any GUI scale). */
    private static final int DEADZONE = 8;
    private volatile int selected = -1; // index into GroupsModule.PING_CATS, or -1 = generic (centre)

    public PingWheelScreen() {
        super((Component)Component.translatable((String)"zombiezcompanion.groups.ping.wheel.title"));
    }

    /** The category the cursor currently points at ("" = generic), read by GroupsModule on key release. */
    public String selectedCategory() {
        int s = this.selected;
        return s < 0 ? "" : GroupsModule.PING_CATS[s];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    //? if >= 26.1 {
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
        int cx = this.width / 2;
        int cy = this.height / 2;
        int dx = mouseX - cx;
        int dy = mouseY - cy;
        int sel = -1;
        if (dx * dx + dy * dy >= DEADZONE * DEADZONE) {
            if (Math.abs(dy) >= Math.abs(dx)) {
                sel = dy < 0 ? 0 : 1; // up = danger (0), down = loot (1)
            } else {
                sel = dx < 0 ? 2 : 3; // left = help (2), right = enemy (3)
            }
        }
        this.selected = sel;
        // Light dim over the frozen world.
        ctx.fill(0, 0, this.width, this.height, 0x66000000);
        int r = 52;
        // A coloured line from the centre toward the selected (cardinal) sector — clear direction feedback.
        if (sel >= 0) {
            int lc = GroupsModule.pingColor(GroupsModule.PING_CATS[sel]);
            switch (sel) {
                case 0: ctx.fill(cx - 1, cy - r + 8, cx + 1, cy - 6, lc); break; // up
                case 1: ctx.fill(cx - 1, cy + 6, cx + 1, cy + r - 8, lc); break; // down
                case 2: ctx.fill(cx - r + 8, cy - 1, cx - 6, cy + 1, lc); break; // left
                case 3: ctx.fill(cx + 6, cy - 1, cx + r - 8, cy + 1, lc); break; // right
            }
        }
        // Centre marker.
        ctx.fill(cx - 2, cy - 2, cx + 2, cy + 2, sel < 0 ? -1 : -12303292);
        this.drawSector(ctx, cx, cy - r, 0, sel);
        this.drawSector(ctx, cx, cy + r, 1, sel);
        this.drawSector(ctx, cx - r, cy, 2, sel);
        this.drawSector(ctx, cx + r, cy, 3, sel);
        ctx.centeredText(this.font, (Component)Component.translatable((String)"zombiezcompanion.groups.ping.wheel.hint"), cx, cy + r + 12, -8355712);
    }

    private void drawSector(GuiGraphicsExtractor ctx, int x, int y, int idx, int sel) {
        boolean on = idx == sel;
        int color = GroupsModule.pingColor(GroupsModule.PING_CATS[idx]);
        String label = GroupsModule.pingCatLabel(GroupsModule.PING_CATS[idx]);
        int bw = this.font.width(label) + 12;
        int bh = 16;
        int bx = x - bw / 2;
        int by = y - bh / 2;
        // Selected: box tinted with the category colour + white text. Unselected: dark box + coloured text.
        ctx.fill(bx, by, bx + bw, by + bh, on ? (0x66000000 | color & 0xFFFFFF) : 0x88000000);
        ctx.outline(bx, by, bw, bh, on ? color : (color & 0x66FFFFFF));
        ctx.centeredText(this.font, (Component)Component.literal((String)label), x, y - 4, on ? -1 : color);
    }
}
