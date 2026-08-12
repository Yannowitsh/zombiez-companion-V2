package io.github.keoz5.zombiezcompanion.modules.autotext;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.AutoTextConfig;
import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleCategory;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.core.ModuleManager;
import io.github.keoz5.zombiezcompanion.hud.HudAnchor;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import io.github.keoz5.zombiezcompanion.modules.autotext.AutoTextOptionsScreen;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZDetector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public final class AutoTextModule
implements Module {
    public static final String ID = "auto_text";
    /** HUD element id for the clickable chat preset bar (positioned via the HUD editor). */
    public static final String BAR_ELEMENT = "auto_text_bar";
    public static final double BAR_DEF_FX = 0.01;
    public static final double BAR_DEF_FY = 0.30;
    /** Absolute hit rects of the bar's slots for the current frame, and the text each one sends. */
    private static final List<int[]> slotRects = new ArrayList<int[]>();
    private static final List<String> slotTexts = new ArrayList<String>();
    private static final List<Boolean> slotAutoSend = new ArrayList<Boolean>();

    private ConfigManager configManager;
    private final boolean[] pressedLastTick = new boolean[AutoTextConfig.MAX_PRESETS];
    private boolean migrated;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Textes auto";
    }

    @Override
    public String description() {
        return Component.translatable((String)"zombiezcompanion.module.auto_text.desc").getString();
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.COMFORT;
    }

    @Override
    public List<String> searchKeywords() {
        return List.of("auto", "texte", "commande", "macro", "raccourci", "message", "chat", "autotext");
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
    public void onRegister(ModuleContext ctx) {
        this.configManager = ctx.configManager();
        this.migrateIfNeeded();
    }

    /** One-time migration of the legacy 5-entry (text+keybind) config into the unified preset list. */
    private void migrateIfNeeded() {
        if (this.migrated) {
            return;
        }
        this.migrated = true;
        AutoTextConfig cfg = this.config();
        if (cfg.presets == null) {
            cfg.presets = new ArrayList<AutoTextConfig.Preset>();
        }
        if (cfg.entries != null && !cfg.entries.isEmpty() && cfg.presets.isEmpty()) {
            for (AutoTextConfig.Entry e : cfg.entries) {
                if (e == null || e.text == null || e.text.isBlank()) continue;
                AutoTextConfig.Preset p = new AutoTextConfig.Preset();
                p.id = UUID.randomUUID().toString();
                p.text = e.text;
                p.name = AutoTextModule.deriveName(e.text);
                p.keyCode = e.keyCode;
                p.showInBar = false; // legacy entries were keybind-only
                cfg.presets.add(p);
            }
            cfg.entries = null;
            this.configManager.save();
        }
    }

    static String deriveName(String text) {
        if (text == null) return "";
        String t = text.trim();
        if (t.startsWith("/")) t = t.substring(1);
        return t.length() > 18 ? t.substring(0, 18) : t;
    }

    @Override
    public Screen createOptionsScreen(Screen parent) {
        return new AutoTextOptionsScreen(parent, this, this.configManager);
    }

    @Override
    public void onDisable() {
        Arrays.fill(this.pressedLastTick, false);
    }

    @Override
    public void onClientTick(Minecraft client) {
        int i;
        AutoTextConfig cfg = this.config();
        if (client.screen != null || client.player == null || client.getConnection() == null || !ZombieZDetector.isOnZombieZ()) {
            Arrays.fill(this.pressedLastTick, false);
            return;
        }
        List<AutoTextConfig.Preset> presets = cfg.presets;
        if (presets == null || presets.isEmpty()) {
            Arrays.fill(this.pressedLastTick, false);
            return;
        }
        int count = Math.min(presets.size(), this.pressedLastTick.length);
        for (i = 0; i < count; ++i) {
            boolean pressed;
            AutoTextConfig.Preset p = presets.get(i);
            if (p == null || p.keyCode == -1) {
                this.pressedLastTick[i] = false;
                continue;
            }
            pressed = GLFW.glfwGetKey((long)client.getWindow().handle(), (int)p.keyCode) == 1;
            if (pressed && !this.pressedLastTick[i]) {
                this.fire(client, p.text, p.autoSend);
            }
            this.pressedLastTick[i] = pressed;
        }
        for (i = count; i < this.pressedLastTick.length; ++i) {
            this.pressedLastTick[i] = false;
        }
    }

    /** Fires a preset now (used by the clickable chat bar): sends it, or drops it into the chat unsent. */
    public void trigger(String text, boolean autoSend) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            this.fire(client, text, autoSend);
        }
    }

    /** Either sends the text immediately (autoSend) or opens the chat pre-filled with it, unsent. */
    private void fire(Minecraft client, String rawText, boolean autoSend) {
        if (autoSend) {
            this.sendConfiguredText(client, rawText);
        } else {
            this.openChatWith(client, rawText);
        }
    }

    /** Opens (or replaces) the chat screen with the raw text pre-filled and cursor ready, without sending. */
    private void openChatWith(Minecraft client, String rawText) {
        if (rawText == null) {
            return;
        }
        String text = rawText.trim();
        if (text.isEmpty()) {
            return;
        }
        //? if >= 26.1 {
        client.setScreen((Screen)new ChatScreen(text, false));
        //?} else {
        /*client.setScreen((Screen)new ChatScreen(text));
        *///?}
    }

    // --- Clickable chat bar -------------------------------------------------

    @Override
    public void onHudRender(GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        slotRects.clear();
        slotTexts.clear();
        slotAutoSend.clear();
        if (client.player == null || client.options.hideGui) {
            return;
        }
        AutoTextConfig cfg = this.config();
        if (!cfg.barEnabled) {
            return;
        }
        boolean chatOpen = client.screen instanceof ChatScreen;
        // Show while chat is open; if the "only when chat open" option is off, show during normal play too.
        if (cfg.barOnlyWhenChatOpen ? !chatOpen : client.screen != null) {
            return;
        }
        if (!ZombieZDetector.isOnZombieZ()) {
            return;
        }
        ArrayList<AutoTextConfig.Preset> bar = new ArrayList<AutoTextConfig.Preset>();
        for (AutoTextConfig.Preset p : cfg.presets) {
            if (p != null && p.showInBar && p.text != null && !p.text.isBlank()) {
                bar.add(p);
            }
        }
        if (bar.isEmpty()) {
            return;
        }
        boolean horiz = !"vertical".equals(cfg.barOrientation);
        int icon = 16;
        int slot = Math.max(icon + 2, cfg.barIconSize);
        int pad = 3;
        int spacing = 2;
        int n = bar.size();
        int boxW = horiz ? pad * 2 + n * slot + (n - 1) * spacing : pad * 2 + slot;
        int boxH = horiz ? pad * 2 + slot : pad * 2 + n * slot + (n - 1) * spacing;
        HudConfig hud = this.configManager.get().hud;
        int boxX = HudAnchor.resolveX(hud, BAR_ELEMENT, ctx.guiWidth(), boxW, BAR_DEF_FX);
        int boxY = HudAnchor.resolveY(hud, BAR_ELEMENT, ctx.guiHeight(), boxH, BAR_DEF_FY);
        if (cfg.barShowBackground) {
            ctx.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0x90000000);
        }
        for (int i = 0; i < n; ++i) {
            AutoTextConfig.Preset p = bar.get(i);
            int sx = horiz ? boxX + pad + i * (slot + spacing) : boxX + pad;
            int sy = horiz ? boxY + pad : boxY + pad + i * (slot + spacing);
            ctx.fill(sx, sy, sx + slot, sy + slot, p.backgroundColor);
            ctx.outline(sx, sy, slot, slot, 0x40FFFFFF);
            int ix = sx + (slot - icon) / 2;
            int iy = sy + (slot - icon) / 2;
            ctx.item(AutoTextModule.iconStack(p.itemId), ix, iy);
            slotRects.add(new int[]{sx, sy, sx + slot, sy + slot});
            slotTexts.add(p.text);
            slotAutoSend.add(Boolean.valueOf(p.autoSend));
        }
        HudElements.report(BAR_ELEMENT, boxX, boxY, boxW, boxH);
        // Chat open: preview a preset's content when the cursor hovers its icon, so you can see what
        // you'll send before clicking.
        if (chatOpen) {
            AutoTextModule.renderHoverPreview(ctx, client);
        }
    }

    /** Draws a tooltip with the hovered slot's text (only while the chat is open). */
    private static void renderHoverPreview(GuiGraphicsExtractor ctx, Minecraft client) {
        if (slotRects.isEmpty()) {
            return;
        }
        int winW = client.getWindow().getWidth();
        int winH = client.getWindow().getHeight();
        if (winW <= 0 || winH <= 0) {
            return;
        }
        int screenW = ctx.guiWidth();
        int screenH = ctx.guiHeight();
        int mx = (int)Math.round(client.mouseHandler.xpos() * (double)screenW / (double)winW);
        int my = (int)Math.round(client.mouseHandler.ypos() * (double)screenH / (double)winH);
        for (int i = 0; i < slotRects.size(); ++i) {
            int[] r = slotRects.get(i);
            if (mx >= r[0] && mx <= r[2] && my >= r[1] && my <= r[3]) {
                AutoTextModule.drawPresetTooltip(ctx, client.font, slotTexts.get(i), mx, my, screenW, screenH);
                return;
            }
        }
    }

    private static void drawPresetTooltip(GuiGraphicsExtractor ctx, net.minecraft.client.gui.Font font, String text, int mouseX, int mouseY, int screenW, int screenH) {
        if (text == null || text.isBlank()) {
            return;
        }
        int maxW = Math.min(240, screenW - 20);
        List<String> lines = AutoTextModule.wrapText(font, text.trim(), maxW);
        int textW = 0;
        for (String ln : lines) {
            textW = Math.max(textW, font.width(ln));
        }
        int pad = 4;
        int lineH = font.lineHeight + 2;
        int boxW = textW + pad * 2;
        int boxH = lines.size() * lineH - 2 + pad * 2;
        int bx = mouseX + 10;
        int by = mouseY - boxH - 6;
        if (bx + boxW > screenW) {
            bx = screenW - boxW - 2;
        }
        if (bx < 2) {
            bx = 2;
        }
        if (by < 2) {
            by = mouseY + 12;
        }
        if (by + boxH > screenH) {
            by = screenH - boxH - 2;
        }
        ctx.fill(bx, by, bx + boxW, by + boxH, 0xF0100018);
        ctx.outline(bx, by, boxW, boxH, 0xFF7A4CD0);
        int ty = by + pad;
        for (String ln : lines) {
            ctx.text(font, (Component)Component.literal((String)ln), bx + pad, ty, -1, false);
            ty += lineH;
        }
    }

    /** Greedy word-wrap of a raw string to a max pixel width, so long presets don't overflow the screen. */
    private static List<String> wrapText(net.minecraft.client.gui.Font font, String text, int maxW) {
        ArrayList<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String cand = cur.length() == 0 ? word : cur + " " + word;
            if (font.width(cand) > maxW && cur.length() > 0) {
                out.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(cand);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        if (out.isEmpty()) {
            out.add(text);
        }
        return out;
    }

    /** Called by the ChatScreen mixin: if a bar slot was clicked, send it and consume the click. */
    public static boolean handleBarClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null || !mm.isEnabled(ID)) {
            return false;
        }
        AutoTextModule m = AutoTextModule.get();
        if (m == null) {
            return false;
        }
        for (int i = 0; i < slotRects.size(); ++i) {
            int[] r = slotRects.get(i);
            if (mouseX >= (double)r[0] && mouseX <= (double)r[2] && mouseY >= (double)r[1] && mouseY <= (double)r[3]) {
                m.trigger(slotTexts.get(i), slotAutoSend.get(i).booleanValue());
                return true;
            }
        }
        return false;
    }

    private static AutoTextModule get() {
        ModuleManager mm = ZombieZCompanionClient.moduleManager();
        if (mm == null) {
            return null;
        }
        for (Module mod : mm.modules()) {
            if (mod instanceof AutoTextModule) {
                return (AutoTextModule)mod;
            }
        }
        return null;
    }

    /** Resolves a preset's icon item id to an ItemStack for rendering (falls back to paper). */
    public static ItemStack iconStack(String itemId) {
        if (itemId != null && !itemId.isBlank()) {
            Identifier id = Identifier.tryParse((String)itemId);
            if (id != null) {
                Item item = (Item)BuiltInRegistries.ITEM.getValue(id);
                if (item != null && item != Items.AIR) {
                    return new ItemStack((net.minecraft.world.level.ItemLike)item);
                }
            }
        }
        return new ItemStack((net.minecraft.world.level.ItemLike)Items.PAPER);
    }

    private void sendConfiguredText(Minecraft client, String rawText) {
        if (rawText == null) {
            return;
        }
        String text = rawText.trim();
        if (text.isEmpty() || client.getConnection() == null) {
            return;
        }
        if (text.startsWith("/")) {
            String command = text.substring(1).trim();
            if (!command.isEmpty()) {
                client.getConnection().sendCommand(command);
            }
        } else {
            client.getConnection().sendChat(text);
        }
    }

    public AutoTextConfig config() {
        return this.configManager.get().autoText;
    }

    static String keyLabel(int keyCode) {
        if (keyCode == -1) {
            return Component.translatable((String)"zombiezcompanion.autotext.key.none").getString();
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
    }
}

