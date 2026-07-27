package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import dev.architectury.networking.NetworkManager;
//? if >=1.21.10 {
//?} else {
/*import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
*///?}
import net.minecraft.network.RegistryFriendlyByteBuf;
//? if >=1.21.10 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

//? if >=1.21.10 {
public class SetHotbarItemPacket implements CustomPacketPayload {
//?} else {
/*public class SetHotbarItemPacket extends BaseC2SMessage {
*///?}

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
	//? if >=1.21.10 {
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.SET_HOTBAR_ITEM_TYPE;
	//?} else {
	/*public MessageType getType() {
		return AllPackets.SET_HOTBAR_ITEM;
	*///?}
	}

	//? if >=1.21.10 {
	public static void write(RegistryFriendlyByteBuf buffer, SetHotbarItemPacket packet) {
		buffer.writeInt(packet.slot);
		ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.stack);
	//?} else {
	/*@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(slot);
		ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
	*///?}
	}

	//? if >=1.21.10 {
	public static void handle(SetHotbarItemPacket packet, NetworkManager.PacketContext context) {
	//?} else {
	/*@Override
	public void handle(NetworkManager.PacketContext context) {
	*///?}
		context.queue(() -> {
			Player player = context.getPlayer();
			if (!player.isCreative())
				return;

			//? if >=1.21.10 {
			player.getInventory().setItem(packet.slot, packet.stack);
			//?} else {
			/*player.getInventory().setItem(slot, stack);
			*///?}
			player.inventoryMenu.broadcastChanges();
		});
	}

}
