/*
 * Decompiled with CFR 0.152.
 */
package io.github.keoz5.zombiezcompanion.core;

import io.github.keoz5.zombiezcompanion.config.ConfigManager;
import io.github.keoz5.zombiezcompanion.event.EventBus;

public record ModuleContext(ConfigManager configManager, EventBus eventBus) {
}

