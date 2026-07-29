package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.item.ArchitectWandItem;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;

//? if >=1.21.4 {
//?} else {
/*import java.util.ArrayList;

*///?}
public class AllItems {
	public static AllItems ARCHITECT_WAND;

	public Item item;

	private AllItems(Item item) {
		this.item = item;
	}

	public static Properties standardProperties() {
		return new Properties();
	}

	//? if >=1.21.11 {
	private static Properties itemProps(String id) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, id));
		return new Properties().setId(key);
	}

	//?} else if >=1.21.4 {
	/*private static Properties itemProps(String id) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, id));
		return new Properties().setId(key);
	}

	*///?} else {
	/*
	*///?}
	public static void registerItems(ModRegistrar<Item> itemRegistry) {
		//? if >=1.21.4 {
		itemRegistry.register("architect_wand", () -> { ARCHITECT_WAND = new AllItems(new ArchitectWandItem(itemProps("architect_wand"))); return ARCHITECT_WAND.get(); });
		//?} else {
		/*itemRegistry.register("architect_wand", () -> { ARCHITECT_WAND = new AllItems(new ArchitectWandItem(standardProperties())); return ARCHITECT_WAND.get(); });
		*///?}
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
