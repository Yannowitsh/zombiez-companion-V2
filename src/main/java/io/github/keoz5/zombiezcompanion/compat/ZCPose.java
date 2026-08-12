package io.github.keoz5.zombiezcompanion.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Bridges the two GUI matrix-stack APIs that straddle the 26.1 boundary.
 *
 * <p>In 26.1 {@code GuiGraphicsExtractor#pose()} returns a 2D {@code Matrix3x2fStack}
 * ({@code translate(x, y)}, {@code scale(x, y)}, {@code rotate(radians)}); on
 * pre-26.1 (Mojmap) it returns the 3D {@code PoseStack} whose transforms take a
 * z component and whose rotation goes through {@code mulPose(Axis.ZP.rotation)}.
 * {@code pushMatrix}/{@code popMatrix} are pure renames handled by replacements
 * in the Stonecutter controller; only the arity/rotation differences live here.
 */
public final class ZCPose {
    private ZCPose() {
    }

    public static void translate(GuiGraphicsExtractor ctx, float x, float y) {
        //? if >= 26.1 {
        ctx.pose().translate(x, y);
        //?} else {
        /*ctx.pose().translate(x, y, 0.0f);
        *///?}
    }

    public static void scale(GuiGraphicsExtractor ctx, float x, float y) {
        //? if >= 26.1 {
        ctx.pose().scale(x, y);
        //?} else {
        /*ctx.pose().scale(x, y, 1.0f);
        *///?}
    }

    public static void rotateZ(GuiGraphicsExtractor ctx, float radians) {
        //? if >= 26.1 {
        ctx.pose().rotate(radians);
        //?} else {
        /*ctx.pose().mulPose(com.mojang.math.Axis.ZP.rotation(radians));
        *///?}
    }
}
