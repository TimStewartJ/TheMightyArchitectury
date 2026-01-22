package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class InstantPrintPacket extends BaseC2SMessage {

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
	public MessageType getType() {
		return AllPackets.INSTANT_PRINT;
	}

	@Override
	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeInt(blocks.size);
		blocks.blocks.forEach((pos, state) -> {
			buf.writeNbt(NbtUtils.writeBlockState(state));
			buf.writeBlockPos(pos);
		});
	}
	
	@Override
	public void handle(NetworkManager.PacketContext context) {
		context.queue(() -> {
			var holderGetter = context.getPlayer().level().holderLookup(Registries.BLOCK);
			if (blocks.rawData != null) {
				// Decode from raw data on server side
				for (BlockData data : blocks.rawData) {
					BlockState state = NbtUtils.readBlockState(holderGetter, data.tag);
					context.getPlayer().level().setBlock(data.pos, state, 3);
				}
			} else {
				// Already decoded (shouldn't happen for C2S)
				blocks.blocks.forEach((pos, state) -> {
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
