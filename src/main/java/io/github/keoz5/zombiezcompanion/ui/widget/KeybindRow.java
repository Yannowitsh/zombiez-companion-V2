package io.github.keoz5.zombiezcompanion.ui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class KeybindRow {
    private final KeyMapping binding;
    private final int x;
    private final int y;
    private final Component label;
    private final StyledButton keyBtn;
    private final StyledButton resetBtn;
    private boolean listening;

    public KeybindRow(KeyMapping binding, int x, int y, int width, Component label) {
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
        this.resetBtn = new StyledButton(keyBtnX + keyBtnW + gap, y, resetW, btnH, (Component)Component.translatable((String)"zombiezcompanion.keybinds.reset"), btn -> {
            binding.setKey(binding.getDefaultKey());
            this.saveAndRefresh();
        }, -266723542, -265932737, -854792);
    }

    public StyledButton keyButton() {
        return this.keyBtn;
    }

    public StyledButton resetButton() {
        return this.resetBtn;
    }

    public KeyMapping binding() {
        return this.binding;
    }

    public boolean isListening() {
        return this.listening;
    }

    public void setListening(boolean v) {
        this.listening = v;
        this.keyBtn.setMessage(this.keyLabelText(v));
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
        InputConstants.Key key = keyCode == -1 ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate((int)keyCode);
        this.binding.setKey(key);
        this.setListening(false);
        this.saveAndRefresh();
        return true;
    }

    public boolean handleMouseRebind(int mouseButton) {
        if (!this.listening) {
            return false;
        }
        this.binding.setKey(InputConstants.Type.MOUSE.getOrCreate(mouseButton));
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
        if (!this.keyBtn.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.binding.setKey(InputConstants.UNKNOWN);
        this.saveAndRefresh();
        return true;
    }

    public void renderLabel(GuiGraphicsExtractor ctx, Font tr) {
        ctx.text(tr, this.label, this.x, this.y + 6, -854792, false);
    }

    private void saveAndRefresh() {
        KeyMapping.resetMapping();
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            mc.options.save();
        }
        this.keyBtn.setMessage(this.keyLabelText(this.listening));
    }

    private Component keyLabelText(boolean listeningState) {
        if (listeningState) {
            return Component.translatable((String)"zombiezcompanion.keybinds.listening");
        }
        if (this.binding.isUnbound()) {
            return Component.translatable((String)"zombiezcompanion.keybinds.unbound");
        }
        return this.binding.getTranslatedKeyMessage();
    }
}

