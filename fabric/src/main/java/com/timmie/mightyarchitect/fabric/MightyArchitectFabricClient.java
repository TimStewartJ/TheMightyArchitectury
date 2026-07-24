package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.MightyClient;
import net.fabricmc.loader.api.FabricLoader;

final class MightyArchitectFabricClient {

	private MightyArchitectFabricClient() {
	}

	static void init() {
		MightyClient.iris_presence = FabricLoader.getInstance().isModLoaded("iris");
		OnRenderWorld.RegisterRenderEvent();
	}
}
