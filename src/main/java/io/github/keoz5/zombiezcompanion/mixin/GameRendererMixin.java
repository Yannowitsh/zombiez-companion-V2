package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.zoom.ZoomState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={GameRenderer.class})
public abstract class GameRendererMixin {
    @Inject(method={"getFov"}, at={@At(value="RETURN")}, cancellable=true)
    private void zombiezcompanion$applyZoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float multiplier = ZoomState.fovMultiplier(tickDelta);
        if (multiplier < 0.999f) {
            cir.setReturnValue(Float.valueOf(((Float)cir.getReturnValue()).floatValue() * multiplier));
        }
    }
}

