package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.players.ModUserTag;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 26.1: PlayerRenderer#renderLabelIfPresent was removed; name-tag text is now
// produced by EntityRenderer#getNameTag(T). We decorate its return value.
// The ModUserTag.matches() filter keeps this effectively player-only.
@Mixin(value={EntityRenderer.class})
public abstract class PlayerEntityRendererMixin {
    @Inject(method={"getNameTag"}, at={@At(value="RETURN")}, cancellable=true)
    private void zombiezcompanion$tagModUser(Entity entity, CallbackInfoReturnable<Component> cir) {
        Component text = cir.getReturnValue();
        if (text == null || !ModUserTag.enabled()) {
            return;
        }
        if (!ModUserTag.matches(text.getString())) {
            return;
        }
        cir.setReturnValue(ModUserTag.decorate(text));
    }
}
