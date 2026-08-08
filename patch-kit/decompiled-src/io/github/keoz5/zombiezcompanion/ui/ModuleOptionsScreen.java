/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_304
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 */
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
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_5348;

public abstract class ModuleOptionsScreen
extends class_437 {
    protected final class_437 parent;
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

    protected ModuleOptionsScreen(class_437 parent, Module module, ConfigManager configManager) {
        super((class_2561)class_2561.method_43470((String)module.displayName()));
        this.parent = parent;
        this.module = module;
        this.configManager = configManager;
    }

    protected KeybindRow addKeybindRow(int x, int y, int width, class_304 binding, class_2561 label) {
        if (binding == null) {
            return null;
        }
        KeybindRow row = new KeybindRow(binding, x, y, width, label);
        this.method_37063((class_364)row.keyButton());
        this.method_37063((class_364)row.resetButton());
        this.keybindRows.add(row);
        return row;
    }

    protected StyledButton addCrossLink(int x, int y, int width, String moduleId, class_2561 label) {
        StyledButton btn = new StyledButton(x, y, width, 18, label, b -> {
            ModuleManager mm = ZombieZCompanionClient.moduleManager();
            if (mm == null) {
                return;
            }
            mm.findById(moduleId).ifPresent(m -> {
                if (m.hasOptions() && this.field_22787 != null) {
                    this.field_22787.method_1507(m.createOptionsScreen(this));
                }
            });
        }, -266723542, -265932737, -8874241);
        this.method_37063((class_364)btn);
        return btn;
    }

    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        for (KeybindRow row : this.keybindRows) {
            if (!row.isListening() || !row.handleKey(keyCode, scanCode)) continue;
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    public boolean method_25402(double mouseX, double mouseY, int button) {
        for (KeybindRow row : this.keybindRows) {
            if (!row.isListening() || !row.handleMouseRebind(button)) continue;
            return true;
        }
        for (KeybindRow row : this.keybindRows) {
            if (!row.handleRightClickUnbind(mouseX, mouseY, button)) continue;
            return true;
        }
        return super.method_25402(mouseX, mouseY, button);
    }

    protected final void method_25426() {
        this.keybindRows.clear();
        this.computePanelRect();
        int btnH = 20;
        int btnY = this.footerY1 + (34 - btnH) / 2;
        this.method_37063((class_364)new StyledButton(this.panelX1 + 12, btnY, 100, btnH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.button.back"), b -> this.method_25419(), -266723542, -265932737, -854792));
        this.method_37063((class_364)new StyledButton(this.panelX2 - 12 - 22, this.titleY1 + 10, 22, 22, (class_2561)class_2561.method_43470((String)"X"), b -> this.method_25419(), -266723542, -265932737, -854792));
        this.initOptions();
    }

    private void computePanelRect() {
        int margin = Math.max(4, Math.min(28, Math.min(this.field_22789, this.field_22790) / 16));
        int panelW = Math.min(960, this.field_22789 - 2 * margin);
        int panelH = Math.min(580, this.field_22790 - 2 * margin);
        this.panelX1 = (this.field_22789 - panelW) / 2;
        this.panelY1 = (this.field_22790 - panelH) / 2;
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

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, -872415232);
        ctx.method_25294(this.panelX1 + 3, this.panelY1 + 6, this.panelX2 + 3, this.panelY2 + 6, -1442840576);
        ctx.method_25294(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.method_25294(this.panelX1, this.titleY1, this.panelX2, this.titleY2, -183232737);
        ctx.method_25294(this.panelX1, this.contentY1, this.panelX2, this.contentY2, -183825134);
        ctx.method_25294(this.panelX1, this.footerY1, this.panelX2, this.footerY2, -183232737);
        ctx.method_25294(this.panelX1, this.titleY1, this.panelX2, this.titleY1 + 2, -8874241);
        ctx.method_25294(this.panelX1, this.titleY1 + 2, this.panelX2, this.titleY1 + 3, 0x33FFFFFF);
        ctx.method_25294(this.panelX1, this.titleY2 - 2, this.panelX2, this.titleY2 - 1, 0x55000000);
        ctx.method_25294(this.panelX1, this.footerY1 + 1, this.panelX2, this.footerY1 + 2, 0x33FFFFFF);
        ctx.method_25294(this.panelX1, this.titleY2 - 1, this.panelX2, this.titleY2, -14736594);
        ctx.method_25294(this.panelX1, this.footerY1, this.panelX2, this.footerY1 + 1, -14736594);
        ctx.method_25294(this.panelX1 + 1, this.panelY1, this.panelX2 - 1, this.panelY1 + 1, -13880766);
        ctx.method_25294(this.panelX1 + 1, this.panelY2 - 1, this.panelX2 - 1, this.panelY2, -13880766);
        ctx.method_25294(this.panelX1, this.panelY1 + 1, this.panelX1 + 1, this.panelY2 - 1, -13880766);
        ctx.method_25294(this.panelX2 - 1, this.panelY1 + 1, this.panelX2, this.panelY2 - 1, -13880766);
        ctx.method_25294(this.panelX1, this.panelY1 - 1, this.panelX2, this.panelY1, 1148753663);
        ctx.method_25294(this.panelX1 - 1, this.panelY1, this.panelX1, this.panelY2, 1148753663);
        ctx.method_25294(this.panelX2, this.panelY1, this.panelX2 + 1, this.panelY2, 1148753663);
        this.renderOptionsBackground(ctx);
        super.method_25394(ctx, mouseX, mouseY, delta);
        int titleY = this.titleY1 + 10;
        int titleX = this.panelX1 + 18;
        int nameW = this.field_22793.method_1727(this.module.displayName());
        class_5250 catText = class_2561.method_43470((String)ModuleOptionsScreen.categoryName(this.module.category()).toUpperCase(Locale.ROOT));
        int catW = this.field_22793.method_27525((class_5348)catText) + 10;
        ctx.method_25294(titleX + 1, titleY - 4, titleX + catW - 1, titleY + 4, -14867392);
        ctx.method_25294(titleX, titleY - 3, titleX + 1, titleY + 3, -14867392);
        ctx.method_25294(titleX + catW - 1, titleY - 3, titleX + catW, titleY + 3, -14867392);
        ctx.method_25294(titleX + 1, titleY - 4, titleX + catW - 1, titleY - 3, -8874241);
        ctx.method_51439(this.field_22793, (class_2561)catText, titleX + 5, titleY - 3, -8874241, false);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43470((String)this.module.displayName()), titleX, titleY + 8, -854792, true);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43470((String)("\u00b7 " + this.module.id())), titleX + nameW + 6, titleY + 8, -12235684, false);
        ctx.method_25294(titleX, titleY + 22, titleX + Math.max(42, nameW), titleY + 23, -8874241);
        ctx.method_25294(titleX, titleY + 23, titleX + Math.max(28, nameW / 2), titleY + 24, -11441921);
        for (KeybindRow row : this.keybindRows) {
            row.renderLabel(ctx, this.field_22793);
        }
    }

    protected void renderOptionsBackground(class_332 ctx) {
    }

    private static String categoryName(ModuleCategory category) {
        return class_2561.method_43471((String)("zombiezcompanion.category." + category.name().toLowerCase(Locale.ROOT))).getString();
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

