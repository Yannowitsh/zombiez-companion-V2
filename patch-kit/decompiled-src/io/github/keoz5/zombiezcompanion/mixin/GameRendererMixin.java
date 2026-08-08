/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4184
 *  net.minecraft.class_757
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.zoom.ZoomState;
import net.minecraft.class_4184;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_757.class})
public abstract class GameRendererMixin {
    @Inject(method={"getFov"}, at={@At(value="RETURN")}, cancellable=true)
    private void zombiezcompanion$applyZoom(class_4184 camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float multiplier = ZoomState.fovMultiplier(tickDelta);
        if (multiplier < 0.999f) {
            cir.setReturnValue((Object)Float.valueOf(((Float)cir.getReturnValue()).floatValue() * multiplier));
        }
    }
}

