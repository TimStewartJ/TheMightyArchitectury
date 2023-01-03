package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.fabricmc.api.ModInitializer;

public class TheMightyArchitectFabric implements ModInitializer {
	@Override
	public void onInitialize()
	{
		TheMightyArchitect.Init();
		OnRenderWorld.RegisterRenderEvent();
	}
}
