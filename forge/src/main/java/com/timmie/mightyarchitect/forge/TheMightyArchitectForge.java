package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(TheMightyArchitectForge.ID)
public class TheMightyArchitectForge {

	public static final String ID = "mightyarchitect";

	public TheMightyArchitectForge(IEventBus modEventBus)
	{
		TheMightyArchitect.Init();
	}
}