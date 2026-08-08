package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.brightness.BrightnessOverride;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={LightmapTextureManager.class})
public abstract class LightmapTextureManagerMixin {
    @Redirect(method={"update(F)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object zombiezcompanion$boostGamma(SimpleOption<?> instance) {
        Object original = instance.getValue();
        if (!BrightnessOverride.isActive()) {
            return original;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) {
            return original;
        }
        if (instance == mc.options.getGamma()) {
            return BrightnessOverride.target();
        }
        return original;
    }
}

