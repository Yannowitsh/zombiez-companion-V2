/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_357
 *  net.minecraft.class_5348
 */
package io.github.keoz5.zombiezcompanion.ui.widget;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_357;
import net.minecraft.class_5348;

public final class StyledSlider
extends class_357 {
    private final double min;
    private final double max;
    private final DoubleConsumer onChange;
    private final DoubleFunction<class_2561> labeller;

    public StyledSlider(int x, int y, int width, int height, double initial, double min, double max, DoubleConsumer onChange, DoubleFunction<class_2561> labeller) {
        super(x, y, width, height, (class_2561)class_2561.method_43473(), StyledSlider.normalize(initial, min, max));
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        this.labeller = labeller;
        this.method_25346();
    }

    private static double normalize(double v, double mn, double mx) {
        if (mx <= mn) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (v - mn) / (mx - mn)));
    }

    public double currentValue() {
        return this.min + (this.max - this.min) * this.field_22753;
    }

    protected void method_25346() {
        if (this.labeller != null) {
            this.method_25355(this.labeller.apply(this.currentValue()));
        }
    }

    protected void method_25344() {
        if (this.onChange != null) {
            this.onChange.accept(this.currentValue());
        }
    }

    public void method_48579(class_332 ctx, int mouseX, int mouseY, float delta) {
        int x1 = this.method_46426();
        int y1 = this.method_46427();
        int x2 = x1 + this.method_25368();
        int y2 = y1 + this.method_25364();
        boolean focused = this.method_49606() || this.method_25370();
        ctx.method_25294(x1 + 1, y2, x2 - 1, y2 + 2, 0x66000000);
        ctx.method_25294(x1 + 1, y1, x2 - 1, y2, -267777006);
        ctx.method_25294(x1, y1 + 1, x1 + 1, y2 - 1, -267777006);
        ctx.method_25294(x2 - 1, y1 + 1, x2, y2 - 1, -267777006);
        ctx.method_25294(x1 + 1, y1, x2 - 1, y1 + 1, 0x33FFFFFF);
        ctx.method_25294(x1 + 1, y2 - 1, x2 - 1, y2, 0x55000000);
        int fillW = (int)Math.round(this.field_22753 * (double)(this.method_25368() - 4));
        if (fillW > 0) {
            int fx1 = x1 + 2;
            int fx2 = x1 + 2 + fillW;
            int fy1 = y1 + 2;
            int fy2 = y2 - 2;
            ctx.method_25294(fx1, fy1, fx2, fy2, -14867392);
            ctx.method_25294(fx1, fy2 - 2, fx2, fy2, -8874241);
            ctx.method_25294(fx1, fy1, fx2, fy1 + 1, 0x44FFFFFF);
        }
        int border = focused ? -8874241 : -14736594;
        ctx.method_25294(x1 + 1, y1, x2 - 1, y1 + 1, border);
        ctx.method_25294(x1 + 1, y2 - 1, x2 - 1, y2, border);
        ctx.method_25294(x1, y1 + 1, x1 + 1, y2 - 1, border);
        ctx.method_25294(x2 - 1, y1 + 1, x2, y2 - 1, border);
        int handleX = x1 + (int)Math.round(this.field_22753 * (double)(this.method_25368() - 8));
        int handleW = 8;
        int hx1 = handleX;
        int hx2 = handleX + handleW;
        int hy1 = y1 - 2;
        int hy2 = y2 + 2;
        ctx.method_25294(hx1 + 1, hy2, hx2 - 1, hy2 + 2, -1442840576);
        int handleBg = focused ? -8874241 : -11441921;
        ctx.method_25294(hx1 + 1, hy1, hx2 - 1, hy2, handleBg);
        ctx.method_25294(hx1, hy1 + 1, hx1 + 1, hy2 - 1, handleBg);
        ctx.method_25294(hx2 - 1, hy1 + 1, hx2, hy2 - 1, handleBg);
        ctx.method_25294(hx1 + 1, hy1, hx2 - 1, hy1 + 1, 0x55FFFFFF);
        int dotX = hx1 + handleW / 2;
        int dotY = hy1 + (hy2 - hy1) / 2;
        ctx.method_25294(dotX - 1, dotY - 1, dotX + 1, dotY + 1, focused ? -854792 : 1148753663);
        int hb = focused ? -8874241 : -13880766;
        ctx.method_25294(hx1 + 1, hy1, hx2 - 1, hy1 + 1, hb);
        ctx.method_25294(hx1 + 1, hy2 - 1, hx2 - 1, hy2, hb);
        ctx.method_25294(hx1, hy1 + 1, hx1 + 1, hy2 - 1, hb);
        ctx.method_25294(hx2 - 1, hy1 + 1, hx2, hy2 - 1, hb);
        class_310 mc = class_310.method_1551();
        int textW = mc.field_1772.method_27525((class_5348)this.method_25369());
        int tx = x1 + (this.method_25368() - textW) / 2;
        int ty = y1 + (this.method_25364() - 8) / 2;
        ctx.method_27535(mc.field_1772, this.method_25369(), tx, ty, -854792);
    }
}

