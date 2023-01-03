package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.item.ArchitectWandItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public class AllItems {
	public static AllItems ARCHITECT_WAND;

	public Item item;

	private AllItems(Item item) {
		this.item = item;
	}

	public static Properties standardProperties() {
		return new Properties();
	}

	public static ArrayList<RegistrySupplier<Item>> registerItems(DeferredRegister<Item> itemRegistry) {
		var output = new ArrayList<RegistrySupplier<Item>>();
		output.add(itemRegistry.register("architect_wand", () -> { ARCHITECT_WAND = new AllItems(new ArchitectWandItem(standardProperties())); return ARCHITECT_WAND.get(); }));
		return output;
	}

	public Item get() {
		return item;
	}

	public boolean typeOf(ItemStack stack) {
		return stack.getItem() == item;
	}

	public static void initColorHandlers() {
	}

}
