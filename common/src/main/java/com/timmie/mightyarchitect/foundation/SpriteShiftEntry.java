package com.timmie.mightyarchitect.foundation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public class SpriteShiftEntry {
	protected ResourceLocation originalTextureLocation;
	protected ResourceLocation targetTextureLocation;
	protected TextureAtlasSprite original;
	protected TextureAtlasSprite target;

	public void set(ResourceLocation originalTextureLocation, ResourceLocation targetTextureLocation) {
		this.originalTextureLocation = originalTextureLocation;
		this.targetTextureLocation = targetTextureLocation;
	}

	protected void loadTextures() {
		TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
		original = atlas.getSprite(originalTextureLocation);
		target = atlas.getSprite(targetTextureLocation);
	}

	public ResourceLocation getTargetResourceLocation() {
		return targetTextureLocation;
	}

	public TextureAtlasSprite getTarget() {
		if (target == null)
			loadTextures();
		return target;
	}

	public TextureAtlasSprite getOriginal() {
		if (original == null)
			loadTextures();
		return original;
	}
}