package io.github.keoz5.zombiezcompanion.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={GameRenderer.class})
public interface GameRendererAccessor {
    @Invoker(value="getFov")
    public float zombiezcompanion$invokeGetFov(Camera var1, float var2, boolean var3);
}

