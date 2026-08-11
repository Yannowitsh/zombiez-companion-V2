package io.github.keoz5.zombiezcompanion.modules.minievents;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.MiniEventsConfig;
import io.github.keoz5.zombiezcompanion.keybind.Keybinds;
import io.github.keoz5.zombiezcompanion.modules.minievents.MiniEventsModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledSlider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        this.addToggle(x, this.evY, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.fuyeur"), half, () -> this.config().fuyeur, v -> {
            this.config().fuyeur = v;
        });
        this.addToggle(x + half + gap, this.evY, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.colis"), half, () -> this.config().colis, v -> {
            this.config().colis = v;
        });
        this.addToggle(x, this.evY + 26, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.faille"), half, () -> this.config().faille, v -> {
            this.config().faille = v;
        });
        this.addToggle(x + half + gap, this.evY + 26, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.pinata"), half, () -> this.config().pinata, v -> {
            this.config().pinata = v;
        });
        this.addToggle(x, this.evY + 52, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.bombe"), half, () -> this.config().bombe, v -> {
            this.config().bombe = v;
        });
        this.addToggle(x + half + gap, this.evY + 52, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.jackpot"), half, () -> this.config().jackpot, v -> {
            this.config().jackpot = v;
        });
        this.addToggle(x, this.evY + 78, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.marchand"), half, () -> this.config().marchand, v -> {
            this.config().marchand = v;
        });
        this.addToggle(x + half + gap, this.evY + 78, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.assaut"), half, () -> this.config().assaut, v -> {
            this.config().assaut = v;
        });
        this.addToggle(x, this.evY + 104, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.world_boss"), half, () -> this.config().worldBoss, v -> {
            this.config().worldBoss = v;
        });
        this.addRenderableWidget(new StyledSlider(x, this.evY + 132, optionW, 22, this.config().detectionRange, 32.0, 100.0, v -> {
            this.config().detectionRange = (int)Math.round(v);
        }, v -> Component.translatable((String)"zombiezcompanion.mini_events.slider.range", (Object[])new Object[]{(int)Math.round(v)})));
        this.rangeY = this.evY + 130;
        this.timersY = this.evY + 166;
        this.addToggle(x, this.timersY + 14, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.marchand_timer"), half, () -> this.config().marchandTimer, v -> {
            this.config().marchandTimer = v;
        });
        this.addToggle(x + half + gap, this.timersY + 14, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.world_boss_timer"), half, () -> this.config().worldBossTimer, v -> {
            this.config().worldBossTimer = v;
        });
        this.keybindY = this.timersY + 100;
        this.addKeybindRow(x, this.keybindY + 14, optionW, Keybinds.tpRefuge(), (Component)Component.translatable((String)"key.zombiezcompanion.tp_refuge"));
    }

    private void addToggle(int x, int y, Component label, int w, BoolGetter getter, BoolSetter setter) {
        this.addRenderableWidget(new StyledButton(x, y, w, 20, MiniEventsOptionsScreen.toggleLabel(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.set(next);
            button.setMessage(MiniEventsOptionsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, getter.get() ? -14709924 : -12965328, getter.get() ? -14179731 : -11716288, -854792));
    }

    private static Component toggleLabel(Component label, boolean enabled) {
        return Component.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, Component.translatable((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private MiniEventsConfig config() {
        return this.moduleRef.config();
    }

    /** One event's stats block in the menu: probable next spawn, distribution histogram, range + median. */
    private void drawSpawnBlock(GuiGraphicsExtractor ctx, int bx, int w, boolean boss) {
        String labelKey = boss ? "zombiezcompanion.mini_events.next.world_boss" : "zombiezcompanion.mini_events.next.marchand";
        ctx.text(this.font, (Component)Component.translatable((String)labelKey, (Object[])new Object[]{this.nextValue(boss)}), bx, this.timersY + 40, -8353376, false);
        MiniEventsModule.drawSpawnHistogram(ctx, bx, this.timersY + 52, w, 28, this.moduleRef.spawnHistory(boss), this.moduleRef.accentColor(boss));
        String med = Component.translatable((String)"zombiezcompanion.mini_events.median").getString();
        String sub = this.moduleRef.intervalRangeText(boss) + "  ·  " + med + " " + this.moduleRef.medianText(boss);
        ctx.text(this.font, (Component)Component.literal((String)sub), bx, this.timersY + 84, -8355712, false);
    }

    /** "dans ~Xm" / "en retard ~Xm" / "pas assez de données" for the probable next spawn. */
    private Component nextValue(boolean boss) {
        Long rem = this.moduleRef.nextProbableRemainingMs(boss);
        if (rem == null) {
            return Component.translatable((String)"zombiezcompanion.mini_events.next.nodata");
        }
        int m = (int)Math.round((double)Math.abs(rem.longValue()) / 60000.0);
        return rem.longValue() > 0L
            ? Component.translatable((String)"zombiezcompanion.mini_events.next.in", (Object[])new Object[]{m})
            : Component.translatable((String)"zombiezcompanion.mini_events.next.overdue", (Object[])new Object[]{m});
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        int x = this.panelX1 + 36;
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.mini_events.options.header"), x, this.contentY1 + 12, -854792);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.mini_events.section.events"), x, this.evY - 14, -8874241, false);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.mini_events.section.timers"), x, this.timersY, -8874241, false);
        int gap = 18;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        int half = Math.max(100, (optionW - gap) / 2);
        int rx = x + half + gap;
        this.drawSpawnBlock(ctx, x, half, false);
        this.drawSpawnBlock(ctx, rx, half, true);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.mini_events.section.keybind"), x, this.keybindY, -8874241, false);
    }

    @Override
    protected void renderOptionsBackground(GuiGraphicsExtractor ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 330;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        int divY = this.timersY - 8;
        ctx.fill(x + 12, divY, x + w - 12, divY + 1, -14736594);
        int divY2 = this.keybindY - 8;
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

