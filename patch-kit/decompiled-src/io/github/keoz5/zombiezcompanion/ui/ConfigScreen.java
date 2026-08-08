/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_156
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_364
 *  net.minecraft.class_4185
 *  net.minecraft.class_437
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 */
package io.github.keoz5.zombiezcompanion.ui;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.ui.FeedbackScreen;
import io.github.keoz5.zombiezcompanion.ui.HudEditorScreen;
import io.github.keoz5.zombiezcompanion.ui.KeybindsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.CategoryTabButton;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_5348;

public final class ConfigScreen
extends class_437 {
    private final class_437 parent;
    private final ConfigManager configManager;
    private final ModuleManager moduleManager;
    private class_342 searchField;
    private String searchText = "";
    private ModuleCategory selectedCategory = null;
    private int panelX1;
    private int panelY1;
    private int panelX2;
    private int panelY2;
    private int titleY1;
    private int titleY2;
    private int toolbarY1;
    private int toolbarY2;
    private int contentY1;
    private int contentY2;
    private int footerY1;
    private int footerY2;
    private int mainX1;
    private int gridLeft;
    private int columns;
    private int scrollOffset;
    private final List<CardLayout> cards = new ArrayList<CardLayout>();
    private long openedAt = 0L;

    public ConfigScreen(class_437 parent, ConfigManager configManager, ModuleManager moduleManager) {
        super((class_2561)class_2561.method_43470((String)"ZombieZ Companion"));
        this.parent = parent;
        this.configManager = configManager;
        this.moduleManager = moduleManager;
    }

    protected void method_25426() {
        this.cards.clear();
        if (this.openedAt == 0L) {
            this.openedAt = System.currentTimeMillis();
        }
        this.computePanelRect();
        this.method_37063((class_364)new StyledButton(this.panelX2 - 12 - 18, this.titleY1 + 8, 18, 18, (class_2561)class_2561.method_43470((String)"X"), btn -> this.method_25419(), -266723542, -265932737, -854792));
        int tabsX = this.mainX1 + 12;
        int tabsY = this.toolbarY1 + 7;
        tabsX += this.addTab(class_2561.method_43471((String)"zombiezcompanion.tab.all").getString().toUpperCase(Locale.ROOT), null, tabsX, tabsY) + 6;
        for (ModuleCategory c : this.usedCategories()) {
            tabsX += this.addTab(ConfigScreen.categoryLabel(c).toUpperCase(Locale.ROOT), c, tabsX, tabsY) + 6;
        }
        int searchX = this.panelX2 - 12 - 156;
        int searchY = this.toolbarY1 + 7;
        this.searchField = new class_342(this.field_22793, searchX, searchY, 156, 22, (class_2561)class_2561.method_43470((String)""));
        this.searchField.method_1880(64);
        this.searchField.method_47404((class_2561)class_2561.method_43471((String)"zombiezcompanion.search.placeholder"));
        this.searchField.method_1852(this.searchText);
        this.searchField.method_1863(s -> {
            this.searchText = s;
            this.layoutCards();
        });
        this.method_37063((class_364)this.searchField);
        int usableWidth = this.panelX2 - this.mainX1 - 24;
        this.columns = Math.max(1, Math.min(4, (usableWidth + 10) / 164));
        int totalGridW = this.columns * 154 + (this.columns - 1) * 10;
        this.gridLeft = this.mainX1 + (this.panelX2 - this.mainX1 - totalGridW) / 2;
        this.layoutCards();
        int btnH = 18;
        int btnY = this.footerY1 + (34 - btnH) / 2;
        this.method_37063((class_364)new StyledButton(this.panelX1 + 12, btnY, 92, btnH, this.debugLabel(), btn -> {
            boolean next;
            this.configManager.get().debugMode = next = !this.configManager.get().debugMode;
            this.configManager.save();
            btn.method_25355(this.debugLabel());
        }, -266723542, -265932737, -854792));
        this.method_37063((class_364)new StyledButton(this.panelX1 + 12 + 92 + 6, btnY, 78, btnH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.keybinds.open"), btn -> {
            if (this.field_22787 != null) {
                this.field_22787.method_1507((class_437)new KeybindsScreen(this));
            }
        }, -266723542, -265932737, -854792));
        this.method_37063((class_364)new StyledButton(this.panelX1 + 12 + 92 + 6 + 78 + 6, btnY, 90, btnH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.hud.editor.open"), btn -> {
            if (this.field_22787 != null) {
                this.field_22787.method_1507((class_437)new HudEditorScreen(this, this.configManager));
            }
        }, -266723542, -265932737, -854792));
        this.method_37063((class_364)new StyledButton(this.panelX2 - 12 - 92 - 6 - 78, btnY, 78, btnH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.title.button.discord"), btn -> class_156.method_668().method_673(URI.create("https://discord.gg/mqJp9CKcEX")), -11441921, -8874241, -854792));
        this.method_37063((class_364)new StyledButton(this.panelX2 - 12 - 92 - 6 - 78 - 6 - 88, btnY, 88, btnH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.feedback.button"), btn -> {
            if (this.field_22787 != null) {
                this.field_22787.method_1507((class_437)new FeedbackScreen(this, this.configManager));
            }
        }, -266723542, -265932737, -854792));
        this.method_37063((class_364)new StyledButton(this.panelX2 - 12 - 92, btnY, 92, btnH, (class_2561)class_2561.method_43471((String)"zombiezcompanion.button.close"), btn -> this.method_25419(), -266723542, -265932737, -854792));
    }

    private static String categoryLabel(ModuleCategory category) {
        return class_2561.method_43471((String)("zombiezcompanion.category." + category.name().toLowerCase(Locale.ROOT))).getString();
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
        this.toolbarY1 = this.titleY2 = this.titleY1 + 42;
        this.toolbarY2 = this.toolbarY1 + 36;
        this.footerY2 = this.panelY2;
        this.footerY1 = this.footerY2 - 34;
        this.contentY1 = this.toolbarY2;
        this.contentY2 = this.footerY1;
        this.mainX1 = this.panelX1;
    }

    private List<ModuleCategory> usedCategories() {
        return this.moduleManager.modules().stream().map(Module::category).distinct().sorted(Comparator.comparing(Enum::ordinal)).toList();
    }

    private class_2561 debugLabel() {
        boolean on = this.configManager.get().debugMode;
        return class_2561.method_43471((String)(on ? "zombiezcompanion.debug.label.on" : "zombiezcompanion.debug.label.off"));
    }

    private int addTab(String label, ModuleCategory cat, int x, int y) {
        int w = Math.max(40, this.field_22793.method_1727(label) + 14);
        this.method_37063((class_364)new CategoryTabButton(x, y, w, 22, (class_2561)class_2561.method_43470((String)label), btn -> {
            this.selectedCategory = cat;
            this.layoutCards();
        }, () -> Objects.equals((Object)this.selectedCategory, (Object)cat)));
        return w;
    }

    private void layoutCards() {
        for (CardLayout c : this.cards) {
            this.method_37066((class_364)c.optionsBtn);
            this.method_37066((class_364)c.toggleBtn);
        }
        this.cards.clear();
        List<Module> filtered = this.filterModules();
        int rowsTotal = (filtered.size() + this.columns - 1) / this.columns;
        int rowH = 102;
        int visibleHeight = this.contentY2 - this.contentY1 - 24;
        int visibleRows = Math.max(1, visibleHeight / rowH);
        int maxOffset = Math.max(0, rowsTotal - visibleRows);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));
        int gridTop = this.contentY1 + 12 - this.scrollOffset * rowH;
        for (int i = 0; i < filtered.size(); ++i) {
            int row = i / this.columns;
            int col = i % this.columns;
            int x = this.gridLeft + col * 164;
            int y = gridTop + row * rowH;
            CardLayout card = this.buildCard(filtered.get(i), x, y);
            card.optionsBtn.field_22764 = y >= this.contentY1 && y + 92 <= this.contentY2;
            card.toggleBtn.field_22764 = card.optionsBtn.field_22764;
            card.optionsBtn.field_22763 = card.optionsBtn.field_22764 && filtered.get(i).hasOptions();
            this.cards.add(card);
        }
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY < (double)this.contentY1 || mouseY > (double)this.contentY2) {
            return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int delta = verticalAmount > 0.0 ? -1 : 1;
        this.scrollOffset = Math.max(0, this.scrollOffset + delta);
        this.layoutCards();
        return true;
    }

    private List<Module> filterModules() {
        String q = this.searchText.toLowerCase(Locale.ROOT).trim();
        return this.moduleManager.modules().stream().filter(m -> !m.hidden()).filter(m -> this.selectedCategory == null || m.category() == this.selectedCategory).filter(m -> q.isEmpty() || m.displayName().toLowerCase(Locale.ROOT).contains(q) || m.id().toLowerCase(Locale.ROOT).contains(q) || m.category().displayName().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q) || m.searchKeywords().stream().anyMatch(k -> k.toLowerCase(Locale.ROOT).contains(q))).toList();
    }

    private CardLayout buildCard(Module m, int x, int y) {
        int btnW = 142;
        int btnX = x + 6;
        int optionsY = y + 92 - 36;
        int toggleY = y + 92 - 18;
        StyledButton options = new StyledButton(btnX, optionsY, btnW, 16, (class_2561)class_2561.method_43471((String)"zombiezcompanion.card.options"), btn -> this.openOptions(m), -266723542, -265932737, -854792);
        options.field_22763 = m.hasOptions();
        this.method_37063((class_364)options);
        boolean enabled = this.moduleManager.isEnabled(m.id());
        StyledButton toggle = new StyledButton(btnX, toggleY, btnW, 16, ConfigScreen.toggleLabel(enabled), btn -> this.toggleModule(m, btn), enabled ? -14709924 : -12965328, enabled ? -14179731 : -11716288, -854792);
        this.method_37063((class_364)toggle);
        return new CardLayout(m, x, y, options, toggle);
    }

    private static class_2561 toggleLabel(boolean enabled) {
        return class_2561.method_43471((String)(enabled ? "zombiezcompanion.card.toggle.on" : "zombiezcompanion.card.toggle.off"));
    }

    private void toggleModule(Module m, class_4185 btn) {
        boolean next = !this.moduleManager.isEnabled(m.id());
        this.moduleManager.setEnabled(m.id(), next);
        StyledButton sb = (StyledButton)btn;
        sb.method_25355(ConfigScreen.toggleLabel(next));
        sb.setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
    }

    private void openOptions(Module m) {
        if (!m.hasOptions() || this.field_22787 == null) {
            return;
        }
        class_437 opts = m.createOptionsScreen(this);
        if (opts != null) {
            this.field_22787.method_1507(opts);
        }
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, -872415232);
        ctx.method_25294(this.panelX1 + 3, this.panelY1 + 6, this.panelX2 + 3, this.panelY2 + 6, -1442840576);
        ctx.method_25294(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.method_25294(this.panelX1, this.titleY1, this.panelX2, this.titleY2, -183232737);
        ctx.method_25294(this.panelX1, this.titleY1 + 2, this.panelX2, this.titleY1 + 3, 0x33FFFFFF);
        ctx.method_25294(this.panelX1, this.titleY2 - 2, this.panelX2, this.titleY2 - 1, 0x55000000);
        ctx.method_25294(this.panelX1, this.toolbarY1, this.panelX2, this.toolbarY2, -183232737);
        ctx.method_25294(this.panelX1, this.contentY1, this.panelX2, this.contentY2, -183825134);
        ctx.method_25294(this.panelX1, this.footerY1, this.panelX2, this.footerY2, -183232737);
        ctx.method_25294(this.panelX1, this.footerY1 + 1, this.panelX2, this.footerY1 + 2, 0x33FFFFFF);
        ctx.method_25294(this.panelX1, this.titleY2 - 1, this.panelX2, this.titleY2, -14736594);
        ctx.method_25294(this.panelX1, this.toolbarY2 - 1, this.panelX2, this.toolbarY2, -14736594);
        ctx.method_25294(this.panelX1, this.footerY1, this.panelX2, this.footerY1 + 1, -14736594);
        ctx.method_25294(this.panelX1, this.titleY1, this.panelX2, this.titleY1 + 2, -8874241);
        ctx.method_25294(this.panelX1, this.titleY1 + 1, this.panelX2, this.titleY1 + 2, -11441921);
        ctx.method_25294(this.panelX1 + 1, this.panelY1, this.panelX2 - 1, this.panelY1 + 1, -13880766);
        ctx.method_25294(this.panelX1 + 1, this.panelY2 - 1, this.panelX2 - 1, this.panelY2, -13880766);
        ctx.method_25294(this.panelX1, this.panelY1 + 1, this.panelX1 + 1, this.panelY2 - 1, -13880766);
        ctx.method_25294(this.panelX2 - 1, this.panelY1 + 1, this.panelX2, this.panelY2 - 1, -13880766);
        ctx.method_25294(this.panelX1, this.panelY1 - 1, this.panelX2, this.panelY1, 1148753663);
        ctx.method_25294(this.panelX1 - 1, this.panelY1, this.panelX1, this.panelY2, 1148753663);
        ctx.method_25294(this.panelX2, this.panelY1, this.panelX2 + 1, this.panelY2, 1148753663);
        ctx.method_44379(this.panelX1, this.contentY1, this.panelX2, this.contentY2);
        long elapsed = System.currentTimeMillis() - this.openedAt;
        for (int i = 0; i < this.cards.size(); ++i) {
            CardLayout c = this.cards.get(i);
            if (c.y + 92 < this.contentY1 || c.y > this.contentY2) continue;
            int yOff = ConfigScreen.cardAnimY(elapsed, i);
            if (yOff != 0) {
                ctx.method_51448().method_22903();
                ctx.method_51448().method_46416(0.0f, (float)yOff, 0.0f);
            }
            this.drawCardBackground(ctx, c, mouseX, mouseY - yOff);
            if (yOff == 0) continue;
            ctx.method_51448().method_22909();
        }
        ctx.method_44380();
        if (this.cards.isEmpty()) {
            boolean noModules = this.moduleManager.modules().isEmpty();
            class_5250 primary = class_2561.method_43471((String)(noModules ? "zombiezcompanion.empty.no_modules.primary" : "zombiezcompanion.empty.no_match.primary"));
            class_5250 hint = class_2561.method_43471((String)(noModules ? "zombiezcompanion.empty.no_modules.hint" : "zombiezcompanion.empty.no_match.hint"));
            int cx = (this.mainX1 + this.panelX2) / 2;
            int cy = (this.contentY1 + this.contentY2) / 2 - 6;
            ctx.method_27534(this.field_22793, (class_2561)primary, cx, cy, -8353376);
            ctx.method_27534(this.field_22793, (class_2561)hint, cx, cy + 12, -12235684);
        }
        super.method_25394(ctx, mouseX, mouseY, delta);
        this.renderForegroundText(ctx);
        this.renderCardTooltip(ctx, mouseX, mouseY);
    }

    private void renderCardTooltip(class_332 ctx, int mouseX, int mouseY) {
        if (mouseY < this.contentY1 || mouseY > this.contentY2) {
            return;
        }
        for (CardLayout c : this.cards) {
            if (c.y + 92 < this.contentY1 || c.y > this.contentY2 || mouseX < c.x || mouseX >= c.x + 154 || mouseY < c.y || mouseY >= c.y + 92) continue;
            String desc = c.module.description();
            if (desc.isEmpty()) {
                return;
            }
            int tw = this.field_22793.method_1727(desc);
            int boxW = Math.min(tw + 12, 240);
            int boxH = 18;
            int tx = Math.min(mouseX + 10, this.field_22789 - boxW - 4);
            int ty = Math.max(4, mouseY - boxH - 4);
            ctx.method_25294(tx + 1, ty + 2, tx + boxW + 1, ty + boxH + 2, -1442840576);
            ctx.method_25294(tx, ty, tx + boxW, ty + boxH, -183627755);
            ctx.method_49601(tx, ty, boxW, boxH, -8874241);
            ctx.method_51433(this.field_22793, this.field_22793.method_27523(desc, boxW - 12), tx + 6, ty + 5, -854792, false);
            return;
        }
    }

    private void renderForegroundText(class_332 ctx) {
        int logoX = this.panelX1 + 12;
        int logoY = this.titleY1 + 8;
        this.drawLogo(ctx, logoX, logoY);
        int titleX = logoX + 30;
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43470((String)"ZOMBIEZ"), titleX, this.titleY1 + 7, -854792, true);
        ctx.method_51439(this.field_22793, (class_2561)class_2561.method_43470((String)"COMPANION"), titleX + 62, this.titleY1 + 7, -8874241, true);
        class_5250 version = class_2561.method_43469((String)"zombiezcompanion.header.version", (Object[])new Object[]{ConfigScreen.currentVersion()});
        int vw = this.field_22793.method_27525((class_5348)version);
        ctx.method_25294(titleX + 1, this.titleY1 + 19, titleX + vw + 11, this.titleY1 + 29, -14867392);
        ctx.method_25294(titleX, this.titleY1 + 20, titleX + 1, this.titleY1 + 28, -14867392);
        ctx.method_25294(titleX + vw + 11, this.titleY1 + 20, titleX + vw + 12, this.titleY1 + 28, -14867392);
        ctx.method_25294(titleX + 1, this.titleY1 + 19, titleX + vw + 11, this.titleY1 + 20, -8874241);
        ctx.method_51439(this.field_22793, (class_2561)version, titleX + 5, this.titleY1 + 21, -8874241, false);
        ctx.method_44379(this.panelX1, this.contentY1, this.panelX2, this.contentY2);
        long elapsedG = System.currentTimeMillis() - this.openedAt;
        for (int i = 0; i < this.cards.size(); ++i) {
            CardLayout c = this.cards.get(i);
            if (c.y + 92 < this.contentY1 || c.y > this.contentY2) continue;
            int yOff = ConfigScreen.cardAnimY(elapsedG, i);
            if (yOff != 0) {
                ctx.method_51448().method_22903();
                ctx.method_51448().method_46416(0.0f, (float)yOff, 0.0f);
            }
            int x = c.x + 77;
            this.drawModuleGlyph(ctx, c.module, x, c.y + 16);
            ctx.method_27534(this.field_22793, (class_2561)class_2561.method_43470((String)c.module.displayName()), x, c.y + 34, -854792);
            if (yOff == 0) continue;
            ctx.method_51448().method_22909();
        }
        ctx.method_44380();
    }

    private void drawLogo(class_332 ctx, int x, int y) {
        int s = 22;
        ctx.method_25294(x + 1, y + 2, x + s + 1, y + s + 2, 0x66000000);
        ctx.method_25294(x + 1, y, x + s - 1, y + s, -183825134);
        ctx.method_25294(x, y + 1, x + 1, y + s - 1, -183825134);
        ctx.method_25294(x + s - 1, y + 1, x + s, y + s - 1, -183825134);
        ctx.method_25294(x + 1, y, x + s - 1, y + 1, 0x33FFFFFF);
        ctx.method_25294(x + 1, y, x + s - 1, y + 1, -8874241);
        ctx.method_25294(x + 1, y + s - 1, x + s - 1, y + s, -8874241);
        ctx.method_25294(x, y + 1, x + 1, y + s - 1, -8874241);
        ctx.method_25294(x + s - 1, y + 1, x + s, y + s - 1, -8874241);
        int cx = x + s / 2;
        int cy = y + s / 2;
        ctx.method_25294(x + 5, y + 5, x + s - 5, y + 7, -8874241);
        ctx.method_25294(x + 5, y + s - 7, x + s - 5, y + s - 5, -8874241);
        ctx.method_25294(cx - 1, cy - 4, cx + 1, cy + 4, -11441921);
        ctx.method_25294(cx - 1, cy - 1, cx + 1, cy + 1, -854792);
    }

    private void drawModuleGlyph(class_332 ctx, Module module, int centerX, int centerY) {
        int w = 38;
        int h = 26;
        int x = centerX - w / 2;
        int y = centerY - h / 2;
        ctx.method_25294(x + 1, y, x + w - 1, y + h, -435549166);
        ctx.method_25294(x, y + 1, x + 1, y + h - 1, -435549166);
        ctx.method_25294(x + w - 1, y + 1, x + w, y + h - 1, -435549166);
        ctx.method_25294(x + 1, y, x + w - 1, y + 1, 0x33FFFFFF);
        ctx.method_25294(x + 1, y + 1, x + 3, y + h - 1, -8874241);
        ctx.method_25294(x + 1, y, x + w - 1, y + 1, 1719770367);
        ctx.method_25294(x + 1, y + h - 1, x + w - 1, y + h, 1719770367);
        ctx.method_25294(x, y + 1, x + 1, y + h - 1, 1719770367);
        ctx.method_25294(x + w - 1, y + 1, x + w, y + h - 1, 1719770367);
        String glyph = this.moduleGlyph(module);
        int textW = this.field_22793.method_1727(glyph);
        ctx.method_27535(this.field_22793, (class_2561)class_2561.method_43470((String)glyph), centerX - textW / 2 + 1, centerY - 4, -8874241);
    }

    private String moduleGlyph(Module module) {
        if ("mini_map".equals(module.id())) {
            return "MAP";
        }
        if ("waypoints".equals(module.id())) {
            return "WP";
        }
        if ("brightness".equals(module.id())) {
            return "BR";
        }
        if ("auto_text".equals(module.id())) {
            return "TXT";
        }
        if ("zoom".equals(module.id())) {
            return "ZM";
        }
        if ("drop_alert".equals(module.id())) {
            return "DRP";
        }
        return switch (module.category()) {
            default -> throw new MatchException(null, null);
            case ModuleCategory.MAP -> "MAP";
            case ModuleCategory.EVENTS -> "EVT";
            case ModuleCategory.PROGRESSION -> "PRG";
            case ModuleCategory.PLAYERS -> "PLR";
            case ModuleCategory.COMFORT -> "CFT";
        };
    }

    private static int cardAnimY(long elapsed, int idx) {
        long delay = (long)idx * 35L;
        long dur = 220L;
        if (elapsed >= delay + dur) {
            return 0;
        }
        if (elapsed < delay) {
            return 14;
        }
        float t = (float)(elapsed - delay) / (float)dur;
        float ease = 1.0f - (1.0f - t) * (1.0f - t);
        return Math.round((1.0f - ease) * 14.0f);
    }

    private void drawCardBackground(class_332 ctx, CardLayout c, int mx, int my) {
        int x1 = c.x;
        int y1 = c.y;
        int x2 = x1 + 154;
        int y2 = y1 + 92;
        boolean hovered = mx >= x1 && mx < x2 && my >= y1 && my < y2;
        boolean enabled = this.moduleManager.isEnabled(c.module.id());
        int shadowOff = hovered ? 5 : 3;
        ctx.method_25294(x1 + 2, y1 + shadowOff, x2 + 2, y2 + shadowOff, hovered ? -1442840576 : 0x66000000);
        int bg = hovered ? -266526160 : -267053025;
        ctx.method_25294(x1 + 1, y1, x2 - 1, y2, bg);
        ctx.method_25294(x1, y1 + 1, x1 + 1, y2 - 1, bg);
        ctx.method_25294(x2 - 1, y1 + 1, x2, y2 - 1, bg);
        int stripe = enabled ? -8874241 : 1715817564;
        ctx.method_25294(x1 + 1, y1, x2 - 1, y1 + 3, stripe);
        ctx.method_25294(x1 + 1, y1, x2 - 1, y1 + 1, hovered ? 0x55FFFFFF : 0x33FFFFFF);
        ctx.method_25294(x1 + 1, y2 - 1, x2 - 1, y2, 0x55000000);
        ctx.method_25294(x1 + 6, y2 - 41, x2 - 6, y2 - 40, 1428103982);
        int border = hovered ? -8874241 : -14736594;
        ctx.method_25294(x1 + 1, y1, x2 - 1, y1 + 1, border);
        ctx.method_25294(x1 + 1, y2 - 1, x2 - 1, y2, border);
        ctx.method_25294(x1, y1 + 1, x1 + 1, y2 - 1, border);
        ctx.method_25294(x2 - 1, y1 + 1, x2, y2 - 1, border);
        if (hovered) {
            ctx.method_25294(x1, y1 - 1, x2, y1, 1148753663);
            ctx.method_25294(x1 - 1, y1, x1, y2, 1148753663);
            ctx.method_25294(x2, y1, x2 + 1, y2, 1148753663);
        }
    }

    private static String currentVersion() {
        try {
            return FabricLoader.getInstance().getModContainer("zombiezcompanion").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
        }
        catch (Throwable t) {
            return "?";
        }
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

    private static final class CardLayout {
        final Module module;
        final int x;
        final int y;
        final StyledButton optionsBtn;
        final StyledButton toggleBtn;

        CardLayout(Module module, int x, int y, StyledButton optionsBtn, StyledButton toggleBtn) {
            this.module = module;
            this.x = x;
            this.y = y;
            this.optionsBtn = optionsBtn;
            this.toggleBtn = toggleBtn;
        }
    }
}

