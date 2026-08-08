package io.github.keoz5.zombiezcompanion.modules.stats;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.stats.StatsModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.StatsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

public final class StatsOptionsScreen
extends ModuleOptionsScreen {
    public StatsOptionsScreen(Screen parent, StatsModule module, ConfigManager configManager) {
        super(parent, module, configManager);
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 30;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        this.addDrawableChild(new StyledButton(x, y, optionW, 22, (Text)Text.translatable((String)"zombiezcompanion.stats.open"), btn -> MinecraftClient.getInstance().setScreen((Screen)new StatsScreen(this, this.configManager)), -11441921, -8874241, -854792));
        this.addKeybindRow(x, y + 36, optionW, Keybinds.openStats(), (Text)Text.translatable((String)"key.zombiezcompanion.open_stats"));
        this.addCrossLink(x, y + 96, optionW, "drop_alert", (Text)Text.translatable((String)"zombiezcompanion.crosslink.drop_alert"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.stats.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.stats.options.hint"), this.panelX1 + 36, this.contentY1 + 100, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(DrawContext ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 148;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        int divY = this.contentY1 + 30 + 88;
        ctx.fill(x + 12, divY, x + w - 12, divY + 1, -14736594);
        ctx.drawBorder(x, y, w, h, -14736594);
    }
}

