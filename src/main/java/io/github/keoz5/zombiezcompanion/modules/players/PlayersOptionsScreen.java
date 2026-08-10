package io.github.keoz5.zombiezcompanion.modules.players;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.PlayersConfig;
import io.github.keoz5.zombiezcompanion.modules.players.PlayersModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PlayersOptionsScreen
extends ModuleOptionsScreen {
    private final PlayersModule moduleRef;

    public PlayersOptionsScreen(Screen parent, PlayersModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 30;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        this.addToggle(x, y, optionW, (Component)Component.translatable((String)"zombiezcompanion.players.toggle.broadcast"), () -> this.config().broadcastPosition, v -> {
            this.config().broadcastPosition = v;
        });
        this.addToggle(x, y + 28, optionW, (Component)Component.translatable((String)"zombiezcompanion.players.toggle.show_names"), () -> this.config().showNames, v -> {
            this.config().showNames = v;
        });
        this.addToggle(x, y + 56, optionW, (Component)Component.translatable((String)"zombiezcompanion.players.toggle.show_coords"), () -> this.config().showCoords, v -> {
            this.config().showCoords = v;
        });
        this.addToggle(x, y + 84, optionW, (Component)Component.translatable((String)"zombiezcompanion.players.toggle.mod_user_nametag"), () -> this.config().modUserNametag, v -> {
            this.config().modUserNametag = v;
        });
    }

    private void addToggle(int x, int y, int w, Component label, BoolGetter getter, BoolSetter setter) {
        this.addRenderableWidget(new StyledButton(x, y, w, 22, PlayersOptionsScreen.toggleLabel(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.set(next);
            this.configManager.save();
            button.setMessage(PlayersOptionsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, getter.get() ? -14709924 : -12965328, getter.get() ? -14179731 : -11716288, -854792));
    }

    private static Component toggleLabel(Component label, boolean enabled) {
        return Component.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, Component.translatable((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private PlayersConfig config() {
        return this.moduleRef.config();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.players.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.players.options.hint"), this.panelX1 + 36, this.contentY1 + 150, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(GuiGraphicsExtractor ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 158;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.outline(x, y, w, h, -14736594);
    }

    private static interface BoolGetter {
        public boolean get();
    }

    private static interface BoolSetter {
        public void set(boolean var1);
    }
}

