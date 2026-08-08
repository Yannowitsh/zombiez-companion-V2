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

import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_5348;

public class StyledButton
extends class_4185 {
    private int bgIdle;
    private int bgHover;
    private final int textColor;

    public StyledButton(int x, int y, int width, int height, class_2561 message, class_4185.class_4241 action, int bgIdle, int bgHover, int textColor) {
        super(x, y, width, height, message, action, field_40754);
        this.bgIdle = bgIdle;
        this.bgHover = bgHover;
        this.textColor = textColor;
    }

    public void setColors(int bgIdle, int bgHover) {
        this.bgIdle = bgIdle;
        this.bgHover = bgHover;
    }

    protected void method_48579(class_332 ctx, int mouseX, int mouseY, float delta) {
        int textArgb;
        int bg;
        boolean hovered;
        boolean active = this.field_22763;
        boolean bl = hovered = active && (this.method_49606() || this.method_25370());
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
        int x1 = this.method_46426();
        int y1 = this.method_46427();
        int x2 = x1 + this.method_25368();
        int y2 = y1 + this.method_25364();
        ctx.method_25294(x1 + 1, y2, x2 - 1, y2 + 2, hovered ? -1442840576 : 0x66000000);
        ctx.method_25294(x1 + 1, y1, x2 - 1, y2, bg);
        ctx.method_25294(x1, y1 + 1, x1 + 1, y2 - 1, bg);
        ctx.method_25294(x2 - 1, y1 + 1, x2, y2 - 1, bg);
        if (active) {
            ctx.method_25294(x1 + 1, y1, x2 - 1, y1 + 1, hovered ? 0x55FFFFFF : 0x33FFFFFF);
            ctx.method_25294(x1 + 1, y2 - 1, x2 - 1, y2, 0x55000000);
        }
        if (hovered) {
            ctx.method_25294(x1, y1 + 1, x1 + 2, y2 - 1, -8874241);
        }
        int border = hovered ? -8874241 : -14736594;
        ctx.method_25294(x1 + 1, y1, x2 - 1, y1 + 1, border);
        ctx.method_25294(x1 + 1, y2 - 1, x2 - 1, y2, border);
        ctx.method_25294(x1, y1 + 1, x1 + 1, y2 - 1, border);
        ctx.method_25294(x2 - 1, y1 + 1, x2, y2 - 1, border);
        if (hovered) {
            ctx.method_25294(x1, y1 - 1, x2, y1, 1148753663);
            ctx.method_25294(x1, y2, x2, y2 + 1, 1148753663);
            ctx.method_25294(x1 - 1, y1, x1, y2, 1148753663);
            ctx.method_25294(x2, y1, x2 + 1, y2, 1148753663);
        }
        class_310 mc = class_310.method_1551();
        int textWidth = mc.field_1772.method_27525((class_5348)this.method_25369());
        int textHeight = 8;
        int tx = x1 + (this.method_25368() - textWidth) / 2;
        int ty = y1 + (this.method_25364() - textHeight) / 2;
        ctx.method_27535(mc.field_1772, this.method_25369(), tx, ty, textArgb);
    }
}

