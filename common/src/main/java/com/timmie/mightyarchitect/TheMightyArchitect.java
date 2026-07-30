package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.platform.ModRegistrar;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TheMightyArchitect {

	public static final String ID = "mightyarchitect";
	public static final String NAME = "The Mighty Architect";

	public static TheMightyArchitect instance;
	public static Logger logger = LogManager.getLogger();

	// ResourceLocation's public constructor was replaced by the fromNamespaceAndPath factory in
	// 1.21, and the class itself was renamed to Identifier in 1.21.11.
	//? if >=1.21.11 {
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}

	//?} else if >=1.21 {
	/*public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}

	*///?} else {
	/*public static ResourceLocation id(String path) {
		return new ResourceLocation(ID, path);
	}

	*///?}
	// Registers the mod's content through whatever registration mechanism the loader handed in.
	// Blocks go first: the block item factories read the block instances the block factories build.
	public static void Init(ModRegistrar<Block> blocks, ModRegistrar<Item> items)
	{
		AllBlocks.registerBlocks(blocks);
		AllItems.registerItems(items);
		AllBlocks.registerItemBlocks(items);
	}
}
