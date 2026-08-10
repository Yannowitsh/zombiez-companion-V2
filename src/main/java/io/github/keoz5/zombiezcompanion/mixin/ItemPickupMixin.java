package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.stats.StatsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientPacketListener.class})
public abstract class ItemPickupMixin {
    @Inject(method={"handleTakeItemEntity"}, at={@At(value="HEAD")})
    private void zombiezcompanion$onPickup(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread() || mc.player == null || mc.level == null) {
            return;
        }
        if (packet.getPlayerId() != mc.player.getId()) {
            return;
        }
        Entity e = mc.level.getEntity(packet.getItemId());
        if (e instanceof ItemEntity) {
            ItemEntity item = (ItemEntity)e;
            StatsModule.onLocalPickup(item.getItem(), packet.getAmount());
        }
    }
}

