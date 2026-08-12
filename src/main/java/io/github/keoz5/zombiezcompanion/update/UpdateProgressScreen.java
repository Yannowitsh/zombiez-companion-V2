package io.github.keoz5.zombiezcompanion.update;

import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.nio.file.Path;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Downloads the update jar and shows the result, with buttons to open the folder or quit the game. */
public final class UpdateProgressScreen
extends Screen {
    private final Screen parent;
    private boolean started;
    private boolean done;
    private String error;
    private Path result;

    public UpdateProgressScreen(Screen parent) {
        super((Component)Component.translatable((String)"zombiezcompanion.update.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!this.started) {
            this.started = true;
            UpdateChecker.downloadAsync((path, err) -> Minecraft.getInstance().execute(() -> {
                this.result = path;
                this.error = err;
                this.done = true;
                this.rebuildWidgets();
            }));
        }
        int cx = this.width / 2;
        int y = this.height / 2 + 24;
        if (!this.done) {
            return;
        }
        if (this.error != null) {
            this.addRenderableWidget(new StyledButton(cx - 125, y, 120, 20, (Component)Component.translatable((String)"zombiezcompanion.update.btn.retry"), b -> {
                this.done = false;
                this.started = false;
                this.error = null;
                this.result = null;
                this.rebuildWidgets();
            }, -266723542, -265932737, -854792));
            this.addRenderableWidget(new StyledButton(cx + 5, y, 120, 20, (Component)Component.translatable((String)"zombiezcompanion.button.back"), b -> this.back(), -266723542, -265932737, -854792));
            return;
        }
        this.addRenderableWidget(new StyledButton(cx - 185, y, 120, 20, (Component)Component.translatable((String)"zombiezcompanion.update.btn.open_folder"), b -> Util.getPlatform().openUri(UpdateChecker.updateDir().toUri()), -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(cx - 60, y, 120, 20, (Component)Component.translatable((String)"zombiezcompanion.update.btn.quit"), b -> {
            if (this.minecraft != null) {
                this.minecraft.stop();
            }
        }, -14709924, -14179731, -854792));
        this.addRenderableWidget(new StyledButton(cx + 65, y, 120, 20, (Component)Component.translatable((String)"zombiezcompanion.button.back"), b -> this.back(), -266723542, -265932737, -854792));
    }

    private void back() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    //? if >= 26.1 {
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
        //? if >= 26.1 {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        //?} else {
        /*super.render(ctx, mouseX, mouseY, delta);
        *///?}
        int cx = this.width / 2;
        int cy = this.height / 2;
        this.centered(ctx, (Component)Component.translatable((String)"zombiezcompanion.update.screen.title"), cx, cy - 40, -854792);
        Component status = this.done
            ? (this.error != null
                ? Component.translatable((String)"zombiezcompanion.update.screen.error", (Object[])new Object[]{this.error})
                : Component.translatable((String)"zombiezcompanion.update.screen.done"))
            : Component.translatable((String)"zombiezcompanion.update.screen.downloading");
        this.centered(ctx, status, cx, cy - 16, this.error != null ? -43691 : -3355444);
        if (this.done && this.error == null && this.result != null) {
            this.centered(ctx, (Component)Component.literal((String)this.result.getFileName().toString()), cx, cy, -8355712);
            this.centered(ctx, (Component)Component.translatable((String)"zombiezcompanion.update.screen.instructions"), cx, cy + 14, -8353376);
        }
    }

    private void centered(GuiGraphicsExtractor ctx, Component text, int cx, int y, int color) {
        int w = this.font.width((net.minecraft.network.chat.FormattedText)text);
        ctx.text(this.font, text, cx - w / 2, y, color, false);
    }

    public boolean isPauseScreen() {
        return true;
    }
}
