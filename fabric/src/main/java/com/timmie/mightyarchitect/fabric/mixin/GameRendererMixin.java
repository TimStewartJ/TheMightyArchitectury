package com.timmie.mightyarchitect.fabric.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
//? if >=26 {
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.timmie.mightyarchitect.foundation.utility.PostChainManager;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}

/**
 * Mixin to apply post-processing shaders at the correct point in the render pipeline.
 * This injects after renderLevel() completes but before the GUI is rendered,
 * which is the correct timing for post-processing effects.
 *
 * On Minecraft 1.21.1 the post-processing shader is driven differently (see OnRenderWorld),
 * so this mixin is an inert shell there.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    //? if >=26 {
    @Shadow
    @Final
    private CrossFrameResourcePool resourcePool;

    /**
     * Inject after renderLevel() to apply post-processing shaders.
     * The injection point is at the TAIL of renderLevel(), ensuring the world
     * has been fully rendered to the framebuffer before we process it.
     */
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void mightyarchitect$afterRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        PostChainManager.processShader(partialTicks, resourcePool);
    }
    //?}
}
