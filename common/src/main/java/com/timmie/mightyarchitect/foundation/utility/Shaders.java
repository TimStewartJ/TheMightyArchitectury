package com.timmie.mightyarchitect.foundation.utility;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public enum Shaders {

	Blueprint("blueprint.json"), None("");

	private ResourceLocation location;

	private Shaders(String filename) {
		location = ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "shaders/post/" + filename);
	}

	public boolean isActive() {
		// TODO: Post-processing shader API changed significantly in 1.21.4
		// The GameRenderer no longer exposes currentEffect() directly
		// This needs to be reimplemented using the new post-processing system
		return false;
	}

	public void setActive(boolean active) {
		// TODO: Post-processing shader API changed significantly in 1.21.4
		// loadEffect(), shutdownEffect() no longer exist on GameRenderer
		// This needs to be reimplemented using the new post-processing system
	}

}
