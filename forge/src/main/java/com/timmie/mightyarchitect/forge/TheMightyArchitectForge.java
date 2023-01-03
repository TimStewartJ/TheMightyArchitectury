package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.AllBlocks;
import com.timmie.mightyarchitect.AllItems;
import com.timmie.mightyarchitect.TheMightyArchitect;
import dev.architectury.platform.forge.EventBuses;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;

@Mod(TheMightyArchitectForge.ID)
public class TheMightyArchitectForge {

	public static final String ID = "mightyarchitect";
	public static final String NAME = "The Mighty Architect";
	public static final String VERSION = "0.6";

	public TheMightyArchitectForge()
	{
		IEventBus modEventBus = FMLJavaModLoadingContext.get()
				.getModEventBus();
		EventBuses.registerModEventBus(TheMightyArchitectForge.ID, modEventBus);
		//TheMightyArchitect.firstRegistrationStep();
		TheMightyArchitect.Init();
	}
}