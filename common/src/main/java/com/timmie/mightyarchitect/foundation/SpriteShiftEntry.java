package com.timmie.mightyarchitect.foundation;

import net.minecraft.client.Minecraft;
//? if >=1.21.4 {
import net.minecraft.client.renderer.texture.TextureAtlas;
//?} else {
/*
*///?}
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else if >=1.21.4 {
/*import net.minecraft.resources.ResourceLocation;
*///?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
*///?}

//? if >=1.21.10 {
//?} else {
/*import java.util.function.Function;

*///?}
public class SpriteShiftEntry {
	//? if >=1.21.11 {
	protected Identifier originalTextureLocation;
	protected Identifier targetTextureLocation;
	//?} else {
	/*protected ResourceLocation originalTextureLocation;
	protected ResourceLocation targetTextureLocation;
	*///?}
	protected TextureAtlasSprite original;
	protected TextureAtlasSprite target;

	//? if >=1.21.11 {
	public void set(Identifier originalTextureLocation, Identifier targetTextureLocation) {
	//?} else {
	/*public void set(ResourceLocation originalTextureLocation, ResourceLocation targetTextureLocation) {
	*///?}
		this.originalTextureLocation = originalTextureLocation;
		this.targetTextureLocation = targetTextureLocation;
	}

	protected void loadTextures() {
		//? if >=1.21.10 {
		TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
		original = atlas.getSprite(originalTextureLocation);
		target = atlas.getSprite(targetTextureLocation);
		//?} else if >=1.21.4 {
		/*Function<ResourceLocation, TextureAtlasSprite> textureMap = Minecraft.getInstance()
			.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
		original = textureMap.apply(originalTextureLocation);
		target = textureMap.apply(targetTextureLocation);
		*///?} else {
		/*Function<ResourceLocation, TextureAtlasSprite> textureMap = Minecraft.getInstance()
			.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
		original = textureMap.apply(originalTextureLocation);
		target = textureMap.apply(targetTextureLocation);
		*///?}
	}

	//? if >=1.21.11 {
	public Identifier getTargetResourceLocation() {
	//?} else {
	/*public ResourceLocation getTargetResourceLocation() {
	*///?}
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
