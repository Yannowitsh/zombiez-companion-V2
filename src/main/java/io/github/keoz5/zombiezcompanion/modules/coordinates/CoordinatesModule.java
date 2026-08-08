package io.github.keoz5.zombiezcompanion.modules.coordinates;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.CoordinatesConfig;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.hud.HudAnchor;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.modules.coordinates.CoordinatesOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import java.util.List;
import java.util.Locale;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public final class CoordinatesModule
implements Module {
    public static final String ID = "coordinates";
    private ConfigManager configManager;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Coordonn\u00e9es";
    }

    @Override
    public String description() {
        return Text.translatable((String)"zombiezcompanion.module.coordinates.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.MAP;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("coordonn\u00e9es", "coords", "xyz", "position", "orientation", "boussole", "facing", "partout");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new CoordinatesOptionsScreen(parent, this, this.configManager);
    }

    public CoordinatesConfig config() {
        return this.configManager.get().coordinates;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        int boxY;
        int boxX;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (client.options.hudHidden) {
            return;
        }
        if (client.currentScreen != null) {
            return;
        }
        CoordinatesConfig cfg = this.config();
        if (!cfg.everywhere && !ZombieZDetector.isOnZombieZ()) {
            return;
        }
        TextRenderer tr = client.textRenderer;
        int x = (int)Math.round(client.player.getX());
        int y = (int)Math.round(client.player.getY());
        int z = (int)Math.round(client.player.getZ());
        String coords = cfg.showY ? String.format(Locale.ROOT, "X %d  Y %d  Z %d", x, y, z) : String.format(Locale.ROOT, "X %d  Z %d", x, z);
        String facing = CoordinatesModule.facingLabel(client.player.getYaw());
        int lineH = 10;
        int width = tr.getWidth(coords);
        if (cfg.showFacing) {
            width = Math.max(width, tr.getWidth(facing));
        }
        int padding = 4;
        int boxW = width + padding * 2;
        int boxH = (cfg.showFacing ? lineH * 2 + 2 : lineH) + padding * 2;
        int screenW = ctx.getScaledWindowWidth();
        int screenH = ctx.getScaledWindowHeight();
        HudConfig hud = this.configManager.get().hud;
        double scale = HudAnchor.scale(hud, ID);
        int scaledW = (int)Math.round((double)boxW * scale);
        int scaledH = (int)Math.round((double)boxH * scale);
        if (HudAnchor.hasCustom(hud, ID)) {
            boxX = HudAnchor.resolveX(hud, ID, screenW, scaledW, 0.0);
            boxY = HudAnchor.resolveY(hud, ID, screenH, scaledH, 0.0);
        } else {
            int margin = 4;
            boxX = switch (cfg.corner) {
                case 1, 3 -> screenW - scaledW - margin;
                default -> margin;
            };
            boxY = switch (cfg.corner) {
                case 2, 3 -> screenH - scaledH - margin;
                default -> margin;
            };
        }
        ctx.getMatrices().push();
        ctx.getMatrices().translate((float)boxX, (float)boxY, 0.0f);
        if (scale != 1.0) {
            ctx.getMatrices().scale((float)scale, (float)scale, 1.0f);
        }
        if (cfg.background) {
            ctx.fill(0, 0, boxW, boxH, -1442840576);
            ctx.fill(0, 0, boxW, 1, -8874241);
        }
        ctx.drawTextWithShadow(tr, (Text)Text.literal((String)coords), padding, padding, -854792);
        if (cfg.showFacing) {
            ctx.drawTextWithShadow(tr, (Text)Text.literal((String)facing), padding, padding + lineH + 2, -8353376);
        }
        ctx.getMatrices().pop();
        HudElements.report(ID, boxX, boxY, scaledW, scaledH);
    }

    private static String facingLabel(float yaw) {
        float n = (yaw % 360.0f + 360.0f) % 360.0f;
        String key = n >= 315.0f || n < 45.0f ? "zombiezcompanion.coordinates.facing.south" : (n < 135.0f ? "zombiezcompanion.coordinates.facing.west" : (n < 225.0f ? "zombiezcompanion.coordinates.facing.north" : "zombiezcompanion.coordinates.facing.east"));
        return Text.translatable((String)"zombiezcompanion.coordinates.facing", (Object[])new Object[]{Text.translatable((String)key)}).getString();
    }

    public static CoordinatesModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module m : mm.modules()) {
            if (!(m instanceof CoordinatesModule)) continue;
            CoordinatesModule c = (CoordinatesModule)m;
            return c;
        }
        return null;
    }
}

