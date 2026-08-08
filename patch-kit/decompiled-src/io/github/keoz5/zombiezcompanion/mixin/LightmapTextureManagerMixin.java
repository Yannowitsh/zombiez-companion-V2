/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_7172
 *  net.minecraft.class_765
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.brightness.BrightnessOverride;
import net.minecraft.class_310;
import net.minecraft.class_7172;
import net.minecraft.class_765;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={class_765.class})
public abstract class LightmapTextureManagerMixin {
    @Redirect(method={"update(F)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object zombiezcompanion$boostGamma(class_7172<?> instance) {
        Object original = instance.method_41753();
        if (!BrightnessOverride.isActive()) {
            return original;
        }
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1690 == null) {
            return original;
        }
        if (instance == mc.field_1690.method_42473()) {
            return BrightnessOverride.target();
        }
        return original;
    }
}

