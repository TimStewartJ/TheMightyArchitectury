package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.MightyClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class OnRenderWorld {
    public static void RegisterRenderEvent() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register((context) -> MightyClient.onRenderWorld(context.matrixStack()));
    }
}
