package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A single place to recolor highlighted elements (mutant frame, event pings…). Each row is a button
 * that cycles through the shared palette on click; right-click resets the element to its default.
 */
public final class ColorsScreen
extends Screen {
    private final Screen parent;
    private final ConfigManager configManager;
    private int panelX1;
    private int panelY1;
    private int panelX2;
    private int panelY2;
    private int titleY1;
    private int titleY2;
    private int contentY1;
    private int footerY1;
    private final List<Row> rows = new ArrayList<Row>();

    private record Row(Colors.Element el, StyledButton btn) {
    }

    public ColorsScreen(Screen parent, ConfigManager configManager) {
        super((Component)Component.translatable((String)"zombiezcompanion.colors.title"));
        this.parent = parent;
        this.configManager = configManager;
    }

    protected void init() {
        this.rows.clear();
        this.computePanel();
        this.addRenderableWidget(new StyledButton(this.panelX1 + 12, this.footerY1 + 7, 100, 20, (Component)Component.translatable((String)"zombiezcompanion.button.back"), b -> this.onClose(), -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(this.panelX2 - 12 - 22, this.titleY1 + 10, 22, 22, (Component)Component.literal((String)"X"), b -> this.onClose(), -266723542, -265932737, -854792));
        int x = this.panelX1 + 24;
        int w = this.panelX2 - this.panelX1 - 48;
        int y = this.contentY1 + 24;
        for (Colors.Element el : Colors.ELEMENTS) {
            StyledButton btn = new StyledButton(x, y, w, 20, this.rowLabel(el), b -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen((Screen)new ColorPickerScreen(this, this.configManager, el));
                }
            }, -266723542, -265932737, -854792);
            this.addRenderableWidget(btn);
            this.rows.add(new Row(el, btn));
            y += 24;
        }
    }

    private Component rowLabel(Colors.Element el) {
        int c = Colors.get(el.id(), el.def());
        return Component.literal(Component.translatable((String)el.langKey()).getString() + "   #" + String.format(Locale.ROOT, "%08X", c));
    }

    private void computePanel() {
        int margin = Math.max(6, Math.min(28, Math.min(this.width, this.height) / 16));
        int panelW = Math.min(520, this.width - 2 * margin);
        int panelH = Math.min(430, this.height - 2 * margin);
        this.panelX1 = (this.width - panelW) / 2;
        this.panelY1 = (this.height - panelH) / 2;
        this.panelX2 = this.panelX1 + panelW;
        this.panelY2 = this.panelY1 + panelH;
        this.titleY1 = this.panelY1;
        this.titleY2 = this.titleY1 + 42;
        this.footerY1 = this.panelY2 - 34;
        this.contentY1 = this.titleY2;
    }

    //? if >= 26.1 {
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        double mx = event.x();
        double my = event.y();
    //?} else {
    /*public boolean mouseClicked(double mx, double my, int button) {
    *///?}
        if (button == 1) {
            for (Row r : this.rows) {
                StyledButton b = r.btn();
                if (mx < (double)b.getX() || mx >= (double)(b.getX() + b.getWidth()) || my < (double)b.getY() || my >= (double)(b.getY() + b.getHeight())) continue;
                Colors.reset(r.el().id());
                b.setMessage(this.rowLabel(r.el()));
                return true;
            }
        }
        //? if >= 26.1 {
        return super.mouseClicked(event, doubleClick);
        //?} else {
        /*return super.mouseClicked(mx, my, button);
        *///?}
    }

    //? if >= 26.1 {
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
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
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.colors.title"), this.panelX1 + 18, this.titleY1 + 16, -854792, true);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.colors.hint"), this.panelX1 + 24, this.contentY1 + 10, -8353376, false);
        //? if >= 26.1 {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        //?} else {
        /*super.render(ctx, mouseX, mouseY, delta);
        *///?}
        for (Row r : this.rows) {
            StyledButton b = r.btn();
            int c = 0xFF000000 | Colors.get(r.el().id(), r.el().def());
            int sx = b.getX() + b.getWidth() - 22;
            int sy = b.getY() + 2;
            ctx.fill(sx - 1, sy - 1, sx + 17, sy + 17, -16777216);
            ctx.fill(sx, sy, sx + 16, sy + 16, c);
        }
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
