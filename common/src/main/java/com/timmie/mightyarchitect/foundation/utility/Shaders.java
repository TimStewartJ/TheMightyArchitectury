package com.timmie.mightyarchitect.foundation.utility;

import com.timmie.mightyarchitect.TheMightyArchitect;
//? if >=1.21.4 {
//?} else {
/*import com.timmie.mightyarchitect.mixin.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
*///?}
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * The mod's post-processing effects.
 * <p>
 * Two eras. 1.21.4 introduced the declarative {@code post_effect} API, which the mod drives through
 * {@link PostChainManager}. Older versions load a {@code PostChain} through
 * {@code GameRenderer.loadEffect}, which is not public on any of them; the way in is
 * {@code GameRendererAccessor}, so the binding is checked when the mixin is applied instead of
 * being probed at runtime.
 * <p>
 * Every guarded arm below is a whole method. The arms deliberately do not share closing braces,
 * so a change to one cannot silently unbalance another.
 */
public enum Shaders {

	//? if >=1.21.4 {
	Blueprint("blueprint"),
	None("");
	//?} else {
	/*Blueprint("blueprint.json"),
	None("");
	*///?}

	//? if >=1.21.11 {
	private final Identifier location;
	//?} else {
	/*private final ResourceLocation location;
	*///?}

	// From 1.21.4 a post chain is addressed as namespace:name and resolved to
	// post_effect/<name>.json. Before that the location is the literal resource path.
	//? if >=1.21.11 {
	Shaders(String name) {
		location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, name);
	}
	//?} else if >=1.21.4 {
	/*Shaders(String name) {
		location = ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, name);
	}
	*///?} else if >=1.21 {
	/*Shaders(String filename) {
		location = ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "shaders/post/" + filename);
	}
	*///?} else {
	/*Shaders(String filename) {
		location = new ResourceLocation(TheMightyArchitect.ID, "shaders/post/" + filename);
	}
	*///?}

	/** Whether this effect is the one currently applied; {@link #None} means "nothing applied". */
	//? if >=1.21.4 {
	public boolean isActive() {
		if (this == None)
			return !PostChainManager.isShaderActive();
		return PostChainManager.isShaderActive(location);
	}
	//?} else {
	/*public boolean isActive() {
		PostChain applied = Minecraft.getInstance().gameRenderer.currentEffect();
		if (this == None)
			return applied == null;
		return applied != null && applied.getName()
			.equals(location.toString());
	}
	*///?}

	/** Applies or clears this effect. */
	//? if >=1.21.4 {
	public void setActive(boolean active) {
		if (!active) {
			if (isActive())
				PostChainManager.shutdownShader();
			return;
		}
		if (this == None)
			PostChainManager.shutdownShader();
		else
			PostChainManager.loadShader(location);
	}
	//?} else {
	/*public void setActive(boolean active) {
		Minecraft mc = Minecraft.getInstance();
		if (this == None) {
			mc.gameRenderer.shutdownEffect();
			return;
		}
		if (active == isActive())
			return;
		if (!active) {
			mc.gameRenderer.shutdownEffect();
			return;
		}
		((GameRendererAccessor) mc.gameRenderer).invokeLoadEffect(location);
		if (!isActive())
			TheMightyArchitect.logger.error("Unable to load shader {}", location);
	}
	*///?}
}
