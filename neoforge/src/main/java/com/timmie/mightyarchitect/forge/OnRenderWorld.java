package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.MightyClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class OnRenderWorld {
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        // Render outlines and schematics after particles
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            MightyClient.onRenderWorld(event.getPoseStack());
        }

        // Apply post-processing shaders after level rendering is complete
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            MightyClient.onPostRender(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }
}
