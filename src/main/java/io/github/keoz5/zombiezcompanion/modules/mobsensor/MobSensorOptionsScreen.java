package io.github.keoz5.zombiezcompanion.modules.mobsensor;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MobSensorConfig;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledSlider;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MobSensorOptionsScreen
extends ModuleOptionsScreen {
    private final MobSensorModule moduleRef;
    private int optY;
    private int rangeY;
    private int slotsY;

    public MobSensorOptionsScreen(Screen parent, MobSensorModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        int gap = 18;
        int half = Math.max(100, (optionW - gap) / 2);
        this.optY = this.contentY1 + 42;
        this.addToggle(x, this.optY, (Component)Component.translatable((String)"zombiezcompanion.mob_sensor.toggle.outline"), half, () -> this.config().outline, v -> {
            this.config().outline = v;
        });
        this.addToggle(x + half + gap, this.optY, (Component)Component.translatable((String)"zombiezcompanion.mob_sensor.toggle.hud"), half, () -> this.config().hud, v -> {
            this.config().hud = v;
        });
        this.rangeY = this.optY + 32;
        this.addRenderableWidget(new StyledSlider(x, this.rangeY + 14, optionW, 22, this.config().detectionRange, 32.0, 100.0, v -> {
            this.config().detectionRange = (int)Math.round(v);
        }, v -> Component.translatable((String)"zombiezcompanion.mob_sensor.slider.range", (Object[])new Object[]{(int)Math.round(v)})));
        this.slotsY = this.rangeY + 44;
        List<MobSensorConfig.Track> tracks = this.config().tracks;
        int toggleW = 92;
        int boxW = Math.max(80, optionW - toggleW - gap);
        for (int i = 0; i < MobSensorConfig.SLOTS && i < tracks.size(); ++i) {
            int y = this.slotsY + 14 + i * 24;
            MobSensorConfig.Track track = tracks.get(i);
            EditBox box = new EditBox(this.font, x, y, boxW, 18, (Component)Component.translatable((String)"zombiezcompanion.mob_sensor.slot.hint"));
            box.setMaxLength(32);
            box.setValue(track.query == null ? "" : track.query);
            box.setResponder(s -> {
                track.query = s;
            });
            this.addRenderableWidget(box);
            this.addToggle(x + boxW + gap, y, (Component)Component.translatable((String)"zombiezcompanion.mob_sensor.slot.enable"), toggleW, () -> track.enabled, v -> {
                track.enabled = v;
            });
        }
    }

    private void addToggle(int x, int y, Component label, int w, BoolGetter getter, BoolSetter setter) {
        this.addRenderableWidget(new StyledButton(x, y, w, 20, MobSensorOptionsScreen.toggleLabel(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.set(next);
            button.setMessage(MobSensorOptionsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, getter.get() ? -14709924 : -12965328, getter.get() ? -14179731 : -11716288, -854792));
    }

    private static Component toggleLabel(Component label, boolean enabled) {
        return Component.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, Component.translatable((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private MobSensorConfig config() {
        return this.moduleRef.config();
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
        int x = this.panelX1 + 36;
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.mob_sensor.options.header"), x, this.contentY1 + 12, -854792);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.mob_sensor.section.display"), x, this.optY - 14, -8874241, false);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.mob_sensor.section.range"), x, this.rangeY, -8874241, false);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.mob_sensor.section.slots"), x, this.slotsY, -8874241, false);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.mob_sensor.options.hint"), x, this.slotsY + 14 + MobSensorConfig.SLOTS * 24, -8353376, false);
    }

    @Override
    protected void renderOptionsBackground(GuiGraphicsExtractor ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 262;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        int divY = this.rangeY - 8;
        ctx.fill(x + 12, divY, x + w - 12, divY + 1, -14736594);
        int divY2 = this.slotsY - 8;
        ctx.fill(x + 12, divY2, x + w - 12, divY2 + 1, -14736594);
        ctx.outline(x, y, w, h, -14736594);
    }

    private static interface BoolGetter {
        public boolean get();
    }

    private static interface BoolSetter {
        public void set(boolean var1);
    }
}
