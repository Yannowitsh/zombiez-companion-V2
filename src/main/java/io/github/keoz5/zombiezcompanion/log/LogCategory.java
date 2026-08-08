package io.github.keoz5.zombiezcompanion.log;

public enum LogCategory {
    CORE("Core"),
    CONFIG("Config"),
    MODULE("Module"),
    EVENT("Event"),
    CHAT("Chat"),
    HUD("Hud");

    private final String tag;

    private LogCategory(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return this.tag;
    }
}

