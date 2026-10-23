package com.timmie.mightyarchitect.control.design;

import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.design.DesignSlice.DesignSliceTrait;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
//?} else {
/*
*///?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The theme exporter's in-world tools, made entirely of vanilla content.
 * <p>
 * The mod used to register a wand item and two marker blocks for this. Any registered block or item
 * makes a server - or a LAN host - turn away every player who does not have it: from 1.21.4,
 * Fabric's registry sync disconnects them, and there is no per-entry way to opt out. So the tools
 * are vanilla things the mod recognises instead:
 * <ul>
 * <li>the <b>Design Exporter</b> is a blaze rod carrying a hidden tag; an ordinary blaze rod does
 * nothing,</li>
 * <li>the <b>Design Marker</b> (where a design starts) is a lodestone,</li>
 * <li>each <b>layer marker</b> trait is one colour of glazed terracotta.</li>
 * </ul>
 * Everything that reads them - the exporter, the interaction hook and the labels - runs on the
 * client, so a player without the mod just sees the vanilla blocks.
 */
public final class ExportTools {

	/** The key the Design Exporter carries in its custom data (an NBT tag before 1.20.5). */
	public static final String WAND_TAG = TheMightyArchitect.ID + ":design_exporter";

	public static final String WAND_NAME = "Design Exporter";
	public static final String ANCHOR_NAME = "Design Marker";
	public static final String MARKER_NAME = "Layer Marker";

	private ExportTools() {
	}

	public static ItemStack createWand() {
		ItemStack stack = new ItemStack(Items.BLAZE_ROD);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(WAND_TAG, true);
		//? if >=1.20.5 {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		//?} else {
		/*stack.getOrCreateTag().merge(tag);
		*///?}
		return named(stack, WAND_NAME);
	}

	/** Whether this stack is the Design Exporter. Decided by the tag alone, never the base item. */
	public static boolean isWand(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return false;
		//? if >=1.20.5 {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		// CustomData.contains(String) is gone from 1.21.10; copyTag() exists on every version.
		return data != null && data.copyTag()
			.contains(WAND_TAG);
		//?} else {
		/*CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(WAND_TAG);
		*///?}
	}

	public static Block anchorBlock() {
		return Blocks.LODESTONE;
	}

	public static boolean isAnchor(BlockState state) {
		return state.getBlock() == anchorBlock();
	}

	public static ItemStack createAnchor() {
		return named(new ItemStack(anchorBlock()), ANCHOR_NAME);
	}

	/** The block that stands for this trait in a layer-marker column. */
	public static Block markerBlock(DesignSliceTrait trait) {
		return glazedTerracotta(markerColor(trait));
	}

	public static BlockState markerState(DesignSliceTrait trait) {
		return markerBlock(trait).defaultBlockState();
	}

	/** The trait a block stands for, or {@code null} when it is not a layer marker. */
	public static DesignSliceTrait traitOf(BlockState state) {
		Block block = state.getBlock();
		for (DesignSliceTrait trait : DesignSliceTrait.values())
			if (block == markerBlock(trait))
				return trait;
		return null;
	}

	public static boolean isMarker(BlockState state) {
		return traitOf(state) != null;
	}

	/**
	 * The marker for the next (or, going backwards, previous) trait, keeping the way the current
	 * one faces so cycling a column does not visibly spin its blocks.
	 */
	public static BlockState cycledMarker(BlockState current, boolean backwards) {
		DesignSliceTrait trait = traitOf(current);
		if (trait == null)
			throw new IllegalArgumentException("not a layer marker: " + current);
		BlockState next = markerState(trait.cycle(backwards ? -1 : 1));
		if (current.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
			&& next.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
			next = next.setValue(BlockStateProperties.HORIZONTAL_FACING,
				current.getValue(BlockStateProperties.HORIZONTAL_FACING));
		return next;
	}

	public static ItemStack createMarker(DesignSliceTrait trait) {
		return named(new ItemStack(markerBlock(trait)), MARKER_NAME + ": " + label(trait));
	}

	/** Short name shown above a marker in the world and on its toolkit item. */
	public static String label(DesignSliceTrait trait) {
		return switch (trait) {
			case Standard -> "Once";
			case CloneOnce -> "Clone x1";
			case CloneThrice -> "Clone x3";
			case Optional -> "Optional";
			case MaskAbove -> "Mask above";
			case MaskBelow -> "Mask below";
		};
	}

	static DyeColor markerColor(DesignSliceTrait trait) {
		return switch (trait) {
			case Standard -> DyeColor.WHITE;
			case CloneOnce -> DyeColor.LIME;
			case CloneThrice -> DyeColor.GREEN;
			case Optional -> DyeColor.YELLOW;
			case MaskAbove -> DyeColor.LIGHT_BLUE;
			case MaskBelow -> DyeColor.BLUE;
		};
	}

	// 26.2 folded the sixteen per-colour constants into one collection; the block ids did not change.
	//? if >=26.2 {
	/*private static Block glazedTerracotta(DyeColor color) {
		return Blocks.GLAZED_TERRACOTTA.pick(color);
	}
	*///?} else {
	private static Block glazedTerracotta(DyeColor color) {
		return switch (color) {
			case WHITE -> Blocks.WHITE_GLAZED_TERRACOTTA;
			case LIME -> Blocks.LIME_GLAZED_TERRACOTTA;
			case GREEN -> Blocks.GREEN_GLAZED_TERRACOTTA;
			case YELLOW -> Blocks.YELLOW_GLAZED_TERRACOTTA;
			case LIGHT_BLUE -> Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA;
			case BLUE -> Blocks.BLUE_GLAZED_TERRACOTTA;
			default -> throw new IllegalArgumentException("no layer marker is " + color);
		};
	}
	//?}

	// A custom name is shown in italics unless told otherwise; these are tools, not renamed items.
	private static ItemStack named(ItemStack stack, String name) {
		Component display = Component.literal(name)
			.withStyle(style -> style.withItalic(false));
		//? if >=1.20.5 {
		stack.set(DataComponents.CUSTOM_NAME, display);
		//?} else {
		/*stack.setHoverName(display);
		*///?}
		return stack;
	}
}
