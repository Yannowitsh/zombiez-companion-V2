/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 */
package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.hud.HudAnchor;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_5348;

public final class HudEditorScreen
extends class_437 {
    private static final int SNAP = 6;
    private static final int HANDLE = 9;
    private static final int MINIMAP_MIN = 80;
    private static final int MINIMAP_MAX = 260;
    private final class_437 parent;
    private final ConfigManager configManager;
    private final List<Box> boxes = new ArrayList<Box>();
    private Box dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private Box resizing;

    public HudEditorScreen(class_437 parent, ConfigManager configManager) {
        super((class_2561)class_2561.method_43471((String)"zombiezcompanion.hud.editor.title"));
        this.parent = parent;
        this.configManager = configManager;
    }

    protected void method_25426() {
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
            int x = live ? e.x : HudAnchor.resolveX(hud, e.id, this.field_22789, w, e.defaultFx);
            int n = y = live ? e.y : HudAnchor.resolveY(hud, e.id, this.field_22790, h, e.defaultFy);
            if (HudAnchor.hasCustom(hud, e.id)) {
                x = HudAnchor.resolveX(hud, e.id, this.field_22789, w, e.defaultFx);
                y = HudAnchor.resolveY(hud, e.id, this.field_22790, h, e.defaultFy);
            }
            this.boxes.add(new Box(e, HudEditorScreen.clamp(x, 0, this.field_22789 - w), HudEditorScreen.clamp(y, 0, this.field_22790 - h), w, h, baseW, baseH, scale));
        }
        this.method_37063((class_364)new StyledButton(this.field_22789 / 2 - 160, this.field_22790 - 34, 150, 22, (class_2561)class_2561.method_43471((String)"zombiezcompanion.hud.editor.reset_all"), b -> {
            HudAnchor.resetAll(this.configManager.get().hud);
            this.configManager.save();
            this.method_41843();
        }, -12965328, -11716288, -854792));
        this.method_37063((class_364)new StyledButton(this.field_22789 / 2 + 10, this.field_22790 - 34, 150, 22, (class_2561)class_2561.method_43471((String)"zombiezcompanion.button.close"), b -> this.method_25419(), -11441921, -8874241, -854792));
    }

    public boolean method_25402(double mouseX, double mouseY, int button) {
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
        return super.method_25402(mouseX, mouseY, button);
    }

    public boolean method_25403(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.resizing != null) {
            if (this.resizing.element.id.equals("mini_map")) {
                int newSize = (int)Math.round(Math.max(mouseX - (double)this.resizing.x, mouseY - (double)this.resizing.y));
                this.resizing.w = newSize = Math.max(80, Math.min(260, newSize));
                this.resizing.h = newSize;
            } else {
                double rw = (mouseX - (double)this.resizing.x) / this.resizing.baseW;
                double rh = (mouseY - (double)this.resizing.y) / this.resizing.baseH;
                double s = Math.max(rw, rh);
                this.resizing.scale = s = Math.max(0.5, Math.min(3.0, s));
                this.resizing.w = Math.max(8, (int)Math.round(this.resizing.baseW * s));
                this.resizing.h = Math.max(8, (int)Math.round(this.resizing.baseH * s));
            }
            this.resizing.x = HudEditorScreen.clamp(this.resizing.x, 0, Math.max(0, this.field_22789 - this.resizing.w));
            this.resizing.y = HudEditorScreen.clamp(this.resizing.y, 0, Math.max(0, this.field_22790 - this.resizing.h));
            return true;
        }
        if (this.dragging != null) {
            int nx = (int)Math.round(mouseX - (double)this.dragOffsetX);
            int ny = (int)Math.round(mouseY - (double)this.dragOffsetY);
            int[] snapped = this.applySnap(this.dragging, nx, ny);
            this.dragging.x = HudEditorScreen.clamp(snapped[0], 0, this.field_22789 - this.dragging.w);
            this.dragging.y = HudEditorScreen.clamp(snapped[1], 0, this.field_22790 - this.dragging.h);
            return true;
        }
        return super.method_25403(mouseX, mouseY, button, dx, dy);
    }

    public boolean method_25406(double mouseX, double mouseY, int button) {
        if (this.resizing != null) {
            if (this.resizing.element.id.equals("mini_map")) {
                this.configManager.get().map.miniMapSize = this.resizing.w;
            } else {
                HudAnchor.setScale(this.configManager.get().hud, this.resizing.element.id, this.resizing.scale);
            }
            HudAnchor.setPosition(this.configManager.get().hud, this.resizing.element.id, this.resizing.x, this.resizing.y, this.resizing.w, this.resizing.h, this.field_22789, this.field_22790);
            this.configManager.save();
            this.resizing = null;
            return true;
        }
        if (this.dragging != null) {
            HudAnchor.setPosition(this.configManager.get().hud, this.dragging.element.id, this.dragging.x, this.dragging.y, this.dragging.w, this.dragging.h, this.field_22789, this.field_22790);
            this.configManager.save();
            this.dragging = null;
            return true;
        }
        return super.method_25406(mouseX, mouseY, button);
    }

    private boolean inHandle(Box box, double mouseX, double mouseY) {
        int hx = box.x + box.w - 9;
        int hy = box.y + box.h - 9;
        return mouseX >= (double)hx && mouseX <= (double)(box.x + box.w) && mouseY >= (double)hy && mouseY <= (double)(box.y + box.h);
    }

    private int[] applySnap(Box box, int nx, int ny) {
        int cx = this.field_22789 / 2 - box.w / 2;
        int cy = this.field_22790 / 2 - box.h / 2;
        int rightX = this.field_22789 - box.w;
        int bottomY = this.field_22790 - box.h;
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

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, -871756782);
        ctx.method_25294(this.field_22789 / 2, 0, this.field_22789 / 2 + 1, this.field_22790, 0x22FFFFFF);
        ctx.method_25294(0, this.field_22790 / 2, this.field_22789, this.field_22790 / 2 + 1, 0x22FFFFFF);
        for (Box box : this.boxes) {
            boolean active;
            boolean hovered = mouseX >= box.x && mouseX <= box.x + box.w && mouseY >= box.y && mouseY <= box.y + box.h;
            boolean bl = active = box == this.dragging || box == this.resizing;
            int fill = active ? -2006023425 : (hovered ? 1716611327 : 1142233121);
            ctx.method_25294(box.x, box.y, box.x + box.w, box.y + box.h, fill);
            ctx.method_49601(box.x, box.y, box.w, box.h, active || hovered ? -8874241 : 1719770367);
            class_5250 label = class_2561.method_43471((String)box.element.labelKey);
            int lw = this.field_22793.method_27525((class_5348)label);
            int lx = box.x + Math.max(2, (box.w - lw) / 2);
            int ly = box.y + Math.max(2, (box.h - 8) / 2);
            ctx.method_27535(this.field_22793, (class_2561)label, lx, ly, -854792);
            if (!box.element.scalable) continue;
            boolean hoverHandle = this.inHandle(box, mouseX, mouseY) || box == this.resizing;
            int hx = box.x + box.w - 9;
            int hy = box.y + box.h - 9;
            ctx.method_25294(hx, hy, box.x + box.w, box.y + box.h, hoverHandle ? -9534721 : -863333438);
            ctx.method_49601(hx, hy, 9, 9, -8874241);
            if (!active && !hovered) continue;
            String tag = box.element.id.equals("mini_map") ? box.w + " px" : Math.round(box.scale * 100.0) + "%";
            ctx.method_25303(this.field_22793, tag, box.x + 2, box.y - 10, -8874241);
        }
        ctx.method_27534(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.hud.editor.title"), this.field_22789 / 2, 14, -854792);
        ctx.method_27534(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.hud.editor.hint"), this.field_22789 / 2, 28, -8353376);
        super.method_25394(ctx, mouseX, mouseY, delta);
    }

    public void method_25420(class_332 ctx, int mouseX, int mouseY, float delta) {
    }

    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.method_25419();
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    public void method_25419() {
        this.configManager.save();
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    public boolean method_25421() {
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

