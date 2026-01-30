package com.timmie.mightyarchitect.foundation;

import com.timmie.mightyarchitect.AllSpecialTextures;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class RenderTypes extends RenderStateShard {

	// In 1.21.6, use the built-in RenderType factory methods instead of custom composite states
	// The CompositeState.builder() methods are now protected

	public static RenderType getOutlineSolid(ResourceLocation texture) {
		return RenderType.entityCutout(texture);
	}

	private static final RenderType DEFAULT_OUTLINE_SOLID =
			getOutlineSolid(AllSpecialTextures.BLANK.getLocation());

	public static RenderType getOutlineSolid() {
		return DEFAULT_OUTLINE_SOLID;
	}

	public static RenderType getOutlineTranslucent(ResourceLocation texture, boolean cull) {
		// Use entityTranslucent for translucent outline rendering
		return RenderType.entityTranslucent(texture, !cull);
	}

	public static RenderType getGlowingSolid(ResourceLocation texture) {
		return RenderType.entitySolid(texture);
	}

	@SuppressWarnings("deprecation")
	private static final RenderType GLOWING_SOLID_DEFAULT = getGlowingSolid(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getGlowingSolid() {
		return GLOWING_SOLID_DEFAULT;
	}

	public static RenderType getGlowingTranslucent(ResourceLocation texture) {
		return RenderType.entityTranslucent(texture);
	}

	@SuppressWarnings("deprecation")
	private static final RenderType GLOWING_TRANSLUCENT_DEFAULT = getGlowingTranslucent(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getGlowingTranslucent() {
		return GLOWING_TRANSLUCENT_DEFAULT;
	}

	@SuppressWarnings("deprecation")
	private static final RenderType ITEM_PARTIAL_SOLID =
			RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getItemPartialSolid() {
		return ITEM_PARTIAL_SOLID;
	}

	@SuppressWarnings("deprecation")
	private static final RenderType ITEM_PARTIAL_TRANSLUCENT =
			RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getItemPartialTranslucent() {
		return ITEM_PARTIAL_TRANSLUCENT;
	}

	@SuppressWarnings("deprecation")
	private static final RenderType FLUID =
			RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getFluid() {
		return FLUID;
	}

	// Mmm gimme those protected fields
	public RenderTypes() {
		super(null, null, null);
	}
}
