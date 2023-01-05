package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.TheMightyArchitect;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TheMightyArchitectForge.ID)
public class TheMightyArchitectForge {

	public static final String ID = "mightyarchitect";

	public TheMightyArchitectForge()
	{
		IEventBus modEventBus = FMLJavaModLoadingContext.get()
				.getModEventBus();
		EventBuses.registerModEventBus(TheMightyArchitectForge.ID, modEventBus);
		TheMightyArchitect.Init();
	}
}