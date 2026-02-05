package com.timmie.mightyarchitect.foundation;

import com.timmie.mightyarchitect.AllSpecialTextures;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * In 1.21.11, RenderType static factory methods moved to
 * net.minecraft.client.renderer.rendertype.RenderTypes (MC's RenderTypes).
 * RenderStateShard was removed entirely.
 * This class delegates to MC's RenderTypes to avoid name conflicts.
 */
public class RenderTypes {

	private static final net.minecraft.client.renderer.rendertype.RenderTypes MC_RENDER_TYPES = null; // not instantiated, static only

	public static RenderType getOutlineSolid(Identifier texture) {
		return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(texture);
	}

	private static final RenderType DEFAULT_OUTLINE_SOLID =
			getOutlineSolid(AllSpecialTextures.BLANK.getLocation());

	public static RenderType getOutlineSolid() {
		return DEFAULT_OUTLINE_SOLID;
	}

	public static RenderType getOutlineTranslucent(Identifier texture, boolean cull) {
		return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture, !cull);
	}

	public static RenderType getGlowingSolid(Identifier texture) {
		return net.minecraft.client.renderer.rendertype.RenderTypes.entitySolid(texture);
	}

	@SuppressWarnings("deprecation")
	private static final RenderType GLOWING_SOLID_DEFAULT = getGlowingSolid(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getGlowingSolid() {
		return GLOWING_SOLID_DEFAULT;
	}

	public static RenderType getGlowingTranslucent(Identifier texture) {
		return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
	}

	@SuppressWarnings("deprecation")
	private static final RenderType GLOWING_TRANSLUCENT_DEFAULT = getGlowingTranslucent(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getGlowingTranslucent() {
		return GLOWING_TRANSLUCENT_DEFAULT;
	}

	@SuppressWarnings("deprecation")
	private static final RenderType ITEM_PARTIAL_SOLID =
			net.minecraft.client.renderer.rendertype.RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getItemPartialSolid() {
		return ITEM_PARTIAL_SOLID;
	}

	@SuppressWarnings("deprecation")
	private static final RenderType ITEM_PARTIAL_TRANSLUCENT =
			net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getItemPartialTranslucent() {
		return ITEM_PARTIAL_TRANSLUCENT;
	}

	@SuppressWarnings("deprecation")
	private static final RenderType FLUID =
			net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getFluid() {
		return FLUID;
	}
}
