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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import io.netty.handler.codec.DecoderException;

public class SetHotbarItemPacket implements MightyPacket {

	/** The packet only ever addresses the hotbar; anything else is not something to honour. */
	private static final int HOTBAR_SIZE = 9;

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
		this.slot = validateSlot(buffer.readInt());
		this.stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
	}
	//?} else {
	/*public SetHotbarItemPacket(FriendlyByteBuf buffer) {
		this.slot = validateSlot(buffer.readInt());
		this.stack = buffer.readItem();
	}
	*///?}

	// The slot goes straight into Inventory.setItem, so it has to be a slot this packet is allowed
	// to address before anything else reads it.
	private static int validateSlot(int slot) {
		if (slot < 0 || slot >= HOTBAR_SIZE)
			throw new DecoderException("SetHotbarItemPacket addressed slot " + slot + "; the hotbar is 0.."
				+ (HOTBAR_SIZE - 1));
		return slot;
	}

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
		ServerPlayer player = context.player();
		context.enqueue(() -> {
			if (!ServerBuildGuard.mayReceiveHotbarKit(player)) {
				ServerBuildGuard.reportDenied(player, "hand out a toolkit");
				return;
			}
			// The decoder already rejects out-of-range slots; this keeps the invariant local to the
			// call that depends on it, since a slot that is not a slot corrupts the inventory.
			if (packet.slot < 0 || packet.slot >= HOTBAR_SIZE)
				return;

			player.getInventory().setItem(packet.slot, packet.stack);
			player.inventoryMenu.broadcastChanges();
		});
	}

}
