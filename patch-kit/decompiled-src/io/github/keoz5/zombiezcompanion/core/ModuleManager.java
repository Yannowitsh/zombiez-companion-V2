/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 */
package io.github.keoz5.zombiezcompanion.core;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.config.ModConfig;
import io.github.keoz5.zombiezcompanion.core.Module;
import io.github.keoz5.zombiezcompanion.core.ModuleContext;
import io.github.keoz5.zombiezcompanion.log.Log;
import io.github.keoz5.zombiezcompanion.log.LogCategory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;

public final class ModuleManager {
    private final ConfigManager configManager;
    private final ModuleContext context;
    private final List<Module> modules = new ArrayList<Module>();
    private final Map<String, Module> byId = new HashMap<String, Module>();

    public ModuleManager(ConfigManager configManager, ModuleContext context) {
        this.configManager = configManager;
        this.context = context;
    }

    public void register(Module module) {
        if (this.byId.containsKey(module.id())) {
            throw new IllegalStateException("Duplicate module id: " + module.id());
        }
        this.modules.add(module);
        this.byId.put(module.id(), module);
        ModConfig cfg = this.configManager.get();
        cfg.moduleEnabled.putIfAbsent(module.id(), module.defaultEnabled());
        try {
            module.onRegister(this.context);
        }
        catch (Throwable t) {
            Log.error("Module " + module.id() + " threw during onRegister", t);
        }
        Log.debug(LogCategory.MODULE, "registered " + module.id());
    }

    public void startEnabledModules() {
        for (Module m : this.modules) {
            if (!this.isEnabled(m.id())) continue;
            this.safeEnable(m);
        }
    }

    public List<Module> modules() {
        return Collections.unmodifiableList(this.modules);
    }

    public Optional<Module> findById(String id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    public boolean isEnabled(String id) {
        return Boolean.TRUE.equals(this.configManager.get().moduleEnabled.get(id));
    }

    public void setEnabled(String id, boolean enabled) {
        Module m = this.byId.get(id);
        if (m == null) {
            return;
        }
        boolean was = this.isEnabled(id);
        if (was == enabled) {
            return;
        }
        this.configManager.get().moduleEnabled.put(id, enabled);
        this.configManager.save();
        if (enabled) {
            this.safeEnable(m);
        } else {
            this.safeDisable(m);
        }
    }

    private void safeEnable(Module m) {
        try {
            m.onEnable();
            Log.debug(LogCategory.MODULE, "enabled " + m.id());
        }
        catch (Throwable t) {
            Log.error("Module " + m.id() + " threw during onEnable", t);
        }
    }

    private void safeDisable(Module m) {
        try {
            m.onDisable();
            Log.debug(LogCategory.MODULE, "disabled " + m.id());
        }
        catch (Throwable t) {
            Log.error("Module " + m.id() + " threw during onDisable", t);
        }
    }

    public void onClientTick(class_310 client) {
        for (Module m : this.modules) {
            if (!this.isEnabled(m.id())) continue;
            try {
                m.onClientTick(client);
            }
            catch (Throwable t) {
                Log.error("Module " + m.id() + " threw onClientTick", t);
            }
        }
    }

    public void onChatMessage(class_2561 message, boolean overlay) {
        for (Module m : this.modules) {
            if (!this.isEnabled(m.id())) continue;
            try {
                m.onChatMessage(message, overlay);
            }
            catch (Throwable t) {
                Log.error("Module " + m.id() + " threw onChatMessage", t);
            }
        }
    }

    public void onHudRender(class_332 drawContext, float tickDelta) {
        for (Module m : this.modules) {
            if (!this.isEnabled(m.id())) continue;
            try {
                m.onHudRender(drawContext, tickDelta);
            }
            catch (Throwable t) {
                Log.error("Module " + m.id() + " threw onHudRender", t);
            }
        }
    }

    public void onJoinWorld() {
        for (Module m : this.modules) {
            if (!this.isEnabled(m.id())) continue;
            try {
                m.onJoinWorld();
            }
            catch (Throwable t) {
                Log.error("Module " + m.id() + " threw onJoinWorld", t);
            }
        }
    }

    public void onLeaveWorld() {
        for (Module m : this.modules) {
            if (!this.isEnabled(m.id())) continue;
            try {
                m.onLeaveWorld();
            }
            catch (Throwable t) {
                Log.error("Module " + m.id() + " threw onLeaveWorld", t);
            }
        }
    }
}

