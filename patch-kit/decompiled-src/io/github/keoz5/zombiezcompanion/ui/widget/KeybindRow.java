/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_304
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_3675
 *  net.minecraft.class_3675$class_306
 *  net.minecraft.class_3675$class_307
 */
package io.github.keoz5.zombiezcompanion.ui.widget;

import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3675;

public final class KeybindRow {
    private final class_304 binding;
    private final int x;
    private final int y;
    private final class_2561 label;
    private final StyledButton keyBtn;
    private final StyledButton resetBtn;
    private boolean listening;

    public KeybindRow(class_304 binding, int x, int y, int width, class_2561 label) {
        this.binding = binding;
        this.x = x;
        this.y = y;
        this.label = label;
        int btnH = 20;
        int resetW = 48;
        int gap = 6;
        int keyBtnW = Math.max(60, Math.min(150, (int)((double)width * 0.32)));
        int keyBtnX = x + width - keyBtnW - resetW - gap;
        this.keyBtn = new StyledButton(keyBtnX, y, keyBtnW, btnH, this.keyLabelText(false), btn -> this.setListening(true), -266723542, -265932737, -854792);
        this.resetBtn = new StyledButton(keyBtnX + keyBtnW + gap, y, resetW, btnH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.keybinds.reset"), btn -> {
            binding.method_1422(binding.method_1429());
            this.saveAndRefresh();
        }, -266723542, -265932737, -854792);
    }

    public StyledButton keyButton() {
        return this.keyBtn;
    }

    public StyledButton resetButton() {
        return this.resetBtn;
    }

    public class_304 binding() {
        return this.binding;
    }

    public boolean isListening() {
        return this.listening;
    }

    public void setListening(boolean v) {
        this.listening = v;
        this.keyBtn.method_25355(this.keyLabelText(v));
    }

    public boolean handleKey(int keyCode, int scanCode) {
        if (!this.listening) {
            return false;
        }
        if (keyCode == 256) {
            this.setListening(false);
            this.saveAndRefresh();
            return true;
        }
        class_3675.class_306 key = keyCode == -1 ? class_3675.field_16237 : class_3675.method_15985((int)keyCode, (int)scanCode);
        this.binding.method_1422(key);
        this.setListening(false);
        this.saveAndRefresh();
        return true;
    }

    public boolean handleMouseRebind(int mouseButton) {
        if (!this.listening) {
            return false;
        }
        this.binding.method_1422(class_3675.class_307.field_1672.method_1447(mouseButton));
        this.setListening(false);
        this.saveAndRefresh();
        return true;
    }

    public boolean handleRightClickUnbind(double mouseX, double mouseY, int mouseButton) {
        if (this.listening) {
            return false;
        }
        if (mouseButton != 1) {
            return false;
        }
        if (!this.keyBtn.method_25405(mouseX, mouseY)) {
            return false;
        }
        this.binding.method_1422(class_3675.field_16237);
        this.saveAndRefresh();
        return true;
    }

    public void renderLabel(class_332 ctx, class_327 tr) {
        ctx.method_51439(tr, this.label, this.x, this.y + 6, -854792, false);
    }

    private void saveAndRefresh() {
        class_304.method_1426();
        class_310 mc = class_310.method_1551();
        if (mc.field_1690 != null) {
            mc.field_1690.method_1640();
        }
        this.keyBtn.method_25355(this.keyLabelText(this.listening));
    }

    private class_2561 keyLabelText(boolean listeningState) {
        if (listeningState) {
            return class_2561.method_43471((String)"zombiezcompanion.keybinds.listening");
        }
        if (this.binding.method_1415()) {
            return class_2561.method_43471((String)"zombiezcompanion.keybinds.unbound");
        }
        return this.binding.method_16007();
    }
}

