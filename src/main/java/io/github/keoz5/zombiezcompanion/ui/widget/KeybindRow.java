package io.github.keoz5.zombiezcompanion.ui.widget;

import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

public final class KeybindRow {
    private final KeyBinding binding;
    private final int x;
    private final int y;
    private final Text label;
    private final StyledButton keyBtn;
    private final StyledButton resetBtn;
    private boolean listening;

    public KeybindRow(KeyBinding binding, int x, int y, int width, Text label) {
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
        this.resetBtn = new StyledButton(keyBtnX + keyBtnW + gap, y, resetW, btnH, (Text)Text.translatable((String)"zombiezcompanion.keybinds.reset"), btn -> {
            binding.setBoundKey(binding.getDefaultKey());
            this.saveAndRefresh();
        }, -266723542, -265932737, -854792);
    }

    public StyledButton keyButton() {
        return this.keyBtn;
    }

    public StyledButton resetButton() {
        return this.resetBtn;
    }

    public KeyBinding binding() {
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
        InputUtil.Key key = keyCode == -1 ? InputUtil.UNKNOWN_KEY : InputUtil.fromKeyCode((int)keyCode, (int)scanCode);
        this.binding.setBoundKey(key);
        this.setListening(false);
        this.saveAndRefresh();
        return true;
    }

    public boolean handleMouseRebind(int mouseButton) {
        if (!this.listening) {
            return false;
        }
        this.binding.setBoundKey(InputUtil.Type.MOUSE.createFromCode(mouseButton));
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
        this.binding.setBoundKey(InputUtil.UNKNOWN_KEY);
        this.saveAndRefresh();
        return true;
    }

    public void renderLabel(DrawContext ctx, TextRenderer tr) {
        ctx.drawText(tr, this.label, this.x, this.y + 6, -854792, false);
    }

    private void saveAndRefresh() {
        KeyBinding.updateKeysByCode();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            mc.options.write();
        }
        this.keyBtn.setMessage(this.keyLabelText(this.listening));
    }

    private Text keyLabelText(boolean listeningState) {
        if (listeningState) {
            return Text.translatable((String)"zombiezcompanion.keybinds.listening");
        }
        if (this.binding.isUnbound()) {
            return Text.translatable((String)"zombiezcompanion.keybinds.unbound");
        }
        return this.binding.getBoundKeyLocalizedText();
    }
}

