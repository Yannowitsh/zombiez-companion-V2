package io.github.keoz5.zombiezcompanion.realtime;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.config.HudConfig;
import io.github.keoz5.zombiezcompanion.hud.HudAnchor;
import io.github.keoz5.zombiezcompanion.hud.HudElements;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/**
 * Center-screen toasts pushed from the backend (a Discord {@code /broadcast}). Rendered directly on the HUD
 * (not gated by any module), positionable via the HUD editor. The queue is fed from the WebSocket reader
 * thread, so mutation and rendering are synchronized.
 */
public final class BroadcastToasts {
    public static final String ELEMENT = "broadcast_toast";
    private static final int MAX = 4;         // keep at most this many on screen
    private static final int WRAP_W = 260;    // wrap text past this width
    private static final List<Toast> toasts = new ArrayList<Toast>();

    private BroadcastToasts() {
    }

    /** Queue a toast. Thread-safe (called from the WebSocket reader thread). Severity kept for future styling. */
    public static synchronized void push(String text, String severity, long ms) {
        if (text == null || text.isBlank()) {
            return;
        }
        toasts.add(new Toast(text.trim(), System.currentTimeMillis(), Math.max(1000L, ms)));
        while (toasts.size() > MAX) {
            toasts.remove(0);
        }
    }

    /** Render + expire the active toasts as a single neutral banner. Called every frame from the HUD renderer. */
    public static synchronized void render(GuiGraphicsExtractor ctx) {
        if (toasts.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }
        long now = System.currentTimeMillis();
        toasts.removeIf(t -> now - t.spawnedAt() > t.ttlMs());
        if (toasts.isEmpty()) {
            return;
        }

        Font font = client.font;
        int padX = 10, padY = 5, gap = 3, lineH = 10;
        List<List<FormattedCharSequence>> wrapped = new ArrayList<List<FormattedCharSequence>>();
        int contentW = 0, totalLines = 0;
        for (Toast t : toasts) {
            List<FormattedCharSequence> lines = font.split((FormattedText) Component.literal((String) t.text()), WRAP_W);
            wrapped.add(lines);
            totalLines += lines.size();
            for (FormattedCharSequence l : lines) {
                contentW = Math.max(contentW, font.width(l));
            }
        }
        int boxW = contentW + padX * 2;
        int boxH = totalLines * lineH + padY * 2 + Math.max(0, toasts.size() - 1) * gap;

        HudConfig hud = ZombieZCompanionClient.configManager().get().hud;
        int x = HudAnchor.resolveX(hud, ELEMENT, ctx.guiWidth(), boxW, 0.5);
        int y = HudAnchor.resolveY(hud, ELEMENT, ctx.guiHeight(), boxH, 0.12);

        // One global neutral frame around the whole banner.
        ctx.fill(x, y, x + boxW, y + boxH, 0xC8000000);
        ctx.outline(x, y, boxW, boxH, 0x66FFFFFF);
        int ty = y + padY;
        for (List<FormattedCharSequence> lines : wrapped) {
            for (FormattedCharSequence line : lines) {
                ctx.centeredText(font, line, x + boxW / 2, ty, 0xFFFFFFFF);
                ty += lineH;
            }
            ty += gap;
        }
        HudElements.report(ELEMENT, x, y, boxW, boxH);
    }

    private record Toast(String text, long spawnedAt, long ttlMs) {
    }
}
