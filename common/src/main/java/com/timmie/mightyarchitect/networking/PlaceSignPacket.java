package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import dev.architectury.networking.NetworkManager;
//? if >=1.21.10 {
//?} else {
/*import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
//? if >=1.21.10 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*
*///?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

//? if >=1.21.10 {
public class PlaceSignPacket implements CustomPacketPayload {
//?} else {
/*public class PlaceSignPacket extends BaseC2SMessage {
*///?}

	private final String text1;
	private final String text2;
	private final BlockPos position;

	public PlaceSignPacket(String textLine1, String textLine2, BlockPos position) {
		this.text1 = textLine1;
		this.text2 = textLine2;
		this.position = position;
	}

	public PlaceSignPacket(RegistryFriendlyByteBuf buffer) {
		this(buffer.readUtf(128), buffer.readUtf(128), buffer.readBlockPos());
	}

	@Override
	//? if >=1.21.10 {
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.PLACE_SIGN_TYPE;
	//?} else {
	/*public MessageType getType() {
		return AllPackets.PLACE_SIGN;
	*///?}
	}

	//? if >=1.21.10 {
	public static void write(RegistryFriendlyByteBuf buffer, PlaceSignPacket packet) {
		buffer.writeUtf(packet.text1);
		buffer.writeUtf(packet.text2);
		buffer.writeBlockPos(packet.position);
	//?} else {
	/*@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeUtf(text1);
		buffer.writeUtf(text2);
		buffer.writeBlockPos(position);
	*///?}
	}

	//? if >=1.21.10 {
	public static void handle(PlaceSignPacket packet, NetworkManager.PacketContext context) {
	//?} else {
	/*@Override
	public void handle(NetworkManager.PacketContext context) {
	*///?}
		context.queue(() -> {
			Level entityWorld = context.getPlayer().level();
			//? if >=1.21.10 {
			entityWorld.setBlockAndUpdate(packet.position, Blocks.SPRUCE_SIGN.defaultBlockState());
			SignBlockEntity sign = (SignBlockEntity) entityWorld.getBlockEntity(packet.position);
			//?} else {
			/*entityWorld.setBlockAndUpdate(position, Blocks.SPRUCE_SIGN.defaultBlockState());
			SignBlockEntity sign = (SignBlockEntity) entityWorld.getBlockEntity(position);
			*///?}

			if (sign != null) {
				//? if >=1.21.10 {
				sign.setText(new SignText().setMessage(0, Component.literal(packet.text1)).setMessage(1, Component.literal(packet.text2)), true);
				//?} else {
				/*sign.setText(new SignText().setMessage(0, Component.literal(text1)).setMessage(1, Component.literal(text2)), true);
				*///?}
			}
		});
	}

}
