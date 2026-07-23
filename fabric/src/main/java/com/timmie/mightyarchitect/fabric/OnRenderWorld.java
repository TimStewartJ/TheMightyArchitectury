package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.MightyClient;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public class OnRenderWorld {
    public static void RegisterRenderEvent() {
        // Render world-space outlines, schematics and measurement labels after translucent features.
        // This mirrors the NeoForge AfterTranslucentParticles stage: the level's translucent pass is
        // still active, so translucent overlays and text glyphs composite into the frame, and the
        // active modelview still holds the camera rotation. Rendering at END_MAIN drops them.
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register((context) -> {
            MightyClient.onRenderWorld();
        });

        // Note: Post-processing shader is now handled via GameRendererMixin
        // to ensure correct timing in the render pipeline
    }
}
