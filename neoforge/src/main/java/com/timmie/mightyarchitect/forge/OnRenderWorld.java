package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.MightyClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterLevel;

@EventBusSubscriber(value = Dist.CLIENT)
public class OnRenderWorld {
    @SubscribeEvent
    public static void onRenderWorld(AfterLevel event) {
        // Render outlines and schematics
        MightyClient.onRenderWorld();
        
        // Apply post-processing shaders after level rendering
        MightyClient.onPostRender(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
}
