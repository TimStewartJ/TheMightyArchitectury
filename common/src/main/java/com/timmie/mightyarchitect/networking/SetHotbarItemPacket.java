package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.platform.MightyPacket;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
// The buffer gained a registry-access view in 1.20.5, which is also when payloads stopped being
// written through the interface and started going through a stream codec. RegistryFriendlyByteBuf
// extends FriendlyByteBuf, so the writer itself is shared.
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SetHotbarItemPacket implements MightyPacket {

	private final int slot;
	private final ItemStack stack;

	public SetHotbarItemPacket(int slot, ItemStack stack) {
		this.slot = slot;
		this.stack = stack;
	}

	// ItemStack's stream codec arrived with the data components in 1.20.5; before that the buffer
	// carries the item itself.
	//? if >=1.20.5 {
	public SetHotbarItemPacket(RegistryFriendlyByteBuf buffer) {
		this.slot = buffer.readInt();
		this.stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
	}
	//?} else {
	/*public SetHotbarItemPacket(FriendlyByteBuf buffer) {
		this.slot = buffer.readInt();
		this.stack = buffer.readItem();
	}
	*///?}

	@Override
	//? if >=1.20.5 {
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.SET_HOTBAR_ITEM_TYPE;
	}

	public static void write(RegistryFriendlyByteBuf buffer, SetHotbarItemPacket packet) {
		buffer.writeInt(packet.slot);
		ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.stack);
	}
	//?} else {
	/*public ResourceLocation id() {
		return AllPackets.SET_HOTBAR_ITEM_ID;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(slot);
		buffer.writeItem(stack);
	}
	*///?}

	public static void handle(SetHotbarItemPacket packet, PacketContext context) {
		context.enqueue(() -> {
			Player player = context.player();
			if (!player.isCreative())
				return;

			player.getInventory().setItem(packet.slot, packet.stack);
			player.inventoryMenu.broadcastChanges();
		});
	}

}
