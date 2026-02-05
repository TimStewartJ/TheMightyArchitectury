package com.timmie.mightyarchitect.foundation.utility;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.resources.Identifier;

/**
 * Post-processing shader effects for the mod.
 * Uses the PostChainManager to handle the 1.21.4+ post-processing API.
 */
public enum Shaders {

	Blueprint("blueprint"), 
	None("");

	private final Identifier location;

	private Shaders(String name) {
		if (name.isEmpty()) {
			location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "");
		} else {
			// In 1.21.4+, post chains are loaded from post_effect/<name>.json
			// ResourceLocation should just be namespace:name without path prefix or extension
			location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, name);
		}
	}

	/**
	 * Checks if this shader is currently active.
	 *
	 * @return true if this shader is the currently active post-processing shader
	 */
	public boolean isActive() {
		if (this == None) {
			return !PostChainManager.isShaderActive();
		}
		return PostChainManager.isShaderActive(location);
	}

	/**
	 * Activates or deactivates this shader.
	 *
	 * @param active true to activate, false to deactivate
	 */
	public void setActive(boolean active) {
		if (active) {
			if (this == None) {
				PostChainManager.shutdownShader();
			} else {
				PostChainManager.loadShader(location);
			}
		} else {
			// Only shutdown if this shader is currently active
			if (isActive()) {
				PostChainManager.shutdownShader();
			}
		}
	}

	/**
	 * Gets the resource location of this shader.
	 *
	 * @return The shader's resource location
	 */
	public Identifier getLocation() {
		return location;
	}
}
