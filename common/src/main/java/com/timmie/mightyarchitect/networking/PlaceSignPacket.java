package com.timmie.mightyarchitect.networking;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import java.util.function.Supplier;

public class PlaceSignPacket {
	
	public String text1;
	public String text2;
	public BlockPos position;

	public PlaceSignPacket() {
	}
	
	public PlaceSignPacket(String textLine1, String textLine2, BlockPos position) {
		this.text1 = textLine1;
		this.text2 = textLine2;
		this.position = position;
	}
	
	public PlaceSignPacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf(128), buffer.readUtf(128), buffer.readBlockPos());
	}

	public void toBytes(FriendlyByteBuf buffer) {
		buffer.writeUtf(text1);
		buffer.writeUtf(text2);
		buffer.writeBlockPos(position);
	}
	
	public void handle(Supplier<NetworkManager.PacketContext> context) {
		context.get().queue(() -> {
			Level entityWorld = context.get().getPlayer().getCommandSenderWorld();
			entityWorld.setBlockAndUpdate(position, Blocks.SPRUCE_SIGN.defaultBlockState());
			SignBlockEntity sign = (SignBlockEntity) entityWorld.getBlockEntity(position);

			sign.setText(new SignText().setMessage(0, Component.literal(text1)).setMessage(1, Component.literal(text2)), true);
		});
	}
	
}
