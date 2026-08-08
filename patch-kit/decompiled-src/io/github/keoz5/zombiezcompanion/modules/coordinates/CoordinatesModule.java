/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 */
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
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;

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
        return class_2561.method_43471((String)"zombiezcompanion.module.coordinates.desc").getString();
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
    public class_437 createOptionsScreen(class_437 parent) {
        return new CoordinatesOptionsScreen(parent, this, this.configManager);
    }

    public CoordinatesConfig config() {
        return this.configManager.get().coordinates;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    @Override
    public void onHudRender(class_332 ctx, float tickDelta) {
        int boxY;
        int boxX;
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null) {
            return;
        }
        if (client.field_1690.field_1842) {
            return;
        }
        if (client.field_1755 != null) {
            return;
        }
        CoordinatesConfig cfg = this.config();
        if (!cfg.everywhere && !ZombieZDetector.isOnZombieZ()) {
            return;
        }
        class_327 tr = client.field_1772;
        int x = (int)Math.round(client.field_1724.method_23317());
        int y = (int)Math.round(client.field_1724.method_23318());
        int z = (int)Math.round(client.field_1724.method_23321());
        String coords = cfg.showY ? String.format(Locale.ROOT, "X %d  Y %d  Z %d", x, y, z) : String.format(Locale.ROOT, "X %d  Z %d", x, z);
        String facing = CoordinatesModule.facingLabel(client.field_1724.method_36454());
        int lineH = 10;
        int width = tr.method_1727(coords);
        if (cfg.showFacing) {
            width = Math.max(width, tr.method_1727(facing));
        }
        int padding = 4;
        int boxW = width + padding * 2;
        int boxH = (cfg.showFacing ? lineH * 2 + 2 : lineH) + padding * 2;
        int screenW = ctx.method_51421();
        int screenH = ctx.method_51443();
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
        ctx.method_51448().method_22903();
        ctx.method_51448().method_46416((float)boxX, (float)boxY, 0.0f);
        if (scale != 1.0) {
            ctx.method_51448().method_22905((float)scale, (float)scale, 1.0f);
        }
        if (cfg.background) {
            ctx.method_25294(0, 0, boxW, boxH, -1442840576);
            ctx.method_25294(0, 0, boxW, 1, -8874241);
        }
        ctx.method_27535(tr, (class_2561)class_2561.method_43470((String)coords), padding, padding, -854792);
        if (cfg.showFacing) {
            ctx.method_27535(tr, (class_2561)class_2561.method_43470((String)facing), padding, padding + lineH + 2, -8353376);
        }
        ctx.method_51448().method_22909();
        HudElements.report(ID, boxX, boxY, scaledW, scaledH);
    }

    private static String facingLabel(float yaw) {
        float n = (yaw % 360.0f + 360.0f) % 360.0f;
        String key = n >= 315.0f || n < 45.0f ? "zombiezcompanion.coordinates.facing.south" : (n < 135.0f ? "zombiezcompanion.coordinates.facing.west" : (n < 225.0f ? "zombiezcompanion.coordinates.facing.north" : "zombiezcompanion.coordinates.facing.east"));
        return class_2561.method_43469((String)"zombiezcompanion.coordinates.facing", (Object[])new Object[]{class_2561.method_43471((String)key)}).getString();
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

