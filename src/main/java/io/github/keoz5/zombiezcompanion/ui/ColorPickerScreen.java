package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Full color picker for one {@link Colors.Element}: a saturation/value square, a hue bar, an alpha bar,
 * an editable #AARRGGBB hex field, copy/paste, and the shared palette as quick swatches. Changes preview
 * live (in-memory) and are saved on close.
 */
public final class ColorPickerScreen
extends Screen {
    private static final int DRAG_NONE = 0;
    private static final int DRAG_SV = 1;
    private static final int DRAG_HUE = 2;
    private static final int DRAG_ALPHA = 3;

    private final Screen parent;
    private final ConfigManager configManager;
    private final Colors.Element element;

    private double hue;
    private double sat;
    private double val;
    private int alpha;
    private int drag = DRAG_NONE;
    private boolean suppressHex;
    private EditBox hexField;

    private int panelX1, panelY1, panelX2, panelY2, titleY1, titleY2, footerY1;
    private int svX, svY, svW, svH;
    private int hueX, hueY, hueW, hueH;
    private int alphaX, alphaY, alphaW, alphaH;
    private int prevX, prevY, prevSize;
    private int palX, palY, palCell;

    public ColorPickerScreen(Screen parent, ConfigManager configManager, Colors.Element element) {
        super((Component)Component.translatable((String)element.langKey()));
        this.parent = parent;
        this.configManager = configManager;
        this.element = element;
        this.setFromArgb(Colors.get(element.id(), element.def()));
    }

    protected void init() {
        this.computeLayout();
        this.addRenderableWidget(new StyledButton(this.panelX1 + 12, this.footerY1 + 7, 100, 20, (Component)Component.translatable((String)"zombiezcompanion.button.back"), b -> this.onClose(), -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(this.panelX2 - 12 - 22, this.titleY1 + 10, 22, 22, (Component)Component.literal((String)"X"), b -> this.onClose(), -266723542, -265932737, -854792));
        this.hexField = new EditBox(this.font, this.svX, this.alphaY + 26, 100, 18, (Component)Component.literal((String)""));
        this.hexField.setMaxLength(9);
        this.hexField.setValue(this.hexString());
        this.hexField.setResponder(this::onHexTyped);
        this.addRenderableWidget(this.hexField);
        int bx = this.svX + 100 + 6;
        this.addRenderableWidget(new StyledButton(bx, this.alphaY + 26, 44, 18, (Component)Component.translatable((String)"zombiezcompanion.colors.copy"), b -> this.copyHex(), -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(bx + 44 + 6, this.alphaY + 26, 44, 18, (Component)Component.translatable((String)"zombiezcompanion.colors.paste"), b -> this.pasteHex(), -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(this.panelX2 - 12 - 90, this.footerY1 + 7, 90, 20, (Component)Component.translatable((String)"zombiezcompanion.colors.reset"), b -> {
            this.setFromArgb(this.element.def());
            Colors.setNoSave(this.element.id(), this.element.def());
            this.refreshHex();
        }, -266723542, -265932737, -854792));
    }

    private void computeLayout() {
        int margin = Math.max(6, Math.min(28, Math.min(this.width, this.height) / 16));
        int panelW = Math.min(360, this.width - 2 * margin);
        int panelH = Math.min(340, this.height - 2 * margin);
        this.panelX1 = (this.width - panelW) / 2;
        this.panelY1 = (this.height - panelH) / 2;
        this.panelX2 = this.panelX1 + panelW;
        this.panelY2 = this.panelY1 + panelH;
        this.titleY1 = this.panelY1;
        this.titleY2 = this.titleY1 + 42;
        this.footerY1 = this.panelY2 - 34;
        this.svX = this.panelX1 + 20;
        this.svY = this.titleY2 + 12;
        this.svW = 150;
        this.svH = 120;
        this.hueX = this.svX + this.svW + 14;
        this.hueY = this.svY;
        this.hueW = 16;
        this.hueH = this.svH;
        this.prevX = this.hueX + this.hueW + 16;
        this.prevY = this.svY;
        this.prevSize = 48;
        this.alphaX = this.svX;
        this.alphaY = this.svY + this.svH + 14;
        this.alphaW = this.svW;
        this.alphaH = 14;
        this.palCell = 18;
        this.palX = this.svX;
        this.palY = this.alphaY + 52;
    }

    // --- color state --------------------------------------------------------

    private void setFromArgb(int argb) {
        this.alpha = argb >>> 24 & 0xFF;
        double[] hsv = ColorPickerScreen.rgbToHsv(argb & 0xFFFFFF);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];
    }

    private int currentArgb() {
        return this.alpha << 24 | ColorPickerScreen.hsvToRgb(this.hue, this.sat, this.val);
    }

    private String hexString() {
        return String.format(Locale.ROOT, "#%08X", this.currentArgb());
    }

    /** Live in-memory apply after a control moves; refreshes the hex field. */
    private void apply() {
        Colors.setNoSave(this.element.id(), this.currentArgb());
        this.refreshHex();
    }

    private void refreshHex() {
        if (this.hexField != null) {
            this.suppressHex = true;
            this.hexField.setValue(this.hexString());
            this.suppressHex = false;
        }
    }

    private void onHexTyped(String s) {
        if (this.suppressHex) {
            return;
        }
        Integer argb = ColorPickerScreen.parseHex(s, this.alpha);
        if (argb != null) {
            this.setFromArgb(argb);
            Colors.setNoSave(this.element.id(), argb);
        }
    }

    private void copyHex() {
        Minecraft.getInstance().keyboardHandler.setClipboard(this.hexString());
    }

    private void pasteHex() {
        String cb = Minecraft.getInstance().keyboardHandler.getClipboard();
        Integer argb = ColorPickerScreen.parseHex(cb, this.alpha);
        if (argb != null) {
            this.setFromArgb(argb);
            Colors.setNoSave(this.element.id(), argb);
            this.refreshHex();
        }
    }

    // --- input --------------------------------------------------------------

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (event.button() == 0) {
            if (ColorPickerScreen.inRect(mx, my, this.svX, this.svY, this.svW, this.svH)) {
                this.drag = DRAG_SV;
                this.applySV(mx, my);
                return true;
            }
            if (ColorPickerScreen.inRect(mx, my, this.hueX, this.hueY, this.hueW, this.hueH)) {
                this.drag = DRAG_HUE;
                this.applyHue(my);
                return true;
            }
            if (ColorPickerScreen.inRect(mx, my, this.alphaX, this.alphaY, this.alphaW, this.alphaH)) {
                this.drag = DRAG_ALPHA;
                this.applyAlpha(mx);
                return true;
            }
            if (my >= (double)this.palY && my < (double)(this.palY + 14)) {
                for (int k = 0; k < Colors.PALETTE.length; ++k) {
                    int px = this.palX + k * this.palCell;
                    if (mx < (double)px || mx >= (double)(px + this.palCell - 2)) continue;
                    this.setFromArgb(this.alpha << 24 | Colors.PALETTE[k] & 0xFFFFFF);
                    this.apply();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        switch (this.drag) {
            case DRAG_SV: {
                this.applySV(event.x(), event.y());
                return true;
            }
            case DRAG_HUE: {
                this.applyHue(event.y());
                return true;
            }
            case DRAG_ALPHA: {
                this.applyAlpha(event.x());
                return true;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        this.drag = DRAG_NONE;
        return super.mouseReleased(event);
    }

    private void applySV(double mx, double my) {
        this.sat = ColorPickerScreen.clamp01((mx - (double)this.svX) / (double)(this.svW - 1));
        this.val = ColorPickerScreen.clamp01(1.0 - (my - (double)this.svY) / (double)(this.svH - 1));
        this.apply();
    }

    private void applyHue(double my) {
        this.hue = ColorPickerScreen.clamp01((my - (double)this.hueY) / (double)(this.hueH - 1)) * 360.0;
        this.apply();
    }

    private void applyAlpha(double mx) {
        this.alpha = (int)Math.round(ColorPickerScreen.clamp01((mx - (double)this.alphaX) / (double)(this.alphaW - 1)) * 255.0);
        this.apply();
    }

    // --- render -------------------------------------------------------------

    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, -872415232);
        ctx.fill(this.panelX1 + 3, this.panelY1 + 6, this.panelX2 + 3, this.panelY2 + 6, -1442840576);
        ctx.fill(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.fill(this.panelX1, this.titleY1, this.panelX2, this.titleY2, -183232737);
        ctx.fill(this.panelX1, this.titleY1, this.panelX2, this.titleY1 + 2, -8874241);
        ctx.fill(this.panelX1, this.titleY2 - 1, this.panelX2, this.titleY2, -14736594);
        ctx.fill(this.panelX1, this.footerY1, this.panelX2, this.footerY1 + 1, -14736594);
        ctx.fill(this.panelX1 + 1, this.panelY1, this.panelX2 - 1, this.panelY1 + 1, -13880766);
        ctx.fill(this.panelX1 + 1, this.panelY2 - 1, this.panelX2 - 1, this.panelY2, -13880766);
        ctx.fill(this.panelX1, this.panelY1 + 1, this.panelX1 + 1, this.panelY2 - 1, -13880766);
        ctx.fill(this.panelX2 - 1, this.panelY1 + 1, this.panelX2, this.panelY2 - 1, -13880766);
        ctx.text(this.font, (Component)Component.translatable((String)this.element.langKey()), this.panelX1 + 18, this.titleY1 + 16, -854792, true);
        this.drawSvSquare(ctx);
        this.drawHueBar(ctx);
        this.drawAlphaBar(ctx);
        this.drawPreview(ctx);
        this.drawPalette(ctx);
        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    private void drawSvSquare(GuiGraphicsExtractor ctx) {
        for (int i = 0; i < this.svW; ++i) {
            double s = this.svW <= 1 ? 0.0 : (double)i / (double)(this.svW - 1);
            int top = 0xFF000000 | ColorPickerScreen.hsvToRgb(this.hue, s, 1.0);
            ctx.fillGradient(this.svX + i, this.svY, this.svX + i + 1, this.svY + this.svH, top, 0xFF000000);
        }
        ctx.outline(this.svX - 1, this.svY - 1, this.svW + 2, this.svH + 2, -16777216);
        int cxp = this.svX + (int)Math.round(this.sat * (double)(this.svW - 1));
        int cyp = this.svY + (int)Math.round((1.0 - this.val) * (double)(this.svH - 1));
        ctx.outline(cxp - 4, cyp - 4, 8, 8, -16777216);
        ctx.outline(cxp - 3, cyp - 3, 6, 6, -1);
    }

    private void drawHueBar(GuiGraphicsExtractor ctx) {
        for (int j = 0; j < this.hueH; ++j) {
            double h = (double)j / (double)(this.hueH - 1) * 360.0;
            ctx.fill(this.hueX, this.hueY + j, this.hueX + this.hueW, this.hueY + j + 1, 0xFF000000 | ColorPickerScreen.hsvToRgb(h, 1.0, 1.0));
        }
        ctx.outline(this.hueX - 1, this.hueY - 1, this.hueW + 2, this.hueH + 2, -16777216);
        int hy = this.hueY + (int)Math.round(this.hue / 360.0 * (double)(this.hueH - 1));
        ctx.fill(this.hueX - 2, hy - 1, this.hueX + this.hueW + 2, hy + 2, -1);
        ctx.outline(this.hueX - 2, hy - 1, this.hueW + 4, 3, -16777216);
    }

    private void drawAlphaBar(GuiGraphicsExtractor ctx) {
        ColorPickerScreen.drawChecker(ctx, this.alphaX, this.alphaY, this.alphaW, this.alphaH);
        int rgb = ColorPickerScreen.hsvToRgb(this.hue, this.sat, this.val);
        for (int i = 0; i < this.alphaW; ++i) {
            int aa = this.alphaW <= 1 ? 255 : (int)Math.round((double)i / (double)(this.alphaW - 1) * 255.0);
            ctx.fill(this.alphaX + i, this.alphaY, this.alphaX + i + 1, this.alphaY + this.alphaH, aa << 24 | rgb);
        }
        ctx.outline(this.alphaX - 1, this.alphaY - 1, this.alphaW + 2, this.alphaH + 2, -16777216);
        int axp = this.alphaX + (int)Math.round((double)this.alpha / 255.0 * (double)(this.alphaW - 1));
        ctx.fill(axp - 1, this.alphaY - 2, axp + 2, this.alphaY + this.alphaH + 2, -1);
        ctx.outline(axp - 1, this.alphaY - 2, 3, this.alphaH + 4, -16777216);
    }

    private void drawPreview(GuiGraphicsExtractor ctx) {
        ColorPickerScreen.drawChecker(ctx, this.prevX, this.prevY, this.prevSize, this.prevSize);
        ctx.fill(this.prevX, this.prevY, this.prevX + this.prevSize, this.prevY + this.prevSize, this.currentArgb());
        ctx.outline(this.prevX - 1, this.prevY - 1, this.prevSize + 2, this.prevSize + 2, -16777216);
    }

    private void drawPalette(GuiGraphicsExtractor ctx) {
        for (int k = 0; k < Colors.PALETTE.length; ++k) {
            int px = this.palX + k * this.palCell;
            ctx.fill(px, this.palY, px + this.palCell - 2, this.palY + 14, 0xFF000000 | Colors.PALETTE[k] & 0xFFFFFF);
            ctx.outline(px, this.palY, this.palCell - 2, 14, -16777216);
        }
    }

    private static void drawChecker(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        int cell = 4;
        for (int yy = 0; yy < h; yy += cell) {
            for (int xx = 0; xx < w; xx += cell) {
                boolean light = (xx / cell + yy / cell) % 2 == 0;
                ctx.fill(x + xx, y + yy, Math.min(x + xx + cell, x + w), Math.min(y + yy + cell, y + h), light ? 0xFFCCCCCC : 0xFF777777);
            }
        }
    }

    // --- helpers ------------------------------------------------------------

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static int hsvToRgb(double h, double s, double v) {
        double c = v * s;
        double hh = (h % 360.0) / 60.0;
        if (hh < 0.0) {
            hh += 6.0;
        }
        double x = c * (1.0 - Math.abs(hh % 2.0 - 1.0));
        double r = 0.0, g = 0.0, b = 0.0;
        if (hh < 1.0) { r = c; g = x; }
        else if (hh < 2.0) { r = x; g = c; }
        else if (hh < 3.0) { g = c; b = x; }
        else if (hh < 4.0) { g = x; b = c; }
        else if (hh < 5.0) { r = x; b = c; }
        else { r = c; b = x; }
        double m = v - c;
        int R = (int)Math.round((r + m) * 255.0);
        int G = (int)Math.round((g + m) * 255.0);
        int B = (int)Math.round((b + m) * 255.0);
        return (R & 0xFF) << 16 | (G & 0xFF) << 8 | B & 0xFF;
    }

    private static double[] rgbToHsv(int rgb) {
        double r = (double)(rgb >> 16 & 0xFF) / 255.0;
        double g = (double)(rgb >> 8 & 0xFF) / 255.0;
        double b = (double)(rgb & 0xFF) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double d = max - min;
        double h = 0.0;
        if (d != 0.0) {
            if (max == r) {
                h = 60.0 * (((g - b) / d) % 6.0);
            } else if (max == g) {
                h = 60.0 * ((b - r) / d + 2.0);
            } else {
                h = 60.0 * ((r - g) / d + 4.0);
            }
        }
        if (h < 0.0) {
            h += 360.0;
        }
        double s = max == 0.0 ? 0.0 : d / max;
        return new double[]{h, s, max};
    }

    /** Parses "#RRGGBB" / "RRGGBB" (keeps fallbackAlpha) or "#AARRGGBB" / "AARRGGBB". Null if invalid. */
    private static Integer parseHex(String s, int fallbackAlpha) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.startsWith("#")) {
            t = t.substring(1);
        }
        try {
            if (t.length() == 6) {
                int rgb = (int)(Long.parseLong(t, 16) & 0xFFFFFF);
                return (fallbackAlpha & 0xFF) << 24 | rgb;
            }
            if (t.length() == 8) {
                return (int)(Long.parseLong(t, 16) & 0xFFFFFFFFL);
            }
        } catch (NumberFormatException ignored) {
            // not a valid hex number
        }
        return null;
    }

    public void onClose() {
        this.configManager.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    public boolean isPauseScreen() {
        return false;
    }
}
