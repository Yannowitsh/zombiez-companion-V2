package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.hud.HudAnchor;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;

public final class HudEditorScreen
extends Screen {
    private static final int SNAP = 6;
    private static final int HANDLE = 9;
    private static final int MINIMAP_MIN = 80;
    private static final int MINIMAP_MAX = 260;
    private final Screen parent;
    private final ConfigManager configManager;
    private final List<Box> boxes = new ArrayList<Box>();
    private Box dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private Box resizing;

    public HudEditorScreen(Screen parent, ConfigManager configManager) {
        super((Component)Component.translatable((String)"zombiezcompanion.hud.editor.title"));
        this.parent = parent;
        this.configManager = configManager;
    }

    protected void init() {
        this.boxes.clear();
        HudConfig hud = this.configManager.get().hud;
        for (HudElements.Element e : HudElements.all()) {
            int y;
            boolean live = e.reportedAt > 0L;
            double scale = HudAnchor.scale(hud, e.id);
            int w = Math.max(24, live ? e.w : (int)Math.round((double)e.defaultW * scale));
            int h = Math.max(14, live ? e.h : (int)Math.round((double)e.defaultH * scale));
            double baseW = live ? (double)w / scale : (double)e.defaultW;
            double baseH = live ? (double)h / scale : (double)e.defaultH;
            int x = live ? e.x : HudAnchor.resolveX(hud, e.id, this.width, w, e.defaultFx);
            int n = y = live ? e.y : HudAnchor.resolveY(hud, e.id, this.height, h, e.defaultFy);
            if (HudAnchor.hasCustom(hud, e.id)) {
                x = HudAnchor.resolveX(hud, e.id, this.width, w, e.defaultFx);
                y = HudAnchor.resolveY(hud, e.id, this.height, h, e.defaultFy);
            }
            this.boxes.add(new Box(e, HudEditorScreen.clamp(x, 0, this.width - w), HudEditorScreen.clamp(y, 0, this.height - h), w, h, baseW, baseH, scale));
        }
        this.addRenderableWidget(new StyledButton(this.width / 2 - 160, this.height - 34, 150, 22, (Component)Component.translatable((String)"zombiezcompanion.hud.editor.reset_all"), b -> {
            HudAnchor.resetAll(this.configManager.get().hud);
            this.configManager.save();
            this.rebuildWidgets();
        }, -12965328, -11716288, -854792));
        this.addRenderableWidget(new StyledButton(this.width / 2 + 10, this.height - 34, 150, 22, (Component)Component.translatable((String)"zombiezcompanion.button.close"), b -> this.onClose(), -11441921, -8874241, -854792));
    }

    //? if >= 26.1 {
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y(); int button = event.button();
    //?} else {
    /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
    *///?}
        if (button == 0) {
            Box box;
            int i;
            for (i = this.boxes.size() - 1; i >= 0; --i) {
                box = this.boxes.get(i);
                if (!box.element.scalable || !this.inHandle(box, mouseX, mouseY)) continue;
                this.resizing = box;
                return true;
            }
            for (i = this.boxes.size() - 1; i >= 0; --i) {
                box = this.boxes.get(i);
                if (!(mouseX >= (double)box.x) || !(mouseX <= (double)(box.x + box.w)) || !(mouseY >= (double)box.y) || !(mouseY <= (double)(box.y + box.h))) continue;
                this.dragging = box;
                this.dragOffsetX = (int)Math.round(mouseX - (double)box.x);
                this.dragOffsetY = (int)Math.round(mouseY - (double)box.y);
                return true;
            }
        }
        //? if >= 26.1 {
        return super.mouseClicked(event, doubleClick);
        //?} else {
        /*return super.mouseClicked(mouseX, mouseY, button);
        *///?}
    }

    //? if >= 26.1 {
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x(), mouseY = event.y(); int button = event.button();
    //?} else {
    /*public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
    *///?}
        if (this.resizing != null) {
            double rw = (mouseX - (double)this.resizing.x) / this.resizing.baseW;
            double rh = (mouseY - (double)this.resizing.y) / this.resizing.baseH;
            double s = Math.max(rw, rh);
            this.resizing.scale = s = Math.max(0.5, Math.min(3.0, s));
            this.resizing.w = Math.max(8, (int)Math.round(this.resizing.baseW * s));
            this.resizing.h = Math.max(8, (int)Math.round(this.resizing.baseH * s));
            this.resizing.x = HudEditorScreen.clamp(this.resizing.x, 0, Math.max(0, this.width - this.resizing.w));
            this.resizing.y = HudEditorScreen.clamp(this.resizing.y, 0, Math.max(0, this.height - this.resizing.h));
            return true;
        }
        if (this.dragging != null) {
            int nx = (int)Math.round(mouseX - (double)this.dragOffsetX);
            int ny = (int)Math.round(mouseY - (double)this.dragOffsetY);
            int[] snapped = this.applySnap(this.dragging, nx, ny);
            this.dragging.x = HudEditorScreen.clamp(snapped[0], 0, this.width - this.dragging.w);
            this.dragging.y = HudEditorScreen.clamp(snapped[1], 0, this.height - this.dragging.h);
            return true;
        }
        //? if >= 26.1 {
        return super.mouseDragged(event, dx, dy);
        //?} else {
        /*return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        *///?}
    }

    //? if >= 26.1 {
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x(), mouseY = event.y(); int button = event.button();
    //?} else {
    /*public boolean mouseReleased(double mouseX, double mouseY, int button) {
    *///?}
        if (this.resizing != null) {
            HudAnchor.setScale(this.configManager.get().hud, this.resizing.element.id, this.resizing.scale);
            HudAnchor.setPosition(this.configManager.get().hud, this.resizing.element.id, this.resizing.x, this.resizing.y, this.resizing.w, this.resizing.h, this.width, this.height);
            this.configManager.save();
            this.resizing = null;
            return true;
        }
        if (this.dragging != null) {
            HudAnchor.setPosition(this.configManager.get().hud, this.dragging.element.id, this.dragging.x, this.dragging.y, this.dragging.w, this.dragging.h, this.width, this.height);
            this.configManager.save();
            this.dragging = null;
            return true;
        }
        //? if >= 26.1 {
        return super.mouseReleased(event);
        //?} else {
        /*return super.mouseReleased(mouseX, mouseY, button);
        *///?}
    }

    private boolean inHandle(Box box, double mouseX, double mouseY) {
        int hx = box.x + box.w - 9;
        int hy = box.y + box.h - 9;
        return mouseX >= (double)hx && mouseX <= (double)(box.x + box.w) && mouseY >= (double)hy && mouseY <= (double)(box.y + box.h);
    }

    private int[] applySnap(Box box, int nx, int ny) {
        int cx = this.width / 2 - box.w / 2;
        int cy = this.height / 2 - box.h / 2;
        int rightX = this.width - box.w;
        int bottomY = this.height - box.h;
        if (Math.abs(nx) <= 6) {
            nx = 0;
        }
        if (Math.abs(nx - cx) <= 6) {
            nx = cx;
        }
        if (Math.abs(nx - rightX) <= 6) {
            nx = rightX;
        }
        if (Math.abs(ny) <= 6) {
            ny = 0;
        }
        if (Math.abs(ny - cy) <= 6) {
            ny = cy;
        }
        if (Math.abs(ny - bottomY) <= 6) {
            ny = bottomY;
        }
        return new int[]{nx, ny};
    }

    //? if >= 26.1 {
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
        ctx.fill(0, 0, this.width, this.height, -871756782);
        ctx.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x22FFFFFF);
        ctx.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x22FFFFFF);
        for (Box box : this.boxes) {
            boolean active;
            boolean hovered = mouseX >= box.x && mouseX <= box.x + box.w && mouseY >= box.y && mouseY <= box.y + box.h;
            boolean bl = active = box == this.dragging || box == this.resizing;
            int fill = active ? -2006023425 : (hovered ? 1716611327 : 1142233121);
            ctx.fill(box.x, box.y, box.x + box.w, box.y + box.h, fill);
            ctx.outline(box.x, box.y, box.w, box.h, active || hovered ? -8874241 : 1719770367);
            MutableComponent label = Component.translatable((String)box.element.labelKey);
            int lw = this.font.width((FormattedText)label);
            int lx = box.x + Math.max(2, (box.w - lw) / 2);
            int ly = box.y + Math.max(2, (box.h - 8) / 2);
            ctx.text(this.font, (Component)label, lx, ly, -854792);
            if (!box.element.scalable) continue;
            boolean hoverHandle = this.inHandle(box, mouseX, mouseY) || box == this.resizing;
            int hx = box.x + box.w - 9;
            int hy = box.y + box.h - 9;
            ctx.fill(hx, hy, box.x + box.w, box.y + box.h, hoverHandle ? -9534721 : -863333438);
            ctx.outline(hx, hy, 9, 9, -8874241);
            if (!active && !hovered) continue;
            String tag = Math.round(box.scale * 100.0) + "%";
            ctx.text(this.font, tag, box.x + 2, box.y - 10, -8874241);
        }
        ctx.centeredText(this.font, (Component)Component.translatable((String)"zombiezcompanion.hud.editor.title"), this.width / 2, 14, -854792);
        ctx.centeredText(this.font, (Component)Component.translatable((String)"zombiezcompanion.hud.editor.hint"), this.width / 2, 28, -8353376);
        //? if >= 26.1 {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        //?} else {
        /*super.render(ctx, mouseX, mouseY, delta);
        *///?}
    }

    //? if >= 26.1 {
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void renderBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
    }

    //? if >= 26.1 {
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    //?} else {
    /*public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    *///?}
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        //? if >= 26.1 {
        return super.keyPressed(event);
        //?} else {
        /*return super.keyPressed(keyCode, scanCode, modifiers);
        *///?}
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

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, Math.max(min, v)));
    }

    private static final class Box {
        final HudElements.Element element;
        int x;
        int y;
        int w;
        int h;
        final double baseW;
        final double baseH;
        double scale;

        Box(HudElements.Element e, int x, int y, int w, int h, double baseW, double baseH, double scale) {
            this.element = e;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.baseW = baseW;
            this.baseH = baseH;
            this.scale = scale;
        }
    }
}

