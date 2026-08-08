package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.stats.StatsModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientPlayNetworkHandler.class})
public abstract class ItemPickupMixin {
    @Inject(method={"onItemPickupAnimation"}, at={@At(value="HEAD")})
    private void zombiezcompanion$onPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isOnThread() || mc.player == null || mc.world == null) {
            return;
        }
        if (packet.getCollectorEntityId() != mc.player.getId()) {
            return;
        }
        Entity e = mc.world.getEntityById(packet.getEntityId());
        if (e instanceof ItemEntity) {
            ItemEntity item = (ItemEntity)e;
            StatsModule.onLocalPickup(item.getStack(), packet.getStackAmount());
        }
    }
}

