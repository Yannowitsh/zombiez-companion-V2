plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "26.1.2-fabric" /* [SC] DO NOT EDIT */

// See https://stonecutter.kikugie.dev/wiki/config/params
//
// Canonical source is written in Minecraft 26.1 (official) names. For pre-26.1
// targets (1.21.4, Mojmap), swap to the older Mojang mappings. Only pure,
// collision-free renames live here; genuine API differences (render-state
// extraction, input events, HUD/ChatScreen APIs) are handled with `//?`
// conditionals in the source (see Task #3).
stonecutter parameters {
    replacements {
        // net.minecraft.resources.Identifier (26.1) -> ResourceLocation (<=1.21)
        // Same package, so this fixes both the import and every usage.
        string {
            direction = eval(current.version, "<26.1")
            replace("Identifier", "ResourceLocation")
        }
        // net.minecraft.client.gui.GuiGraphicsExtractor (26.1) -> GuiGraphics (<=1.21)
        string {
            direction = eval(current.version, "<26.1")
            replace("GuiGraphicsExtractor", "GuiGraphics")
        }
        // Util moved from net.minecraft.util.Util (26.1) to net.minecraft.Util (<=1.21).
        string {
            direction = eval(current.version, "<26.1")
            replace("net.minecraft.util.Util", "net.minecraft.Util")
        }
        // Fabric key-binding helper: renamed keymapping.v1.KeyMappingHelper (26.1) to
        // keybinding.v1.KeyBindingHelper (<=1.21). Both tokens are unique per version.
        string {
            direction = eval(current.version, "<26.1")
            replace(
                "net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper",
                "net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper",
            )
        }
        string {
            direction = eval(current.version, "<26.1")
            replace("KeyMappingHelper.registerKeyMapping", "KeyBindingHelper.registerKeyBinding")
        }
        // Zombie moved to net.minecraft.world.entity.monster.zombie.Zombie (26.1) from
        // net.minecraft.world.entity.monster.Zombie (<=1.21). FQN is unique per version.
        string {
            direction = eval(current.version, "<26.1")
            replace(
                "net.minecraft.world.entity.monster.zombie.Zombie",
                "net.minecraft.world.entity.monster.Zombie",
            )
        }
        // RenderType static line factory: 26.1 split it onto RenderTypes.lines(); <=1.21
        // keeps it on RenderType.lines(). The full call is unique per version.
        string {
            direction = eval(current.version, "<26.1")
            replace("RenderTypes.lines()", "RenderType.lines()")
        }
        // Fabric world-render API: 26.1 renamed WorldRenderEvents/WorldRenderContext to
        // LevelRenderEvents/LevelRenderContext (class names unique per version). The import
        // PACKAGE (rendering.v1.level vs rendering.v1), the AFTER_TRANSLUCENT[_TERRAIN]
        // constant, and poseStack()/matrixStack() still differ -> handled by //? in-source.
        string {
            direction = eval(current.version, "<26.1")
            replace("LevelRenderContext", "WorldRenderContext")
        }
        string {
            direction = eval(current.version, "<26.1")
            replace("LevelRenderEvents", "WorldRenderEvents")
        }
        // GuiGraphics draw helpers were renamed in 26.1 (text/outline/item/centeredText)
        // from the <=1.21 names (drawString/renderOutline/renderItem/drawCenteredString).
        // Argument lists are identical -> pure renames. Scoped to the `ctx.` receiver so
        // the tokens are unique per version and collision-free (the lone `t.text(` stays).
        string {
            direction = eval(current.version, "<26.1")
            replace("ctx.text(", "ctx.drawString(")
        }
        string {
            direction = eval(current.version, "<26.1")
            replace("ctx.outline(", "ctx.renderOutline(")
        }
        string {
            direction = eval(current.version, "<26.1")
            replace("ctx.item(", "ctx.renderItem(")
        }
        string {
            direction = eval(current.version, "<26.1")
            replace("ctx.centeredText(", "ctx.drawCenteredString(")
        }
        // GUI matrix stack push/pop: 26.1 Matrix3x2fStack pushMatrix/popMatrix vs the
        // pre-26.1 PoseStack pushPose/popPose. Pure renames (all are `ctx.pose().X()`);
        // translate/scale/rotate differ in arity/API and go through ZCPose instead.
        string {
            direction = eval(current.version, "<26.1")
            replace("ctx.pose().pushMatrix()", "ctx.pose().pushPose()")
        }
        string {
            direction = eval(current.version, "<26.1")
            replace("ctx.pose().popMatrix()", "ctx.pose().popPose()")
        }
        // Camera accessors renamed in 26.1 (yaw/xRot/position) vs pre-26.1 Mojmap
        // (getYRot/getXRot/getPosition). SCOPED to the `camera.` receiver: Entity also
        // has getYRot/getXRot/position, but those calls go through `player`, so scoping
        // keeps the reverse collision-free (no `camera.getYRot(` etc. exist in 26.1).
        string {
            direction = eval(current.version, "<26.1")
            replace("camera.position(", "camera.getPosition(")
        }
        string {
            direction = eval(current.version, "<26.1")
            replace("camera.xRot(", "camera.getXRot(")
        }
        string {
            direction = eval(current.version, "<26.1")
            replace("camera.yaw(", "camera.getYRot(")
        }
        // ResourceKey#identifier() (26.1) -> location() (<=1.21). Scoped to dimension().
        string {
            direction = eval(current.version, "<26.1")
            replace(".dimension().identifier()", ".dimension().location()")
        }
        // Entity#entityTags() (26.1) -> getTags() (<=1.21).
        string {
            direction = eval(current.version, "<26.1")
            replace(".entityTags()", ".getTags()")
        }
        // ChatComponent#addClientSystemMessage (26.1) -> addMessage (<=1.21).
        string {
            direction = eval(current.version, "<26.1")
            replace("getChat().addClientSystemMessage(", "getChat().addMessage(")
        }
        // Window#handle() (26.1) -> getWindow() returns the GLFW handle (<=1.21). Scoped
        // to getWindow().handle() so CompletableFuture#handle() elsewhere is untouched.
        string {
            direction = eval(current.version, "<26.1")
            replace("getWindow().handle()", "getWindow().getWindow()")
        }
        // Fabric client-command entrypoint: class ClientCommands (26.1) was ClientCommandManager
        // (<=1.21), same package. The token has a trailing 's' so it never matches
        // ClientCommandRegistrationCallback / FabricClientCommandSource.
        string {
            direction = eval(current.version, "<26.1")
            replace("ClientCommands", "ClientCommandManager")
        }
        // authlib GameProfile#name() (26.1 record accessor) -> getName() (<=1.21).
        string {
            direction = eval(current.version, "<26.1")
            replace("getGameProfile().name()", "getGameProfile().getName()")
        }
    }
}
