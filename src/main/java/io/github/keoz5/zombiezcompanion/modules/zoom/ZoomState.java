package io.github.keoz5.zombiezcompanion.modules.zoom;

public final class ZoomState {
    private static final float SMOOTHING = 0.18f;
    private static float previousMultiplier = 1.0f;
    private static float currentMultiplier = 1.0f;
    private static float targetMultiplier = 1.0f;

    private ZoomState() {
    }

    public static void update(boolean zooming, double zoomFactor) {
        float factor = (float)Math.max(2.0, Math.min(8.0, zoomFactor));
        targetMultiplier = zooming ? 1.0f / factor : 1.0f;
        previousMultiplier = currentMultiplier;
        if (Math.abs(targetMultiplier - (currentMultiplier += (targetMultiplier - currentMultiplier) * 0.18f)) < 0.001f) {
            currentMultiplier = targetMultiplier;
        }
    }

    public static void reset() {
        previousMultiplier = 1.0f;
        currentMultiplier = 1.0f;
        targetMultiplier = 1.0f;
    }

    public static float fovMultiplier(float tickDelta) {
        float clampedTickDelta = Math.max(0.0f, Math.min(1.0f, tickDelta));
        return previousMultiplier + (currentMultiplier - previousMultiplier) * clampedTickDelta;
    }
}

