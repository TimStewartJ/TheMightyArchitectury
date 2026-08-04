package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.platform.MightyPacket;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import io.netty.handler.codec.DecoderException;

import java.util.*;
import java.util.function.Predicate;

public class InstantPrintPacket implements MightyPacket {

	/**
	 * Largest number of blocks one packet may carry.
	 * <p>
	 * Both a chunking bound on the way out and a decode-time bound on the way in: the block count
	 * is read straight off the wire and drives an allocating loop, so a packet declaring more than
	 * {@link #sendSchematic} could ever produce is refused rather than trusted.
	 */
	public static final int MAX_BLOCKS_PER_PACKET = 32;

	private BunchOfBlocks blocks;

	public InstantPrintPacket(BunchOfBlocks blocks) {
		this.blocks = blocks;
	}

	//? if >=1.20.5 {
	public InstantPrintPacket(RegistryFriendlyByteBuf buf) {
	//?} else {
	/*public InstantPrintPacket(FriendlyByteBuf buf) {
	*///?}
		// Store raw NBT data to decode on server side with proper registry
		int size = buf.readInt();
		// sendSchematic never emits more than MAX_BLOCKS_PER_PACKET per packet, so anything larger
		// is either corrupt or hostile - and this loop allocates per iteration.
		if (size < 0 || size > MAX_BLOCKS_PER_PACKET)
			throw new DecoderException(
				"InstantPrintPacket declared " + size + " blocks; the limit is " + MAX_BLOCKS_PER_PACKET);
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
	//? if >=1.20.5 {
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.INSTANT_PRINT_TYPE;
	}

	public static void write(RegistryFriendlyByteBuf buf, InstantPrintPacket packet) {
		writeTo(buf, packet);
	}
	//?} else {
	/*public ResourceLocation id() {
		return AllPackets.INSTANT_PRINT_ID;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeTo(buf, this);
	}
	*///?}

	private static void writeTo(FriendlyByteBuf buf, InstantPrintPacket packet) {
		buf.writeInt(packet.blocks.size);
		packet.blocks.blocks.forEach((pos, state) -> {
			buf.writeNbt(NbtUtils.writeBlockState(state));
			buf.writeBlockPos(pos);
		});
	}

	public static void handle(InstantPrintPacket packet, PacketContext context) {
		ServerPlayer player = context.player();
		context.enqueue(() -> {
			if (!ServerBuildGuard.mayBuild(player)) {
				ServerBuildGuard.reportDenied(player, "print blocks");
				return;
			}
			if (!ServerBuildGuard.claimBlockBudget(player, packet.blocks.size))
				return;
			apply(ServerBuildGuard.levelOf(player), packet, pos -> ServerBuildGuard.mayBuildAt(player, pos));
		});
	}

	/**
	 * Places every block in the packet, unconditionally.
	 * <p>
	 * <b>Unauthorised.</b> Anything that got this packet off the wire must go through
	 * {@link ServerBuildGuard} first - {@link #handle} does. This overload exists for the automated
	 * server test, which drives the same placement logic against a real {@code ServerLevel} without
	 * a connected player to authorise.
	 */
	public static void apply(Level level, InstantPrintPacket packet) {
		apply(level, packet, pos -> true);
	}

	/** Places the blocks the given filter accepts; see {@link ServerBuildGuard#mayBuildAt}. */
	public static void apply(Level level, InstantPrintPacket packet, Predicate<BlockPos> allowed) {
		var holderGetter = level.holderLookup(Registries.BLOCK);
		if (packet.blocks.rawData != null) {
			// Decode from raw data on server side
			for (BlockData data : packet.blocks.rawData) {
				if (!allowed.test(data.pos))
					continue;
				BlockState state = NbtUtils.readBlockState(holderGetter, data.tag);
				level.setBlock(data.pos, state, 3);
			}
		} else {
			// Already decoded (shouldn't happen for C2S)
			packet.blocks.blocks.forEach((pos, state) -> {
				if (allowed.test(pos))
					level.setBlock(pos, state, 3);
			});
		}
	}

	public static List<InstantPrintPacket> sendSchematic(Map<BlockPos, BlockState> blockMap, BlockPos anchor) {
		List<InstantPrintPacket> packets = new LinkedList<>();

		Map<BlockPos, BlockState> currentMap = new HashMap<>(MAX_BLOCKS_PER_PACKET);
		List<BlockPos> posList = new ArrayList<>(blockMap.keySet());

		for (int i = 0; i < blockMap.size(); i++) {
			if (currentMap.size() >= MAX_BLOCKS_PER_PACKET) {
				packets.add(new InstantPrintPacket(new BunchOfBlocks(currentMap)));
				currentMap = new HashMap<>(MAX_BLOCKS_PER_PACKET);
			}
			currentMap.put(posList.get(i).offset(anchor), blockMap.get(posList.get(i)));
		}
		packets.add(new InstantPrintPacket(new BunchOfBlocks(currentMap)));

		return packets;
	}

	/**
	 * The blocks this packet is carrying, at their final world positions.
	 * <p>
	 * Populated on the sending side by {@link #sendSchematic}. A packet that came off the wire
	 * carries undecoded NBT instead and returns an empty map here - {@link #apply} is what reads
	 * that side.
	 */
	public Map<BlockPos, BlockState> blocks() {
		return Collections.unmodifiableMap(blocks.blocks);
	}

	/** How many blocks this packet declares, which is what the decoder bounds-checks. */
	public int size() {
		return blocks.size;
	}

	record BlockData(CompoundTag tag, BlockPos pos) {}

	static class BunchOfBlocks {
		Map<BlockPos, BlockState> blocks;
		List<BlockData> rawData;
		int size;

		public BunchOfBlocks(Map<BlockPos, BlockState> blocks) {
			this.blocks = blocks;
			this.size = blocks.size();
		}

	}

}
