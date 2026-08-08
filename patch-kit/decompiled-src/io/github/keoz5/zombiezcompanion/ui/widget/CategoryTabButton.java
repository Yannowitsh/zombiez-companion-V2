/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_4185
 *  net.minecraft.class_4185$class_4241
 *  net.minecraft.class_5348
 */
package io.github.keoz5.zombiezcompanion.ui.widget;

import java.util.function.BooleanSupplier;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_5348;

public final class CategoryTabButton
extends class_4185 {
    private final BooleanSupplier selected;

    public CategoryTabButton(int x, int y, int width, int height, class_2561 message, class_4185.class_4241 action, BooleanSupplier selected) {
        super(x, y, width, height, message, action, field_40754);
        this.selected = selected;
    }

    protected void method_48579(class_332 ctx, int mouseX, int mouseY, float delta) {
        int textArgb;
        int bg;
        boolean hovered;
        boolean isSelected = this.selected.getAsBoolean();
        boolean bl = hovered = this.method_49606() || this.method_25370();
        if (isSelected) {
            bg = hovered ? -9534721 : -11441921;
            textArgb = -854792;
        } else {
            bg = hovered ? -266394832 : -434693598;
            textArgb = hovered ? -854792 : -8353376;
        }
        int x1 = this.method_46426();
        int y1 = this.method_46427();
        int x2 = x1 + this.method_25368();
        int y2 = y1 + this.method_25364();
        if (isSelected) {
            ctx.method_25294(x1 + 1, y2, x2 - 1, y2 + 2, 0x66000000);
        }
        ctx.method_25294(x1 + 1, y1, x2 - 1, y2, bg);
        ctx.method_25294(x1, y1 + 1, x1 + 1, y2 - 1, bg);
        ctx.method_25294(x2 - 1, y1 + 1, x2, y2 - 1, bg);
        ctx.method_25294(x1 + 1, y1, x2 - 1, y1 + 1, isSelected ? 0x55FFFFFF : 0x33FFFFFF);
        if (isSelected) {
            ctx.method_25294(x1 + 1, y2 - 2, x2 - 1, y2, -8874241);
        }
        int border = isSelected ? -8874241 : -14736594;
        ctx.method_25294(x1 + 1, y1, x2 - 1, y1 + 1, border);
        ctx.method_25294(x1 + 1, y2 - 1, x2 - 1, y2, border);
        ctx.method_25294(x1, y1 + 1, x1 + 1, y2 - 1, border);
        ctx.method_25294(x2 - 1, y1 + 1, x2, y2 - 1, border);
        class_310 mc = class_310.method_1551();
        int textWidth = mc.field_1772.method_27525((class_5348)this.method_25369());
        int tx = x1 + (this.method_25368() - textWidth) / 2;
        int ty = y1 + (this.method_25364() - 8) / 2;
        ctx.method_27535(mc.field_1772, this.method_25369(), tx, ty, textArgb);
    }
}

