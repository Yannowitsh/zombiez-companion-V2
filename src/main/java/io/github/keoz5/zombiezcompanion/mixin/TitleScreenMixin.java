package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.ModInfo;
import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.ui.ConfigScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.net.URI;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={TitleScreen.class})
public abstract class TitleScreenMixin
extends Screen {
    private TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method={"init"}, at={@At(value="TAIL")})
    private void zombiezcompanion$addCompanionButtons(CallbackInfo ci) {
        int rh;
        int rw;
        int ry;
        int rx;
        Button realms = this.findButton("menu.online");
        Button options = this.findButton("menu.options");
        if (realms != null) {
            rx = realms.getX();
            ry = realms.getY();
            rw = realms.getWidth();
            rh = realms.getHeight();
            this.removeWidget((GuiEventListener)realms);
        } else {
            rx = this.width / 2 - 100;
            ry = this.height / 4 + 96;
            rw = 200;
            rh = 20;
        }
        this.addRenderableWidget(new StyledButton(rx, ry, rw, rh, (Component)Component.translatable((String)"zombiezcompanion.title.button.rinaorc"), btn -> {
            Minecraft mc = Minecraft.getInstance();
            ServerData info = new ServerData("Rinaorc", "rinaorc.com", ServerData.Type.OTHER);
            ConnectScreen.startConnecting((Screen)this, (Minecraft)mc, (ServerAddress)ServerAddress.parseString((String)"rinaorc.com"), (ServerData)info, (boolean)false, null);
        }, -11441921, -8874241, -854792));
        int compY = options != null ? options.getY() + 24 : this.height / 4 + 144;
        compY = Math.min(compY, this.height - 32);
        int compX = this.width / 2 - 100;
        this.addRenderableWidget(new StyledButton(compX, compY, 98, 20, (Component)Component.translatable((String)"zombiezcompanion.title.button.menu"), btn -> {
            if (ZombieZCompanionClient.configManager() == null || ZombieZCompanionClient.moduleManager() == null) {
                return;
            }
            Minecraft.getInstance().setScreen((Screen)new ConfigScreen(this, ZombieZCompanionClient.configManager(), ZombieZCompanionClient.moduleManager()));
        }, -266723542, -265932737, -854792));
        this.addRenderableWidget(new StyledButton(compX + 102, compY, 98, 20, (Component)Component.translatable((String)"zombiezcompanion.title.button.discord"), btn -> this.openDiscord(), -266723542, -265932737, -854792));
    }

    private void openDiscord() {
        Util.getPlatform().openUri(URI.create(ModInfo.DISCORD_URL));
    }

    private Button findButton(String translationKey) {
        for (GuiEventListener e : this.children()) {
            TranslatableContents t;
            Button bw;
            ComponentContents c;
            if (!(e instanceof Button) || !((c = (bw = (Button)e).getMessage().getContents()) instanceof TranslatableContents) || !translationKey.equals((t = (TranslatableContents)c).getKey())) continue;
            return bw;
        }
        return null;
    }
}

