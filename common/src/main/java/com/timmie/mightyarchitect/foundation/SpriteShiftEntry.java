package com.timmie.mightyarchitect.foundation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public class SpriteShiftEntry {
	protected Identifier originalTextureLocation;
	protected Identifier targetTextureLocation;
	protected TextureAtlasSprite original;
	protected TextureAtlasSprite target;

	public void set(Identifier originalTextureLocation, Identifier targetTextureLocation) {
		this.originalTextureLocation = originalTextureLocation;
		this.targetTextureLocation = targetTextureLocation;
	}

	protected void loadTextures() {
		TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
		original = atlas.getSprite(originalTextureLocation);
		target = atlas.getSprite(targetTextureLocation);
	}

	public Identifier getTargetResourceLocation() {
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