package io.github.keoz5.zombiezcompanion.modules.autotext;

import io.github.keoz5.zombiezcompanion.config.AutoTextConfig;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Scrollable editor for the unified preset list (up to {@link AutoTextConfig#MAX_PRESETS}). An empty slot
 * is always kept at the end and a new one is added as soon as it's filled. Per-preset text + keybind here;
 * icon/colors for the chat bar come from the detail editor (later phase).
 */
public final class AutoTextOptionsScreen
extends ModuleOptionsScreen {
    private static final int ROW_H = 24;
    private final AutoTextModule moduleRef;
    private final List<EditBox> textFields = new ArrayList<EditBox>();
    private final List<StyledButton> rowButtons = new ArrayList<StyledButton>();
    private final List<StyledButton> keyButtons = new ArrayList<StyledButton>();
    private int scrollOffset;
    private int waitingIndex = -1;

    public AutoTextOptionsScreen(Screen parent, AutoTextModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        // Chat-bar settings (persistent, above the scrolling list).
        int x = this.panelX1 + 36;
        int w = Math.max(200, this.panelX2 - this.panelX1 - 72);
        int third = (w - 16) / 3;
        int by = this.contentY1 + 52;
        this.addRenderableWidget(new StyledButton(x, by, third, 20, this.barToggleLabel("zombiezcompanion.autotext.bar.enabled", this.config().barEnabled), b -> {
            this.config().barEnabled = !this.config().barEnabled;
            b.setMessage(this.barToggleLabel("zombiezcompanion.autotext.bar.enabled", this.config().barEnabled));
        }, -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(x + third + 8, by, third, 20, this.barToggleLabel("zombiezcompanion.autotext.bar.only_chat", this.config().barOnlyWhenChatOpen), b -> {
            this.config().barOnlyWhenChatOpen = !this.config().barOnlyWhenChatOpen;
            b.setMessage(this.barToggleLabel("zombiezcompanion.autotext.bar.only_chat", this.config().barOnlyWhenChatOpen));
        }, -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(x + 2 * (third + 8), by, third, 20, this.orientationLabel(), b -> {
            this.config().barOrientation = "vertical".equals(this.config().barOrientation) ? "horizontal" : "vertical";
            b.setMessage(this.orientationLabel());
        }, -266723542, -265932737, -854792));

        this.ensureTrailingEmpty();
        this.rebuildRows();
    }

    private Component barToggleLabel(String key, boolean on) {
        return Component.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{Component.translatable((String)key), Component.translatable((String)(on ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private Component orientationLabel() {
        boolean vertical = "vertical".equals(this.config().barOrientation);
        return Component.translatable((String)"zombiezcompanion.autotext.bar.orientation", (Object[])new Object[]{Component.translatable((String)(vertical ? "zombiezcompanion.autotext.bar.vertical" : "zombiezcompanion.autotext.bar.horizontal"))});
    }

    private int rowsStartY() {
        return this.contentY1 + 80;
    }

    private int maxVisibleRows() {
        return Math.max(1, (this.contentY2 - this.rowsStartY() - 8) / ROW_H);
    }

    private void rebuildRows() {
        for (EditBox field : this.textFields) {
            this.removeWidget((GuiEventListener)field);
        }
        for (StyledButton button : this.rowButtons) {
            this.removeWidget((GuiEventListener)button);
        }
        this.textFields.clear();
        this.rowButtons.clear();
        this.keyButtons.clear();

        List<AutoTextConfig.Preset> presets = this.config().presets;
        int maxRows = this.maxVisibleRows();
        int maxOffset = Math.max(0, presets.size() - maxRows);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));

        int x = this.panelX1 + 36;
        int iconW = 20;
        int editW = 22;
        int removeW = 22;
        int availableW = Math.max(200, this.panelX2 - this.panelX1 - 72);
        int keyW = Math.max(64, Math.min(140, availableW / 4));
        int textW = availableW - iconW - editW - keyW - removeW - 20;
        int y = this.rowsStartY();

        for (int visible = 0; visible < maxRows; ++visible) {
            int index = this.scrollOffset + visible;
            if (index >= presets.size()) break;
            AutoTextConfig.Preset preset = presets.get(index);
            int rowY = y + visible * ROW_H;
            int absIndex = index;
            int cx = x + iconW + 4;

            EditBox field = new EditBox(this.font, cx, rowY, textW, 20, (Component)Component.literal((String)""));
            field.setMaxLength(256);
            field.setValue(preset.text == null ? "" : preset.text);
            field.setHint((Component)Component.translatable((String)"zombiezcompanion.autotext.placeholder"));
            field.setResponder(value -> this.onTextChanged(absIndex, value));
            this.textFields.add(field);
            this.addRenderableWidget(field);

            int ex = cx + textW + 4;
            StyledButton editButton = new StyledButton(ex, rowY, editW, 20, (Component)Component.literal((String)"✎"), b -> this.openDetail(absIndex), -266723542, -265932737, -854792);
            this.rowButtons.add(editButton);
            this.addRenderableWidget(editButton);

            StyledButton keyButton = new StyledButton(ex + editW + 4, rowY, keyW, 20, this.keyButtonLabel(absIndex), b -> this.beginKeyCapture(absIndex), -266723542, -265932737, -854792);
            this.keyButtons.add(keyButton);
            this.rowButtons.add(keyButton);
            this.addRenderableWidget(keyButton);

            StyledButton removeButton = new StyledButton(ex + editW + keyW + 8, rowY, removeW, 20, (Component)Component.literal((String)"✕"), b -> this.removePreset(absIndex), -12965328, -11716288, -854792);
            this.rowButtons.add(removeButton);
            this.addRenderableWidget(removeButton);
        }
    }

    private void openDetail(int index) {
        List<AutoTextConfig.Preset> presets = this.config().presets;
        if (index < 0 || index >= presets.size()) {
            return;
        }
        AutoTextConfig.Preset preset = presets.get(index);
        if (preset.id == null || preset.id.isEmpty()) {
            preset.id = UUID.randomUUID().toString();
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen((Screen)new PresetEditScreen((Screen)this, this.configManager, preset));
        }
    }

    private void onTextChanged(int index, String value) {
        List<AutoTextConfig.Preset> presets = this.config().presets;
        if (index < 0 || index >= presets.size()) {
            return;
        }
        AutoTextConfig.Preset preset = presets.get(index);
        boolean wasLast = index == presets.size() - 1;
        preset.text = value;
        if (preset.id == null || preset.id.isEmpty()) {
            preset.id = UUID.randomUUID().toString();
        }
        // Auto-add: filling the trailing empty slot spawns a fresh one below it.
        if (wasLast && value != null && !value.isBlank() && presets.size() < AutoTextConfig.MAX_PRESETS) {
            this.ensureTrailingEmpty();
            this.rebuildRows();
            this.refocusRow(index);
        }
    }

    /** Re-focus the text field for an absolute preset index after a rebuild (keeps typing seamless). */
    private void refocusRow(int index) {
        int visible = index - this.scrollOffset;
        if (visible >= 0 && visible < this.textFields.size()) {
            EditBox field = this.textFields.get(visible);
            this.setFocused((GuiEventListener)field);
            field.setFocused(true);
            field.moveCursorToEnd(false);
        }
    }

    private void ensureTrailingEmpty() {
        List<AutoTextConfig.Preset> presets = this.config().presets;
        if (presets.size() >= AutoTextConfig.MAX_PRESETS) {
            return;
        }
        if (presets.isEmpty() || !AutoTextOptionsScreen.isBlank(presets.get(presets.size() - 1))) {
            AutoTextConfig.Preset p = new AutoTextConfig.Preset();
            p.id = UUID.randomUUID().toString();
            presets.add(p);
        }
    }

    private static boolean isBlank(AutoTextConfig.Preset p) {
        return p == null || p.text == null || p.text.isBlank();
    }

    private void beginKeyCapture(int index) {
        this.waitingIndex = index;
        for (EditBox field : this.textFields) {
            field.setFocused(false);
        }
        this.updateKeyButtons();
    }

    private void updateKeyButtons() {
        for (int visible = 0; visible < this.keyButtons.size(); ++visible) {
            int index = this.scrollOffset + visible;
            if (index >= this.config().presets.size()) break;
            StyledButton keyButton = this.keyButtons.get(visible);
            keyButton.setMessage(this.keyButtonLabel(index));
            keyButton.setColors(this.waitingIndex == index ? -14867392 : -266723542, this.waitingIndex == index ? -9534721 : -265932737);
        }
    }

    private Component keyButtonLabel(int index) {
        if (this.waitingIndex == index) {
            return Component.translatable((String)"zombiezcompanion.autotext.key.waiting");
        }
        return Component.translatable((String)"zombiezcompanion.autotext.key.bound", (Object[])new Object[]{AutoTextModule.keyLabel(this.config().presets.get((int)index).keyCode)});
    }

    private void removePreset(int index) {
        List<AutoTextConfig.Preset> presets = this.config().presets;
        if (index < 0 || index >= presets.size()) {
            return;
        }
        presets.remove(index);
        this.waitingIndex = -1;
        this.ensureTrailingEmpty();
        this.rebuildRows();
    }

    private AutoTextConfig config() {
        return this.moduleRef.config();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<AutoTextConfig.Preset> presets = this.config().presets;
        int maxOffset = Math.max(0, presets.size() - this.maxVisibleRows());
        int next = this.scrollOffset + (verticalAmount < 0.0 ? 1 : -1);
        int clamped = Math.max(0, Math.min(maxOffset, next));
        if (clamped != this.scrollOffset) {
            this.scrollOffset = clamped;
            this.waitingIndex = -1;
            this.rebuildRows();
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        if (this.waitingIndex >= 0) {
            List<AutoTextConfig.Preset> presets = this.config().presets;
            if (this.waitingIndex < presets.size()) {
                if (keyCode == 256) {
                    this.waitingIndex = -1;
                } else if (keyCode == 259 || keyCode == 261) {
                    presets.get((int)this.waitingIndex).keyCode = -1;
                    this.waitingIndex = -1;
                } else if (keyCode != -1) {
                    presets.get((int)this.waitingIndex).keyCode = keyCode;
                    this.waitingIndex = -1;
                }
            } else {
                this.waitingIndex = -1;
            }
            this.updateKeyButtons();
            return true;
        }
        return super.keyPressed(event);
    }

    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (this.waitingIndex >= 0) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        // Drop empty presets (no text and no keybind) so the trailing slot isn't persisted.
        List<AutoTextConfig.Preset> presets = this.config().presets;
        presets.removeIf(p -> AutoTextOptionsScreen.isBlank(p) && p.keyCode == -1);
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        int x = this.panelX1 + 36;
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.autotext.header"), x, this.contentY1 + 20, -854792);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.autotext.hint.capture"), x, this.contentY1 + 34, -8353376, false);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.autotext.hint.command"), x, this.contentY1 + 44, -8353376, false);
        List<AutoTextConfig.Preset> presets = this.config().presets;
        // Per-row item icon preview (left of each text field).
        int maxRows = this.maxVisibleRows();
        int rowY = this.rowsStartY();
        for (int visible = 0; visible < maxRows; ++visible) {
            int index = this.scrollOffset + visible;
            if (index >= presets.size()) break;
            AutoTextConfig.Preset preset = presets.get(index);
            int iy = rowY + visible * ROW_H + 2;
            ctx.item(AutoTextModule.iconStack(preset.itemId), x, iy);
        }
        if (presets.size() > maxRows) {
            int from = this.scrollOffset + 1;
            int to = Math.min(this.scrollOffset + maxRows, presets.size());
            ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.autotext.scroll", (Object[])new Object[]{from, to, presets.size()}), x, this.contentY2 - 12, -8353376, false);
        }
    }

    @Override
    protected void renderOptionsBackground(GuiGraphicsExtractor ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 12;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = this.contentY2 - y - 4;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.outline(x, y, w, h, -14736594);
    }
}
