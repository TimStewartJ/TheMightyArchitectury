package com.timmie.mightyarchitect;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TheMightyArchitect {

	public static final String ID = "mightyarchitect";
	public static final String NAME = "The Mighty Architect";

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ID, Registry.ITEM_REGISTRY);
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ID, Registry.BLOCK_REGISTRY);

	public static TheMightyArchitect instance;
	public static Logger logger = LogManager.getLogger();

	public static void Init()
	{
		// TODO: figure out how to not hardcode blocks and items to register again
		AllItems.registerItems(ITEMS);
		AllBlocks.registerBlocks(BLOCKS);
		AllBlocks.registerItemBlocks(ITEMS);

		BLOCKS.register();
		ITEMS.register();

		LifecycleEvent.SETUP.register(AllPackets::registerPackets);
		EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> ClientLifecycleEvent.CLIENT_SETUP.register((c) -> MightyClient.init()));
	}
}