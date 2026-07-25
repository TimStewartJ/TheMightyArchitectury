package com.timmie.mightyarchitect.foundation.utility;

import com.timmie.mightyarchitect.TheMightyArchitect;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else if >=1.21.4 {
/*import net.minecraft.resources.ResourceLocation;
*///?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
*///?}

//*
 //* Post-processing shader effects for the mod.
 //* Uses the PostChainManager to handle the 1.21.4+ post-processing API.
 //
public enum Shaders {

	//? if >=1.21.4 {
	Blueprint("blueprint"),
	None("");
	//?} else {
	/*Blueprint("blueprint.json"), None("");
	*///?}

	//? if >=1.21.11 {
	private final Identifier location;
	//?} else if >=1.21.4 {
	/*private final ResourceLocation location;
	*///?} else {
	/*private ResourceLocation location;
	*///?}

	//? if >=26 {
	private Shaders(String name) {
		if (name.isEmpty()) {
			location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "");
		} else {
			// Post chains are loaded from post_effect/<name>.json.
			// ResourceLocation should just be namespace:name without path prefix or extension
			location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, name);
		}
	//?} else if >=1.21.11 {
	/*private Shaders(String name) {
		if (name.isEmpty()) {
			location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "");
		} else {
			// In 1.21.4+, post chains are loaded from post_effect/<name>.json
			// ResourceLocation should just be namespace:name without path prefix or extension
			location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, name);
		}
	*///?} else if >=1.21.4 {
	/*private Shaders(String name) {
		if (name.isEmpty()) {
			location = ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "");
		} else {
			// In 1.21.4+, post chains are loaded from post_effect/<name>.json
			// ResourceLocation should just be namespace:name without path prefix or extension
			location = ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, name);
		}
	*///?} else {
	/*private Shaders(String filename) {
		location = ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "shaders/post/" + filename);
	*///?}
	}

	//*
	 //* Checks if this shader is currently active.
	 //*
	 //* @return true if this shader is the currently active post-processing shader
	 //
	public boolean isActive() {
		//? if >=1.21.4 {
		//?} else {
		/*Minecraft mc = Minecraft.getInstance();
		PostChain shaderGroup = mc.gameRenderer.currentEffect();
		return shaderGroup != null && shaderGroup.getName()
			.equals(location.toString());
	}

	public void setActive(boolean active) {
		Minecraft mc = Minecraft.getInstance();

		*///?}
		if (this == None) {
			//? if >=1.21.4 {
			return !PostChainManager.isShaderActive();
			//?} else {
			/*mc.gameRenderer.shutdownEffect();
			return;
			*///?}
		}
		//? if >=1.21.4 {
		return PostChainManager.isShaderActive(location);
		//?} else {
		/*
		if (active && !isActive()) {
			loadEffect(mc, location);
			return;
		}

		if (!active && isActive()) {
			mc.gameRenderer.shutdownEffect();
			return;
		}
		*///?}
	}

	//? if >=1.21.4 {
	//*
	 //* Activates or deactivates this shader.
	 //*
	 //* @param active true to activate, false to deactivate
	 //
	public void setActive(boolean active) {
		if (active) {
			if (this == None) {
				PostChainManager.shutdownShader();
			} else {
				PostChainManager.loadShader(location);
	//?} else {
	/*private static void loadEffect(Minecraft mc, ResourceLocation location) {
		for (java.lang.reflect.Method candidate : findLoadEffectCandidates(mc)) {
			try {
				candidate.setAccessible(true);
				candidate.invoke(mc.gameRenderer, location);
			} catch (ReflectiveOperationException e) {
				continue;
	*///?}
			}
		//? if >=1.21.4 {
		} else {
			// Only shutdown if this shader is currently active
			if (isActive()) {
				PostChainManager.shutdownShader();
			}
		//?} else {
		/*PostChain applied = mc.gameRenderer.currentEffect();
			if (applied != null && applied.getName()
				.equals(location.toString()))
				return;
		*///?}
		}
		//? if >=1.21.4 {
		//?} else {
		/*TheMightyArchitect.logger.error("Unable to load shader {}", location);
		*///?}
	}

	//*
	 //* Gets the resource location of this shader.
	 //*
	 //* @return The shader's resource location
	 //
	//? if >=1.21.11 {
	public Identifier getLocation() {
		return location;
	//?} else if >=1.21.4 {
	/*public ResourceLocation getLocation() {
		return location;
	*///?} else {
	/*private static java.util.List<java.lang.reflect.Method> findLoadEffectCandidates(Minecraft mc) {
		java.util.List<java.lang.reflect.Method> candidates = new java.util.ArrayList<>();
		try {
			candidates.add(mc.gameRenderer.getClass()
				.getDeclaredMethod("loadEffect", ResourceLocation.class));
		} catch (NoSuchMethodException ignored) {
			// Remapped runtime; fall through to scanning below.
		}
		for (java.lang.reflect.Method method : mc.gameRenderer.getClass()
			.getDeclaredMethods()) {
			if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == ResourceLocation.class
				&& method.getReturnType() == Void.TYPE && !candidates.contains(method))
				candidates.add(method);
		}
		return candidates;
	*///?}
	}
}
