package com.timmie.mightyarchitect.control;

import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.design.DesignSlice.DesignSliceTrait;
import com.timmie.mightyarchitect.control.design.ExportTools;
import com.timmie.mightyarchitect.control.palette.Palette;
import net.minecraft.ChatFormatting;
// Item display names moved from NBT hover-name to a data component in 1.20.5.
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?} else {
/*
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ArchitectKits {

	public static void ExporterToolkit() {
		if (!mayHandOutKit())
			return;
		setHotbarItem(0, ExportTools.createWand());
		setHotbarItem(1, ExportTools.createAnchor());
		setHotbarItem(2, ExportTools.createMarker(DesignSliceTrait.Standard));
		clearHotbarItem(3);
		setHotbarBlock(4, Palette.CLEAR);
		setHotbarBlock(5, Palette.FLOOR);
		clearHotbarItem(6);
		clearHotbarItem(7);
		clearHotbarItem(8);
	}

	public static void FoundationToolkit() {
		if (!mayHandOutKit())
			return;
		setHotbarBlock(0, Palette.HEAVY_POST);
		setHotbarBlock(1, Palette.HEAVY_PRIMARY);
		setHotbarBlock(2, Palette.HEAVY_SECONDARY);
		setHotbarBlock(3, Palette.HEAVY_WINDOW);
		setHotbarBlock(4, Palette.INNER_DETAIL);
		setHotbarBlock(5, Palette.INNER_PRIMARY);
		setHotbarBlock(6, Palette.OUTER_SLAB);
		setHotbarBlock(7, Palette.OUTER_THICK);
		setHotbarBlock(8, Palette.OUTER_THIN);
	}

	public static void RegularToolkit() {
		if (!mayHandOutKit())
			return;
		setHotbarBlock(0, Palette.FLOOR);
		setHotbarBlock(1, Palette.INNER_PRIMARY);
		setHotbarBlock(2, Palette.INNER_DETAIL);
		setHotbarBlock(3, Palette.INNER_SECONDARY);
		setHotbarBlock(4, Palette.OUTER_FLAT);
		setHotbarBlock(5, Palette.WINDOW);
		setHotbarBlock(6, Palette.OUTER_SLAB);
		setHotbarBlock(7, Palette.OUTER_THICK);
		setHotbarBlock(8, Palette.OUTER_THIN);
	}

	public static void RoofingToolkit() {
		if (!mayHandOutKit())
			return;
		setHotbarBlock(0, Palette.ROOF_PRIMARY);
		setHotbarBlock(1, Palette.ROOF_DETAIL);
		setHotbarBlock(2, Palette.ROOF_SLAB);
		setHotbarBlock(3, Palette.WINDOW);
		setHotbarBlock(4, Palette.INNER_PRIMARY);
		setHotbarBlock(5, Palette.INNER_DETAIL);
		setHotbarBlock(6, Palette.INNER_SECONDARY);
		setHotbarBlock(7, Palette.OUTER_THICK);
		setHotbarBlock(8, Palette.OUTER_THIN);
	}

	private static void clearHotbarItem(int slot) {
		setHotbarItem(slot, ItemStack.EMPTY);
	}

	/**
	 * Toolkits are handed out the way vanilla's creative inventory does it: set the slot locally and
	 * report it to the server, which accepts that from a creative player on any server, modded or
	 * not. Nothing else would accept it, so outside creative mode there is nothing to try.
	 */
	private static boolean mayHandOutKit() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null)
			return false;
		if (minecraft.gameMode.getPlayerMode()
			.isCreative())
			return true;
		ArchitectManager.status(ChatFormatting.RED + "Toolkits are only handed out in creative mode.");
		return false;
	}

	private static void setHotbarItem(int slot, ItemStack stack) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.player.getInventory()
			.setItem(slot, stack);
		minecraft.gameMode.handleCreativeModeItemAdd(stack, InventoryMenu.USE_ROW_SLOT_START + slot);
	}

	private static void setHotbarBlock(int slot, Palette palette) {
		BlockState state = DesignExporter.getTheme().getDefaultPalette().get(palette);
		ItemStack stack = new ItemStack(state.getBlock().asItem());
		//? if >=1.20.5 {
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(ChatFormatting.RESET + "" + ChatFormatting.GOLD
				+ palette.getDisplayName() + ChatFormatting.WHITE + " (" + ChatFormatting.GRAY
				+ stack.getHoverName().getString() + ChatFormatting.WHITE + ")"));
		//?} else {
		/*stack.setHoverName(Component.literal(ChatFormatting.RESET + "" + ChatFormatting.GOLD
				+ palette.getDisplayName() + ChatFormatting.WHITE + " (" + ChatFormatting.GRAY
				+ stack.getHoverName().getString() + ChatFormatting.WHITE + ")"));
		*///?}
		setHotbarItem(slot, stack);
	}

}
