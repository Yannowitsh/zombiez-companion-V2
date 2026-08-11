package io.github.keoz5.zombiezcompanion.modules.friends;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.FriendsConfig;
import io.github.keoz5.zombiezcompanion.modules.telemetry.PresenceCache;
import io.github.keoz5.zombiezcompanion.ui.ColorPickerScreen;
import io.github.keoz5.zombiezcompanion.ui.Colors;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledSlider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FriendsScreen
extends ModuleOptionsScreen {
    private static final int GREEN_A = 0xFF2E7D46, GREEN_B = 0xFF369A55;
    private static final int GREY_A = 0xFF3A3F45, GREY_B = 0xFF4A5058;
    private static final int BLUE_A = 0xFF2E5AA2, BLUE_B = 0xFF3A6FC0;
    private static final int RED_A = 0xFF7A2E2E, RED_B = 0xFF9A3A3A;
    private static final int TEXT = 0xFFF0E9E8;
    private static final int ACCENT = 0xFF8FD3FF;
    private static final int MUTED = 0xFF8A9199;
    private static final int PER_SECTION = 5;

    private final FriendsModule moduleRef;
    private List<Label> labels = new ArrayList<Label>();
    private String signature = "";
    private EditBox addField;

    public FriendsScreen(Screen parent, FriendsModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    private FriendsConfig config() {
        return this.moduleRef.config();
    }

    @Override
    protected void initOptions() {
        this.labels = new ArrayList<Label>();
        int x = this.panelX1 + 30;
        int w = this.panelX2 - this.panelX1 - 60;
        int right = x + w;
        int y = this.contentY1 + 14;

        // Master render toggle.
        this.addToggle(x, y, 220, (Component)Component.translatable((String)"zombiezcompanion.friends.toggle.show"), () -> this.config().showFriends, v -> this.config().showFriends = v);
        y += 30;

        // Near/far threshold: at/above it a friend shows as the full labeled waypoint; below it, the near marker.
        this.addRenderableWidget(new StyledSlider(x, y, w, 22, this.config().nearHudRange, 20.0, 500.0, v -> {
            this.config().nearHudRange = (int)Math.round(v);
            this.configManager.save();
        }, v -> Component.translatable((String)"zombiezcompanion.friends.slider.near_range", (Object[])new Object[]{(int)Math.round(v)})));
        y += 30;

        // Global marker style (cycles auto -> waypoint -> box).
        this.addRenderableWidget(new StyledButton(x, y, 220, 22, FriendsScreen.styleLabel(this.config().markerStyle), b -> {
            String next = FriendsScreen.nextStyle(this.config().markerStyle);
            this.config().markerStyle = next;
            this.configManager.save();
            b.setMessage(FriendsScreen.styleLabel(next));
        }, GREY_A, GREY_B, TEXT));
        y += 30;

        Set<String> friendIds = new HashSet<String>();
        Set<String> pendingIds = new HashSet<String>();
        for (FriendsCache.Friend f : FriendsCache.friends()) friendIds.add(f.uuid());
        for (FriendsCache.Request r : FriendsCache.outgoing()) pendingIds.add(r.uuid());
        for (FriendsCache.Request r : FriendsCache.incoming()) pendingIds.add(r.uuid());

        // Incoming requests.
        this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.section.incoming"), x, y, ACCENT));
        y += 15;
        List<FriendsCache.Request> incoming = FriendsCache.incoming();
        if (incoming.isEmpty()) {
            this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.none"), x + 8, y, MUTED));
            y += 18;
        } else {
            int shown = 0;
            for (FriendsCache.Request r : incoming) {
                if (shown >= PER_SECTION) { this.labels.add(overflow(x + 8, y, incoming.size() - shown)); y += 16; break; }
                this.labels.add(new Label((Component)Component.literal((String)r.name()), x + 8, y + 5, TEXT));
                String uuid = r.uuid();
                this.button(right - 78, y, 78, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.btn.accept"), b -> this.moduleRef.accept(uuid), GREEN_A, GREEN_B);
                this.button(right - 78 - 68, y, 64, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.btn.decline"), b -> this.moduleRef.decline(uuid), GREY_A, GREY_B);
                y += 22;
                ++shown;
            }
        }
        y += 8;

        // Friends list.
        this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.section.friends"), x, y, ACCENT));
        y += 15;
        List<FriendsCache.Friend> friends = FriendsCache.friends();
        if (friends.isEmpty()) {
            this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.no_friends"), x + 8, y, MUTED));
            y += 18;
        } else {
            Minecraft mc = Minecraft.getInstance();
            String playerDim = mc.player != null && mc.level != null ? mc.level.dimension().identifier().toString() : "";
            int shown = 0;
            for (FriendsCache.Friend f : friends) {
                if (shown >= PER_SECTION) { this.labels.add(overflow(x + 8, y, friends.size() - shown)); y += 16; break; }
                String uuid = f.uuid();
                boolean visible = this.moduleRef.isVisible(uuid);
                PresenceCache.Presence p = FriendsModule.presenceOf(uuid);
                boolean online = p != null;
                // "Voir" toggle.
                this.button(x, y, 62, 18, (Component)Component.translatable((String)(visible ? "zombiezcompanion.friends.see.on" : "zombiezcompanion.friends.see.off")), b -> this.moduleRef.setVisible(uuid, !visible), visible ? GREEN_A : GREY_A, visible ? GREEN_B : GREY_B);
                // Per-friend color swatch (the button itself shows the color and opens the full picker).
                int swatch = 0xFF000000 | (FriendsModule.colorOf(uuid) & 0xFFFFFF);
                this.addRenderableWidget(new StyledButton(x + 68, y, 18, 18, (Component)Component.empty(), b -> {
                    if (this.minecraft != null) this.minecraft.setScreen((Screen)new ColorPickerScreen(this, this.configManager, new Colors.Element("friend:" + uuid, "zombiezcompanion.friends.color.pick", FriendsModule.FRIEND_COLOR)));
                }, swatch, swatch, TEXT));
                this.labels.add(new Label((Component)Component.literal((String)f.name()), x + 90, y + 5, TEXT));
                this.labels.add(new Label(this.statusText(mc, p, playerDim), x + 90, y + 5 + 11, online ? MUTED : 0xFF6A7079));
                if (online) {
                    this.button(right - 56, y, 56, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.btn.tp"), b -> { if (this.minecraft != null) this.minecraft.setScreen((Screen)null); FriendsModule.tpTo(p); }, BLUE_A, BLUE_B);
                }
                this.button(right - 56 - 68, y, 64, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.btn.remove"), b -> this.moduleRef.removeFriend(uuid), RED_A, RED_B);
                y += 26;
                ++shown;
            }
        }
        y += 8;

        // Add a friend: by pseudo (works even offline) or from the online roster.
        this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.section.add"), x, y, ACCENT));
        y += 15;
        this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.add.by_name"), x + 8, y + 5, MUTED));
        if (this.addField == null) {
            this.addField = new EditBox(this.font, x + 118, y, w - 118 - 92, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.add.field"));
            this.addField.setMaxLength(16);
        } else {
            this.addField.setX(x + 118);
            this.addField.setY(y);
            this.addField.setWidth(w - 118 - 92);
        }
        this.addRenderableWidget(this.addField);
        this.button(right - 88, y, 88, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.btn.send"), b -> {
            String n = this.addField.getValue().trim();
            if (!n.isEmpty()) {
                this.moduleRef.sendRequestByName(n);
                this.addField.setValue("");
            }
        }, GREEN_A, GREEN_B);
        y += 24;
        this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.add.online"), x + 8, y, MUTED));
        y += 14;
        String self = FriendsModule.selfMcUuid();
        List<PresenceCache.Presence> roster = new ArrayList<PresenceCache.Presence>();
        for (PresenceCache.Presence p : PresenceCache.presences()) {
            String id = p.mcuuid();
            if (id == null || id.isBlank() || id.equals(self)) continue;
            if (friendIds.contains(id) || pendingIds.contains(id)) continue;
            roster.add(p);
        }
        if (roster.isEmpty()) {
            this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.no_roster"), x + 8, y, MUTED));
            y += 18;
        } else {
            int shown = 0;
            for (PresenceCache.Presence p : roster) {
                if (shown >= PER_SECTION) { this.labels.add(overflow(x + 8, y, roster.size() - shown)); y += 16; break; }
                String id = p.mcuuid();
                String name = p.name();
                this.labels.add(new Label((Component)Component.literal((String)name), x + 8, y + 5, TEXT));
                this.button(right - 78, y, 78, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.btn.add"), b -> this.moduleRef.sendRequest(id, name), GREEN_A, GREEN_B);
                y += 22;
                ++shown;
            }
        }
        this.signature = this.computeSignature();
    }

    private Component statusText(Minecraft mc, PresenceCache.Presence p, String playerDim) {
        if (p == null) {
            return Component.translatable((String)"zombiezcompanion.friends.status.offline");
        }
        boolean otherWorld = !playerDim.isEmpty() && !p.dim().isEmpty() && !playerDim.equals(p.dim());
        if (otherWorld) {
            return Component.translatable((String)"zombiezcompanion.friends.status.other_world");
        }
        if (mc.player != null) {
            int dist = (int)Math.round(Math.sqrt(mc.player.distanceToSqr(p.x(), p.y(), p.z())));
            return Component.translatable((String)"zombiezcompanion.friends.status.online_dist", (Object[])new Object[]{dist});
        }
        return Component.translatable((String)"zombiezcompanion.friends.status.online");
    }

    private Label overflow(int x, int y, int more) {
        return new Label((Component)Component.translatable((String)"zombiezcompanion.friends.more", (Object[])new Object[]{more}), x, y, MUTED);
    }

    private StyledButton button(int x, int y, int w, int h, Component label, net.minecraft.client.gui.components.Button.OnPress onPress, int idle, int hover) {
        StyledButton b = new StyledButton(x, y, w, h, label, onPress, idle, hover, TEXT);
        this.addRenderableWidget(b);
        return b;
    }

    private void addToggle(int x, int y, int w, Component label, BoolGetter getter, BoolSetter setter) {
        boolean on = getter.get();
        this.addRenderableWidget(new StyledButton(x, y, w, 22, FriendsScreen.toggleLabel(label, on), button -> {
            boolean next = !getter.get();
            setter.set(next);
            this.configManager.save();
            button.setMessage(FriendsScreen.toggleLabel(label, next));
            ((StyledButton)button).setColors(next ? GREEN_A : GREY_A, next ? GREEN_B : GREY_B);
        }, on ? GREEN_A : GREY_A, on ? GREEN_B : GREY_B, TEXT));
    }

    private static Component toggleLabel(Component label, boolean enabled) {
        return Component.translatable((String)"zombiezcompanion.toggle.format", (Object[])new Object[]{label, Component.translatable((String)(enabled ? "zombiezcompanion.state.on" : "zombiezcompanion.state.off"))});
    }

    private static String nextStyle(String style) {
        if ("auto".equals(style)) return "waypoint";
        if ("waypoint".equals(style)) return "box";
        return "auto";
    }

    private static Component styleLabel(String style) {
        String s = "waypoint".equals(style) || "box".equals(style) ? style : "auto";
        return Component.translatable((String)"zombiezcompanion.friends.style.label", (Object[])new Object[]{Component.translatable((String)("zombiezcompanion.friends.style." + s))});
    }

    @Override
    public void tick() {
        super.tick();
        // Don't rebuild (which re-lays widgets) while the user is typing a name.
        if (this.addField != null && this.addField.isFocused()) {
            return;
        }
        if (!this.computeSignature().equals(this.signature)) {
            this.rebuildWidgets();
        }
    }

    private String computeSignature() {
        StringBuilder sb = new StringBuilder();
        for (FriendsCache.Friend f : FriendsCache.friends()) {
            sb.append(f.uuid()).append(this.moduleRef.isVisible(f.uuid()) ? '1' : '0').append(FriendsModule.presenceOf(f.uuid()) != null ? 'o' : 'x').append(';');
        }
        sb.append('|');
        for (FriendsCache.Request r : FriendsCache.incoming()) sb.append(r.uuid()).append(';');
        sb.append('|');
        for (FriendsCache.Request r : FriendsCache.outgoing()) sb.append(r.uuid()).append(';');
        sb.append('|');
        String self = FriendsModule.selfMcUuid();
        for (PresenceCache.Presence p : PresenceCache.presences()) {
            if (p.mcuuid() != null && !p.mcuuid().isBlank() && !p.mcuuid().equals(self)) sb.append(p.mcuuid()).append(';');
        }
        return sb.toString();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.friends.header"), this.panelX1 + 30, this.contentY1 + 2, ACCENT, false);
        for (Label l : this.labels) {
            ctx.text(this.font, l.text, l.x, l.y, l.color, false);
        }
    }

    private record Label(Component text, int x, int y, int color) {
    }

    private static interface BoolGetter {
        public boolean get();
    }

    private static interface BoolSetter {
        public void set(boolean var1);
    }
}
