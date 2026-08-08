package io.github.keoz5.zombiezcompanion.modules.coordinates;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.CoordinatesConfig;
import io.github.keoz5.zombiezcompanion.modules.coordinates.CoordinatesModule;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;

public final class CoordinatesOptionsScreen
extends ModuleOptionsScreen {
    private final CoordinatesModule moduleRef;

    public CoordinatesOptionsScreen(Screen parent, CoordinatesModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    @Override
    protected void initOptions() {
        int x = this.panelX1 + 36;
        int y = this.contentY1 + 30;
        int optionW = Math.max(220, this.panelX2 - this.panelX1 - 72);
        this.addDrawableChild(new StyledButton(x, y, optionW, 22, this.cornerLabel(), btn -> {
            this.config().corner = (this.config().corner + 1) % 4;
            this.configManager.save();
            btn.setMessage(this.cornerLabel());
        }, -266723542, -265932737, -854792));
        this.addToggle(x, y + 30, optionW, (Text)Text.translatable((String)"zombiezcompanion.coordinates.toggle.show_y"), () -> this.config().showY, v -> {
            this.config().showY = v;
        });
        this.addToggle(x, y + 58, optionW, (Text)Text.translatable((String)"zombiezcompanion.coordinates.toggle.show_facing"), () -> this.config().showFacing, v -> {
            this.config().showFacing = v;
        });
        this.addToggle(x, y + 86, optionW, (Text)Text.translatable((String)"zombiezcompanion.coordinates.toggle.background"), () -> this.config().background, v -> {
            this.config().background = v;
        });
        this.addToggle(x, y + 114, optionW, (Text)Text.translatable((String)"zombiezcompanion.coordinates.toggle.everywhere"), () -> this.config().everywhere, v -> {
            this.config().everywhere = v;
        });
    }

    private Text cornerLabel() {
        String key = switch (this.config().corner) {
            case 1 -> "zombiezcompanion.coordinates.corner.top_right";
            case 2 -> "zombiezcompanion.coordinates.corner.bottom_left";
            case 3 -> "zombiezcompanion.coordinates.corner.bottom_right";
            default -> "zombiezcompanion.coordinates.corner.top_left";
        };
        return Text.translatable((String)"zombiezcompanion.coordinates.corner.label", (Object[])new Object[]{Text.translatable((String)key)});
    }

    private void addToggle(int x, int y, int w, Text label, BoolGetter getter, BoolSetter setter) {
        this.addDrawableChild(new StyledButton(x, y, w, 22, CoordinatesOptionsScreen.toggleLabel(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.set(next);
            this.configManager.save();
            button.setMessage(CoordinatesOptionsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? -14709924 : -12965328, next ? -14179731 : -11716288);
        }, getter.get() ? -14709924 : -12965328, getter.get() ? -14179731 : -11716288, -854792));
    }

    private static Text toggleLabel(Text label, boolean enabled) {
        return Text.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, Text.translatable((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private CoordinatesConfig config() {
        return this.moduleRef.config();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.coordinates.options.header"), this.panelX1 + 36, this.contentY1 + 12, -854792);
    }

    @Override
    protected void renderOptionsBackground(DrawContext ctx) {
        int x = this.panelX1 + 24;
        int y = this.contentY1 + 18;
        int w = this.panelX2 - this.panelX1 - 48;
        int h = 158;
        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, -1442840576);
        ctx.fill(x, y, x + w, y + h, -267053025);
        ctx.fill(x, y, x + w, y + 2, -8874241);
        ctx.drawBorder(x, y, w, h, -14736594);
    }

    private static interface BoolGetter {
        public boolean get();
    }

    private static interface BoolSetter {
        public void set(boolean var1);
    }
}

