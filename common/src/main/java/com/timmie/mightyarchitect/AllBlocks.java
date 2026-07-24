//? if >=26 {
package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.block.DesignAnchorBlock;
import com.timmie.mightyarchitect.block.SliceMarkerBlock;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class AllBlocks {

	public static AllBlocks SLICE_MARKER;
	public static AllBlocks DESIGN_ANCHOR;

	public Block block;

	private AllBlocks(Block block) {
		this.block = block;
	}

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

	public static void registerBlocks(DeferredRegister<Block> registry) {
		registry.register("slice_marker", () -> { SLICE_MARKER = new AllBlocks(new SliceMarkerBlock(blockProps("slice_marker"))); return SLICE_MARKER.get(); });
		registry.register("design_anchor", () -> { DESIGN_ANCHOR = new AllBlocks(new DesignAnchorBlock(blockProps("design_anchor"))); return DESIGN_ANCHOR.get(); });
	}

	public static void registerItemBlocks(DeferredRegister<Item> registry) {
		registry.register("slice_marker", () -> new BlockItem(SLICE_MARKER.get(), blockItemProps("slice_marker")));
		registry.register("design_anchor", () -> new BlockItem(DESIGN_ANCHOR.get(), blockItemProps("design_anchor")));
	}

	public Block get() {
		return block;
	}

	public boolean typeOf(BlockState state) {
		return state.getBlock() == block;
	}

}
//?} else if >=1.21.4 {
/*package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.block.DesignAnchorBlock;
import com.timmie.mightyarchitect.block.SliceMarkerBlock;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class AllBlocks {

	public static AllBlocks SLICE_MARKER;
	public static AllBlocks DESIGN_ANCHOR;

	public Block block;

	private AllBlocks(Block block) {
		this.block = block;
	}

	private static BlockBehaviour.Properties blockProps(String id) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK,
				ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, id));
		return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).setId(key);
	}

	private static Item.Properties blockItemProps(String id) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, id));
		return new Item.Properties().setId(key).useBlockDescriptionPrefix();
	}

	public static void registerBlocks(DeferredRegister<Block> registry) {
		registry.register("slice_marker", () -> { SLICE_MARKER = new AllBlocks(new SliceMarkerBlock(blockProps("slice_marker"))); return SLICE_MARKER.get(); });
		registry.register("design_anchor", () -> { DESIGN_ANCHOR = new AllBlocks(new DesignAnchorBlock(blockProps("design_anchor"))); return DESIGN_ANCHOR.get(); });
	}

	public static void registerItemBlocks(DeferredRegister<Item> registry) {
		registry.register("slice_marker", () -> new BlockItem(SLICE_MARKER.get(), blockItemProps("slice_marker")));
		registry.register("design_anchor", () -> new BlockItem(DESIGN_ANCHOR.get(), blockItemProps("design_anchor")));
	}

	public Block get() {
		return block;
	}

	public boolean typeOf(BlockState state) {
		return state.getBlock() == block;
	}

}*/
//?} else {
/*package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.block.DesignAnchorBlock;
import com.timmie.mightyarchitect.block.SliceMarkerBlock;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class AllBlocks {

	public static AllBlocks SLICE_MARKER;
	public static AllBlocks DESIGN_ANCHOR;

	public Block block;

	private AllBlocks(Block block) {
		this.block = block;
	}

	public static void registerBlocks(DeferredRegister<Block> registry) {
		registry.register("slice_marker", () -> { SLICE_MARKER = new AllBlocks(new SliceMarkerBlock()); return SLICE_MARKER.get(); });
		registry.register("design_anchor", () -> { DESIGN_ANCHOR = new AllBlocks(new DesignAnchorBlock()); return DESIGN_ANCHOR.get(); });
	}

	public static void registerItemBlocks(DeferredRegister<Item> registry) {
		registry.register("slice_marker", () -> new BlockItem(SLICE_MARKER.get(), AllItems.standardProperties()));
		registry.register("design_anchor", () -> new BlockItem(DESIGN_ANCHOR.get(), AllItems.standardProperties()));
	}

	public Block get() {
		return block;
	}

	public boolean typeOf(BlockState state) {
		return state.getBlock() == block;
	}

}*///?}
