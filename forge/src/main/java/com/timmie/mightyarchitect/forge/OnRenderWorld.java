package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.platform.ClientHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class OnRenderWorld {

    // AFTER_PARTICLES keeps the level's translucent pass active, so translucent overlays and
    // world-space text glyphs still composite into the frame (see the NeoForge branch for the
    // full rationale).
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            MightyClient.onRenderWorld(event.getPoseStack());
        }
    }

    // ForgeGui replaces the vanilla Gui instance and overrides render() without calling super, so
    // the shared GuiMixin never fires on this loader. This event is what ForgeGui posts at the end
    // of that override, which is the same point the mixin targets.
    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        //? if >=1.20 {
        ClientHooks.renderHud(event.getGuiGraphics(), event.getPartialTick());
        //?} else {
        /*ClientHooks.renderHud(
            new com.timmie.mightyarchitect.foundation.gui.GuiGraphics(event.getPoseStack()),
            event.getPartialTick());
        *///?}
    }
}
