package io.github.keoz5.zombiezcompanion.mixin;

import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.ui.ConfigScreen;
import io.github.keoz5.zombiezcompanion.ui.widget.StyledButton;
import java.net.URI;
import net.minecraft.util.Util;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.TextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={TitleScreen.class})
public abstract class TitleScreenMixin
extends Screen {
    private TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method={"init"}, at={@At(value="TAIL")})
    private void zombiezcompanion$addCompanionButtons(CallbackInfo ci) {
        int rh;
        int rw;
        int ry;
        int rx;
        ButtonWidget realms = this.findButton("menu.online");
        ButtonWidget options = this.findButton("menu.options");
        if (realms != null) {
            rx = realms.getX();
            ry = realms.getY();
            rw = realms.getWidth();
            rh = realms.getHeight();
            this.remove((Element)realms);
        } else {
            rx = this.width / 2 - 100;
            ry = this.height / 4 + 96;
            rw = 200;
            rh = 20;
        }
        this.addDrawableChild(new StyledButton(rx, ry, rw, rh, (Text)Text.translatable((String)"zombiezcompanion.title.button.rinaorc"), btn -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            ServerInfo info = new ServerInfo("Rinaorc", "rinaorc.com", ServerInfo.ServerType.OTHER);
            ConnectScreen.connect((Screen)this, (MinecraftClient)mc, (ServerAddress)ServerAddress.parse((String)"rinaorc.com"), (ServerInfo)info, (boolean)false, null);
        }, -11441921, -8874241, -854792));
        int compY = options != null ? options.getY() + 24 : this.height / 4 + 144;
        compY = Math.min(compY, this.height - 32);
        int compX = this.width / 2 - 100;
        this.addDrawableChild(new StyledButton(compX, compY, 98, 20, (Text)Text.translatable((String)"zombiezcompanion.title.button.menu"), btn -> {
            if (ZombieZCompanionClient.configManager() == null || ZombieZCompanionClient.moduleManager() == null) {
                return;
            }
            MinecraftClient.getInstance().setScreen((Screen)new ConfigScreen(this, ZombieZCompanionClient.configManager(), ZombieZCompanionClient.moduleManager()));
        }, -266723542, -265932737, -854792));
        this.addDrawableChild(new StyledButton(compX + 102, compY, 98, 20, (Text)Text.translatable((String)"zombiezcompanion.title.button.discord"), btn -> this.openDiscord(), -266723542, -265932737, -854792));
    }

    private void openDiscord() {
        Util.getOperatingSystem().open(URI.create("https://discord.gg/mqJp9CKcEX"));
    }

    private ButtonWidget findButton(String translationKey) {
        for (Element e : this.children()) {
            TranslatableTextContent t;
            ButtonWidget bw;
            TextContent c;
            if (!(e instanceof ButtonWidget) || !((c = (bw = (ButtonWidget)e).getMessage().getContent()) instanceof TranslatableTextContent) || !translationKey.equals((t = (TranslatableTextContent)c).getKey())) continue;
            return bw;
        }
        return null;
    }
}

