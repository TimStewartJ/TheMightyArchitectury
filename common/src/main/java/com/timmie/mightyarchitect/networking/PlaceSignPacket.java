package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.platform.MightyPacket;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.minecraft.core.BlockPos;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
// The SignText record, and the front/back text split it belongs to, arrived with the 1.20 sign rework.
//? if >=1.20 {
import net.minecraft.world.level.block.entity.SignText;
//?} else {
/*
*///?}

public class PlaceSignPacket implements MightyPacket {

	private final String text1;
	private final String text2;
	private final BlockPos position;

	public PlaceSignPacket(String textLine1, String textLine2, BlockPos position) {
		this.text1 = textLine1;
		this.text2 = textLine2;
		this.position = position;
	}

	//? if >=1.20.5 {
	public PlaceSignPacket(RegistryFriendlyByteBuf buffer) {
	//?} else {
	/*public PlaceSignPacket(FriendlyByteBuf buffer) {
	*///?}
		this(buffer.readUtf(128), buffer.readUtf(128), buffer.readBlockPos());
	}

	@Override
	//? if >=1.20.5 {
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.PLACE_SIGN_TYPE;
	}

	public static void write(RegistryFriendlyByteBuf buffer, PlaceSignPacket packet) {
		writeTo(buffer, packet);
	}
	//?} else {
	/*public ResourceLocation id() {
		return AllPackets.PLACE_SIGN_ID;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		writeTo(buffer, this);
	}
	*///?}

	private static void writeTo(FriendlyByteBuf buffer, PlaceSignPacket packet) {
		buffer.writeUtf(packet.text1);
		buffer.writeUtf(packet.text2);
		buffer.writeBlockPos(packet.position);
	}

	public static void handle(PlaceSignPacket packet, PacketContext context) {
		ServerPlayer player = context.player();
		context.enqueue(() -> {
			if (!ServerBuildGuard.mayBuildAt(player, packet.position)) {
				ServerBuildGuard.reportDenied(player, "place a design sign");
				return;
			}
			if (!ServerBuildGuard.claimBlockBudget(player, 1))
				return;

			Level entityWorld = ServerBuildGuard.levelOf(player);
			entityWorld.setBlockAndUpdate(packet.position, Blocks.SPRUCE_SIGN.defaultBlockState());

			if (entityWorld.getBlockEntity(packet.position) instanceof SignBlockEntity sign) {
				//? if >=1.20 {
				sign.setText(new SignText().setMessage(0, Component.literal(packet.text1)).setMessage(1, Component.literal(packet.text2)), true);
				//?} else {
				/*sign.setMessage(0, Component.literal(packet.text1));
				sign.setMessage(1, Component.literal(packet.text2));
				sign.setChanged();
				*///?}
			}
		});
	}

}
