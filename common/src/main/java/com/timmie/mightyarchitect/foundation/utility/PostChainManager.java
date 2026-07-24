//? if >=26 {
package com.timmie.mightyarchitect.foundation.utility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

//*
 //* Manages post-processing shader effects using the 1.21.4 PostChain API.
 //* <p>
 //* The GameRenderer.loadEffect()/shutdownEffect() methods were removed in 1.21.4.
 //* This class provides equivalent functionality by using ShaderManager.getPostChain().
 //
public class PostChainManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TheMightyArchitect.ID);

    @Nullable
    private static PostChain activePostChain = null;

    @Nullable
    private static Identifier activeShaderLocation = null;

    //*
     //* Loads and activates a post-processing shader.
     //*
     //* @param shaderLocation The resource location of the shader (e.g., "mightyarchitect:blueprint")
     //* @return true if the shader was loaded successfully, false otherwise
     //
    public static boolean loadShader(Identifier shaderLocation) {
        Minecraft mc = Minecraft.getInstance();

        // Don't reload if the same shader is already active
        if (activePostChain != null && shaderLocation.equals(activeShaderLocation)) {
            return true;
        }

        // Shutdown any existing shader first
        shutdownShader();

        if (shaderLocation.getPath().isEmpty()) {
            // Empty path means "no shader"
            return true;
        }

        try {
            // PostChain is obtained via ShaderManager.getPostChain().
            // Use LevelTargetBundle.MAIN_TARGETS to declare available external targets (like vanilla does)
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

    //*
     //* Shuts down the currently active post-processing shader.
     //
    public static void shutdownShader() {
        if (activePostChain != null) {
            activePostChain = null;
            activeShaderLocation = null;
            LOGGER.debug("Shutdown post-processing shader");
        }
    }

    //*
     //* Processes the active post-processing shader using the UNPOOLED resource allocator.
     //* This is the NeoForge entry point (via {@code RenderLevelStageEvent}), which does not
     //* expose GameRenderer's pooled resource allocator. The Fabric mixin path uses the
     //* {@link #processShader(float, GraphicsResourceAllocator)} overload with the pooled allocator.
     //*
     //* @param partialTicks The partial tick time
     //
    public static void processShader(float partialTicks) {
        processShader(partialTicks, GraphicsResourceAllocator.UNPOOLED);
    }

    //*
     //* Processes the active post-processing shader.
     //* This should be called after the world has been rendered to the main framebuffer.
     //*
     //* @param partialTicks The partial tick time
     //* @param resourceAllocator The graphics resource allocator (use GameRenderer's resourcePool)
     //
    public static void processShader(float partialTicks, GraphicsResourceAllocator resourceAllocator) {
        if (activePostChain == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.getMainRenderTarget();

        // Apply the post-processing effect using the provided resource allocator (like vanilla does)
        activePostChain.process(mainTarget, resourceAllocator);
    }

    //*
     //* Checks if a shader is currently active.
     //*
     //* @return true if a post-processing shader is active
     //
    public static boolean isShaderActive() {
        return activePostChain != null;
    }

    //*
     //* Checks if a specific shader is currently active.
     //*
     //* @param shaderLocation The shader location to check
     //* @return true if the specified shader is active
     //
    public static boolean isShaderActive(Identifier shaderLocation) {
        return activePostChain != null && shaderLocation.equals(activeShaderLocation);
    }

    //*
     //* Gets the currently active shader location.
     //*
     //* @return The active shader location, or null if no shader is active
     //
    @Nullable
    public static Identifier getActiveShaderLocation() {
        return activeShaderLocation;
    }
}
//?} else if >=1.21.4 {
/*package com.timmie.mightyarchitect.foundation.utility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

//*
 //* Manages post-processing shader effects using the 1.21.4 PostChain API.
 //* <p>
 //* The GameRenderer.loadEffect()/shutdownEffect() methods were removed in 1.21.4.
 //* This class provides equivalent functionality by using ShaderManager.getPostChain().
 //
public class PostChainManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TheMightyArchitect.ID);

    @Nullable
    private static PostChain activePostChain = null;

    @Nullable
    private static ResourceLocation activeShaderLocation = null;

    //*
     //* Loads and activates a post-processing shader.
     //*
     //* @param shaderLocation The resource location of the shader (e.g., "mightyarchitect:blueprint")
     //* @return true if the shader was loaded successfully, false otherwise
     //
    public static boolean loadShader(ResourceLocation shaderLocation) {
        Minecraft mc = Minecraft.getInstance();

        // Don't reload if the same shader is already active
        if (activePostChain != null && shaderLocation.equals(activeShaderLocation)) {
            return true;
        }

        // Shutdown any existing shader first
        shutdownShader();

        if (shaderLocation.getPath().isEmpty()) {
            // Empty path means "no shader"
            return true;
        }

        try {
            // In 1.21.4, PostChain is obtained via ShaderManager.getPostChain()
            // Use LevelTargetBundle.MAIN_TARGETS to declare available external targets (like vanilla does)
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

    //*
     //* Shuts down the currently active post-processing shader.
     //
    public static void shutdownShader() {
        if (activePostChain != null) {
            activePostChain = null;
            activeShaderLocation = null;
            LOGGER.debug("Shutdown post-processing shader");
        }
    }

    //*
     //* Processes the active post-processing shader using UNPOOLED resource allocator.
     //* For optimal performance, prefer the overload that accepts a GraphicsResourceAllocator.
     //*
     //* @param partialTicks The partial tick time
     //* @deprecated Use {@link #processShader(float, GraphicsResourceAllocator)} with GameRenderer's resourcePool
     //
    @Deprecated
    public static void processShader(float partialTicks) {
        processShader(partialTicks, GraphicsResourceAllocator.UNPOOLED);
    }

    //*
     //* Processes the active post-processing shader.
     //* This should be called after the world has been rendered to the main framebuffer.
     //*
     //* @param partialTicks The partial tick time
     //* @param resourceAllocator The graphics resource allocator (use GameRenderer's resourcePool)
     //
    public static void processShader(float partialTicks, GraphicsResourceAllocator resourceAllocator) {
        if (activePostChain == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.getMainRenderTarget();

        // Apply the post-processing effect using the provided resource allocator (like vanilla does)
        activePostChain.process(mainTarget, resourceAllocator);

        // Bind the main framebuffer back for subsequent rendering
        mainTarget.bindWrite(false);
    }

    //*
     //* Checks if a shader is currently active.
     //*
     //* @return true if a post-processing shader is active
     //
    public static boolean isShaderActive() {
        return activePostChain != null;
    }

    //*
     //* Checks if a specific shader is currently active.
     //*
     //* @param shaderLocation The shader location to check
     //* @return true if the specified shader is active
     //
    public static boolean isShaderActive(ResourceLocation shaderLocation) {
        return activePostChain != null && shaderLocation.equals(activeShaderLocation);
    }

    //*
     //* Gets the currently active shader location.
     //*
     //* @return The active shader location, or null if no shader is active
     //
    @Nullable
    public static ResourceLocation getActiveShaderLocation() {
        return activeShaderLocation;
    }
}*/
//?} else {
/**///?}
