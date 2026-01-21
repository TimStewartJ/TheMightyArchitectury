package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.TheMightyArchitect;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class TheMightyArchitectFabric implements ModInitializer {
	@Override
	public void onInitialize()
	{
		MightyClient.iris_presence = FabricLoader.getInstance().isModLoaded("iris");

		TheMightyArchitect.Init();
		EnvExecutor.runInEnv(Env.CLIENT, () -> () -> OnRenderWorld.RegisterRenderEvent());
	}
}
