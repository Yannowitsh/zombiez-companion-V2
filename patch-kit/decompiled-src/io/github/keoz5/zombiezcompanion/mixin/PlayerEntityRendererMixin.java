/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1007
 *  net.minecraft.class_2561
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 */
package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.players.ModUserTag;
import net.minecraft.class_1007;
import net.minecraft.class_2561;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value={class_1007.class})
public abstract class PlayerEntityRendererMixin {
    @ModifyVariable(method={"renderLabelIfPresent"}, at=@At(value="HEAD"), argsOnly=true)
    private class_2561 zombiezcompanion$tagModUser(class_2561 text) {
        if (text == null || !ModUserTag.enabled()) {
            return text;
        }
        if (!ModUserTag.matches(text.getString())) {
            return text;
        }
        return ModUserTag.decorate(text);
    }
}

