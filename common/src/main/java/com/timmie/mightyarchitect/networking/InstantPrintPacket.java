package com.timmie.mightyarchitect.networking;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Supplier;

public class InstantPrintPacket {

	private BunchOfBlocks blocks;

	public InstantPrintPacket() {
	}

	public InstantPrintPacket(BunchOfBlocks blocks) {
		this.blocks = blocks;
	}

	public InstantPrintPacket(FriendlyByteBuf buf) {
		Map<BlockPos, BlockState> blocks = new HashMap<>();

		// for reading block state later
		var holderGetter = Minecraft.getInstance().level.holderLookup(Registries.BLOCK);
		int size = buf.readInt();
		for (int i = 0; i < size; i++) {
			CompoundTag blockTag = buf.readNbt();
			BlockPos pos = buf.readBlockPos();
			blocks.put(pos, NbtUtils.readBlockState(holderGetter, blockTag));
		}
		this.blocks = new BunchOfBlocks(blocks);
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(blocks.size);
		blocks.blocks.forEach((pos, state) -> {
			buf.writeNbt(NbtUtils.writeBlockState(state));
			buf.writeBlockPos(pos);
		});
	}
	
	public void handle(Supplier<NetworkManager.PacketContext> context) {
		context.get().queue(() -> {
			blocks.blocks.forEach((pos, state) -> {
				context.get().getPlayer().getCommandSenderWorld().setBlock(pos, state, 3);
			});
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
	
	static class BunchOfBlocks {
		static final int MAX_SIZE = 32;
		Map<BlockPos, BlockState> blocks;
		int size;
		
		public BunchOfBlocks(Map<BlockPos, BlockState> blocks) {
			this.blocks = blocks;
			this.size = blocks.size();
		}
		
	}

}
