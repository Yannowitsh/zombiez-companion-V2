package io.github.keoz5.zombiezcompanion.ui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.keoz5.zombiezcompanion.ZombieZCompanionClient;
import io.github.keoz5.zombiezcompanion.ui.ConfigScreen;

public final class ModMenuIntegration
implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigScreen(parent, ZombieZCompanionClient.configManager(), ZombieZCompanionClient.moduleManager());
    }
}

