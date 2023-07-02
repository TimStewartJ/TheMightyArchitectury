package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.MightyClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class OnRenderWorld {
    public static void RegisterRenderEvent() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register((context) -> MightyClient.onRenderWorld(new GuiGraphics(Minecraft.getInstance(), context.matrixStack(), Minecraft.getInstance().renderBuffers().bufferSource())));
    }
}
