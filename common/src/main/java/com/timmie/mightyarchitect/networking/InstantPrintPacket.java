package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import dev.architectury.networking.NetworkManager;
//? if >=1.21.10 {
//?} else {
/*import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
//? if >=1.21.10 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*
*///?}
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

//? if >=1.21.10 {
public class InstantPrintPacket implements CustomPacketPayload {
//?} else {
/*public class InstantPrintPacket extends BaseC2SMessage {
*///?}

	private BunchOfBlocks blocks;

	public InstantPrintPacket(BunchOfBlocks blocks) {
		this.blocks = blocks;
	}

	public InstantPrintPacket(RegistryFriendlyByteBuf buf) {
		// Store raw NBT data to decode on server side with proper registry
		int size = buf.readInt();
		this.blocks = new BunchOfBlocks(new HashMap<>());
		this.blocks.rawData = new ArrayList<>();
		this.blocks.size = size;

		for (int i = 0; i < size; i++) {
			CompoundTag blockTag = buf.readNbt();
			BlockPos pos = buf.readBlockPos();
			this.blocks.rawData.add(new BlockData(blockTag, pos));
		}
	}

	@Override
	//? if >=1.21.10 {
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.INSTANT_PRINT_TYPE;
	//?} else {
	/*public MessageType getType() {
		return AllPackets.INSTANT_PRINT;
	*///?}
	}

	//? if >=1.21.10 {
	public static void write(RegistryFriendlyByteBuf buf, InstantPrintPacket packet) {
		buf.writeInt(packet.blocks.size);
		packet.blocks.blocks.forEach((pos, state) -> {
	//?} else {
	/*@Override
	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeInt(blocks.size);
		blocks.blocks.forEach((pos, state) -> {
	*///?}
			buf.writeNbt(NbtUtils.writeBlockState(state));
			buf.writeBlockPos(pos);
		});
	}

	//? if >=1.21.10 {
	public static void handle(InstantPrintPacket packet, NetworkManager.PacketContext context) {
	//?} else {
	/*@Override
	public void handle(NetworkManager.PacketContext context) {
	*///?}
		context.queue(() -> {
			var holderGetter = context.getPlayer().level().holderLookup(Registries.BLOCK);
			//? if >=1.21.10 {
			if (packet.blocks.rawData != null) {
			//?} else {
			/*if (blocks.rawData != null) {
			*///?}
				// Decode from raw data on server side
				//? if >=1.21.10 {
				for (BlockData data : packet.blocks.rawData) {
				//?} else {
				/*for (BlockData data : blocks.rawData) {
				*///?}
					BlockState state = NbtUtils.readBlockState(holderGetter, data.tag);
					context.getPlayer().level().setBlock(data.pos, state, 3);
				}
			} else {
				// Already decoded (shouldn't happen for C2S)
				//? if >=1.21.10 {
				packet.blocks.blocks.forEach((pos, state) -> {
				//?} else {
				/*blocks.blocks.forEach((pos, state) -> {
				*///?}
					context.getPlayer().level().setBlock(pos, state, 3);
				});
			}
		});
	}

	public static List<InstantPrintPacket> sendSchematic(Map<BlockPos, BlockState> blockMap, BlockPos anchor) {
		List<InstantPrintPacket> packets = new LinkedList<>();

		Map<BlockPos, BlockState> currentMap = new HashMap<>(BunchOfBlocks.MAX_SIZE);
		List<BlockPos> posList = new ArrayList<>(blockMap.keySet());

		for (int i = 0; i < blockMap.size(); i++) {
			if (currentMap.size() >= BunchOfBlocks.MAX_SIZE) {
				packets.add(new InstantPrintPacket(new BunchOfBlocks(currentMap)));
				currentMap = new HashMap<>(BunchOfBlocks.MAX_SIZE);
			}
			currentMap.put(posList.get(i).offset(anchor), blockMap.get(posList.get(i)));
		}
		packets.add(new InstantPrintPacket(new BunchOfBlocks(currentMap)));

		return packets;
	}

	record BlockData(CompoundTag tag, BlockPos pos) {}

	static class BunchOfBlocks {
		static final int MAX_SIZE = 32;
		Map<BlockPos, BlockState> blocks;
		List<BlockData> rawData;
		int size;

		public BunchOfBlocks(Map<BlockPos, BlockState> blocks) {
			this.blocks = blocks;
			this.size = blocks.size();
		}

	}

}
