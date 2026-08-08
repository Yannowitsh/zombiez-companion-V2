/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4184
 *  net.minecraft.class_757
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package io.github.keoz5.zombiezcompanion.mixin;

import net.minecraft.class_4184;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={class_757.class})
public interface GameRendererAccessor {
    @Invoker(value="getFov")
    public float zombiezcompanion$invokeGetFov(class_4184 var1, float var2, boolean var3);
}

