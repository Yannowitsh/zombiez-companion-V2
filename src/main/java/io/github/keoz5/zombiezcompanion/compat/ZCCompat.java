package io.github.keoz5.zombiezcompanion.compat;

import net.minecraft.client.Camera;

/**
 * Small cross-version shims for API gaps that are not simple renames.
 */
public final class ZCCompat {
    private ZCCompat() {
    }

    /**
     * Vertical field of view in degrees, used as a projection heuristic (callers clamp
     * it to [12, 110]). 26.1 exposes it on {@code Camera#getFov()}; pre-26.1 has no such
     * accessor, so fall back to the player's FOV option setting.
     */
    public static double cameraFov(Camera camera) {
        //? if >= 26.1 {
        return camera.getFov();
        //?} else {
        /*return (double) (int) net.minecraft.client.Minecraft.getInstance().options.fov().get();
        *///?}
    }
}
