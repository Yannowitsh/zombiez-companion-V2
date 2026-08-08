package io.github.keoz5.zombiezcompanion.modules.minievents;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MiniEventsConfig;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.minievents.MiniEventsModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledSlider;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

public final class MiniEventsOptionsScreen
extends ModuleOptionsScreen {
    private final MiniEventsModule moduleRef;
    private int evY;
    private int timersY;
    private int rangeY;
    private int keybindY;

    public MiniEventsOptionsScreen(Screen parent, MiniEventsModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        int gap = 18;
        int half = Math.max(100, (optionW - gap) / 2);
        this.evY = this.contentY1 + 42;
        this.addToggle(x, this.evY, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.fuyeur"), half, () -> this.config().fuyeur, v -> {
            this.config().fuyeur = v;
        });
        this.addToggle(x + half + gap, this.evY, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.colis"), half, () -> this.config().colis, v -> {
            this.config().colis = v;
        });
        this.addToggle(x, this.evY + 26, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.faille"), half, () -> this.config().faille, v -> {
            this.config().faille = v;
        });
        this.addToggle(x + half + gap, this.evY + 26, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.pinata"), half, () -> this.config().pinata, v -> {
            this.config().pinata = v;
        });
        this.addToggle(x, this.evY + 52, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.bombe"), half, () -> this.config().bombe, v -> {
            this.config().bombe = v;
        });
        this.addToggle(x + half + gap, this.evY + 52, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.jackpot"), half, () -> this.config().jackpot, v -> {
            this.config().jackpot = v;
        });
        this.addToggle(x, this.evY + 78, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.marchand"), half, () -> this.config().marchand, v -> {
            this.config().marchand = v;
        });
        this.addToggle(x + half + gap, this.evY + 78, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.assaut"), half, () -> this.config().assaut, v -> {
            this.config().assaut = v;
        });
        this.addToggle(x, this.evY + 104, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.world_boss"), half, () -> this.config().worldBoss, v -> {
            this.config().worldBoss = v;
        });
        this.addDrawableChild(new StyledSlider(x, this.evY + 132, optionW, 22, this.config().detectionRange, 32.0, 100.0, v -> {
            this.config().detectionRange = (int)Math.round(v);
        }, v -> Text.translatable((String)"zombiezcompanion.mini_events.slider.range", (Object[])new Object[]{(int)Math.round(v)})));
        this.rangeY = this.evY + 130;
        this.timersY = this.evY + 166;
        this.addToggle(x, this.timersY + 14, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.marchand_timer"), half, () -> this.config().marchandTimer, v -> {
            this.config().marchandTimer = v;
        });
        this.addToggle(x + half + gap, this.timersY + 14, (Text)Text.translatable((String)"zombiezcompanion.mini_events.toggle.world_boss_timer"), half, () -> this.config().worldBossTimer, v -> {
            this.config().worldBossTimer = v;
        });
        this.keybindY = this.timersY + 56;
        this.addKeybindRow(x, this.keybindY + 14, optionW, Keybinds.tpRefuge(), (Text)Text.translatable((String)"key.zombiezcompanion.tp_refuge"));
    }

    private void addToggle(int x, int y, Text label, int w, BoolGetter getter, BoolSetter setter) {
        this.addDrawableChild(new StyledButton(x, y, w, 20, MiniEventsOptionsScreen.toggleLabel(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.set(next);
            button.setMessage(MiniEventsOptionsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, getter.get() ? -14709924 : -12965328, getter.get() ? -14179731 : -11716288, -854792));
    }

    private static Text toggleLabel(Text label, boolean enabled) {
        return Text.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, Text.translatable((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private MiniEventsConfig config() {
        return this.moduleRef.config();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = this.panelX1 + 36;
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.mini_events.options.header"), x, this.contentY1 + 12, -854792);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.mini_events.section.events"), x, this.evY - 14, -8874241, false);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.mini_events.section.timers"), x, this.timersY, -8874241, false);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.mini_events.options.hint"), x, this.timersY + 40, -8353376, false);
        ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.mini_events.section.keybind"), x, this.keybindY, -8874241, false);
    }

    @Override
    protected void renderOptionsBackground(DrawContext ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 296;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        int divY = this.timersY - 8;
        ctx.fill(x + 12, divY, x + w - 12, divY + 1, -14736594);
        int divY2 = this.keybindY - 8;
        ctx.fill(x + 12, divY2, x + w - 12, divY2 + 1, -14736594);
        ctx.drawBorder(x, y, w, h, -14736594);
    }

    private static interface BoolGetter {
        public boolean get();
    }

    private static interface BoolSetter {
        public void set(boolean var1);
    }
}

