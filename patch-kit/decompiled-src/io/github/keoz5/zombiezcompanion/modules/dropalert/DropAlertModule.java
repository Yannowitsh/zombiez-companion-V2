/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
 *  net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
 *  net.minecraft.class_1109
 *  net.minecraft.class_1113
 *  net.minecraft.class_1297
 *  net.minecraft.class_1542
 *  net.minecraft.class_1799
 *  net.minecraft.class_243
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_3414
 *  net.minecraft.class_3417
 *  net.minecraft.class_4184
 *  net.minecraft.class_437
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 *  net.minecraft.class_7833
 */
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
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_1297;
import net.minecraft.class_1542;
import net.minecraft.class_1799;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_4184;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_5250;
import net.minecraft.class_5348;
import net.minecraft.class_7833;

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
        return class_2561.method_43471((String)"zombiezcompanion.module.drop_alert.desc").getString();
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
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1687 == null || this.activeDrops.isEmpty()) {
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
        class_4184 camera = ctx.camera();
        class_243 cam = camera.method_19326();
        class_4587 matrices = ctx.matrixStack();
        class_4597.class_4598 immediate = mc.method_22940().method_23000();
        boolean drewAny = false;
        matrices.method_22903();
        for (ActiveDrop drop : this.activeDrops.values()) {
            if (!WaypointsModule.isBeaconVisible(ctx.frustum(), drop.x, drop.y, drop.z)) continue;
            int color = 0xFF000000 | drop.rarity.colorRgb;
            WaypointsModule.drawBeacon(matrices, immediate, camera, cam, mc.field_1772, drop.x, drop.y, drop.z, drop.itemName, color);
            drewAny = true;
        }
        matrices.method_22909();
        if (!drewAny) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth((float)4.0f);
        immediate.method_22993();
        RenderSystem.lineWidth((float)1.0f);
        RenderSystem.enableDepthTest();
    }

    @Override
    public class_437 createOptionsScreen(class_437 parent) {
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
    public void onClientTick(class_310 client) {
        boolean dropPressed;
        if (client.field_1724 == null || client.field_1687 == null || !ZombieZDetector.isOnZombieZ()) {
            this.reset();
            return;
        }
        if (ZombieZMapData.isInSpawn(client.field_1724.method_23317(), client.field_1724.method_23321())) {
            this.reset();
            return;
        }
        boolean bl = dropPressed = client.field_1690 != null && client.field_1690.field_1869.method_1434();
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
        List items = client.field_1687.method_8390(class_1542.class, client.field_1724.method_5829().method_1014(48.0), item -> !item.method_31481() && !item.method_6983().method_7960());
        HashSet<UUID> present = new HashSet<UUID>();
        UUID playerUuid = client.field_1724.method_5667();
        for (class_1542 item2 : items) {
            double horiz;
            UUID uuid2 = item2.method_5667();
            class_1799 stack = item2.method_6983();
            int count = stack.method_7947();
            present.add(uuid2);
            this.updateActiveDropPosition(item2);
            if (!this.selfDropIds.contains(uuid2) && !this.knownCounts.containsKey(uuid2) && this.ticksSinceDropKey < 60 && item2.method_6985() < 10 && (horiz = Math.hypot(item2.method_23317() - client.field_1724.method_23317(), item2.method_23321() - client.field_1724.method_23321())) < 2.5 && Math.abs(item2.method_23318() - client.field_1724.method_23318()) < 2.5) {
                this.selfDropIds.add(uuid2);
            }
            if (this.selfDropIds.contains(uuid2) || DropAlertModule.isSelfDrop(item2, playerUuid)) {
                this.knownCounts.put(uuid2, count);
                continue;
            }
            Integer previousCount = this.knownCounts.put(uuid2, count);
            if (!this.initialized || previousCount == null && this.seedOnly(item2) || previousCount != null && count <= previousCount) continue;
            String rawName = stack.method_7964().getString();
            boolean gadget = DropClassifier.isGadget(rawName);
            DropRarity foodRarity = DropClassifier.foodRarity(rawName);
            boolean food = foodRarity != null;
            DropRarity rarity = foodRarity != null ? foodRarity : DropClassifier.rarityOf(stack);
            int addedCount = count - (previousCount == null ? 0 : previousCount);
            if (gadget && !this.config().gadgets || !gadget && !food && !this.config().items || food && !this.config().food || (gadget || food) && DropClassifier.isConsumableHidden(rawName, this.config().hiddenConsumables) || !gadget && !this.isEnabled(rarity)) continue;
            double distance = Math.hypot(item2.method_23317() - client.field_1724.method_23317(), item2.method_23321() - client.field_1724.method_23321());
            String itemName = this.itemName(rawName, addedCount);
            this.pushNotification(rarity, itemName, distance);
            this.maybePlaySound(rarity);
            this.activeDrops.put(uuid2, new ActiveDrop(uuid2, rarity, itemName, item2.method_23317(), item2.method_23318(), item2.method_23321()));
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

    private void updateActiveDropPosition(class_1542 item) {
        ActiveDrop existing = this.activeDrops.get(item.method_5667());
        if (existing == null) {
            return;
        }
        this.activeDrops.put(item.method_5667(), new ActiveDrop(existing.uuid, existing.rarity, existing.itemName, item.method_23317(), item.method_23318(), item.method_23321()));
    }

    private boolean seedOnly(class_1542 item) {
        return item.method_6985() > 8;
    }

    private static boolean isSelfDrop(class_1542 item, UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        class_1297 owner = item.method_24921();
        return owner != null && playerUuid.equals(owner.method_5667());
    }

    @Override
    public void onHudRender(class_332 ctx, float tickDelta) {
        boolean hasDrops;
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null || client.field_1755 != null) {
            return;
        }
        long now = System.currentTimeMillis();
        this.notifications.removeIf(notification -> notification.expiresAt <= now);
        boolean bl = hasDrops = !this.activeDrops.isEmpty();
        if (this.notifications.isEmpty() && !hasDrops) {
            return;
        }
        class_327 tr = client.field_1772;
        int boxW = 220;
        int totalH = Math.max(25, this.notifications.size() * 25 + (hasDrops && this.config().markerStyle == 1 ? 54 : 0));
        HudConfig hud = this.configManager.get().hud;
        double scale = HudAnchor.scale(hud, "drop_notifications");
        int scaledW = (int)Math.round((double)boxW * scale);
        int scaledH = (int)Math.round((double)totalH * scale);
        int leftX = HudAnchor.resolveX(hud, "drop_notifications", ctx.method_51421(), scaledW, 0.0);
        int startY = HudAnchor.resolveY(hud, "drop_notifications", ctx.method_51443(), scaledH, 0.5);
        HudElements.report("drop_notifications", leftX, startY, scaledW, scaledH);
        ctx.method_51448().method_22903();
        ctx.method_51448().method_46416((float)leftX, (float)startY, 0.0f);
        if (scale != 1.0) {
            ctx.method_51448().method_22905((float)scale, (float)scale, 1.0f);
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
        ctx.method_51448().method_22909();
        if (hasDrops && this.config().markerStyle == 0) {
            for (ActiveDrop drop : this.activeDrops.values()) {
                WaypointsModule.renderScreenBeacon(ctx, client, tickDelta, drop.x, drop.y, drop.z, drop.itemName, 0xFF000000 | drop.rarity.colorRgb);
            }
        }
    }

    private void renderNotification(class_332 ctx, class_327 tr, DropNotification notification, int x, int y, long now) {
        class_5250 rarity = class_2561.method_43471((String)("zombiezcompanion.drop_alert.rarity." + notification.rarity.key));
        class_5250 message = class_2561.method_43469((String)"zombiezcompanion.drop_alert.toast", (Object[])new Object[]{rarity, notification.itemName, (int)Math.round(notification.distance)});
        int w = Math.min(288, Math.max(170, tr.method_27525((class_5348)message) + 34));
        int color = 0xFF000000 | notification.rarity.colorRgb;
        float remaining = Math.max(0.0f, Math.min(1.0f, (float)(notification.expiresAt - now) / 1600.0f));
        int alpha = 208 + (int)(32.0f * remaining);
        ctx.method_25294(x + 2, y + 2, x + w + 2, y + 22, -1442840576);
        ctx.method_25294(x, y, x + w, y + 20, alpha << 24 | 0xA0C12);
        ctx.method_25294(x, y, x + 4, y + 20, color);
        ctx.method_49601(x, y, w, 20, color);
        ctx.method_25303(tr, tr.method_27523(message.getString(), w - 16), x + 10, y + 6, -1);
    }

    private void renderDropGuide(class_332 ctx, class_327 tr, class_310 client, ActiveDrop drop, int x, int y) {
        double relative;
        double dx = drop.x - client.field_1724.method_23317();
        double dz = drop.z - client.field_1724.method_23321();
        double distance = Math.hypot(dx, dz);
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        for (relative = bearing - (double)client.field_1724.method_36454(); relative > 180.0; relative -= 360.0) {
        }
        while (relative < -180.0) {
            relative += 360.0;
        }
        int w = 214;
        int h = 44;
        int color = 0xFF000000 | drop.rarity.colorRgb;
        ctx.method_25294(x + 2, y + 2, x + w + 2, y + h + 2, -1442840576);
        ctx.method_25294(x, y, x + w, y + h, -401534184);
        ctx.method_25294(x, y, x + 4, y + h, color);
        ctx.method_49601(x, y, w, h, color);
        ctx.method_51448().method_22903();
        ctx.method_51448().method_46416((float)(x + 20), (float)(y + 22), 0.0f);
        ctx.method_51448().method_22907(class_7833.field_40718.rotationDegrees((float)relative));
        this.drawGuideArrow(ctx, color);
        ctx.method_51448().method_22909();
        class_5250 title = class_2561.method_43471((String)"zombiezcompanion.drop_alert.guide.title");
        class_5250 line = class_2561.method_43469((String)"zombiezcompanion.drop_alert.guide.line", (Object[])new Object[]{drop.itemName, (int)Math.round(distance)});
        ctx.method_51439(tr, (class_2561)title, x + 42, y + 8, -8874241, false);
        ctx.method_25303(tr, tr.method_27523(line.getString(), w - 50), x + 42, y + 22, -1);
        if (this.activeDrops.size() > 1) {
            String counter = "+" + (this.activeDrops.size() - 1);
            int cw = tr.method_1727(counter);
            ctx.method_25303(tr, counter, x + w - cw - 6, y + 6, -8353376);
        }
    }

    private void drawGuideArrow(class_332 ctx, int color) {
        int shadow = -872415232;
        ctx.method_25294(-1, -9, 2, 8, shadow);
        ctx.method_25294(-5, -6, 6, -2, shadow);
        ctx.method_25294(-7, -3, 8, 1, shadow);
        ctx.method_25294(-1, -8, 2, 6, color);
        ctx.method_25294(-4, -6, 5, -2, color);
        ctx.method_25294(-6, -3, 7, 1, color);
        ctx.method_25294(-1, 4, 2, 7, -1);
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
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null) {
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
        mc.method_1483().method_4873((class_1113)class_1109.method_4757((class_3414)((class_3414)class_3417.field_14793.comp_349()), (float)pitch, (float)volume));
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

