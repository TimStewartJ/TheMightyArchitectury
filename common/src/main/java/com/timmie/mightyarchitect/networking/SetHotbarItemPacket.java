package com.timmie.mightyarchitect.networking;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class SetHotbarItemPacket {
	
	private int slot;
	private ItemStack stack;

	public SetHotbarItemPacket(int slot, ItemStack stack) {
		this.slot = slot;
		this.stack = stack;
	}
	
	public SetHotbarItemPacket(FriendlyByteBuf buffer) {
		this(buffer.readInt(), buffer.readItem());
	}

	public void toBytes(FriendlyByteBuf buffer) {
		buffer.writeInt(slot);
		buffer.writeItem(stack);
	}

	public void handle(Supplier<NetworkManager.PacketContext> context) {
		context.get().queue(() -> {
			Player player = context.get().getPlayer();
			if (!player.isCreative())
				return;

			player.getInventory().setItem(slot, stack);
			//player.setSlot(slot, stack);
			player.inventoryMenu.broadcastChanges();
		});
	}
	
}
