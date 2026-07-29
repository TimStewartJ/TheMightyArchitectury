package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public class PlaceSignPacket implements CustomPacketPayload {

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
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.PLACE_SIGN_TYPE;
	}

	public static void write(RegistryFriendlyByteBuf buffer, PlaceSignPacket packet) {
		buffer.writeUtf(packet.text1);
		buffer.writeUtf(packet.text2);
		buffer.writeBlockPos(packet.position);
	}

	public static void handle(PlaceSignPacket packet, PacketContext context) {
		context.enqueue(() -> {
			Level entityWorld = context.player().level();
			entityWorld.setBlockAndUpdate(packet.position, Blocks.SPRUCE_SIGN.defaultBlockState());
			SignBlockEntity sign = (SignBlockEntity) entityWorld.getBlockEntity(packet.position);

			if (sign != null) {
				sign.setText(new SignText().setMessage(0, Component.literal(packet.text1)).setMessage(1, Component.literal(packet.text2)), true);
			}
		});
	}

}
