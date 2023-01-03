package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.TheMightyArchitect;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.ModInitializer;

public class TheMightyArchitectFabric implements ModInitializer {
	@Override
	public void onInitialize()
	{
		TheMightyArchitect.Init();
		EnvExecutor.runInEnv(Env.CLIENT, () -> () -> OnRenderWorld.RegisterRenderEvent());
	}
}
