package io.github.keoz5.zombiezcompanion.modules.autotext;

import io.github.keoz5.zombiezcompanion.config.AutoTextConfig;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Detail editor for one AutoText preset: name, item icon, bar visibility, auto-send, and slot colors. */
public final class PresetEditScreen
extends Screen {
    private static final int[] TEXT_PALETTE = {0xFFFFFFFF, 0xFFF44336, 0xFF4CAF50, 0xFF2196F3, 0xFFFFEB3B, 0xFF00E5FF, 0xFFE040FB, 0xFFFF9800};
    private static final int[] BG_PALETTE = {0x80000000, 0xC0000000, 0x802196F3, 0x804CAF50, 0x80F44336, 0x80FF9800, 0x80E040FB, 0x00000000};
    private final Screen parent;
    private final ConfigManager configManager;
    private final AutoTextConfig.Preset preset;
    private EditBox nameField;
    private EditBox itemField;
    private int layoutX, layoutY, layoutW;

    public PresetEditScreen(Screen parent, ConfigManager configManager, AutoTextConfig.Preset preset) {
        super((Component)Component.translatable((String)"zombiezcompanion.autotext.edit.title"));
        this.parent = parent;
        this.configManager = configManager;
        this.preset = preset;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int w = Math.min(300, this.width - 40);
        int x = cx - w / 2;
        int y = this.height / 2 - 82;
        this.layoutX = x;
        this.layoutY = y;
        this.layoutW = w;

        this.nameField = new EditBox(this.font, x, y + 10, w, 20, (Component)Component.literal((String)""));
        this.nameField.setMaxLength(32);
        this.nameField.setValue(this.preset.name == null ? "" : this.preset.name);
        this.nameField.setHint((Component)Component.translatable((String)"zombiezcompanion.autotext.edit.name"));
        this.nameField.setResponder(v -> this.preset.name = v);
        this.addRenderableWidget(this.nameField);

        this.itemField = new EditBox(this.font, x, y + 44, w - 26, 20, (Component)Component.literal((String)""));
        this.itemField.setMaxLength(64);
        this.itemField.setValue(this.preset.itemId == null ? "" : this.preset.itemId);
        this.itemField.setHint((Component)Component.literal((String)"minecraft:ender_pearl"));
        this.itemField.setResponder(v -> this.preset.itemId = v);
        this.addRenderableWidget(this.itemField);

        // Row of two toggles: show-in-bar and auto-send.
        this.addRenderableWidget(new StyledButton(x, y + 74, w / 2 - 3, 20, this.showInBarLabel(), b -> {
            this.preset.showInBar = !this.preset.showInBar;
            b.setMessage(this.showInBarLabel());
        }, -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(x + w / 2 + 3, y + 74, w / 2 - 3, 20, this.autoSendLabel(), b -> {
            this.preset.autoSend = !this.preset.autoSend;
            b.setMessage(this.autoSendLabel());
        }, -266723542, -265932737, -854792));

        this.addRenderableWidget(new StyledButton(x, y + 98, w / 2 - 3, 20, (Component)Component.translatable((String)"zombiezcompanion.autotext.edit.text_color"), b -> {
            this.preset.color = PresetEditScreen.nextColor(TEXT_PALETTE, this.preset.color);
        }, -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(x + w / 2 + 3, y + 98, w / 2 - 3, 20, (Component)Component.translatable((String)"zombiezcompanion.autotext.edit.bg_color"), b -> {
            this.preset.backgroundColor = PresetEditScreen.nextColor(BG_PALETTE, this.preset.backgroundColor);
        }, -266723542, -265932737, -854792));

        this.addRenderableWidget(new StyledButton(cx - 75, y + 130, 150, 20, (Component)Component.translatable((String)"zombiezcompanion.button.back"), b -> this.onClose(), -11441921, -8874241, -854792));
    }

    /** Screen-space rect of the clickable icon preview next to the item field. */
    private int[] iconRect() {
        int px = this.layoutX + this.layoutW - 20;
        int py = this.layoutY + 44;
        return new int[]{px - 1, py - 1, px + 19, py + 19};
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int[] r = this.iconRect();
            if (event.x() >= (double)r[0] && event.x() < (double)r[2] && event.y() >= (double)r[1] && event.y() < (double)r[3]) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen((Screen)new ItemPickerScreen((Screen)this, this.preset));
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private Component showInBarLabel() {
        return Component.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{Component.translatable((String)"zombiezcompanion.autotext.edit.show_in_bar"), Component.translatable((String)(this.preset.showInBar ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private Component autoSendLabel() {
        return Component.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{Component.translatable((String)"zombiezcompanion.autotext.edit.auto_send"), Component.translatable((String)(this.preset.autoSend ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private static int nextColor(int[] palette, int current) {
        for (int i = 0; i < palette.length; ++i) {
            if (palette[i] == current) {
                return palette[(i + 1) % palette.length];
            }
        }
        return palette[0];
    }

    @Override
    public void onClose() {
        this.configManager.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, -872415232);
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        int x = this.layoutX;
        int y = this.layoutY;
        int w = this.layoutW;
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.autotext.edit.title"), x, y - 14, -854792, false);
        // Clickable live icon preview next to the item field (opens the item picker).
        int px = x + w - 20;
        int py = y + 44;
        int[] r = this.iconRect();
        boolean hover = mouseX >= r[0] && mouseX < r[2] && mouseY >= r[1] && mouseY < r[3];
        ctx.fill(px - 1, py - 1, px + 19, py + 19, this.preset.backgroundColor);
        ctx.outline(px - 1, py - 1, 20, 20, hover ? -1 : -8874241);
        ctx.item(AutoTextModule.iconStack(this.preset.itemId), px + 1, py + 1);
        // Color swatch next to the text-color button.
        ctx.fill(x + w / 2 - 3 - 14, y + 98 + 4, x + w / 2 - 3 - 2, y + 98 + 16, this.preset.color);
    }
}
