//? if >=26 {
package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.MightyClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterTranslucentParticles;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterLevel;

@EventBusSubscriber(value = Dist.CLIENT)
public class OnRenderWorld {
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
}
//?} else if >=1.21.10 {
/*package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.MightyClient;
import net.minecraft.client.Minecraft;
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
        MightyClient.onPostRender(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
}*/
//?} else if >=1.21.6 {
/*package com.timmie.mightyarchitect.forge;

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
}*/
//?} else if >=1.21.4 {
/*package com.timmie.mightyarchitect.forge;

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
}*/
//?} else {
/*package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.MightyClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class OnRenderWorld {
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES)
        {
            MightyClient.onRenderWorld(event.getPoseStack());
        }
    }
}*///?}
