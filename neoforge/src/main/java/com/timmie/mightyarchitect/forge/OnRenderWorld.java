package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.MightyClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterParticles;

@EventBusSubscriber(value = Dist.CLIENT)
public class OnRenderWorld {
    @SubscribeEvent
    public static void onRenderWorld(AfterParticles event) {
        // Render outlines and schematics
        MightyClient.onRenderWorld();
        
        // Apply post-processing shaders after level rendering
        MightyClient.onPostRender(event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}
