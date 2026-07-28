package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.MightyClient;
//? if >=26 {
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
//?} else if >=1.21.10 {
/*import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
*///?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
*///?}

public class OnRenderWorld {
    public static void RegisterRenderEvent() {
        //? if >=26.2 {
        /*// 26.2 draws from submitted nodes rather than flushed buffers, so hand the mod's geometry
        // to the frame's collector during the collect phase.
        LevelRenderEvents.COLLECT_SUBMITS.register((context) -> {
            MightyClient.onRenderWorld(context.submitNodeCollector());
        });

        // Note: Post-processing shader is now handled via GameRendererMixin
        // to ensure correct timing in the render pipeline
        *///?} else if >=26 {
        // Render world-space outlines, schematics and measurement labels after translucent features.
        // This mirrors the NeoForge AfterTranslucentParticles stage: the level's translucent pass is
        // still active, so translucent overlays and text glyphs composite into the frame, and the
        // active modelview still holds the camera rotation. Rendering at END_MAIN drops them.
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register((context) -> {
            MightyClient.onRenderWorld();
        });

        // Note: Post-processing shader is now handled via GameRendererMixin
        // to ensure correct timing in the render pipeline
        //?} else if >=1.21.10 {
        /*// Register world rendering for outlines and schematics
        WorldRenderEvents.END_MAIN.register((context) -> {
            MightyClient.onRenderWorld();
        });

        // Note: Post-processing shader is now handled via GameRendererMixin
        // to ensure correct timing in the render pipeline
        *///?} else if >=1.21.6 {
        /*// Register world rendering for outlines and schematics
        WorldRenderEvents.LAST.register((context) -> {
            MightyClient.onRenderWorld();
        });

        // Note: Post-processing shader is now handled via GameRendererMixin
        // to ensure correct timing in the render pipeline
        *///?} else if >=1.21.4 {
        /*// Register world rendering for outlines and schematics
        WorldRenderEvents.LAST.register((context) -> {
            MightyClient.onRenderWorld(context.matrixStack());
        });

        // Note: Post-processing shader is now handled via GameRendererMixin
        // to ensure correct timing in the render pipeline
        *///?} else {
        /*WorldRenderEvents.LAST.register((context) -> MightyClient.onRenderWorld(context.matrixStack()));
        *///?}
    }
}
