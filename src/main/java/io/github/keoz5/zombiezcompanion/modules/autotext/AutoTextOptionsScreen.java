package io.github.keoz5.zombiezcompanion.modules.autotext;

import io.github.keoz5.zombiezcompanion.config.AutoTextConfig;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.modules.autotext.AutoTextModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

public final class AutoTextOptionsScreen
extends ModuleOptionsScreen {
    private static final int ROW_H = 26;
    private static final int HEADER_Y = 24;
    private static final int HINT1_Y = 36;
    private static final int HINT2_Y = 48;
    private static final int ROWS_START_Y = 68;
    private final AutoTextModule moduleRef;
    private final List<TextFieldWidget> textFields = new ArrayList<TextFieldWidget>();
    private final List<StyledButton> rowButtons = new ArrayList<StyledButton>();
    private StyledButton addButton;
    private int waitingIndex = -1;

    public AutoTextOptionsScreen(Screen parent, AutoTextModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        this.normalizeEntries();
        this.rebuildRows();
    }

    private void rebuildRows() {
        for (TextFieldWidget field : this.textFields) {
            this.remove((Element)field);
        }
        for (StyledButton button2 : this.rowButtons) {
            this.remove((Element)button2);
        }
        if (this.addButton != null) {
            this.remove((Element)this.addButton);
        }
        this.textFields.clear();
        this.rowButtons.clear();
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 68;
        int availableW = Math.max(170, this.panelX2 - this.panelX1 - 72);
        int removeW = Math.max(38, Math.min(62, availableW / 5));
        int keyW = Math.max(66, Math.min(150, availableW / 3));
        int textW = Math.max(50, availableW - keyW - removeW - 16);
        List<AutoTextConfig.Entry> entries = this.config().entries;
        int i = 0;
        while (i < entries.size()) {
            AutoTextConfig.Entry entry = entries.get(i);
            int rowY = y + i * 26;
            int index = i++;
            TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, rowY, textW, 20, (Text)Text.literal((String)""));
            field.setMaxLength(256);
            field.setText(entry.text);
            field.setPlaceholder((Text)Text.translatable((String)"zombiezcompanion.autotext.placeholder"));
            field.setChangedListener(value -> {
                entry.text = value;
            });
            this.textFields.add(field);
            this.addDrawableChild(field);
            StyledButton keyButton = new StyledButton(x + textW + 8, rowY, keyW, 20, this.keyButtonLabel(index), button -> this.beginKeyCapture(index), -266723542, -265932737, -854792);
            this.rowButtons.add(keyButton);
            this.addDrawableChild(keyButton);
            StyledButton removeButton = new StyledButton(x + textW + keyW + 16, rowY, removeW, 20, (Text)Text.translatable((String)"zombiezcompanion.autotext.remove"), button -> this.removeEntry(index), -12965328, -11716288, -854792);
            this.rowButtons.add(removeButton);
            this.addDrawableChild(removeButton);
        }
        this.addButton = new StyledButton(x, y + entries.size() * 26 + 8, Math.min(180, availableW), 20, this.addButtonLabel(), button -> this.addEntry(), -14867392, -11441921, -854792);
        this.addButton.active = entries.size() < 5;
        this.addDrawableChild(this.addButton);
    }

    private void beginKeyCapture(int index) {
        this.waitingIndex = index;
        for (TextFieldWidget field : this.textFields) {
            field.setFocused(false);
        }
        this.updateKeyButtons();
    }

    private void updateKeyButtons() {
        int buttonIndex;
        for (int i = 0; i < this.config().entries.size() && (buttonIndex = i * 2) < this.rowButtons.size(); ++i) {
            StyledButton keyButton = this.rowButtons.get(buttonIndex);
            keyButton.setMessage(this.keyButtonLabel(i));
            keyButton.setColors(this.waitingIndex == i ? -14867392 : -266723542, this.waitingIndex == i ? -9534721 : -265932737);
        }
    }

    private Text keyButtonLabel(int index) {
        if (this.waitingIndex == index) {
            return Text.translatable((String)"zombiezcompanion.autotext.key.waiting");
        }
        return Text.translatable((String)"zombiezcompanion.autotext.key.bound", (Object[])new Object[]{AutoTextModule.keyLabel(this.config().entries.get((int)index).keyCode)});
    }

    private Text addButtonLabel() {
        return Text.translatable((String)"zombiezcompanion.autotext.add", (Object[])new Object[]{this.config().entries.size(), 5});
    }

    private void addEntry() {
        if (this.config().entries.size() >= 5) {
            return;
        }
        AutoTextConfig.Entry entry = new AutoTextConfig.Entry();
        entry.text = "";
        entry.keyCode = -1;
        this.config().entries.add(entry);
        this.waitingIndex = -1;
        this.rebuildRows();
    }

    private void removeEntry(int index) {
        if (index < 0 || index >= this.config().entries.size()) {
            return;
        }
        this.config().entries.remove(index);
        this.waitingIndex = -1;
        this.rebuildRows();
    }

    private void normalizeEntries() {
        if (this.config().entries == null) {
            this.config().entries = new ArrayList<AutoTextConfig.Entry>();
        }
        for (AutoTextConfig.Entry entry : this.config().entries) {
            if (entry.text != null) continue;
            entry.text = "";
        }
        while (this.config().entries.size() > 5) {
            this.config().entries.remove(this.config().entries.size() - 1);
        }
    }

    private AutoTextConfig config() {
        return this.moduleRef.config();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.waitingIndex >= 0) {
            if (keyCode == 256) {
                this.waitingIndex = -1;
            } else if (keyCode == 259 || keyCode == 261) {
                this.config().entries.get((int)this.waitingIndex).keyCode = -1;
                this.waitingIndex = -1;
            } else if (keyCode != -1) {
                this.config().entries.get((int)this.waitingIndex).keyCode = keyCode;
                this.waitingIndex = -1;
            }
            this.updateKeyButtons();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (this.waitingIndex >= 0) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = this.panelX1 + 36;
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.autotext.header"), x, this.contentY1 + 24, -854792);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.autotext.hint.capture"), x, this.contentY1 + 36, -8353376, false);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.autotext.hint.command"), x, this.contentY1 + 48, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(DrawContext ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 14;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = Math.min(this.contentY2 - y - 6, 54 + this.config().entries.size() * 26 + 40);
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.drawBorder(x, y, w, h, -14736594);
    }
}

