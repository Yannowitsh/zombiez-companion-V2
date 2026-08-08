package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;

public final class KeybindsScreen
extends Screen {
    private final Screen parent;
    private final List<KeyBinding> bindings;
    private KeyBinding listening;
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

    public KeybindsScreen(Screen parent) {
        super((Text)Text.translatable((String)"zombiezcompanion.keybinds.title"));
        this.parent = parent;
        this.bindings = Keybinds.all();
    }

    protected void init() {
        this.rows.clear();
        int panelW = Math.min(440, this.width - 40);
        int rowsH = Math.max(1, this.bindings.size()) * 28 + 24;
        int panelH = Math.min(this.height - 40, 64 + rowsH + 48);
        this.panelX1 = (this.width - panelW) / 2;
        this.panelY1 = (this.height - panelH) / 2;
        this.panelX2 = this.panelX1 + panelW;
        this.panelY2 = this.panelY1 + panelH;
        this.titleY1 = this.panelY1;
        this.titleY2 = this.titleY1 + 32;
        this.footerY2 = this.panelY2;
        this.footerY1 = this.footerY2 - 36;
        this.contentY1 = this.titleY2;
        this.contentY2 = this.footerY1;
        this.addDrawableChild(new StyledButton(this.panelX2 - 26, this.titleY1 + 7, 18, 18, (Text)Text.literal((String)"X"), btn -> this.close(), -266723542, -265932737, -854792));
        int rowX = this.panelX1 + 16;
        int rowW = this.panelX2 - this.panelX1 - 32;
        int y = this.contentY1 + 12;
        for (KeyBinding kb : this.bindings) {
            int btnW = 130;
            int resetW = 56;
            int gap = 8;
            int keyX = rowX + rowW - btnW - resetW - gap;
            StyledButton keyBtn = new StyledButton(keyX, y, btnW, 20, this.keyLabel(kb, false), btn -> {
                this.listening = kb;
            }, -266723542, -265932737, -854792);
            StyledButton resetBtn = new StyledButton(keyX + btnW + gap, y, resetW, 20, (Text)Text.translatable((String)"zombiezcompanion.keybinds.reset"), btn -> {
                kb.setBoundKey(kb.getDefaultKey());
                this.listening = null;
                this.saveAndRefresh();
            }, -266723542, -265932737, -854792);
            this.addDrawableChild(keyBtn);
            this.addDrawableChild(resetBtn);
            this.rows.add(new Row(kb, rowX, y, keyBtn, resetBtn));
            y += 28;
        }
        this.addDrawableChild(new StyledButton(this.panelX1 + 14, this.footerY1 + 8, 100, 20, (Text)Text.translatable((String)"zombiezcompanion.keybinds.reset_all"), btn -> {
            for (KeyBinding kb : this.bindings) {
                kb.setBoundKey(kb.getDefaultKey());
            }
            this.listening = null;
            this.saveAndRefresh();
        }, -266723542, -265932737, -854792));
        this.addDrawableChild(new StyledButton(this.panelX2 - 14 - 100, this.footerY1 + 8, 100, 20, (Text)Text.translatable((String)"zombiezcompanion.button.close"), btn -> this.close(), -266723542, -265932737, -854792));
    }

    private Text keyLabel(KeyBinding kb, boolean listeningThis) {
        if (listeningThis) {
            return Text.translatable((String)"zombiezcompanion.keybinds.listening");
        }
        if (kb.isUnbound()) {
            return Text.translatable((String)"zombiezcompanion.keybinds.unbound");
        }
        return kb.getBoundKeyLocalizedText();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.listening != null) {
            if (keyCode == 256) {
                this.listening = null;
                this.saveAndRefresh();
                return true;
            }
            InputUtil.Key key = keyCode == -1 ? InputUtil.UNKNOWN_KEY : InputUtil.fromKeyCode((int)keyCode, (int)scanCode);
            this.listening.setBoundKey(key);
            this.listening = null;
            this.saveAndRefresh();
            return true;
        }
        if (keyCode == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.listening != null) {
            this.listening.setBoundKey(InputUtil.Type.MOUSE.createFromCode(button));
            this.listening = null;
            this.saveAndRefresh();
            return true;
        }
        if (button == 1) {
            for (Row r : this.rows) {
                if (!r.keyBtn.isMouseOver(mouseX, mouseY)) continue;
                r.binding.setBoundKey(InputUtil.UNKNOWN_KEY);
                this.saveAndRefresh();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void saveAndRefresh() {
        KeyBinding.updateKeysByCode();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            client.options.write();
        }
        for (Row r : this.rows) {
            r.keyBtn.setMessage(this.keyLabel(r.binding, false));
        }
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, -872415232);
        ctx.fill(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.fill(this.panelX1, this.titleY1, this.panelX2, this.titleY2, -183232737);
        ctx.fill(this.panelX1, this.contentY1, this.panelX2, this.contentY2, -183825134);
        ctx.fill(this.panelX1, this.footerY1, this.panelX2, this.footerY2, -183232737);
        ctx.fill(this.panelX1, this.titleY1, this.panelX2, this.titleY1 + 2, -8874241);
        ctx.fill(this.panelX1, this.titleY2 - 1, this.panelX2, this.titleY2, -14736594);
        ctx.fill(this.panelX1, this.footerY1, this.panelX2, this.footerY1 + 1, -14736594);
        ctx.drawBorder(this.panelX1, this.panelY1, this.panelX2 - this.panelX1, this.panelY2 - this.panelY1, -13880766);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.keybinds.title"), this.panelX1 + 14, this.titleY1 + 12, -854792, true);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.keybinds.hint"), this.panelX1 + 14, this.footerY2 - 12, -8353376, false);
        for (Row r : this.rows) {
            ctx.drawText(this.textRenderer, (Text)Text.translatable((String)r.binding.getTranslationKey()), r.x, r.y + 6, -854792, false);
            if (this.listening == r.binding) {
                r.keyBtn.setMessage(this.keyLabel(r.binding, true));
            }
            if (!this.isDuplicate(r.binding)) continue;
            ctx.drawBorder(r.keyBtn.getX() - 1, r.keyBtn.getY() - 1, r.keyBtn.getWidth() + 2, r.keyBtn.getHeight() + 2, -1096636);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private boolean isDuplicate(KeyBinding target) {
        if (target.isUnbound()) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) {
            return false;
        }
        for (KeyBinding other : mc.options.allKeys != null ? mc.options.allKeys : new KeyBinding[]{}) {
            if (other == target || other.isUnbound() || !other.getBoundKeyTranslationKey().equals(target.getBoundKeyTranslationKey())) continue;
            return true;
        }
        return false;
    }

    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    public boolean shouldPause() {
        return false;
    }

    private record Row(KeyBinding binding, int x, int y, StyledButton keyBtn, StyledButton resetBtn) {
    }
}

