//? if >=26 {
package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import dev.architectury.networking.NetworkManager;
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
	
	public static void handle(PlaceSignPacket packet, NetworkManager.PacketContext context) {
		context.queue(() -> {
			Level entityWorld = context.getPlayer().level();
			entityWorld.setBlockAndUpdate(packet.position, Blocks.SPRUCE_SIGN.defaultBlockState());
			SignBlockEntity sign = (SignBlockEntity) entityWorld.getBlockEntity(packet.position);

			if (sign != null) {
				sign.setText(new SignText().setMessage(0, Component.literal(packet.text1)).setMessage(1, Component.literal(packet.text2)), true);
			}
		});
	}
	
}
//?} else {
/*package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public class PlaceSignPacket extends BaseC2SMessage {
	
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
	public MessageType getType() {
		return AllPackets.PLACE_SIGN;
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeUtf(text1);
		buffer.writeUtf(text2);
		buffer.writeBlockPos(position);
	}
	
	@Override
	public void handle(NetworkManager.PacketContext context) {
		context.queue(() -> {
			Level entityWorld = context.getPlayer().level();
			entityWorld.setBlockAndUpdate(position, Blocks.SPRUCE_SIGN.defaultBlockState());
			SignBlockEntity sign = (SignBlockEntity) entityWorld.getBlockEntity(position);

			if (sign != null) {
				sign.setText(new SignText().setMessage(0, Component.literal(text1)).setMessage(1, Component.literal(text2)), true);
			}
		});
	}
	
}*///?}
