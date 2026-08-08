/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 */
package io.github.keoz5.zombiezcompanion.modules.dropalert;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.DropAlertConfig;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropClassifier;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;

public final class ConsumablesScreen
extends class_437 {
    private static final int ROW_H = 22;
    private final class_437 parent;
    private final ConfigManager configManager;
    private String filter = "all";
    private int panelX1;
    private int panelY1;
    private int panelX2;
    private int panelY2;
    private int listTop;
    private int listBottom;
    private int scrollRows;

    private List<DropClassifier.Consumable> visibleItems() {
        ArrayList<DropClassifier.Consumable> out = new ArrayList<DropClassifier.Consumable>();
        for (DropClassifier.Consumable c : DropClassifier.consumables()) {
            if (!this.filter.equals("all") && (!this.filter.equals("food") || !c.food()) && (!this.filter.equals("gadget") || c.food())) continue;
            out.add(c);
        }
        return out;
    }

    public ConsumablesScreen(class_437 parent, ConfigManager configManager) {
        super((class_2561)class_2561.method_43471((String)"zombiezcompanion.drop_alert.consumables.title"));
        this.parent = parent;
        this.configManager = configManager;
    }

    private DropAlertConfig config() {
        return this.configManager.get().dropAlert;
    }

    protected void method_25426() {
        int margin = Math.max(4, Math.min(28, Math.min(this.field_22789, this.field_22790) / 16));
        int panelW = Math.min(960, this.field_22789 - 2 * margin);
        int panelH = Math.min(580, this.field_22790 - 2 * margin);
        this.panelX1 = (this.field_22789 - panelW) / 2;
        this.panelY1 = (this.field_22790 - panelH) / 2;
        this.panelX2 = this.panelX1 + panelW;
        this.panelY2 = this.panelY1 + panelH;
        this.listTop = this.panelY1 + 70;
        this.listBottom = this.panelY2 - 40;
        int gap = 8;
        int x0 = this.panelX1 + 12;
        int fy = this.panelY1 + 40;
        this.addFilterButton(x0, fy, "all", "zombiezcompanion.drop_alert.consumables.filter.all");
        this.addFilterButton(x0 + 100, fy, "food", "zombiezcompanion.drop_alert.consumables.filter.food");
        this.addFilterButton(x0 + 200, fy, "gadget", "zombiezcompanion.drop_alert.consumables.filter.gadget");
        List<DropClassifier.Consumable> items = this.visibleItems();
        int colW = (this.panelX2 - this.panelX1 - 24 - gap) / 2;
        int visibleRows = Math.max(1, (this.listBottom - this.listTop) / 22);
        int totalRows = (items.size() + 1) / 2;
        int maxOffset = Math.max(0, totalRows - visibleRows);
        if (this.scrollRows > maxOffset) {
            this.scrollRows = maxOffset;
        }
        if (this.scrollRows < 0) {
            this.scrollRows = 0;
        }
        for (int i = 0; i < items.size(); ++i) {
            int drawRow = i / 2 - this.scrollRows;
            if (drawRow < 0 || drawRow >= visibleRows) continue;
            DropClassifier.Consumable c = items.get(i);
            int bx = x0 + i % 2 * (colW + gap);
            int by = this.listTop + drawRow * 22;
            boolean shown = this.isShown(c);
            this.method_37063((class_364)new StyledButton(bx, by, colW, 18, this.label(c, shown), btn -> {
                this.toggle(c);
                boolean now = this.isShown(c);
                btn.method_25355(this.label(c, now));
                ((StyledButton)btn).setColors(now ? -14709924 : -12965328, now ? -14179731 : -11716288);
            }, shown ? -14709924 : -12965328, shown ? -14179731 : -11716288, -854792));
        }
        this.method_37063((class_364)new StyledButton(x0, this.panelY2 - 30, 120, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.drop_alert.consumables.show_all"), b -> {
            this.config().hiddenConsumables.clear();
            this.configManager.save();
            this.method_41843();
        }, -14709924, -14179731, -854792));
        this.method_37063((class_364)new StyledButton(this.panelX2 - 12 - 110, this.panelY2 - 30, 110, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.button.back"), b -> this.method_25419(), -266723542, -265932737, -854792));
    }

    private void addFilterButton(int x, int y, String value, String key) {
        boolean active = this.filter.equals(value);
        this.method_37063((class_364)new StyledButton(x, y, 92, 18, (class_2561)class_2561.method_43471((String)key), b -> {
            this.filter = value;
            this.scrollRows = 0;
            this.method_41843();
        }, active ? -11441921 : -266723542, active ? -8874241 : -265932737, -854792));
    }

    private boolean isShown(DropClassifier.Consumable c) {
        return !this.config().hiddenConsumables.contains(DropClassifier.normalizeName(c.name()));
    }

    private void toggle(DropClassifier.Consumable c) {
        String key = DropClassifier.normalizeName(c.name());
        if (!this.config().hiddenConsumables.remove(key)) {
            this.config().hiddenConsumables.add(key);
        }
        this.configManager.save();
    }

    private class_2561 label(DropClassifier.Consumable c, boolean shown) {
        return class_2561.method_43470((String)c.name());
    }

    public boolean method_25401(double mouseX, double mouseY, double h, double v) {
        this.scrollRows = Math.max(0, this.scrollRows + (v > 0.0 ? -1 : 1));
        this.method_41843();
        return true;
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, -872415232);
        ctx.method_25294(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.method_25294(this.panelX1, this.panelY1, this.panelX2, this.panelY1 + 2, -8874241);
        ctx.method_49601(this.panelX1, this.panelY1, this.panelX2 - this.panelX1, this.panelY2 - this.panelY1, -13880766);
        ctx.method_27534(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.drop_alert.consumables.title"), (this.panelX1 + this.panelX2) / 2, this.panelY1 + 16, -854792);
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
}

