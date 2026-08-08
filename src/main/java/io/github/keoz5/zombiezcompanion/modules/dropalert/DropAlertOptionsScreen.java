package io.github.keoz5.zombiezcompanion.modules.dropalert;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.modules.dropalert.ConsumablesScreen;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropAlertModule;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropRarity;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

public final class DropAlertOptionsScreen
extends ModuleOptionsScreen {
    private final DropAlertModule moduleRef;

    public DropAlertOptionsScreen(Screen parent, DropAlertModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 42;
        int availableW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        int gap = 8;
        int colW = Math.max(60, (availableW - gap * 2) / 3);
        int rowH = 24;
        DropRarity[] rarities = DropRarity.values();
        int cols = 3;
        for (int i = 0; i < rarities.length; ++i) {
            DropRarity rarity = rarities[i];
            int col = i % cols;
            int row = i / cols;
            this.addToggle(x + col * (colW + gap), y + row * rowH, colW, rarity);
        }
        int rarityRows = (rarities.length + cols - 1) / cols;
        int filterY = y + rarityRows * rowH + 10;
        int half = (availableW - gap) / 2;
        this.addItemsToggle(x, filterY, availableW);
        this.addFoodToggle(x, filterY + 24, half);
        this.addGadgetsToggle(x + half + gap, filterY + 24, half);
        this.addConsumablesButton(x, filterY + 48, availableW);
        this.addMarkerStyleButton(x, filterY + 72, availableW);
        this.addCrossLink(x, filterY + 102, availableW, "stats", (Text)Text.translatable((String)"zombiezcompanion.crosslink.stats"));
    }

    private void addConsumablesButton(int x, int y, int w) {
        this.addDrawableChild(new StyledButton(x, y, w, 20, (Text)Text.translatable((String)"zombiezcompanion.drop_alert.consumables.open"), button -> {
            if (this.client != null) {
                this.client.setScreen((Screen)new ConsumablesScreen(this, this.configManager));
            }
        }, -266723542, -265932737, -854792));
    }

    private void addToggle(int x, int y, int w, DropRarity rarity) {
        this.addDrawableChild(new StyledButton(x, y, w, 20, this.toggleLabel(rarity), button -> {
            boolean next = !this.moduleRef.enabled(rarity);
            this.moduleRef.setEnabled(rarity, next);
            button.setMessage(this.toggleLabel(rarity));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, this.moduleRef.enabled(rarity) ? -14709924 : -12965328, this.moduleRef.enabled(rarity) ? -14179731 : -11716288, -854792));
    }

    private void addFoodToggle(int x, int y, int w) {
        this.addDrawableChild(new StyledButton(x, y, w, 20, this.foodToggleLabel(), button -> {
            boolean next = !this.moduleRef.foodEnabled();
            this.moduleRef.setFoodEnabled(next);
            button.setMessage(this.foodToggleLabel());
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, this.moduleRef.foodEnabled() ? -14709924 : -12965328, this.moduleRef.foodEnabled() ? -14179731 : -11716288, -854792));
    }

    private void addItemsToggle(int x, int y, int w) {
        this.addDrawableChild(new StyledButton(x, y, w, 20, this.itemsToggleLabel(), button -> {
            boolean next = !this.moduleRef.itemsEnabled();
            this.moduleRef.setItemsEnabled(next);
            button.setMessage(this.itemsToggleLabel());
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, this.moduleRef.itemsEnabled() ? -14709924 : -12965328, this.moduleRef.itemsEnabled() ? -14179731 : -11716288, -854792));
    }

    private void addGadgetsToggle(int x, int y, int w) {
        this.addDrawableChild(new StyledButton(x, y, w, 20, this.gadgetsToggleLabel(), button -> {
            boolean next = !this.moduleRef.gadgetsEnabled();
            this.moduleRef.setGadgetsEnabled(next);
            button.setMessage(this.gadgetsToggleLabel());
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, this.moduleRef.gadgetsEnabled() ? -14709924 : -12965328, this.moduleRef.gadgetsEnabled() ? -14179731 : -11716288, -854792));
    }

    private void addMarkerStyleButton(int x, int y, int w) {
        this.addDrawableChild(new StyledButton(x, y, w, 20, this.markerStyleLabel(), button -> {
            this.moduleRef.cycleMarkerStyle();
            button.setMessage(this.markerStyleLabel());
        }, -266723542, -265932737, -854792));
    }

    private Text toggleLabel(DropRarity rarity) {
        return Text.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{Text.translatable((String)("zombiezcompanion.drop_alert.rarity." + rarity.key)), Text.translatable((String)(this.moduleRef.enabled(rarity) ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private Text foodToggleLabel() {
        return Text.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{Text.translatable((String)"zombiezcompanion.drop_alert.food"), Text.translatable((String)(this.moduleRef.foodEnabled() ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private Text itemsToggleLabel() {
        return Text.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{Text.translatable((String)"zombiezcompanion.drop_alert.items"), Text.translatable((String)(this.moduleRef.itemsEnabled() ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private Text gadgetsToggleLabel() {
        return Text.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{Text.translatable((String)"zombiezcompanion.drop_alert.gadgets"), Text.translatable((String)(this.moduleRef.gadgetsEnabled() ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private Text markerStyleLabel() {
        String key = this.moduleRef.markerStyle() == 0 ? "zombiezcompanion.drop_alert.style.beacon" : "zombiezcompanion.drop_alert.style.arrow";
        return Text.translatable((String)"zombiezcompanion.drop_alert.style.label", (Object[])new Object[]{Text.translatable((String)key)});
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.drop_alert.options.header"), this.panelX1 + 36, this.contentY1 + 20, -854792);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.drop_alert.options.hint"), this.panelX1 + 36, this.contentY1 + 34, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(DrawContext ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 14;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = Math.max(0, Math.min(252, this.contentY2 - y - 6));
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.drawBorder(x, y, w, h, -14736594);
    }
}

