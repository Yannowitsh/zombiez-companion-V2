package io.github.keoz5.zombiezcompanion.modules.map;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MapConfig;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.map.MiniMapModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledSlider;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

public final class MiniMapOptionsScreen
extends ModuleOptionsScreen {
    private final MiniMapModule moduleRef;

    public MiniMapOptionsScreen(Screen parent, MiniMapModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 30;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        this.addDrawableChild(new StyledSlider(x, y, optionW, 22, this.config().miniMapSize, 80.0, 260.0, value -> {
            this.config().miniMapSize = (int)Math.round(value);
        }, value -> Text.translatable((String)"zombiezcompanion.minimap.slider.size", (Object[])new Object[]{(int)Math.round(value)})));
        this.addDrawableChild(new StyledSlider(x, y + 28, optionW, 22, this.config().miniMapPeekSize, 120.0, 400.0, value -> {
            this.config().miniMapPeekSize = (int)Math.round(value);
        }, value -> Text.translatable((String)"zombiezcompanion.minimap.slider.peek_size", (Object[])new Object[]{(int)Math.round(value)})));
        this.addDrawableChild(new StyledSlider(x, y + 56, optionW, 22, this.config().miniMapZoom, 0.25, 2.0, value -> {
            this.config().miniMapZoom = (double)Math.round(value * 100.0) / 100.0;
        }, value -> Text.translatable((String)"zombiezcompanion.minimap.slider.zoom", (Object[])new Object[]{Math.round(value * 100.0)})));
        this.addDrawableChild(new StyledButton(x, y + 88, optionW, 20, this.cornerLabel(), button -> {
            this.config().miniMapCorner = (this.config().miniMapCorner + 1) % 4;
            button.setMessage(this.cornerLabel());
        }, -266723542, -265932737, -854792));
        this.addDrawableChild(new StyledSlider(x, y + 114, optionW, 22, this.config().miniMapOffsetY, -200.0, 200.0, value -> {
            this.config().miniMapOffsetY = (int)Math.round(value);
        }, value -> Text.translatable((String)"zombiezcompanion.minimap.slider.offset_y", (Object[])new Object[]{(int)Math.round(value)})));
        this.addKeybindRow(x, y + 146, optionW, Keybinds.openMap(), (Text)Text.translatable((String)"key.zombiezcompanion.open_map"));
        this.addKeybindRow(x, y + 172, optionW, this.moduleRef.peekKey(), (Text)Text.translatable((String)"key.zombiezcompanion.minimap_peek"));
        this.addCrossLink(x, y + 202, optionW, "waypoints", (Text)Text.translatable((String)"zombiezcompanion.crosslink.waypoints"));
    }

    private Text cornerLabel() {
        String key = switch (this.config().miniMapCorner) {
            case 0 -> "zombiezcompanion.minimap.corner.top_left";
            case 2 -> "zombiezcompanion.minimap.corner.bottom_left";
            case 3 -> "zombiezcompanion.minimap.corner.bottom_right";
            default -> "zombiezcompanion.minimap.corner.top_right";
        };
        return Text.translatable((String)"zombiezcompanion.minimap.corner.label", (Object[])new Object[]{Text.translatable((String)key)});
    }

    private MapConfig config() {
        return this.moduleRef.config();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.minimap.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
    }

    @Override
    protected void renderOptionsBackground(DrawContext ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 236;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.drawBorder(x, y, w, h, -14736594);
    }
}

