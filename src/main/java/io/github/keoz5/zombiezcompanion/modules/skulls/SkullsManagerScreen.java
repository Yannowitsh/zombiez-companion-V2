package io.github.keoz5.zombiezcompanion.modules.skulls;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.modules.map.ZombieZMapData;
import io.github.keoz5.zombiezcompanion.modules.skulls.SkullsModule;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;

public final class SkullsManagerScreen
extends Screen {
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 2;
    private final Screen parent;
    private final ConfigManager configManager;
    private final SkullsModule module;
    private final List<ZombieZMapData.Zone> zones;
    private int panelX1;
    private int panelY1;
    private int panelX2;
    private int panelY2;
    private int titleY1;
    private int titleY2;
    private int contentY1;
    private int contentY2;
    private int footerY1;
    private int footerY2;
    private int leftX;
    private int leftW;
    private int rightX;
    private int rightW;
    private int zoneScroll = 0;
    private int skullScroll = 0;
    private ZombieZMapData.Zone selectedZone;
    private final List<ButtonWidget> dynamicWidgets = new ArrayList<ButtonWidget>();

    public SkullsManagerScreen(Screen parent, ConfigManager configManager, SkullsModule module) {
        super((Text)Text.translatable((String)"zombiezcompanion.skulls.title"));
        this.parent = parent;
        this.configManager = configManager;
        this.module = module;
        this.zones = module.zonesWithSkulls();
        if (!this.zones.isEmpty()) {
            this.selectedZone = this.zones.get(0);
        }
    }

    protected void init() {
        int margin = Math.max(4, Math.min(28, Math.min(this.width, this.height) / 16));
        int panelW = Math.min(960, this.width - 2 * margin);
        int panelH = Math.min(580, this.height - 2 * margin);
        this.panelX1 = (this.width - panelW) / 2;
        this.panelY1 = (this.height - panelH) / 2;
        this.panelX2 = this.panelX1 + panelW;
        this.panelY2 = this.panelY1 + panelH;
        this.titleY1 = this.panelY1;
        this.titleY2 = this.titleY1 + 42;
        this.footerY2 = this.panelY2;
        this.footerY1 = this.footerY2 - 34;
        this.contentY1 = this.titleY2 + 4;
        this.contentY2 = this.footerY1 - 4;
        int innerW = this.panelX2 - this.panelX1 - 24;
        this.leftX = this.panelX1 + 12;
        this.leftW = (int)Math.round((double)innerW * 0.42);
        this.rightX = this.leftX + this.leftW + 12;
        this.rightW = innerW - this.leftW - 12;
        this.addDrawableChild(new StyledButton(this.panelX2 - 12 - 22, this.titleY1 + 10, 22, 22, (Text)Text.literal((String)"X"), b -> this.close(), -266723542, -265932737, -854792));
        int footBtnH = 20;
        int footBtnY = this.footerY1 + (34 - footBtnH) / 2;
        int fx = this.panelX1 + 12;
        this.addDrawableChild(new StyledButton(fx, footBtnY, 80, footBtnH, (Text)Text.translatable((String)"zombiezcompanion.button.back"), b -> this.close(), -266723542, -265932737, -854792));
        this.addDrawableChild(new StyledButton(fx += 86, footBtnY, 150, footBtnH, (Text)Text.translatable((String)"zombiezcompanion.skulls.guide_nearest"), b -> {
            this.module.guideToNearestUnvisited();
            this.close();
        }, -11441921, -8874241, -854792));
        this.addDrawableChild(new StyledButton(fx += 156, footBtnY, 132, footBtnH, (Text)Text.translatable((String)"zombiezcompanion.skulls.remove_all_beacons"), b -> {
            this.module.removeAllSkullWaypoints();
            this.rebuildDynamic();
        }, -12965328, -11716288, -854792));
        this.rebuildDynamic();
    }

    private void rebuildDynamic() {
        for (ButtonWidget w : this.dynamicWidgets) {
            this.remove((Element)w);
        }
        this.dynamicWidgets.clear();
        int visibleZoneRows = Math.max(1, (this.contentY2 - this.contentY1 - 8) / 24);
        int maxZoneOffset = Math.max(0, this.zones.size() - visibleZoneRows);
        this.zoneScroll = Math.min(this.zoneScroll, maxZoneOffset);
        int zy = this.contentY1 + 6;
        for (int i = 0; i < visibleZoneRows && this.zoneScroll + i < this.zones.size(); ++i) {
            ZombieZMapData.Zone z = this.zones.get(this.zoneScroll + i);
            int rowY = zy + i * 24;
            int btnW = 60;
            boolean beaconsOn = this.module.hasZoneWaypoints(z.num());
            StyledButton select = new StyledButton(this.leftX, rowY, this.leftW - btnW - 4, 22, this.zoneRowLabel(z), b -> {
                this.selectedZone = z;
                this.skullScroll = 0;
                this.rebuildDynamic();
            }, z == this.selectedZone ? -265932737 : -266723542, -265932737, -854792);
            this.addDrawableChild(select);
            this.dynamicWidgets.add(select);
            StyledButton toggle = new StyledButton(this.leftX + this.leftW - btnW, rowY, btnW, 22, (Text)Text.translatable((String)(beaconsOn ? "zombiezcompanion.skulls.beacons.on" : "zombiezcompanion.skulls.beacons.off")), b -> {
                if (this.module.hasZoneWaypoints(z.num())) {
                    this.module.removeZoneWaypoints(z.num());
                } else {
                    this.module.addZoneWaypoints(z.num());
                }
                this.rebuildDynamic();
            }, beaconsOn ? -11441921 : -266723542, beaconsOn ? -8874241 : -265932737, -854792);
            this.addDrawableChild(toggle);
            this.dynamicWidgets.add(toggle);
        }
        if (this.selectedZone == null || this.selectedZone.skulls().length == 0) {
            return;
        }
        int batchY = this.contentY1 + 42;
        int batchW = (this.rightW - 6) / 2;
        ButtonWidget markZone = (ButtonWidget)this.addDrawableChild(new StyledButton(this.rightX, batchY, batchW, 22, (Text)Text.translatable((String)"zombiezcompanion.skulls.zone.mark_all"), b -> {
            this.module.markZoneVisited(this.selectedZone.num());
            this.rebuildDynamic();
        }, -14709924, -14179731, -854792));
        ButtonWidget unmarkZone = (ButtonWidget)this.addDrawableChild(new StyledButton(this.rightX + batchW + 6, batchY, batchW, 22, (Text)Text.translatable((String)"zombiezcompanion.skulls.zone.unmark_all"), b -> {
            this.module.unmarkZoneVisited(this.selectedZone.num());
            this.rebuildDynamic();
        }, -12965328, -11716288, -854792));
        this.dynamicWidgets.add(markZone);
        this.dynamicWidgets.add(unmarkZone);
        ButtonWidget route = (ButtonWidget)this.addDrawableChild(new StyledButton(this.rightX, batchY + 22 + 4, this.rightW, 22, (Text)Text.translatable((String)"zombiezcompanion.skulls.zone.route"), b -> {
            this.module.buildZoneRoute(this.selectedZone.num());
            this.rebuildDynamic();
        }, -14867392, -11441921, -854792));
        this.dynamicWidgets.add(route);
        int visibleSkullRows = Math.max(1, (this.contentY2 - this.contentY1 - 106) / 24);
        int maxSkullOffset = Math.max(0, this.selectedZone.skulls().length - visibleSkullRows);
        this.skullScroll = Math.min(this.skullScroll, maxSkullOffset);
        int sy = this.contentY1 + 102;
        int chkW = 50;
        int wpW = 96;
        for (int i = 0; i < visibleSkullRows && this.skullScroll + i < this.selectedZone.skulls().length; ++i) {
            ZombieZMapData.Point p = this.selectedZone.skulls()[this.skullScroll + i];
            int rowY = sy + i * 24;
            boolean visited = this.module.isVisited(p.id());
            boolean hasWp = this.module.hasWaypoint(p);
            String visitLabel = (visited ? "\u2714 #" : "#") + SkullsModule.skullNumber(p);
            StyledButton check = new StyledButton(this.rightX, rowY, chkW, 22, (Text)Text.literal((String)visitLabel), b -> {
                this.module.setVisited(p.id(), !visited);
                this.rebuildDynamic();
            }, visited ? -14709924 : -266723542, visited ? -14179731 : -265932737, -854792);
            this.addDrawableChild(check);
            this.dynamicWidgets.add(check);
            StyledButton wpBtn = new StyledButton(this.rightX + this.rightW - wpW, rowY, wpW, 22, (Text)Text.translatable((String)(hasWp ? "zombiezcompanion.skulls.skull.beacon.on" : "zombiezcompanion.skulls.skull.beacon.off")), b -> {
                this.module.toggleSkullWaypoint(p);
                this.rebuildDynamic();
            }, hasWp ? -11441921 : -266723542, hasWp ? -8874241 : -265932737, -854792);
            this.addDrawableChild(wpBtn);
            this.dynamicWidgets.add(wpBtn);
        }
    }

    private Text zoneRowLabel(ZombieZMapData.Zone z) {
        int v = this.module.visitedCount(z.num());
        int t = this.module.totalSkullsCount(z.num());
        return Text.literal((String)("Z" + z.num() + " " + z.name() + "  " + v + "/" + t));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        boolean inRight;
        int delta = verticalAmount > 0.0 ? -1 : 1;
        boolean inLeft = mouseX >= (double)this.leftX && mouseX < (double)(this.leftX + this.leftW) && mouseY >= (double)this.contentY1 && mouseY < (double)this.contentY2;
        boolean bl = inRight = mouseX >= (double)this.rightX && mouseX < (double)(this.rightX + this.rightW) && mouseY >= (double)this.contentY1 && mouseY < (double)this.contentY2;
        if (inLeft) {
            this.zoneScroll = Math.max(0, this.zoneScroll + delta);
            this.rebuildDynamic();
            return true;
        }
        if (inRight) {
            this.skullScroll = Math.max(0, this.skullScroll + delta);
            this.rebuildDynamic();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, -872415232);
        ctx.fill(this.panelX1, this.panelY1, this.panelX2, this.panelY2, -183627755);
        ctx.fill(this.panelX1, this.titleY1, this.panelX2, this.titleY2, -183232737);
        ctx.fill(this.panelX1, this.contentY1, this.panelX2, this.contentY2, -183825134);
        ctx.fill(this.panelX1, this.footerY1, this.panelX2, this.footerY2, -183232737);
        ctx.fill(this.panelX1, this.titleY1, this.panelX2, this.titleY1 + 2, -8874241);
        ctx.fill(this.panelX1, this.titleY2 - 1, this.panelX2, this.titleY2, -14736594);
        ctx.fill(this.panelX1, this.footerY1, this.panelX2, this.footerY1 + 1, -14736594);
        ctx.drawBorder(this.panelX1, this.panelY1, this.panelX2 - this.panelX1, this.panelY2 - this.panelY1, -13880766);
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.skulls.title"), this.panelX1 + 18, this.titleY1 + 12, -854792);
        String totals = this.module.totalVisited() + " / " + this.module.totalSkulls() + " " + Text.translatable((String)"zombiezcompanion.skulls.visited").getString();
        int tw = this.textRenderer.getWidth(totals);
        ctx.drawTextWithShadow(this.textRenderer, (Text)Text.literal((String)totals), this.panelX2 - 18 - 30 - tw, this.titleY1 + 14, -8874241);
        if (this.selectedZone != null) {
            String zh = "Z" + this.selectedZone.num() + " " + this.selectedZone.name() + " \u2014 " + this.module.visitedCount(this.selectedZone.num()) + " / " + this.module.totalSkullsCount(this.selectedZone.num());
            ctx.drawTextWithShadow(this.textRenderer, (Text)Text.literal((String)zh), this.rightX, this.contentY1 + 12, -854792);
            ctx.drawText(this.textRenderer, (Text)Text.translatable((String)"zombiezcompanion.skulls.legend"), this.rightX, this.contentY1 + 26, -8353376, false);
            ctx.fill(this.rightX, this.contentY1 + 38, this.rightX + this.rightW, this.contentY1 + 39, -14736594);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void close() {
        this.configManager.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    public boolean shouldPause() {
        return false;
    }
}

