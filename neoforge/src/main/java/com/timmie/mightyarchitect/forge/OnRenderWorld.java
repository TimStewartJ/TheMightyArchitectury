package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.MightyClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
//? if >=26 {
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterTranslucentParticles;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterLevel;
//?} else {
/*import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
*///?}

@EventBusSubscriber(value = Dist.CLIENT)
public class OnRenderWorld {
    //? if >=26 {
    // Render outlines and schematics at a mid-level stage (after translucent particles). Here the
    // level's translucent pass is still active, so translucent overlays and world-space text glyphs
    // composite into the frame, and the RenderSystem modelview still holds the camera rotation
    // (so MightyClient.onRenderWorld only applies the world-space offset). Rendering at AfterLevel
    // instead drops all translucent geometry (e.g. the measurement labels) from the frame.
    @SubscribeEvent
    public static void onRenderWorld(AfterTranslucentParticles event) {
        MightyClient.onRenderWorld();
    }

    // Apply post-processing shaders once the level render is fully complete.
    @SubscribeEvent
    public static void onPostRender(AfterLevel event) {
        MightyClient.onPostRender(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
    //?} else {
    /*@SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES)
        {
            MightyClient.onRenderWorld(event.getPoseStack());
        }
    }
    *///?}
}
