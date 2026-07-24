//? if >=26 {
package com.timmie.mightyarchitect.foundation.utility;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.resources.Identifier;

//*
 //* Post-processing shader effects for the mod.
 //* Uses the PostChainManager to handle the 1.21.4+ post-processing API.
 //
public enum Shaders {

	Blueprint("blueprint"), 
	None("");

	private final Identifier location;

	private Shaders(String name) {
		if (name.isEmpty()) {
			location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "");
		} else {
			// Post chains are loaded from post_effect/<name>.json.
			// ResourceLocation should just be namespace:name without path prefix or extension
			location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, name);
		}
	}

	//*
	 //* Checks if this shader is currently active.
	 //*
	 //* @return true if this shader is the currently active post-processing shader
	 //
	public boolean isActive() {
		if (this == None) {
			return !PostChainManager.isShaderActive();
		}
		return PostChainManager.isShaderActive(location);
	}

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
			}
		} else {
			// Only shutdown if this shader is currently active
			if (isActive()) {
				PostChainManager.shutdownShader();
			}
		}
	}

	//*
	 //* Gets the resource location of this shader.
	 //*
	 //* @return The shader's resource location
	 //
	public Identifier getLocation() {
		return location;
	}
}
//?} else if >=1.21.4 {
/*package com.timmie.mightyarchitect.foundation.utility;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.resources.ResourceLocation;

//*
 //* Post-processing shader effects for the mod.
 //* Uses the PostChainManager to handle the 1.21.4+ post-processing API.
 //
public enum Shaders {

	Blueprint("blueprint"), 
	None("");

	private final ResourceLocation location;

	private Shaders(String name) {
		if (name.isEmpty()) {
			location = ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "");
		} else {
			// In 1.21.4+, post chains are loaded from post_effect/<name>.json
			// ResourceLocation should just be namespace:name without path prefix or extension
			location = ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, name);
		}
	}

	//*
	 //* Checks if this shader is currently active.
	 //*
	 //* @return true if this shader is the currently active post-processing shader
	 //
	public boolean isActive() {
		if (this == None) {
			return !PostChainManager.isShaderActive();
		}
		return PostChainManager.isShaderActive(location);
	}

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
			}
		} else {
			// Only shutdown if this shader is currently active
			if (isActive()) {
				PostChainManager.shutdownShader();
			}
		}
	}

	//*
	 //* Gets the resource location of this shader.
	 //*
	 //* @return The shader's resource location
	 //
	public ResourceLocation getLocation() {
		return location;
	}
}*/
//?} else {
/*package com.timmie.mightyarchitect.foundation.utility;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

public enum Shaders {

	Blueprint("blueprint.json"), None("");

	private ResourceLocation location;

	private Shaders(String filename) {
		location = ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "shaders/post/" + filename);
	}

	public boolean isActive() {
		Minecraft mc = Minecraft.getInstance();
		PostChain shaderGroup = mc.gameRenderer.currentEffect();
		return shaderGroup != null && shaderGroup.getName()
			.equals(location.toString());
	}

	public void setActive(boolean active) {
		Minecraft mc = Minecraft.getInstance();

		if (this == None) {
			mc.gameRenderer.shutdownEffect();
			return;
		}

		if (active && !isActive()) {
			loadEffect(mc, location);
			return;
		}

		if (!active && isActive()) {
			mc.gameRenderer.shutdownEffect();
			return;
		}
	}

	private static void loadEffect(Minecraft mc, ResourceLocation location) {
		try {
			java.lang.reflect.Method loadEffect = findLoadEffect(mc);
			loadEffect.setAccessible(true);
			loadEffect.invoke(mc.gameRenderer, location);
		} catch (ReflectiveOperationException e) {
			TheMightyArchitect.logger.error("Unable to load shader {}", location, e);
		}
	}

	private static java.lang.reflect.Method findLoadEffect(Minecraft mc) throws NoSuchMethodException {
		try {
			return mc.gameRenderer.getClass()
				.getDeclaredMethod("loadEffect", ResourceLocation.class);
		} catch (NoSuchMethodException ignored) {
			for (java.lang.reflect.Method method : mc.gameRenderer.getClass()
				.getDeclaredMethods()) {
				if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == ResourceLocation.class
					&& method.getReturnType() == Void.TYPE)
					return method;
			}
			throw ignored;
		}
	}

}*///?}
