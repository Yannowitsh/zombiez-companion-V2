/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1542
 *  net.minecraft.class_2775
 *  net.minecraft.class_310
 *  net.minecraft.class_634
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.modules.stats.StatsModule;
import net.minecraft.class_1297;
import net.minecraft.class_1542;
import net.minecraft.class_2775;
import net.minecraft.class_310;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_634.class})
public abstract class ItemPickupMixin {
    @Inject(method={"onItemPickupAnimation"}, at={@At(value="HEAD")})
    private void zombiezcompanion$onPickup(class_2775 packet, CallbackInfo ci) {
        class_310 mc = class_310.method_1551();
        if (!mc.method_18854() || mc.field_1724 == null || mc.field_1687 == null) {
            return;
        }
        if (packet.method_11912() != mc.field_1724.method_5628()) {
            return;
        }
        class_1297 e = mc.field_1687.method_8469(packet.method_11915());
        if (e instanceof class_1542) {
            class_1542 item = (class_1542)e;
            StatsModule.onLocalPickup(item.method_6983(), packet.method_11913());
        }
    }
}

