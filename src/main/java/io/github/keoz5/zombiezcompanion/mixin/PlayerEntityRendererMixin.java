package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.players.ModUserTag;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value={PlayerEntityRenderer.class})
public abstract class PlayerEntityRendererMixin {
    @ModifyVariable(method={"renderLabelIfPresent"}, at=@At(value="HEAD"), argsOnly=true)
    private Text zombiezcompanion$tagModUser(Text text) {
        if (text == null || !ModUserTag.enabled()) {
            return text;
        }
        if (!ModUserTag.matches(text.getString())) {
            return text;
        }
        return ModUserTag.decorate(text);
    }
}

