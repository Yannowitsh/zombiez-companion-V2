package io.github.keoz5.zombiezcompanion.ui.widget;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public final class StyledSlider
extends AbstractSliderButton {
    private final double min;
    private final double max;
    private final DoubleConsumer onChange;
    private final DoubleFunction<Component> labeller;

    public StyledSlider(int x, int y, int width, int height, double initial, double min, double max, DoubleConsumer onChange, DoubleFunction<Component> labeller) {
        super(x, y, width, height, (Component)Component.empty(), StyledSlider.normalize(initial, min, max));
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        this.labeller = labeller;
        this.updateMessage();
    }

    private static double normalize(double v, double mn, double mx) {
        if (mx <= mn) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (v - mn) / (mx - mn)));
    }

    public double currentValue() {
        return this.min + (this.max - this.min) * this.value;
    }

    protected void updateMessage() {
        if (this.labeller != null) {
            this.setMessage(this.labeller.apply(this.currentValue()));
        }
    }

    protected void applyValue() {
        if (this.onChange != null) {
            this.onChange.accept(this.currentValue());
        }
    }

    public void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        int x1 = this.getX();
        int y1 = this.getY();
        int x2 = x1 + this.getWidth();
        int y2 = y1 + this.getHeight();
        boolean focused = this.isHovered() || this.isFocused();
        ctx.fill(x1 + 1, y2, x2 - 1, y2 + 2, 0x66000000);
        ctx.fill(x1 + 1, y1, x2 - 1, y2, -267777006);
        ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, -267777006);
        ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, -267777006);
        ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, 0x33FFFFFF);
        ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, 0x55000000);
        int fillW = (int)Math.round(this.value * (double)(this.getWidth() - 4));
        if (fillW > 0) {
            int fx1 = x1 + 2;
            int fx2 = x1 + 2 + fillW;
            int fy1 = y1 + 2;
            int fy2 = y2 - 2;
            ctx.fill(fx1, fy1, fx2, fy2, -14867392);
            ctx.fill(fx1, fy2 - 2, fx2, fy2, -8874241);
            ctx.fill(fx1, fy1, fx2, fy1 + 1, 0x44FFFFFF);
        }
        int border = focused ? -8874241 : -14736594;
        ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, border);
        ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, border);
        ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, border);
        ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, border);
        int handleX = x1 + (int)Math.round(this.value * (double)(this.getWidth() - 8));
        int handleW = 8;
        int hx1 = handleX;
        int hx2 = handleX + handleW;
        int hy1 = y1 - 2;
        int hy2 = y2 + 2;
        ctx.fill(hx1 + 1, hy2, hx2 - 1, hy2 + 2, -1442840576);
        int handleBg = focused ? -8874241 : -11441921;
        ctx.fill(hx1 + 1, hy1, hx2 - 1, hy2, handleBg);
        ctx.fill(hx1, hy1 + 1, hx1 + 1, hy2 - 1, handleBg);
        ctx.fill(hx2 - 1, hy1 + 1, hx2, hy2 - 1, handleBg);
        ctx.fill(hx1 + 1, hy1, hx2 - 1, hy1 + 1, 0x55FFFFFF);
        int dotX = hx1 + handleW / 2;
        int dotY = hy1 + (hy2 - hy1) / 2;
        ctx.fill(dotX - 1, dotY - 1, dotX + 1, dotY + 1, focused ? -854792 : 1148753663);
        int hb = focused ? -8874241 : -13880766;
        ctx.fill(hx1 + 1, hy1, hx2 - 1, hy1 + 1, hb);
        ctx.fill(hx1 + 1, hy2 - 1, hx2 - 1, hy2, hb);
        ctx.fill(hx1, hy1 + 1, hx1 + 1, hy2 - 1, hb);
        ctx.fill(hx2 - 1, hy1 + 1, hx2, hy2 - 1, hb);
        Minecraft mc = Minecraft.getInstance();
        int textW = mc.font.width((FormattedText)this.getMessage());
        int tx = x1 + (this.getWidth() - textW) / 2;
        int ty = y1 + (this.getHeight() - 8) / 2;
        ctx.text(mc.font, this.getMessage(), tx, ty, -854792);
    }
}

