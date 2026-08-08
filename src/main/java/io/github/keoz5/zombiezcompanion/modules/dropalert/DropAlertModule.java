package io.github.keoz5.zombiezcompanion.modules.dropalert;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.DropAlertConfig;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.hud.HudAnchor;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropAlertOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropClassifier;
import io.github.keoz5.zombiezcompanion.modules.dropalert.DropRarity;
import io.github.keoz5.zombiezcompanion.modules.map.WaypointsModule;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.math.RotationAxis;

public final class DropAlertModule
implements Module {
    public static final String ID = "drop_alert";
    private static final double DETECTION_RANGE = 48.0;
    private static final long NOTIFICATION_MS = 1600L;
    private static final int MAX_NOTIFICATIONS = 4;
    private static final int SCAN_INTERVAL_TICKS = 3;
    private final Map<UUID, Integer> knownCounts = new HashMap<UUID, Integer>();
    private final List<DropNotification> notifications = new ArrayList<DropNotification>();
    private final Set<UUID> selfDropIds = new HashSet<UUID>();
    private final LinkedHashMap<UUID, ActiveDrop> activeDrops = new LinkedHashMap();
    private ConfigManager configManager;
    private boolean initialized;
    private UUID latestDropUuid;
    private boolean wasDropKeyPressed;
    private int ticksSinceDropKey = 1000;
    private int scanTick;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Alertes de loot";
    }

    @Override
    public String description() {
        return Text.translatable((String)"zombiezcompanion.module.drop_alert.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.PROGRESSION;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("drop", "loot", "butin", "raret\u00e9", "items", "nourriture", "food", "gadgets", "alerte", "notification", "balise", "fl\u00e8che", "l\u00e9gendaire", "mythique", "\u00e9pique");
    }

    @Override
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
        WorldRenderEvents.LAST.register(this::renderBeacon);
    }

    private void renderBeacon(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || this.activeDrops.isEmpty()) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        if (this.config().markerStyle != 0) {
            return;
        }
        if (!ZombieZCompanionClient.moduleManager().isEnabled(ID)) {
            return;
        }
        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        boolean drewAny = false;
        matrices.push();
        for (ActiveDrop drop : this.activeDrops.values()) {
            if (!WaypointsModule.isBeaconVisible(ctx.frustum(), drop.x, drop.y, drop.z)) continue;
            int color = 0xFF000000 | drop.rarity.colorRgb;
            WaypointsModule.drawBeacon(matrices, immediate, camera, cam, mc.textRenderer, drop.x, drop.y, drop.z, drop.itemName, color);
            drewAny = true;
        }
        matrices.pop();
        if (!drewAny) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth((float)4.0f);
        immediate.draw();
        RenderSystem.lineWidth((float)1.0f);
        RenderSystem.enableDepthTest();
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new DropAlertOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onDisable() {
        this.reset();
    }

    @Override
    public void onLeaveWorld() {
        this.reset();
    }

    @Override
    public void onClientTick(MinecraftClient client) {
        boolean dropPressed;
        if (client.player == null || client.world == null || !ZombieZDetector.isOnZombieZ()) {
            this.reset();
            return;
        }
        if (ZombieZMapData.isInSpawn(client.player.getX(), client.player.getZ())) {
            this.reset();
            return;
        }
        boolean bl = dropPressed = client.options != null && client.options.dropKey.isPressed();
        if (dropPressed && !this.wasDropKeyPressed) {
            this.ticksSinceDropKey = 0;
        }
        this.wasDropKeyPressed = dropPressed;
        if (this.ticksSinceDropKey < 1000) {
            ++this.ticksSinceDropKey;
        }
        if (++this.scanTick < 3) {
            return;
        }
        this.scanTick = 0;
        List<ItemEntity> items = client.world.getEntitiesByClass(ItemEntity.class, client.player.getBoundingBox().expand(48.0), item -> !item.isRemoved() && !item.getStack().isEmpty());
        HashSet<UUID> present = new HashSet<UUID>();
        UUID playerUuid = client.player.getUuid();
        for (ItemEntity item2 : items) {
            double horiz;
            UUID uuid2 = item2.getUuid();
            ItemStack stack = item2.getStack();
            int count = stack.getCount();
            present.add(uuid2);
            this.updateActiveDropPosition(item2);
            if (!this.selfDropIds.contains(uuid2) && !this.knownCounts.containsKey(uuid2) && this.ticksSinceDropKey < 60 && item2.getItemAge() < 10 && (horiz = Math.hypot(item2.getX() - client.player.getX(), item2.getZ() - client.player.getZ())) < 2.5 && Math.abs(item2.getY() - client.player.getY()) < 2.5) {
                this.selfDropIds.add(uuid2);
            }
            if (this.selfDropIds.contains(uuid2) || DropAlertModule.isSelfDrop(item2, playerUuid)) {
                this.knownCounts.put(uuid2, count);
                continue;
            }
            Integer previousCount = this.knownCounts.put(uuid2, count);
            if (!this.initialized || previousCount == null && this.seedOnly(item2) || previousCount != null && count <= previousCount) continue;
            String rawName = stack.getName().getString();
            boolean gadget = DropClassifier.isGadget(rawName);
            DropRarity foodRarity = DropClassifier.foodRarity(rawName);
            boolean food = foodRarity != null;
            DropRarity rarity = foodRarity != null ? foodRarity : DropClassifier.rarityOf(stack);
            int addedCount = count - (previousCount == null ? 0 : previousCount);
            if (gadget && !this.config().gadgets || !gadget && !food && !this.config().items || food && !this.config().food || (gadget || food) && DropClassifier.isConsumableHidden(rawName, this.config().hiddenConsumables) || !gadget && !this.isEnabled(rarity)) continue;
            double distance = Math.hypot(item2.getX() - client.player.getX(), item2.getZ() - client.player.getZ());
            String itemName = this.itemName(rawName, addedCount);
            this.pushNotification(rarity, itemName, distance);
            this.maybePlaySound(rarity);
            this.activeDrops.put(uuid2, new ActiveDrop(uuid2, rarity, itemName, item2.getX(), item2.getY(), item2.getZ()));
            this.latestDropUuid = uuid2;
        }
        this.knownCounts.keySet().removeIf(uuid -> !present.contains(uuid));
        this.selfDropIds.removeIf(uuid -> !present.contains(uuid));
        this.activeDrops.keySet().removeIf(uuid -> !present.contains(uuid));
        if (this.latestDropUuid != null && !this.activeDrops.containsKey(this.latestDropUuid)) {
            this.latestDropUuid = this.activeDrops.isEmpty() ? null : DropAlertModule.lastKey(this.activeDrops);
        }
        this.initialized = true;
    }

    private static <K, V> K lastKey(LinkedHashMap<K, V> map) {
        K last = null;
        for (K k : map.keySet()) {
            last = k;
        }
        return last;
    }

    private void updateActiveDropPosition(ItemEntity item) {
        ActiveDrop existing = this.activeDrops.get(item.getUuid());
        if (existing == null) {
            return;
        }
        this.activeDrops.put(item.getUuid(), new ActiveDrop(existing.uuid, existing.rarity, existing.itemName, item.getX(), item.getY(), item.getZ()));
    }

    private boolean seedOnly(ItemEntity item) {
        return item.getItemAge() > 8;
    }

    private static boolean isSelfDrop(ItemEntity item, UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        Entity owner = item.getOwner();
        return owner != null && playerUuid.equals(owner.getUuid());
    }

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        boolean hasDrops;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) {
            return;
        }
        long now = System.currentTimeMillis();
        this.notifications.removeIf(notification -> notification.expiresAt <= now);
        boolean bl = hasDrops = !this.activeDrops.isEmpty();
        if (this.notifications.isEmpty() && !hasDrops) {
            return;
        }
        TextRenderer tr = client.textRenderer;
        int boxW = 220;
        int totalH = Math.max(25, this.notifications.size() * 25 + (hasDrops && this.config().markerStyle == 1 ? 54 : 0));
        HudConfig hud = this.configManager.get().hud;
        double scale = HudAnchor.scale(hud, "drop_notifications");
        int scaledW = (int)Math.round((double)boxW * scale);
        int scaledH = (int)Math.round((double)totalH * scale);
        int leftX = HudAnchor.resolveX(hud, "drop_notifications", ctx.getScaledWindowWidth(), scaledW, 0.0);
        int startY = HudAnchor.resolveY(hud, "drop_notifications", ctx.getScaledWindowHeight(), scaledH, 0.5);
        HudElements.report("drop_notifications", leftX, startY, scaledW, scaledH);
        ctx.getMatrices().push();
        ctx.getMatrices().translate((float)leftX, (float)startY, 0.0f);
        if (scale != 1.0) {
            ctx.getMatrices().scale((float)scale, (float)scale, 1.0f);
        }
        int index = 0;
        for (DropNotification notification2 : this.notifications) {
            this.renderNotification(ctx, tr, notification2, 0, index * 25, now);
            ++index;
        }
        if (hasDrops && this.config().markerStyle == 1) {
            ActiveDrop latest;
            ActiveDrop activeDrop = latest = this.latestDropUuid != null ? this.activeDrops.get(this.latestDropUuid) : null;
            if (latest == null) {
                latest = this.activeDrops.values().iterator().next();
            }
            this.renderDropGuide(ctx, tr, client, latest, 0, index * 25 + 6);
        }
        ctx.getMatrices().pop();
        if (hasDrops && this.config().markerStyle == 0) {
            for (ActiveDrop drop : this.activeDrops.values()) {
                WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, drop.x, drop.y, drop.z, drop.itemName, 0xFF000000 | drop.rarity.colorRgb);
            }
        }
    }

    private void renderNotification(DrawContext ctx, TextRenderer tr, DropNotification notification, int x, int y, long now) {
        MutableText rarity = Text.translatable((String)("zombiezcompanion.drop_alert.rarity." + notification.rarity.key));
        MutableText message = Text.translatable((String)"zombiezcompanion.drop_alert.toast", (Object[])new Object[]{rarity, notification.itemName, (int)Math.round(notification.distance)});
        int w = Math.min(288, Math.max(170, tr.getWidth((StringVisitable)message) + 34));
        int color = 0xFF000000 | notification.rarity.colorRgb;
        float remaining = Math.max(0.0f, Math.min(1.0f, (float)(notification.expiresAt - now) / 1600.0f));
        int alpha = 208 + (int)(32.0f * remaining);
        ctx.fill(x + 2, y + 2, x + w + 2, y + 22, -1442840576);
        ctx.fill(x, y, x + w, y + 20, alpha << 24 | 0xA0C12);
        ctx.fill(x, y, x + 4, y + 20, color);
        ctx.drawBorder(x, y, w, 20, color);
        ctx.drawTextWithShadow(tr, tr.trimToWidth(message.getString(), w - 16), x + 10, y + 6, -1);
    }

    private void renderDropGuide(DrawContext ctx, TextRenderer tr, MinecraftClient client, ActiveDrop drop, int x, int y) {
        double relative;
        double dx = drop.x - client.player.getX();
        double dz = drop.z - client.player.getZ();
        double distance = Math.hypot(dx, dz);
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        for (relative = bearing - (double)client.player.getYaw(); relative > 180.0; relative -= 360.0) {
        }
        while (relative < -180.0) {
            relative += 360.0;
        }
        int w = 214;
        int h = 44;
        int color = 0xFF000000 | drop.rarity.colorRgb;
        ctx.fill(x + 2, y + 2, x + w + 2, y + h + 2, -1442840576);
        ctx.fill(x, y, x + w, y + h, -401534184);
        ctx.fill(x, y, x + 4, y + h, color);
        ctx.drawBorder(x, y, w, h, color);
        ctx.getMatrices().push();
        ctx.getMatrices().translate((float)(x + 20), (float)(y + 22), 0.0f);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)relative));
        this.drawGuideArrow(ctx, color);
        ctx.getMatrices().pop();
        MutableText title = Text.translatable((String)"zombiezcompanion.drop_alert.guide.title");
        MutableText line = Text.translatable((String)"zombiezcompanion.drop_alert.guide.line", (Object[])new Object[]{drop.itemName, (int)Math.round(distance)});
        ctx.drawText(tr, (Text)title, x + 42, y + 8, -8874241, false);
        ctx.drawTextWithShadow(tr, tr.trimToWidth(line.getString(), w - 50), x + 42, y + 22, -1);
        if (this.activeDrops.size() > 1) {
            String counter = "+" + (this.activeDrops.size() - 1);
            int cw = tr.getWidth(counter);
            ctx.drawTextWithShadow(tr, counter, x + w - cw - 6, y + 6, -8353376);
        }
    }

    private void drawGuideArrow(DrawContext ctx, int color) {
        int shadow = -872415232;
        ctx.fill(-1, -9, 2, 8, shadow);
        ctx.fill(-5, -6, 6, -2, shadow);
        ctx.fill(-7, -3, 8, 1, shadow);
        ctx.fill(-1, -8, 2, 6, color);
        ctx.fill(-4, -6, 5, -2, color);
        ctx.fill(-6, -3, 7, 1, color);
        ctx.fill(-1, 4, 2, 7, -1);
    }

    private String itemName(String itemName, int addedCount) {
        return addedCount > 1 ? itemName + " x" + addedCount : itemName;
    }

    private void maybePlaySound(DropRarity rarity) {
        int min = this.config().soundMinRarity;
        if (min < 0) {
            return;
        }
        if (rarity.ordinal() < min) {
            return;
        }
        float volume = Math.max(0.0f, Math.min(1.0f, this.config().soundVolume));
        if (volume <= 0.0f) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        float pitch = switch (rarity) {
            default -> throw new MatchException(null, null);
            case DropRarity.COMMON, DropRarity.UNCOMMON -> 0.9f;
            case DropRarity.RARE -> 1.1f;
            case DropRarity.EPIC -> 1.3f;
            case DropRarity.LEGENDARY -> 1.5f;
            case DropRarity.MYTHIC, DropRarity.EXALTED -> 1.8f;
            case DropRarity.PRIMAL -> 2.0f;
        };
        mc.getSoundManager().play((SoundInstance)PositionedSoundInstance.master((SoundEvent)((SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_BELL.value()), (float)pitch, (float)volume));
    }

    private void pushNotification(DropRarity rarity, String itemName, double distance) {
        this.notifications.add(0, new DropNotification(rarity, itemName, distance, System.currentTimeMillis() + 1600L));
        while (this.notifications.size() > 4) {
            this.notifications.remove(this.notifications.size() - 1);
        }
    }

    private boolean isEnabled(DropRarity rarity) {
        DropAlertConfig cfg = this.config();
        return switch (rarity) {
            default -> throw new MatchException(null, null);
            case DropRarity.COMMON -> cfg.common;
            case DropRarity.UNCOMMON -> cfg.uncommon;
            case DropRarity.RARE -> cfg.rare;
            case DropRarity.EPIC -> cfg.epic;
            case DropRarity.LEGENDARY -> cfg.legendary;
            case DropRarity.MYTHIC -> cfg.mythic;
            case DropRarity.EXALTED -> cfg.exalted;
            case DropRarity.PRIMAL -> cfg.primal;
        };
    }

    private void reset() {
        this.initialized = false;
        this.knownCounts.clear();
        this.notifications.clear();
        this.selfDropIds.clear();
        this.activeDrops.clear();
        this.latestDropUuid = null;
        this.wasDropKeyPressed = false;
        this.ticksSinceDropKey = 1000;
        this.scanTick = 0;
    }

    public DropAlertConfig config() {
        return this.configManager.get().dropAlert;
    }

    boolean enabled(DropRarity rarity) {
        return this.isEnabled(rarity);
    }

    boolean foodEnabled() {
        return this.config().food;
    }

    void setFoodEnabled(boolean enabled) {
        this.config().food = enabled;
    }

    boolean itemsEnabled() {
        return this.config().items;
    }

    void setItemsEnabled(boolean enabled) {
        this.config().items = enabled;
    }

    boolean gadgetsEnabled() {
        return this.config().gadgets;
    }

    void setGadgetsEnabled(boolean enabled) {
        this.config().gadgets = enabled;
    }

    int markerStyle() {
        return this.config().markerStyle;
    }

    void cycleMarkerStyle() {
        this.config().markerStyle = (this.config().markerStyle + 1) % 2;
    }

    void setEnabled(DropRarity rarity, boolean enabled) {
        DropAlertConfig cfg = this.config();
        switch (rarity) {
            case COMMON: {
                cfg.common = enabled;
                break;
            }
            case UNCOMMON: {
                cfg.uncommon = enabled;
                break;
            }
            case RARE: {
                cfg.rare = enabled;
                break;
            }
            case EPIC: {
                cfg.epic = enabled;
                break;
            }
            case LEGENDARY: {
                cfg.legendary = enabled;
                break;
            }
            case MYTHIC: {
                cfg.mythic = enabled;
                break;
            }
            case EXALTED: {
                cfg.exalted = enabled;
                break;
            }
            case PRIMAL: {
                cfg.primal = enabled;
            }
        }
    }

    private record ActiveDrop(UUID uuid, DropRarity rarity, String itemName, double x, double y, double z) {
    }

    private record DropNotification(DropRarity rarity, String itemName, double distance, long expiresAt) {
    }
}

