package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class InstantPrintPacket implements CustomPacketPayload {

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
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.INSTANT_PRINT_TYPE;
	}

	public static void write(RegistryFriendlyByteBuf buf, InstantPrintPacket packet) {
		buf.writeInt(packet.blocks.size);
		packet.blocks.blocks.forEach((pos, state) -> {
			buf.writeNbt(NbtUtils.writeBlockState(state));
			buf.writeBlockPos(pos);
		});
	}

	public static void handle(InstantPrintPacket packet, PacketContext context) {
		context.enqueue(() -> {
			var holderGetter = context.player().level().holderLookup(Registries.BLOCK);
			if (packet.blocks.rawData != null) {
				// Decode from raw data on server side
				for (BlockData data : packet.blocks.rawData) {
					BlockState state = NbtUtils.readBlockState(holderGetter, data.tag);
					context.player().level().setBlock(data.pos, state, 3);
				}
			} else {
				// Already decoded (shouldn't happen for C2S)
				packet.blocks.blocks.forEach((pos, state) -> {
					context.player().level().setBlock(pos, state, 3);
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
