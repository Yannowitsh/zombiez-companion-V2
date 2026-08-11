package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.autotext.AutoTextModule;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the AutoText preset bar (rendered over the open chat) consume clicks on its item icons. */
@Mixin(value = {ChatScreen.class})
public abstract class ChatScreenMixin {
    @Inject(method = {"mouseClicked"}, at = @At(value = "HEAD"), cancellable = true)
    private void zombiezcompanion$presetBarClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (AutoTextModule.handleBarClick(event.x(), event.y(), event.button())) {
            cir.setReturnValue(Boolean.valueOf(true));
        }
    }
}
