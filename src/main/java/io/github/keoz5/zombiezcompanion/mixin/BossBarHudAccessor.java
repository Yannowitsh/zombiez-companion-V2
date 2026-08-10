package io.github.keoz5.zombiezcompanion.mixin;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={BossHealthOverlay.class})
public interface BossBarHudAccessor {
    // 26.1: BossHealthOverlay's map field was renamed bossBars -> events.
    @Accessor(value="events")
    public Map<UUID, LerpingBossEvent> getBossBars();
}

