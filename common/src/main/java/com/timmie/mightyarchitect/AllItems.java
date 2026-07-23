package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.item.ArchitectWandItem;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;

public class AllItems {
	public static AllItems ARCHITECT_WAND;

	public Item item;

	private AllItems(Item item) {
		this.item = item;
	}

	public static Properties standardProperties() {
		return new Properties();
	}

	private static Properties itemProps(String id) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, id));
		return new Properties().setId(key);
	}

	public static void registerItems(DeferredRegister<Item> itemRegistry) {
		itemRegistry.register("architect_wand", () -> { ARCHITECT_WAND = new AllItems(new ArchitectWandItem(itemProps("architect_wand"))); return ARCHITECT_WAND.get(); });
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
