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
import io.github.keoz5.zombiezcompanion.modules.dropalert.ConsumablesScreen;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropAlertModule;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropRarity;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;

public final class DropAlertOptionsScreen
extends ModuleOptionsScreen {
    private final DropAlertModule moduleRef;

    public DropAlertOptionsScreen(class_437 parent, DropAlertModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 42;
        int availableW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        int gap = 8;
        int colW = Math.max(60, (availableW - gap * 2) / 3);
        int rowH = 24;
        DropRarity[] rarities = DropRarity.values();
        int cols = 3;
        for (int i = 0; i < rarities.length; ++i) {
            DropRarity rarity = rarities[i];
            int col = i % cols;
            int row = i / cols;
            this.addToggle(x + col * (colW + gap), y + row * rowH, colW, rarity);
        }
        int rarityRows = (rarities.length + cols - 1) / cols;
        int filterY = y + rarityRows * rowH + 10;
        int half = (availableW - gap) / 2;
        this.addItemsToggle(x, filterY, availableW);
        this.addFoodToggle(x, filterY + 24, half);
        this.addGadgetsToggle(x + half + gap, filterY + 24, half);
        this.addConsumablesButton(x, filterY + 48, availableW);
        this.addMarkerStyleButton(x, filterY + 72, availableW);
        this.addCrossLink(x, filterY + 102, availableW, "stats", (class_2561)class_2561.method_43471((String)"zombiezcompanion.crosslink.stats"));
    }

    private void addConsumablesButton(int x, int y, int w) {
        this.method_37063((class_364)new StyledButton(x, y, w, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.drop_alert.consumables.open"), button -> {
            if (this.field_22787 != null) {
                this.field_22787.method_1507((class_437)new ConsumablesScreen(this, this.configManager));
            }
        }, -266723542, -265932737, -854792));
    }

    private void addToggle(int x, int y, int w, DropRarity rarity) {
        this.method_37063((class_364)new StyledButton(x, y, w, 20, this.toggleLabel(rarity), button -> {
            boolean next = !this.moduleRef.enabled(rarity);
            this.moduleRef.setEnabled(rarity, next);
            button.method_25355(this.toggleLabel(rarity));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, this.moduleRef.enabled(rarity) ? -14709924 : -12965328, this.moduleRef.enabled(rarity) ? -14179731 : -11716288, -854792));
    }

    private void addFoodToggle(int x, int y, int w) {
        this.method_37063((class_364)new StyledButton(x, y, w, 20, this.foodToggleLabel(), button -> {
            boolean next = !this.moduleRef.foodEnabled();
            this.moduleRef.setFoodEnabled(next);
            button.method_25355(this.foodToggleLabel());
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, this.moduleRef.foodEnabled() ? -14709924 : -12965328, this.moduleRef.foodEnabled() ? -14179731 : -11716288, -854792));
    }

    private void addItemsToggle(int x, int y, int w) {
        this.method_37063((class_364)new StyledButton(x, y, w, 20, this.itemsToggleLabel(), button -> {
            boolean next = !this.moduleRef.itemsEnabled();
            this.moduleRef.setItemsEnabled(next);
            button.method_25355(this.itemsToggleLabel());
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, this.moduleRef.itemsEnabled() ? -14709924 : -12965328, this.moduleRef.itemsEnabled() ? -14179731 : -11716288, -854792));
    }

    private void addGadgetsToggle(int x, int y, int w) {
        this.method_37063((class_364)new StyledButton(x, y, w, 20, this.gadgetsToggleLabel(), button -> {
            boolean next = !this.moduleRef.gadgetsEnabled();
            this.moduleRef.setGadgetsEnabled(next);
            button.method_25355(this.gadgetsToggleLabel());
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, this.moduleRef.gadgetsEnabled() ? -14709924 : -12965328, this.moduleRef.gadgetsEnabled() ? -14179731 : -11716288, -854792));
    }

    private void addMarkerStyleButton(int x, int y, int w) {
        this.method_37063((class_364)new StyledButton(x, y, w, 20, this.markerStyleLabel(), button -> {
            this.moduleRef.cycleMarkerStyle();
            button.method_25355(this.markerStyleLabel());
        }, -266723542, -265932737, -854792));
    }

    private class_2561 toggleLabel(DropRarity rarity) {
        return class_2561.method_43469((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{class_2561.method_43471((String)("zombiezcompanion.drop_alert.rarity." + rarity.key)), class_2561.method_43471((String)(this.moduleRef.enabled(rarity) ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private class_2561 foodToggleLabel() {
        return class_2561.method_43469((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{class_2561.method_43471((String)"zombiezcompanion.drop_alert.food"), class_2561.method_43471((String)(this.moduleRef.foodEnabled() ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private class_2561 itemsToggleLabel() {
        return class_2561.method_43469((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{class_2561.method_43471((String)"zombiezcompanion.drop_alert.items"), class_2561.method_43471((String)(this.moduleRef.itemsEnabled() ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private class_2561 gadgetsToggleLabel() {
        return class_2561.method_43469((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{class_2561.method_43471((String)"zombiezcompanion.drop_alert.gadgets"), class_2561.method_43471((String)(this.moduleRef.gadgetsEnabled() ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private class_2561 markerStyleLabel() {
        String key = this.moduleRef.markerStyle() == 0 ? "zombiezcompanion.drop_alert.style.beacon" : "zombiezcompanion.drop_alert.style.arrow";
        return class_2561.method_43469((String)"zombiezcompanion.drop_alert.style.label", (Object[])new Object[]{class_2561.method_43471((String)key)});
    }

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        super.method_25394(ctx, mouseX, mouseY, delta);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.drop_alert.options.header"), this.panelX1 + 36, this.contentY1 + 20, -854792);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43471((String)"zombiezcompanion.drop_alert.options.hint"), this.panelX1 + 36, this.contentY1 + 34, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(class_332 ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 14;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = Math.max(0, Math.min(252, this.contentY2 - y - 6));
        ctx.method_25294(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.method_25294(x, y, x + w, y + h, -267053025);
        ctx.method_25294(x, y, x + w, y + 2, -8874241);
        ctx.method_49601(x, y, w, h, -14736594);
    }
}

