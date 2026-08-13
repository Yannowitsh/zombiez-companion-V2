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
    // Y of the spawn-stats blocks (drawn in render), or -1 when the "events" section is collapsed.
    private int spawnBlockY = -1;

    public MiniEventsOptionsScreen(Screen parent, MiniEventsModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 24;
        int optionW = this.panelX2 - this.panelX1 - 48;
        int gap = 18;
        int half = Math.max(100, (optionW - gap) / 2);
        int rx = x + half + gap;
        int y = this.contentY1 + 8;
        this.spawnBlockY = -1;

        // --- Événements ---
        y = this.sectionHeader("events", (Component)Component.translatable((String)"zombiezcompanion.mini_events.section.events"), y);
        if (this.sectionExpanded("events")) {
            this.addToggle(x, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.fuyeur"), half, () -> this.config().fuyeur, v -> this.config().fuyeur = v);
            this.addToggle(rx, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.colis"), half, () -> this.config().colis, v -> this.config().colis = v);
            y += 24;
            this.addToggle(x, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.faille"), half, () -> this.config().faille, v -> this.config().faille = v);
            this.addToggle(rx, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.pinata"), half, () -> this.config().pinata, v -> this.config().pinata = v);
            y += 24;
            this.addToggle(x, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.bombe"), half, () -> this.config().bombe, v -> this.config().bombe = v);
            this.addToggle(rx, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.jackpot"), half, () -> this.config().jackpot, v -> this.config().jackpot = v);
            y += 24;
            this.addToggle(x, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.marchand"), half, () -> this.config().marchand, v -> this.config().marchand = v);
            this.addToggle(rx, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.assaut"), half, () -> this.config().assaut, v -> this.config().assaut = v);
            y += 24;
            this.addToggle(x, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.world_boss"), half, () -> this.config().worldBoss, v -> this.config().worldBoss = v);
            y += 26;
            this.addRenderableWidget(new StyledSlider(x, y, optionW, 22, this.config().detectionRange, 32.0, 100.0, v -> {
                this.config().detectionRange = (int)Math.round(v);
            }, v -> Component.translatable((String)"zombiezcompanion.mini_events.slider.range", (Object[])new Object[]{(int)Math.round(v)})));
            y += 28;
            this.spawnBlockY = y;
            y += 92;
        }
        y += 4;

        // --- Timers ---
        y = this.sectionHeader("timers", (Component)Component.translatable((String)"zombiezcompanion.mini_events.section.timers"), y);
        if (this.sectionExpanded("timers")) {
            this.addToggle(x, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.marchand_timer"), half, () -> this.config().marchandTimer, v -> this.config().marchandTimer = v);
            this.addToggle(rx, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.world_boss_timer"), half, () -> this.config().worldBossTimer, v -> this.config().worldBossTimer = v);
            y += 24;
            this.addToggle(x, y, (Component)Component.translatable((String)"zombiezcompanion.mini_events.toggle.monarch_timer"), half, () -> this.config().monarchTimer, v -> this.config().monarchTimer = v);
            y += 24;
        }
        y += 4;

        // --- Sons ---
        y = this.sectionHeader("sound", (Component)Component.translatable((String)"zombiezcompanion.mini_events.section.sound"), y);
        if (this.sectionExpanded("sound")) {
            this.addSoundButton(x, y, optionW, "zombiezcompanion.mini_events.sound.world_boss", () -> this.config().worldBossSoundId, v -> this.config().worldBossSoundId = v);
            y += 24;
            this.addSoundButton(x, y, optionW, "zombiezcompanion.mini_events.sound.marchand", () -> this.config().marchandSoundId, v -> this.config().marchandSoundId = v);
            y += 24;
            this.addSoundButton(x, y, optionW, "zombiezcompanion.mini_events.sound.monarch", () -> this.config().monarchSoundId, v -> this.config().monarchSoundId = v);
            y += 24;
            this.addRenderableWidget(new StyledSlider(x, y, optionW, 22, this.config().spawnSoundVolume, 0.0, 100.0, v -> {
                this.config().spawnSoundVolume = (int)Math.round(v);
            }, v -> Component.translatable((String)"zombiezcompanion.mini_events.slider.sound_volume", (Object[])new Object[]{(int)Math.round(v)})));
            y += 28;
        }
        y += 4;

        // --- Raccourci ---
        y = this.sectionHeader("keybind", (Component)Component.translatable((String)"zombiezcompanion.mini_events.section.keybind"), y);
        if (this.sectionExpanded("keybind")) {
            this.addKeybindRow(x, y, optionW, Keybinds.tpRefuge(), (Component)Component.translatable((String)"key.zombiezcompanion.tp_refuge"));
        }
    }

    private static Component soundLabel(String prefixKey, String id) {
        String sound = id == null || id.isEmpty()
            ? Component.translatable((String)"zombiezcompanion.mini_events.sound.none").getString()
            : SpawnSounds.label(id);
        return (Component)Component.literal((String)(Component.translatable((String)prefixKey).getString() + " — " + sound));
    }

    private void addSoundButton(int x, int y, int w, String prefixKey, java.util.function.Supplier<String> getter, java.util.function.Consumer<String> setter) {
        this.addRenderableWidget(new StyledButton(x, y, w, 20, MiniEventsOptionsScreen.soundLabel(prefixKey, getter.get()), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen((Screen)new SoundPickerScreen(this, getter.get(), setter, (float)this.config().spawnSoundVolume / 100.0f));
            }
        }, -266723542, -265932737, -854792));
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
    private void drawSpawnBlock(GuiGraphicsExtractor ctx, int bx, int baseY, int w, boolean boss) {
        String labelKey = boss ? "zombiezcompanion.mini_events.next.world_boss" : "zombiezcompanion.mini_events.next.marchand";
        ctx.text(this.font, (Component)Component.translatable((String)labelKey, (Object[])new Object[]{this.nextValue(boss)}), bx, baseY, -8353376, false);
        MiniEventsModule.drawSpawnHistogram(ctx, bx, baseY + 12, w, 28, this.moduleRef.spawnHistory(boss), this.moduleRef.accentColor(boss));
        String med = Component.translatable((String)"zombiezcompanion.mini_events.median").getString();
        String sub = this.moduleRef.intervalRangeText(boss) + "  ·  " + med + " " + this.moduleRef.medianText(boss);
        ctx.text(this.font, (Component)Component.literal((String)sub), bx, baseY + 44, -8355712, false);
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
        // Spawn-stats blocks: drawn just below the range slider, only while the "events" section is expanded.
        if (this.spawnBlockY >= 0) {
            int x = this.panelX1 + 24;
            int gap = 18;
            int optionW = this.panelX2 - this.panelX1 - 48;
            int half = Math.max(100, (optionW - gap) / 2);
            int rx = x + half + gap;
            this.drawSpawnBlock(ctx, x, this.spawnBlockY, half, false);
            this.drawSpawnBlock(ctx, rx, this.spawnBlockY, half, true);
        }
    }

    @Override
    protected void renderOptionsBackground(GuiGraphicsExtractor ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 4;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = this.contentY2 - y - 8;
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

