package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.MightyClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class OnRenderWorld {
    public static void RegisterRenderEvent() {
        // Register world rendering for outlines and schematics
        WorldRenderEvents.LAST.register((context) -> {
            MightyClient.onRenderWorld(new GuiGraphics(Minecraft.getInstance(), context.matrixStack(), Minecraft.getInstance().renderBuffers().bufferSource()));
        });

        // Note: Post-processing shader is now handled via GameRendererMixin
        // to ensure correct timing in the render pipeline
    }
}
