package com.timmie.mightyarchitect.mixin;

import net.minecraft.client.renderer.GameRenderer;
//? if <1.21.4 {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import org.spongepowered.asm.mixin.Mixin;
//? if <1.21.4 {
/*import org.spongepowered.asm.mixin.gen.Invoker;
*///?}

/**
 * Access to the post-effect loader on the versions that predate the {@code PostChainManager} API.
 * <p>
 * Before 1.21.4 the only way in is {@code GameRenderer.loadEffect(ResourceLocation)}, which is
 * package-private up to 1.20.4 and private from 1.20.6. Reaching it by reflection does not work:
 * a by-name lookup fails under any remapped runtime, and the fallback - scanning every declared
 * method for a one-argument {@code ResourceLocation} method returning void - binds by shape, so it
 * would silently pick a different method the moment one is added. An {@code @Invoker} is remapped
 * by the annotation processor, so a rename becomes a load-time failure with a name in it instead
 * of a shader that quietly never applies.
 * <p>
 * 1.21.4 renamed the method to {@code setPostEffect} and routed the mod through
 * {@code PostChainManager}, so the accessor is guarded off there and this mixin applies nothing.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

	//? if <1.21.4 {
	/*@Invoker("loadEffect")
	void invokeLoadEffect(ResourceLocation location);
	*///?}
}
