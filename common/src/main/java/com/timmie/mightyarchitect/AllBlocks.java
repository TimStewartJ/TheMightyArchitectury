package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.block.DesignAnchorBlock;
import com.timmie.mightyarchitect.block.SliceMarkerBlock;
import com.timmie.mightyarchitect.platform.ModRegistrar;
//? if >=1.21.11 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
//?} else if >=1.21.4 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
*///?} else {
/*
*///?}
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
//? if >=1.21.4 {
import net.minecraft.world.level.block.state.BlockBehaviour;
//?} else {
/*
*///?}
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.4 {
import net.minecraft.world.level.material.MapColor;
//?} else {
/*
*///?}

public class AllBlocks {

	public static AllBlocks SLICE_MARKER;
	public static AllBlocks DESIGN_ANCHOR;

	public Block block;

	private AllBlocks(Block block) {
		this.block = block;
	}

	//? if >=1.21.11 {
	private static BlockBehaviour.Properties blockProps(String id) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK,
				Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, id));
		return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).setId(key);
	}

	private static Item.Properties blockItemProps(String id) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, id));
		return new Item.Properties().setId(key).useBlockDescriptionPrefix();
	}

	//?} else if >=1.21.4 {
	/*private static BlockBehaviour.Properties blockProps(String id) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK,
				ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, id));
		return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).setId(key);
	}

	private static Item.Properties blockItemProps(String id) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, id));
		return new Item.Properties().setId(key).useBlockDescriptionPrefix();
	}

	*///?} else {
	/*
	*///?}
	public static void registerBlocks(ModRegistrar<Block> registry) {
		//? if >=1.21.4 {
		registry.register("slice_marker", () -> { SLICE_MARKER = new AllBlocks(new SliceMarkerBlock(blockProps("slice_marker"))); return SLICE_MARKER.get(); });
		registry.register("design_anchor", () -> { DESIGN_ANCHOR = new AllBlocks(new DesignAnchorBlock(blockProps("design_anchor"))); return DESIGN_ANCHOR.get(); });
		//?} else {
		/*registry.register("slice_marker", () -> { SLICE_MARKER = new AllBlocks(new SliceMarkerBlock()); return SLICE_MARKER.get(); });
		registry.register("design_anchor", () -> { DESIGN_ANCHOR = new AllBlocks(new DesignAnchorBlock()); return DESIGN_ANCHOR.get(); });
		*///?}
	}

	public static void registerItemBlocks(ModRegistrar<Item> registry) {
		//? if >=1.21.4 {
		registry.register("slice_marker", () -> new BlockItem(SLICE_MARKER.get(), blockItemProps("slice_marker")));
		registry.register("design_anchor", () -> new BlockItem(DESIGN_ANCHOR.get(), blockItemProps("design_anchor")));
		//?} else {
		/*registry.register("slice_marker", () -> new BlockItem(SLICE_MARKER.get(), AllItems.standardProperties()));
		registry.register("design_anchor", () -> new BlockItem(DESIGN_ANCHOR.get(), AllItems.standardProperties()));
		*///?}
	}

	public Block get() {
		return block;
	}

	public boolean typeOf(BlockState state) {
		return state.getBlock() == block;
	}

}
