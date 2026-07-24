//? if >=26 {
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
//?} else if >=1.21.10 {
/*package com.timmie.mightyarchitect.foundation;

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
}*/
//?} else if >=1.21.4 {
/*package com.timmie.mightyarchitect.foundation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

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
		Function<ResourceLocation, TextureAtlasSprite> textureMap = Minecraft.getInstance()
			.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
		original = textureMap.apply(originalTextureLocation);
		target = textureMap.apply(targetTextureLocation);
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
}*/
//?} else {
/*package com.timmie.mightyarchitect.foundation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.function.Function;

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
		Function<ResourceLocation, TextureAtlasSprite> textureMap = Minecraft.getInstance()
			.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
		original = textureMap.apply(originalTextureLocation);
		target = textureMap.apply(targetTextureLocation);
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
}*///?}
