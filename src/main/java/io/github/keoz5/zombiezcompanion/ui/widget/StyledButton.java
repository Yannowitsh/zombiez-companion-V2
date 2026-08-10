package io.github.keoz5.zombiezcompanion.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class StyledButton
extends Button {
    private int bgIdle;
    private int bgHover;
    private final int textColor;

    public StyledButton(int x, int y, int width, int height, Component message, Button.OnPress action, int bgIdle, int bgHover, int textColor) {
        super(x, y, width, height, message, action, DEFAULT_NARRATION);
        this.bgIdle = bgIdle;
        this.bgHover = bgHover;
        this.textColor = textColor;
    }

    public void setColors(int bgIdle, int bgHover) {
        this.bgIdle = bgIdle;
        this.bgHover = bgHover;
    }

    protected void extractContents(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        int textArgb;
        int bg;
        boolean hovered;
        boolean active = this.active;
        boolean bl = hovered = active && (this.isHovered() || this.isFocused());
        if (!active) {
            bg = this.bgIdle;
            textArgb = -12235684;
        } else if (hovered) {
            bg = this.bgHover;
            textArgb = this.textColor;
        } else {
            bg = this.bgIdle;
            textArgb = this.textColor;
        }
        int x1 = this.getX();
        int y1 = this.getY();
        int x2 = x1 + this.getWidth();
        int y2 = y1 + this.getHeight();
        ctx.fill(x1 + 1, y2, x2 - 1, y2 + 2, hovered ? -1442840576 : 0x66000000);
        ctx.fill(x1 + 1, y1, x2 - 1, y2, bg);
        ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, bg);
        ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, bg);
        if (active) {
            ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, hovered ? 0x55FFFFFF : 0x33FFFFFF);
            ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, 0x55000000);
        }
        if (hovered) {
            ctx.fill(x1, y1 + 1, x1 + 2, y2 - 1, -8874241);
        }
        int border = hovered ? -8874241 : -14736594;
        ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, border);
        ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, border);
        ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, border);
        ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, border);
        if (hovered) {
            ctx.fill(x1, y1 - 1, x2, y1, 1148753663);
            ctx.fill(x1, y2, x2, y2 + 1, 1148753663);
            ctx.fill(x1 - 1, y1, x1, y2, 1148753663);
            ctx.fill(x2, y1, x2 + 1, y2, 1148753663);
        }
        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width((FormattedText)this.getMessage());
        int textHeight = 8;
        int tx = x1 + (this.getWidth() - textWidth) / 2;
        int ty = y1 + (this.getHeight() - textHeight) / 2;
        ctx.text(mc.font, this.getMessage(), tx, ty, textArgb);
    }
}

