package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SetHotbarItemPacket extends BaseC2SMessage {
	
	private final int slot;
	private final ItemStack stack;

	public SetHotbarItemPacket(int slot, ItemStack stack) {
		this.slot = slot;
		this.stack = stack;
	}
	
	public SetHotbarItemPacket(RegistryFriendlyByteBuf buffer) {
		this.slot = buffer.readInt();
		this.stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
	}

	@Override
	public MessageType getType() {
		return AllPackets.SET_HOTBAR_ITEM;
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(slot);
		ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		context.queue(() -> {
			Player player = context.getPlayer();
			if (!player.isCreative())
				return;

			player.getInventory().setItem(slot, stack);
			player.inventoryMenu.broadcastChanges();
		});
	}
	
}
