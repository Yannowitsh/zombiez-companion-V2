/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_156
 *  net.minecraft.class_2561
 *  net.minecraft.class_2588
 *  net.minecraft.class_310
 *  net.minecraft.class_364
 *  net.minecraft.class_412
 *  net.minecraft.class_4185
 *  net.minecraft.class_437
 *  net.minecraft.class_442
 *  net.minecraft.class_639
 *  net.minecraft.class_642
 *  net.minecraft.class_642$class_8678
 *  net.minecraft.class_7417
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.ui.ConfigScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.net.URI;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_2588;
import net.minecraft.class_310;
import net.minecraft.class_364;
import net.minecraft.class_412;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_639;
import net.minecraft.class_642;
import net.minecraft.class_7417;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_442.class})
public abstract class TitleScreenMixin
extends class_437 {
    private TitleScreenMixin(class_2561 title) {
        super(title);
    }

    @Inject(method={"init"}, at={@At(value="TAIL")})
    private void zombiezcompanion$addCompanionButtons(CallbackInfo ci) {
        int rh;
        int rw;
        int ry;
        int rx;
        class_4185 realms = this.findButton("menu.online");
        class_4185 options = this.findButton("menu.options");
        if (realms != null) {
            rx = realms.method_46426();
            ry = realms.method_46427();
            rw = realms.method_25368();
            rh = realms.method_25364();
            this.method_37066((class_364)realms);
        } else {
            rx = this.field_22789 / 2 - 100;
            ry = this.field_22790 / 4 + 96;
            rw = 200;
            rh = 20;
        }
        this.method_37063((class_364)new StyledButton(rx, ry, rw, rh, (class_2561)class_2561.method_43471((String)"zombiezcompanion.title.button.rinaorc"), btn -> {
            class_310 mc = class_310.method_1551();
            class_642 info = new class_642("Rinaorc", "rinaorc.com", class_642.class_8678.field_45611);
            class_412.method_36877((class_437)this, (class_310)mc, (class_639)class_639.method_2950((String)"rinaorc.com"), (class_642)info, (boolean)false, null);
        }, -11441921, -8874241, -854792));
        int compY = options != null ? options.method_46427() + 24 : this.field_22790 / 4 + 144;
        compY = Math.min(compY, this.field_22790 - 32);
        int compX = this.field_22789 / 2 - 100;
        this.method_37063((class_364)new StyledButton(compX, compY, 98, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.title.button.menu"), btn -> {
            if (ZombieZCompanionClient.configManager() == null || ZombieZCompanionClient.moduleManager() == null) {
                return;
            }
            class_310.method_1551().method_1507((class_437)new ConfigScreen(this, ZombieZCompanionClient.configManager(), ZombieZCompanionClient.moduleManager()));
        }, -266723542, -265932737, -854792));
        this.method_37063((class_364)new StyledButton(compX + 102, compY, 98, 20, (class_2561)class_2561.method_43471((String)"zombiezcompanion.title.button.discord"), btn -> this.openDiscord(), -266723542, -265932737, -854792));
    }

    private void openDiscord() {
        class_156.method_668().method_673(URI.create("https://discord.gg/mqJp9CKcEX"));
    }

    private class_4185 findButton(String translationKey) {
        for (class_364 e : this.method_25396()) {
            class_2588 t;
            class_4185 bw;
            class_7417 c;
            if (!(e instanceof class_4185) || !((c = (bw = (class_4185)e).method_25369().method_10851()) instanceof class_2588) || !translationKey.equals((t = (class_2588)c).method_11022())) continue;
            return bw;
        }
        return null;
    }
}

