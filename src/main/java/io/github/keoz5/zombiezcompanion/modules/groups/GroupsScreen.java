package io.github.keoz5.zombiezcompanion.modules.groups;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.GroupsConfig;
import io.github.keoz5.zombiezcompanion.modules.friends.FriendsCache;
import io.github.keoz5.zombiezcompanion.modules.friends.FriendsModule;
import io.github.keoz5.zombiezcompanion.modules.telemetry.PresenceCache;
import io.github.keoz5.zombiezcompanion.ui.ModuleOptionsScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledSlider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Combined "Amis & Groupe" screen: a friends directory (add/accept/remove/invite) plus group management. */
public final class GroupsScreen
extends ModuleOptionsScreen {
    private static final int GREEN_A = 0xFF2E7D46, GREEN_B = 0xFF369A55;
    private static final int GREY_A = 0xFF3A3F45, GREY_B = 0xFF4A5058;
    private static final int BLUE_A = 0xFF2E5AA2, BLUE_B = 0xFF3A6FC0;
    private static final int RED_A = 0xFF7A2E2E, RED_B = 0xFF9A3A3A;
    private static final int GOLD_A = 0xFF8A6A1E, GOLD_B = 0xFFB08A2A;
    private static final int TEXT = 0xFFF0E9E8;
    private static final int ACCENT = 0xFF8FD3FF;
    private static final int GOLD = 0xFFFFC83D;
    private static final int MUTED = 0xFF8A9199;
    private static final int PER_SECTION = 4;

    private final GroupsModule moduleRef;
    private List<Label> labels = new ArrayList<Label>();
    private String signature = "";
    private EditBox addField;

    public GroupsScreen(Screen parent, GroupsModule module, ConfigManager configManager) {
        super(parent, module, configManager);
        this.moduleRef = module;
    }

    private GroupsConfig config() {
        return this.moduleRef.config();
    }

    private FriendsModule friends() {
        return FriendsModule.get();
    }

    @Override
    protected void initOptions() {
        this.labels = new ArrayList<Label>();
        int x = this.panelX1 + 30;
        int w = this.panelX2 - this.panelX1 - 60;
        int right = x + w;
        int y = this.contentY1 + 14;

        // Display options (apply to group-member markers).
        this.addToggle(x, y, 220, (Component)Component.translatable((String)"zombiezcompanion.groups.toggle.show"), () -> this.config().showGroup, v -> this.config().showGroup = v);
        this.addRenderableWidget(new StyledButton(x + 228, y, w - 228, 22, GroupsScreen.styleLabel(this.config().markerStyle), b -> {
            String next = GroupsScreen.nextStyle(this.config().markerStyle);
            this.config().markerStyle = next;
            this.configManager.save();
            b.setMessage(GroupsScreen.styleLabel(next));
        }, GREY_A, GREY_B, TEXT));
        y += 28;
        this.addRenderableWidget(new StyledSlider(x, y, w, 22, this.config().nearHudRange, 20.0, 500.0, v -> {
            this.config().nearHudRange = (int)Math.round(v);
            this.configManager.save();
        }, v -> Component.translatable((String)"zombiezcompanion.groups.slider.near_range", (Object[])new Object[]{(int)Math.round(v)})));
        y += 28;

        // Group automation options.
        this.addToggle(x, y, (w - 8) / 2, (Component)Component.translatable((String)"zombiezcompanion.groups.toggle.follow_chief"), () -> this.config().followChief, v -> this.config().followChief = v);
        this.addToggle(x + (w - 8) / 2 + 8, y, (w - 8) / 2, (Component)Component.translatable((String)"zombiezcompanion.groups.toggle.dungeon_auto"), () -> this.config().dungeonAuto, v -> this.config().dungeonAuto = v);
        y += 30;

        // === GROUP part ===
        GroupsCache.Group g = GroupsCache.group();
        if (g == null) {
            y = this.initNoGroup(x, y, w, right);
        } else {
            y = this.initInGroup(g, x, y, w, right);
        }
        y += 6;

        // === FRIENDS directory part ===
        y = this.initFriends(g, x, y, w, right);
        this.signature = this.computeSignature();
    }

    /** Not in a group: create button + incoming group invites. */
    private int initNoGroup(int x, int y, int w, int right) {
        this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.groups.section.create"), x, y, ACCENT));
        this.button(right - 160, y - 3, 160, 20, (Component)Component.translatable((String)"zombiezcompanion.groups.btn.create"), b -> this.moduleRef.createGroup(), GREEN_A, GREEN_B);
        y += 15;
        this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.groups.create.hint"), x + 8, y + 2, MUTED));
        y += 20;

        List<GroupsCache.Invite> invites = GroupsCache.invites();
        if (!invites.isEmpty()) {
            this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.groups.section.invites"), x, y, ACCENT));
            y += 15;
            int shown = 0;
            for (GroupsCache.Invite inv : invites) {
                if (shown >= PER_SECTION) { this.labels.add(this.overflow(x + 8, y, invites.size() - shown)); y += 16; break; }
                this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.groups.invite.from", (Object[])new Object[]{inv.fromName()}), x + 8, y + 5, TEXT));
                String gid = inv.gid();
                this.button(right - 78, y, 78, 18, (Component)Component.translatable((String)"zombiezcompanion.groups.btn.join"), b -> this.moduleRef.acceptInvite(gid), GREEN_A, GREEN_B);
                this.button(right - 78 - 68, y, 64, 18, (Component)Component.translatable((String)"zombiezcompanion.groups.btn.decline"), b -> this.moduleRef.declineInvite(gid), GREY_A, GREY_B);
                y += 22;
                ++shown;
            }
        }
        return y;
    }

    /** In a group: members (chief marked), tp, chief controls, leave. */
    private int initInGroup(GroupsCache.Group g, int x, int y, int w, int right) {
        boolean chief = this.moduleRef.isChief();
        String self = FriendsModule.selfMcUuid();

        this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.groups.section.members", (Object[])new Object[]{g.members().size(), GroupsModule.MAX_MEMBERS}), x, y, ACCENT));
        this.button(right - 90, y - 3, 90, 18, (Component)Component.translatable((String)"zombiezcompanion.groups.btn.leave"), b -> this.moduleRef.leave(), RED_A, RED_B);
        y += 15;
        for (GroupsCache.Member m : g.members()) {
            String uuid = m.uuid();
            boolean isChiefMember = uuid.equals(g.chief());
            boolean isSelf = uuid.equals(self);
            PresenceCache.Presence p = FriendsModule.presenceOf(uuid);
            boolean online = p != null;
            String tag = isChiefMember ? "★ " : "";
            this.labels.add(new Label((Component)Component.literal((String)(tag + m.name() + (isSelf ? " (moi)" : ""))), x + 8, y + 5, isChiefMember ? GOLD : TEXT));
            int bx = right;
            if (chief && !isSelf) {
                bx -= 74; this.button(bx, y, 74, 18, (Component)Component.translatable((String)"zombiezcompanion.groups.btn.kick"), b -> this.moduleRef.kick(uuid), RED_A, RED_B);
                bx -= 4 + 96; this.button(bx, y, 96, 18, (Component)Component.translatable((String)"zombiezcompanion.groups.btn.transfer"), b -> this.moduleRef.transfer(uuid), GOLD_A, GOLD_B);
            }
            if (!isSelf && online) {
                bx -= 4 + 56; this.button(bx, y, 56, 18, (Component)Component.translatable((String)"zombiezcompanion.groups.btn.tp"), b -> { if (this.minecraft != null) this.minecraft.setScreen((Screen)null); FriendsModule.tpTo(p); }, BLUE_A, BLUE_B);
            }
            y += 22;
        }
        if (!chief) {
            this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.groups.member_note"), x + 8, y, MUTED));
            y += 16;
        }
        return y;
    }

    /** Friends directory: requests, friends (with optional group-invite), add by name, online roster. */
    private int initFriends(GroupsCache.Group g, int x, int y, int w, int right) {
        FriendsModule fm = this.friends();
        boolean canInvite = g != null && this.moduleRef.isChief() && g.members().size() < GroupsModule.MAX_MEMBERS;
        Set<String> members = new HashSet<String>();
        if (g != null) for (GroupsCache.Member m : g.members()) members.add(m.uuid());
        String self = FriendsModule.selfMcUuid();
        Set<String> friendIds = new HashSet<String>();
        Set<String> pendingIds = new HashSet<String>();
        for (FriendsCache.Friend f : FriendsCache.friends()) friendIds.add(f.uuid());
        for (FriendsCache.Request r : FriendsCache.outgoing()) pendingIds.add(r.uuid());
        for (FriendsCache.Request r : FriendsCache.incoming()) pendingIds.add(r.uuid());

        // Incoming friend requests.
        List<FriendsCache.Request> incoming = FriendsCache.incoming();
        if (!incoming.isEmpty()) {
            this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.section.incoming"), x, y, ACCENT));
            y += 15;
            int shown = 0;
            for (FriendsCache.Request r : incoming) {
                if (shown >= PER_SECTION) { this.labels.add(this.overflow(x + 8, y, incoming.size() - shown)); y += 16; break; }
                this.labels.add(new Label((Component)Component.literal((String)r.name()), x + 8, y + 5, TEXT));
                String uuid = r.uuid();
                if (fm != null) {
                    this.button(right - 78, y, 78, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.btn.accept"), b -> fm.accept(uuid), GREEN_A, GREEN_B);
                    this.button(right - 78 - 68, y, 64, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.btn.decline"), b -> fm.decline(uuid), GREY_A, GREY_B);
                }
                y += 22;
                ++shown;
            }
        }

        // Friends list (directory).
        this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.section.friends"), x, y, ACCENT));
        y += 15;
        List<FriendsCache.Friend> friends = FriendsCache.friends();
        if (friends.isEmpty()) {
            this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.no_friends"), x + 8, y, MUTED));
            y += 18;
        } else {
            int shown = 0;
            for (FriendsCache.Friend f : friends) {
                if (shown >= PER_SECTION) { this.labels.add(this.overflow(x + 8, y, friends.size() - shown)); y += 16; break; }
                String uuid = f.uuid();
                String name = f.name();
                boolean online = FriendsModule.presenceOf(uuid) != null;
                this.labels.add(new Label((Component)Component.literal((String)name), x + 8, y + 5, TEXT));
                this.labels.add(new Label((Component)Component.translatable((String)(online ? "zombiezcompanion.friends.status.online" : "zombiezcompanion.friends.status.offline")), x + 8 + this.font.width(name) + 8, y + 5, online ? MUTED : 0xFF6A7079));
                int bx = right;
                bx -= 64; if (fm != null) this.button(bx, y, 64, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.btn.remove"), b -> fm.removeFriend(uuid), RED_A, RED_B);
                if (canInvite && !members.contains(uuid)) {
                    bx -= 4 + 88;
                    boolean pending = this.moduleRef.invitePending(uuid);
                    Component inviteLabel = pending
                        ? (Component)Component.translatable((String)"zombiezcompanion.groups.btn.invited")
                        : (Component)Component.translatable((String)"zombiezcompanion.groups.btn.invite");
                    StyledButton inviteBtn = this.button(bx, y, 88, 18, inviteLabel, b -> {
                        if (this.moduleRef.invitePending(uuid)) {
                            return;
                        }
                        this.moduleRef.invite(uuid, name);
                        b.setMessage((Component)Component.translatable((String)"zombiezcompanion.groups.btn.invited"));
                        ((StyledButton)b).setColors(GREY_A, GREY_B);
                        b.active = false;
                    }, pending ? GREY_A : GREEN_A, pending ? GREY_B : GREEN_B);
                    if (pending) {
                        inviteBtn.active = false;
                    }
                }
                y += 22;
                ++shown;
            }
        }
        y += 4;

        // Add a friend by pseudo (works offline via the backend directory).
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
            if (!n.isEmpty() && fm != null) {
                fm.sendRequestByName(n);
                this.addField.setValue("");
            }
        }, GREEN_A, GREEN_B);
        y += 24;

        // Add from the online roster.
        this.labels.add(new Label((Component)Component.translatable((String)"zombiezcompanion.friends.add.online"), x + 8, y, MUTED));
        y += 14;
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
                if (shown >= PER_SECTION) { this.labels.add(this.overflow(x + 8, y, roster.size() - shown)); y += 16; break; }
                String id = p.mcuuid();
                String name = p.name();
                this.labels.add(new Label((Component)Component.literal((String)name), x + 8, y + 5, TEXT));
                if (fm != null) this.button(right - 78, y, 78, 18, (Component)Component.translatable((String)"zombiezcompanion.friends.btn.add"), b -> fm.sendRequest(id, name), GREEN_A, GREEN_B);
                y += 22;
                ++shown;
            }
        }
        return y;
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
        this.addRenderableWidget(new StyledButton(x, y, w, 22, GroupsScreen.toggleLabel(label, on), button -> {
            boolean next = !getter.get();
            setter.set(next);
            this.configManager.save();
            button.setMessage(GroupsScreen.toggleLabel(label, next));
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
        GroupsCache.Group g = GroupsCache.group();
        if (g != null) {
            sb.append(g.id()).append('@').append(g.chief()).append('|');
            for (GroupsCache.Member m : g.members()) sb.append(m.uuid()).append(FriendsModule.presenceOf(m.uuid()) != null ? 'o' : 'x').append(';');
        }
        sb.append('|');
        for (GroupsCache.Invite inv : GroupsCache.invites()) sb.append(inv.gid()).append(';');
        sb.append('|');
        for (FriendsCache.Friend f : FriendsCache.friends()) sb.append(f.uuid()).append(FriendsModule.presenceOf(f.uuid()) != null ? 'o' : 'x').append(';');
        sb.append('|');
        for (FriendsCache.Request r : FriendsCache.incoming()) sb.append(r.uuid()).append(';');
        sb.append('|');
        String self = FriendsModule.selfMcUuid();
        for (PresenceCache.Presence p : PresenceCache.presences()) {
            if (p.mcuuid() != null && !p.mcuuid().isBlank() && !p.mcuuid().equals(self)) sb.append(p.mcuuid()).append(';');
        }
        return sb.toString();
    }

    @Override
    //? if >= 26.1 {
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    //?} else {
    /*public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
    *///?}
        //? if >= 26.1 {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        //?} else {
        /*super.render(ctx, mouseX, mouseY, delta);
        *///?}
        ctx.text(this.font, (Component)Component.translatable((String)"zombiezcompanion.groups.header"), this.panelX1 + 30, this.contentY1 + 2, ACCENT, false);
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
