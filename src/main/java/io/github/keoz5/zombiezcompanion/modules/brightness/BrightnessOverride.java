package io.github.keoz5.zombiezcompanion.modules.brightness;

public final class BrightnessOverride {
    private static volatile boolean active = false;
    private static volatile double target = 15.0;

    private BrightnessOverride() {
    }

    public static void enable(double targetValue) {
        target = targetValue;
        active = true;
    }

    public static void disable() {
        active = false;
    }

    public static void setTarget(double targetValue) {
        target = targetValue;
    }

    public static boolean isActive() {
        return active;
    }

    public static double target() {
        return target;
    }
}

