//? if >=1.21.4 {
package com.timmie.mightyarchitect.foundation.utility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.foundation.compat.McCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

// The mod's post-processing chain, for the versions that have the declarative post_effect API.
//
// 1.21.4 removed GameRenderer.loadEffect()/shutdownEffect(): a chain is obtained from
// ShaderManager.getPostChain() and processed against the main render target. Below 1.21.4 the
// class does not exist at all - Shaders drives GameRendererAccessor instead - which is why the
// whole file is one arm rather than a set of guarded members.
//
// Documentation here is written as line comments on purpose. An inactive arm is wrapped in a
// block comment, so a nested */ would close the wrapper early and spill the rest into live code.
public class PostChainManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TheMightyArchitect.ID);

    @Nullable
    private static PostChain activePostChain = null;

    @Nullable
    //? if >=1.21.11 {
    private static Identifier activeShaderLocation = null;
    //?} else {
    /*private static ResourceLocation activeShaderLocation = null;
    *///?}

    // Loads and activates a post-processing shader, addressed as namespace:name and resolved to
    // post_effect/<name>.json. An empty path means "no shader". Returns whether the requested
    // state was reached.
    //? if >=1.21.11 {
    public static boolean loadShader(Identifier shaderLocation) {
    //?} else {
    /*public static boolean loadShader(ResourceLocation shaderLocation) {
    *///?}
        Minecraft mc = Minecraft.getInstance();

        // Don't reload if the same shader is already active
        if (activePostChain != null && shaderLocation.equals(activeShaderLocation)) {
            return true;
        }

        // Shutdown any existing shader first
        shutdownShader();

        if (shaderLocation.getPath().isEmpty()) {
            return true;
        }

        try {
            // LevelTargetBundle.MAIN_TARGETS declares the available external targets, as vanilla does
            activePostChain = mc.getShaderManager().getPostChain(
                shaderLocation,
                LevelTargetBundle.MAIN_TARGETS
            );
            activeShaderLocation = shaderLocation;

            LOGGER.debug("Loaded post-processing shader: {}", shaderLocation);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to load post-processing shader: {}", shaderLocation, e);
            activePostChain = null;
            activeShaderLocation = null;
            return false;
        }
    }

    // Drops the active post-processing shader, if there is one.
    public static void shutdownShader() {
        if (activePostChain != null) {
            activePostChain = null;
            activeShaderLocation = null;
            LOGGER.debug("Shutdown post-processing shader");
        }
    }

    // Processes the active shader with the UNPOOLED allocator. This is the NeoForge entry point
    // (via RenderLevelStageEvent), which does not expose GameRenderer's pooled resource allocator.
    // The Fabric mixin path calls the overload below with the pooled one instead.
    public static void processShader(float partialTicks) {
        processShader(partialTicks, GraphicsResourceAllocator.UNPOOLED);
    }

    // Processes the active shader. Call after the world has been rendered to the main framebuffer.
    public static void processShader(float partialTicks, GraphicsResourceAllocator resourceAllocator) {
        if (activePostChain == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = McCompat.mainRenderTarget(mc);

        activePostChain.process(mainTarget, resourceAllocator);

        // Before 1.21.6 the post pass leaves the main target unbound, so subsequent rendering would
        // draw into whatever the chain bound last. From 1.21.6 the pipeline rebinds it itself.
        //? if <1.21.6 {
        /*mainTarget.bindWrite(false);
        *///?}
    }

    // Whether any post-processing shader is active.
    public static boolean isShaderActive() {
        return activePostChain != null;
    }

    // Whether the given post-processing shader is the active one.
    //? if >=1.21.11 {
    public static boolean isShaderActive(Identifier shaderLocation) {
    //?} else {
    /*public static boolean isShaderActive(ResourceLocation shaderLocation) {
    *///?}
        return activePostChain != null && shaderLocation.equals(activeShaderLocation);
    }

    // The active shader's location, or null when no shader is active.
    @Nullable
    //? if >=1.21.11 {
    public static Identifier getActiveShaderLocation() {
    //?} else {
    /*public static ResourceLocation getActiveShaderLocation() {
    *///?}
        return activeShaderLocation;
    }
}
//?}
