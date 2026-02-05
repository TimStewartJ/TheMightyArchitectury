package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.MightyClient;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class OnRenderWorld {
    public static void RegisterRenderEvent() {
        // Register world rendering for outlines and schematics
        WorldRenderEvents.END_MAIN.register((context) -> {
            MightyClient.onRenderWorld();
        });

        // Note: Post-processing shader is now handled via GameRendererMixin
        // to ensure correct timing in the render pipeline
    }
}
