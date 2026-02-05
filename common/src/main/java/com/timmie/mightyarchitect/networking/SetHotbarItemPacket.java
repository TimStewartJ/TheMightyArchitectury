package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SetHotbarItemPacket implements CustomPacketPayload {
	
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
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.SET_HOTBAR_ITEM_TYPE;
	}

	public static void write(RegistryFriendlyByteBuf buffer, SetHotbarItemPacket packet) {
		buffer.writeInt(packet.slot);
		ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.stack);
	}

	public static void handle(SetHotbarItemPacket packet, NetworkManager.PacketContext context) {
		context.queue(() -> {
			Player player = context.getPlayer();
			if (!player.isCreative())
				return;

			player.getInventory().setItem(packet.slot, packet.stack);
			player.inventoryMenu.broadcastChanges();
		});
	}
	
}
