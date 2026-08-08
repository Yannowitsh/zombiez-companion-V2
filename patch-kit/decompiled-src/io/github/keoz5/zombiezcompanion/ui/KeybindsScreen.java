/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_304
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_3675
 *  net.minecraft.class_3675$class_306
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_3675;
import net.minecraft.class_437;

public final class KeybindsScreen
extends class_437 {
    private final class_437 parent;
    private final List<class_304> bindings;
    private class_304 listening;
    private int panelX1;
    private int panelY1;
    private int panelX2;
    private int panelY2;
    private int titleY1;
    private int titleY2;
    private int contentY1;
    private int contentY2;
    private int footerY1;
    private int footerY2;
    private final List<Row> rows = new ArrayList<Row>();

    public KeybindsScreen(class_437 parent) {
        super((class_2561)class_2561.method_43471((String)"zombiezcompanion.keybinds.title"));
        this.parent = parent;
        this.bindings = Keybinds.all();
    }

    protected void method_25426() {
        this.rows.clear();
        int panelW = Math.min(440, this.field_22789 - 40);
        int rowsH = Math.max(1, this.bindings.size()) * 28 + 24;
        int panelH = Math.min(this.field_22790 - 40, 64 + rowsH + 48);
        this.panelX1 = (this.field_22789 - panelW) / 2;
        this.panelY1 = (this.field_22790 - panelH) / 2;
        this.panelX2 = this.panelX1 + panelW;
        this.panelY2 = this.panelY1 + panelH;
        this.titleY1 = this.panelY1;
        this.titleY2 = this.titleY1 + 32;
        this.footerY2 = this.panelY2;
        this.footerY1 = this.footerY2 - 36;
        this.contentY1 = this.titleY2;
        this.contentY2 = this.footerY1;
        this.method_37063((class_364)new StyledButton(this.panelX2 - 26, this.titleY1 + 7, 18, 18, (class_2561)class_2561.method_43470((String)"X"), btn -> this.method_25419(), -266723542, -265932737, -854792));
        int rowX = this.panelX1 + 16;
        int rowW = this.panelX2 - this.panelX1 - 32;
        int y = this.contentY1 + 12;
        for (class_304 kb : this.bindings) {
            int btnW = 130;
            int resetW = 56;
            int gap = 8;
            int keyX = rowX + rowW - btnW - resetW - gap;
            StyledButton keyBtn = new StyledButton(keyX, y, btnW, 20, this.keyLabel(kb, false), btn -> {
                this.listening = kb;
            }, -266723542, -265932737, -854792);
            StyledButton resetBtn = new StyledButton(keyX + btnW + gap, y, resetW, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.keybinds.reset"), btn -> {
                kb.method_1422(kb.method_1429());
                this.listening = null;
                this.saveAndRefresh();
            }, -266723542, -265932737, -854792);
            this.method_37063((class_364)keyBtn);
            this.method_37063((class_364)resetBtn);
            this.rows.add(new Row(kb, rowX, y, keyBtn, resetBtn));
            y += 28;
        }
        this.method_37063((class_364)new StyledButton(this.panelX1 + 14, this.footerY1 + 8, 100, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.keybinds.reset_all"), btn -> {
            for (class_304 kb : this.bindings) {
                kb.method_1422(kb.method_1429());
            }
            this.listening = null;
            this.saveAndRefresh();
        }, -266723542, -265932737, -854792));
        this.method_37063((class_364)new StyledButton(this.panelX2 - 14 - 100, this.footerY1 + 8, 100, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.button.close"), btn -> this.method_25419(), -266723542, -265932737, -854792));
    }

    private class_2561 keyLabel(class_304 kb, boolean listeningThis) {
        if (listeningThis) {
            return class_2561.method_43471((String)"zombiezcompanion.keybinds.listening");
        }
        if (kb.method_1415()) {
            return class_2561.method_43471((String)"zombiezcompanion.keybinds.unbound");
        }
        return kb.method_16007();
    }

    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (this.listening != null) {
            if (keyCode == 256) {
                this.listening = null;
                this.saveAndRefresh();
                return true;
            }
            class_3675.class_306 key = keyCode == -1 ? class_3675.field_16237 : class_3675.method_15985((int)keyCode, (int)scanCode);
            this.listening.method_1422(key);
            this.listening = null;
            this.saveAndRefresh();
            return true;
        }
        if (keyCode == 256) {
            this.method_25419();
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (this.listening != null) {
            this.listening.method_1422(class_3675.class_307.field_1672.method_1447(button));
            this.listening = null;
            this.saveAndRefresh();
            return true;
        }
        if (button == 1) {
            for (Row r : this.rows) {
                if (!r.keyBtn.method_25405(mouseX, mouseY)) continue;
                r.binding.method_1422(class_3675.field_16237);
                this.saveAndRefresh();
                return true;
            }
        }
        return super.method_25402(mouseX, mouseY, button);
    }

    private void saveAndRefresh() {
        class_304.method_1426();
        class_310 client = class_310.method_1551();
        if (client.field_1690 != null) {
            client.field_1690.method_1640();
        }
        for (Row r : this.rows) {
            r.keyBtn.method_25355(this.keyLabel(r.binding, false));
        }
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, -872415232);
        ctx.method_25294(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.method_25294(this.panelX1, this.titleY1, this.panelX2, this.titleY2, -183232737);
        ctx.method_25294(this.panelX1, this.contentY1, this.panelX2, this.contentY2, -183825134);
        ctx.method_25294(this.panelX1, this.footerY1, this.panelX2, this.footerY2, -183232737);
        ctx.method_25294(this.panelX1, this.titleY1, this.panelX2, this.titleY1 + 2, -8874241);
        ctx.method_25294(this.panelX1, this.titleY2 - 1, this.panelX2, this.titleY2, -14736594);
        ctx.method_25294(this.panelX1, this.footerY1, this.panelX2, this.footerY1 + 1, -14736594);
        ctx.method_49601(this.panelX1, this.panelY1, this.panelX2 - this.panelX1, this.panelY2 - this.panelY1, -13880766);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.keybinds.title"), this.panelX1 + 14, this.titleY1 + 12, -854792, true);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.keybinds.hint"), this.panelX1 + 14, this.footerY2 - 12, -8353376, false);
        for (Row r : this.rows) {
            ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)r.binding.method_1431()), r.x, r.y + 6, -854792, false);
            if (this.listening == r.binding) {
                r.keyBtn.method_25355(this.keyLabel(r.binding, true));
            }
            if (!this.isDuplicate(r.binding)) continue;
            ctx.method_49601(r.keyBtn.method_46426() - 1, r.keyBtn.method_46427() - 1, r.keyBtn.method_25368() + 2, r.keyBtn.method_25364() + 2, -1096636);
        }
        super.method_25394(ctx, mouseX, mouseY, delta);
    }

    private boolean isDuplicate(class_304 target) {
        if (target.method_1415()) {
            return false;
        }
        class_310 mc = class_310.method_1551();
        if (mc.field_1690 == null) {
            return false;
        }
        for (class_304 other : mc.field_1690.field_1839 != null ? mc.field_1690.field_1839 : new class_304[]{}) {
            if (other == target || other.method_1415() || !other.method_1428().equals(target.method_1428())) continue;
            return true;
        }
        return false;
    }

    public void method_25419() {
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    public boolean method_25421() {
        return false;
    }

    private record Row(class_304 binding, int x, int y, StyledButton keyBtn, StyledButton resetBtn) {
    }
}

