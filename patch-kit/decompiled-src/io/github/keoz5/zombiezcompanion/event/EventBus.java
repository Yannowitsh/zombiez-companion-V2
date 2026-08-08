/*
 * Decompiled with CFR 0.152.
 */
package io.github.keoz5.zombiezcompanion.event;

import io.github.keoz5.zombiezcompanion.event.Event;
import io.github.keoz5.zombiezcompanion.log.Log;
import io.github.keoz5.zombiezcompanion.log.LogCategory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class EventBus {
    private final Map<Class<? extends Event>, List<Consumer<? extends Event>>> subscribers = new HashMap<Class<? extends Event>, List<Consumer<? extends Event>>>();

    public <E extends Event> void subscribe(Class<E> type, Consumer<E> listener) {
        this.subscribers.computeIfAbsent(type, k -> new ArrayList()).add(listener);
    }

    public <E extends Event> void publish(E event) {
        List<Consumer<? extends Event>> listeners = this.subscribers.get(event.getClass());
        if (listeners == null) {
            return;
        }
        for (Consumer<? extends Event> listener : listeners) {
            try {
                listener.accept(event);
            }
            catch (Throwable t) {
                Log.error("Event listener threw for " + event.getClass().getSimpleName(), t);
            }
        }
        Log.debug(LogCategory.EVENT, event.getClass().getSimpleName());
    }
}

