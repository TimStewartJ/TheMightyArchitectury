//? if >=26 {
package com.timmie.mightyarchitect.foundation;

import com.timmie.mightyarchitect.AllSpecialTextures;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

//*
 //* In 1.21.11, RenderType static factory methods moved to
 //* net.minecraft.client.renderer.rendertype.RenderTypes (MC's RenderTypes).
 //* RenderStateShard was removed entirely.
 //* This class delegates to MC's RenderTypes to avoid name conflicts.
 //
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
//?} else if >=1.21.11 {
/*package com.timmie.mightyarchitect.foundation;

import com.timmie.mightyarchitect.AllSpecialTextures;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

//*
 //* In 1.21.11, RenderType static factory methods moved to
 //* net.minecraft.client.renderer.rendertype.RenderTypes (MC's RenderTypes).
 //* RenderStateShard was removed entirely.
 //* This class delegates to MC's RenderTypes to avoid name conflicts.
 //
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
}*/
//?} else if >=1.21.6 {
/*package com.timmie.mightyarchitect.foundation;

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
}*/
//?} else if >=1.21.4 {
/*package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.timmie.mightyarchitect.AllSpecialTextures;
import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

public class RenderTypes extends RenderStateShard {

	protected static final CullStateShard DISABLE_CULLING = new NoCullState();

	public static RenderType getOutlineSolid(ResourceLocation texture) {
		return RenderType.create(createLayerName("outline_solid"), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true,
				false, RenderType.CompositeState.builder()
						.setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
						.setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
						.setLightmapState(LIGHTMAP)
						.setOverlayState(OVERLAY)
						.createCompositeState(true));
	}

	private static final RenderType DEFAULT_OUTLINE_SOLID =
			getOutlineSolid(AllSpecialTextures.BLANK.getLocation());

	public static RenderType getOutlineSolid() {
		return DEFAULT_OUTLINE_SOLID;
	}

	public static RenderType getOutlineTranslucent(ResourceLocation texture, boolean cull) {
		return RenderType.create(createLayerName("outline_translucent" + (cull ? "_cull" : "")),
				DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
						.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
						.setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setCullState(cull ? CULL : NO_CULL)
						.setLightmapState(LIGHTMAP)
						.setOverlayState(OVERLAY)
						.setWriteMaskState(RenderStateShard.COLOR_WRITE)
						.createCompositeState(true));
	}

	public static RenderType getGlowingSolid(ResourceLocation texture) {
		return RenderType.create(createLayerName("glowing_solid"), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256,
				true, false, RenderType.CompositeState.builder()
						.setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
						.setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
						.setLightmapState(LIGHTMAP)
						.setOverlayState(OVERLAY)
						.createCompositeState(true));
	}

	private static final RenderType GLOWING_SOLID_DEFAULT = getGlowingSolid(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getGlowingSolid() {
		return GLOWING_SOLID_DEFAULT;
	}

	public static RenderType getGlowingTranslucent(ResourceLocation texture) {
		return RenderType.create(createLayerName("glowing_translucent"), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
				256, true, true, RenderType.CompositeState.builder()
						.setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
						.setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setCullState(NO_CULL)
						.setLightmapState(LIGHTMAP)
						.setOverlayState(OVERLAY)
						.createCompositeState(true));
	}

	private static final RenderType GLOWING_TRANSLUCENT_DEFAULT = getGlowingTranslucent(TextureAtlas.LOCATION_BLOCKS);

	public static RenderType getGlowingTranslucent() {
		return GLOWING_TRANSLUCENT_DEFAULT;
	}

	private static final RenderType ITEM_PARTIAL_SOLID =
			RenderType.create(createLayerName("item_partial_solid"), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true,
					false, RenderType.CompositeState.builder()
							.setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
							.setTextureState(BLOCK_SHEET)
							.setTransparencyState(NO_TRANSPARENCY)
							.setLightmapState(LIGHTMAP)
							.setOverlayState(OVERLAY)
							.createCompositeState(true));

	public static RenderType getItemPartialSolid() {
		return ITEM_PARTIAL_SOLID;
	}

	private static final RenderType ITEM_PARTIAL_TRANSLUCENT = RenderType.create(createLayerName("item_partial_translucent"),
			DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
					.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
					.setTextureState(BLOCK_SHEET)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.createCompositeState(true));

	public static RenderType getItemPartialTranslucent() {
		return ITEM_PARTIAL_TRANSLUCENT;
	}

	private static final RenderType FLUID = RenderType.create(createLayerName("fluid"),
			DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
					.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
					.setTextureState(BLOCK_SHEET_MIPPED)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.createCompositeState(true));

	public static RenderType getFluid() {
		return FLUID;
	}

	protected static class NoCullState extends CullStateShard {
		public NoCullState() {
			super(false);
		}

		@Override
		public void setupRenderState() {
			RenderSystem.disableCull();
		}
	}

	private static String createLayerName(String name) {
		return TheMightyArchitect.ID + ":" + name;
	}

	// Mmm gimme those protected fields
	public RenderTypes() {
		super(null, null, null);
	}
}*/
//?} else {
/*package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.timmie.mightyarchitect.AllSpecialTextures;
import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public class RenderTypes extends RenderStateShard {

	protected static final CullStateShard DISABLE_CULLING = new NoCullState();

	public static RenderType getOutlineSolid(ResourceLocation texture) {
		return RenderType.create(createLayerName("outline_solid"), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true,
				false, RenderType.CompositeState.builder()
						.setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
						.setTextureState(new TextureStateShard(texture, false, false))
						.setLightmapState(LIGHTMAP)
						.setOverlayState(OVERLAY)
						.createCompositeState(true));
	}

	private static final RenderType DEFAULT_OUTLINE_SOLID =
			getOutlineSolid(AllSpecialTextures.BLANK.getLocation());

	public static RenderType getOutlineSolid() {
		return DEFAULT_OUTLINE_SOLID;
	}

	public static RenderType getOutlineTranslucent(ResourceLocation texture, boolean cull) {
		return RenderType.create(createLayerName("outline_translucent" + (cull ? "_cull" : "")),
				DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
						.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
						.setTextureState(new TextureStateShard(texture, false, false))
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setCullState(cull ? CULL : NO_CULL)
						.setLightmapState(LIGHTMAP)
						.setOverlayState(OVERLAY)
						.setWriteMaskState(RenderStateShard.COLOR_WRITE)
						.createCompositeState(true));
	}

	public static RenderType getGlowingSolid(ResourceLocation texture) {
		return RenderType.create(createLayerName("glowing_solid"), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256,
				true, false, RenderType.CompositeState.builder()
						.setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
						.setTextureState(new TextureStateShard(texture, false, false))
						.setLightmapState(LIGHTMAP)
						.setOverlayState(OVERLAY)
						.createCompositeState(true));
	}

	private static final RenderType GLOWING_SOLID_DEFAULT = getGlowingSolid(InventoryMenu.BLOCK_ATLAS);

	public static RenderType getGlowingSolid() {
		return GLOWING_SOLID_DEFAULT;
	}

	public static RenderType getGlowingTranslucent(ResourceLocation texture) {
		return RenderType.create(createLayerName("glowing_translucent"), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
				256, true, true, RenderType.CompositeState.builder()
						.setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
						.setTextureState(new TextureStateShard(texture, false, false))
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setCullState(NO_CULL)
						.setLightmapState(LIGHTMAP)
						.setOverlayState(OVERLAY)
						.createCompositeState(true));
	}

	private static final RenderType GLOWING_TRANSLUCENT_DEFAULT = getGlowingTranslucent(InventoryMenu.BLOCK_ATLAS);

	public static RenderType getGlowingTranslucent() {
		return GLOWING_TRANSLUCENT_DEFAULT;
	}

	private static final RenderType ITEM_PARTIAL_SOLID =
			RenderType.create(createLayerName("item_partial_solid"), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true,
					false, RenderType.CompositeState.builder()
							.setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
							.setTextureState(BLOCK_SHEET)
							.setTransparencyState(NO_TRANSPARENCY)
							.setLightmapState(LIGHTMAP)
							.setOverlayState(OVERLAY)
							.createCompositeState(true));

	public static RenderType getItemPartialSolid() {
		return ITEM_PARTIAL_SOLID;
	}

	private static final RenderType ITEM_PARTIAL_TRANSLUCENT = RenderType.create(createLayerName("item_partial_translucent"),
			DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
					.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
					.setTextureState(BLOCK_SHEET)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.createCompositeState(true));

	public static RenderType getItemPartialTranslucent() {
		return ITEM_PARTIAL_TRANSLUCENT;
	}

	private static final RenderType FLUID = RenderType.create(createLayerName("fluid"),
			DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
					.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
					.setTextureState(BLOCK_SHEET_MIPPED)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.createCompositeState(true));

	public static RenderType getFluid() {
		return FLUID;
	}

	protected static class NoCullState extends CullStateShard {
		public NoCullState() {
			super(false);
		}

		@Override
		public void setupRenderState() {
			RenderSystem.disableCull();
		}
	}

	private static String createLayerName(String name) {
		return TheMightyArchitect.ID + ":" + name;
	}

	// Mmm gimme those protected fields
	public RenderTypes() {
		super(null, null, null);
	}
}*///?}
