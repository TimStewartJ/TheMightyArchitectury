package com.timmie.mightyarchitect;

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

}
