package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.ui.widget.KeybindRow;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;

public abstract class ModuleOptionsScreen
extends Screen {
    protected final Screen parent;
    protected final Module module;
    protected final ConfigManager configManager;
    protected int panelX1;
    protected int panelY1;
    protected int panelX2;
    protected int panelY2;
    protected int titleY1;
    protected int titleY2;
    protected int contentY1;
    protected int contentY2;
    protected int footerY1;
    protected int footerY2;
    private final List<KeybindRow> keybindRows = new ArrayList<KeybindRow>();

    protected ModuleOptionsScreen(Screen parent, Module module, ConfigManager configManager) {
        super((Text)Text.literal((String)module.displayName()));
        this.parent = parent;
        this.module = module;
        this.configManager = configManager;
    }

    protected KeybindRow addKeybindRow(int x, int y, int width, KeyBinding binding, Text label) {
        if (binding == null) {
            return null;
        }
        KeybindRow row = new KeybindRow(binding, x, y, width, label);
        this.addDrawableChild(row.keyButton());
        this.addDrawableChild(row.resetButton());
        this.keybindRows.add(row);
        return row;
    }

    protected StyledButton addCrossLink(int x, int y, int width, String moduleId, Text label) {
        StyledButton btn = new StyledButton(x, y, width, 18, label, b -> {
            ModuleManager mm = ZombieZCompanionClient.moduleManager();
            if (mm == null) {
                return;
            }
            mm.findById(moduleId).ifPresent(m -> {
                if (m.hasOptions() && this.client != null) {
                    this.client.setScreen(m.createOptionsScreen(this));
                }
            });
        }, -266723542, -265932737, -8874241);
        this.addDrawableChild(btn);
        return btn;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (KeybindRow row : this.keybindRows) {
            if (!row.isListening() || !row.handleKey(keyCode, scanCode)) continue;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (KeybindRow row : this.keybindRows) {
            if (!row.isListening() || !row.handleMouseRebind(button)) continue;
            return true;
        }
        for (KeybindRow row : this.keybindRows) {
            if (!row.handleRightClickUnbind(mouseX, mouseY, button)) continue;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected final void init() {
        this.keybindRows.clear();
        this.computePanelRect();
        int btnH = 20;
        int btnY = this.footerY1 + (34 - btnH) / 2;
        this.addDrawableChild(new StyledButton(this.panelX1 + 12, btnY, 100, btnH, (Text)Text.translatable((String)"zombiezcompanion.button.back"), b -> this.close(), -266723542, -265932737, -854792));
        this.addDrawableChild(new StyledButton(this.panelX2 - 12 - 22, this.titleY1 + 10, 22, 22, (Text)Text.literal((String)"X"), b -> this.close(), -266723542, -265932737, -854792));
        this.initOptions();
    }

    private void computePanelRect() {
        int margin = Math.max(4, Math.min(28, Math.min(this.width, this.height) / 16));
        int panelW = Math.min(960, this.width - 2 * margin);
        int panelH = Math.min(580, this.height - 2 * margin);
        this.panelX1 = (this.width - panelW) / 2;
        this.panelY1 = (this.height - panelH) / 2;
        this.panelX2 = this.panelX1 + panelW;
        this.panelY2 = this.panelY1 + panelH;
        this.titleY1 = this.panelY1;
        this.titleY2 = this.titleY1 + 42;
        this.footerY2 = this.panelY2;
        this.footerY1 = this.footerY2 - 34;
        this.contentY1 = this.titleY2;
        this.contentY2 = this.footerY1;
    }

    protected abstract void initOptions();

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, -872415232);
        ctx.fill(this.panelX1 + 3, this.panelY1 + 6, this.panelX2 + 3, this.panelY2 + 6, -1442840576);
        ctx.fill(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.fill(this.panelX1, this.titleY1, this.panelX2, this.titleY2, -183232737);
        ctx.fill(this.panelX1, this.contentY1, this.panelX2, this.contentY2, -183825134);
        ctx.fill(this.panelX1, this.footerY1, this.panelX2, this.footerY2, -183232737);
        ctx.fill(this.panelX1, this.titleY1, this.panelX2, this.titleY1 + 2, -8874241);
        ctx.fill(this.panelX1, this.titleY1 + 2, this.panelX2, this.titleY1 + 3, 0x33FFFFFF);
        ctx.fill(this.panelX1, this.titleY2 - 2, this.panelX2, this.titleY2 - 1, 0x55000000);
        ctx.fill(this.panelX1, this.footerY1 + 1, this.panelX2, this.footerY1 + 2, 0x33FFFFFF);
        ctx.fill(this.panelX1, this.titleY2 - 1, this.panelX2, this.titleY2, -14736594);
        ctx.fill(this.panelX1, this.footerY1, this.panelX2, this.footerY1 + 1, -14736594);
        ctx.fill(this.panelX1 + 1, this.panelY1, this.panelX2 - 1, this.panelY1 + 1, -13880766);
        ctx.fill(this.panelX1 + 1, this.panelY2 - 1, this.panelX2 - 1, this.panelY2, -13880766);
        ctx.fill(this.panelX1, this.panelY1 + 1, this.panelX1 + 1, this.panelY2 - 1, -13880766);
        ctx.fill(this.panelX2 - 1, this.panelY1 + 1, this.panelX2, this.panelY2 - 1, -13880766);
        ctx.fill(this.panelX1, this.panelY1 - 1, this.panelX2, this.panelY1, 1148753663);
        ctx.fill(this.panelX1 - 1, this.panelY1, this.panelX1, this.panelY2, 1148753663);
        ctx.fill(this.panelX2, this.panelY1, this.panelX2 + 1, this.panelY2, 1148753663);
        this.renderOptionsBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        int titleY = this.titleY1 + 10;
        int titleX = this.panelX1 + 18;
        int nameW = this.textRenderer.getWidth(this.module.displayName());
        MutableText catText = Text.literal((String)ModuleOptionsScreen.categoryName(this.module.category()).toUpperCase(Locale.ROOT));
        int catW = this.textRenderer.getWidth((StringVisitable)catText) + 10;
        ctx.fill(titleX + 1, titleY - 4, titleX + catW - 1, titleY + 4, -14867392);
        ctx.fill(titleX, titleY - 3, titleX + 1, titleY + 3, -14867392);
        ctx.fill(titleX + catW - 1, titleY - 3, titleX + catW, titleY + 3, -14867392);
        ctx.fill(titleX + 1, titleY - 4, titleX + catW - 1, titleY - 3, -8874241);
        ctx.drawText(this.textRenderer, (Text)catText, titleX + 5, titleY - 3, -8874241, false);
        ctx.drawText(this.textRenderer, (Text)Text.literal((String)this.module.displayName()), titleX, titleY + 8, -854792, true);
        ctx.drawText(this.textRenderer, (Text)Text.literal((String)("\u00b7 " + this.module.id())), titleX + nameW + 6, titleY + 8, -12235684, false);
        ctx.fill(titleX, titleY + 22, titleX + Math.max(42, nameW), titleY + 23, -8874241);
        ctx.fill(titleX, titleY + 23, titleX + Math.max(28, nameW / 2), titleY + 24, -11441921);
        for (KeybindRow row : this.keybindRows) {
            row.renderLabel(ctx, this.textRenderer);
        }
    }

    protected void renderOptionsBackground(DrawContext ctx) {
    }

    private static String categoryName(ModuleCategory category) {
        return Text.translatable((String)("zombiezcompanion.category." + category.name().toLowerCase(Locale.ROOT))).getString();
    }

    public void close() {
        this.configManager.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    public boolean shouldPause() {
        return false;
    }
}

