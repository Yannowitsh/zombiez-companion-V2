package io.github.keoz5.zombiezcompanion.ui.widget;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public final class CategoryTabButton
extends Button {
    private final BooleanSupplier selected;

    public CategoryTabButton(int x, int y, int width, int height, Component message, Button.OnPress action, BooleanSupplier selected) {
        super(x, y, width, height, message, action, DEFAULT_NARRATION);
        this.selected = selected;
    }

    protected void extractContents(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        int textArgb;
        int bg;
        boolean hovered;
        boolean isSelected = this.selected.getAsBoolean();
        boolean bl = hovered = this.isHovered() || this.isFocused();
        if (isSelected) {
            bg = hovered ? -9534721 : -11441921;
            textArgb = -854792;
        } else {
            bg = hovered ? -266394832 : -434693598;
            textArgb = hovered ? -854792 : -8353376;
        }
        int x1 = this.getX();
        int y1 = this.getY();
        int x2 = x1 + this.getWidth();
        int y2 = y1 + this.getHeight();
        if (isSelected) {
            ctx.fill(x1 + 1, y2, x2 - 1, y2 + 2, 0x66000000);
        }
        ctx.fill(x1 + 1, y1, x2 - 1, y2, bg);
        ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, bg);
        ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, bg);
        ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, isSelected ? 0x55FFFFFF : 0x33FFFFFF);
        if (isSelected) {
            ctx.fill(x1 + 1, y2 - 2, x2 - 1, y2, -8874241);
        }
        int border = isSelected ? -8874241 : -14736594;
        ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, border);
        ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, border);
        ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, border);
        ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, border);
        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width((FormattedText)this.getMessage());
        int tx = x1 + (this.getWidth() - textWidth) / 2;
        int ty = y1 + (this.getHeight() - 8) / 2;
        ctx.text(mc.font, this.getMessage(), tx, ty, textArgb);
    }
}

