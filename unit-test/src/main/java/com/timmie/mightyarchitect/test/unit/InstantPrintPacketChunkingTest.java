package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Printing a building sends its blocks to the server split across many packets. The split is the
 * only thing standing between a large build and a payload the connection refuses, and the same
 * bound is what the decoder trusts on the way in - a packet declaring more blocks than
 * {@link InstantPrintPacket#MAX_BLOCKS_PER_PACKET} is refused, so a sender that ever exceeded it
 * would silently fail to print.
 * <p>
 * The server-test companion already drives this against a real level, but only asserts that the
 * split happened at all. The arithmetic itself - how many packets, which blocks in them, where the
 * anchor lands - is pure, and asserting it here costs milliseconds instead of a dedicated server
 * on 25 targets.
 */
@Bootstrapped
@DisplayName("InstantPrintPacket.sendSchematic")
class InstantPrintPacketChunkingTest {

	private static final int MAX = InstantPrintPacket.MAX_BLOCKS_PER_PACKET;
	private static final BlockPos ANCHOR = new BlockPos(100, 64, -200);

	/** A schematic of {@code count} distinct positions, each carrying a distinguishable state. */
	private static Map<BlockPos, BlockState> schematic(int count) {
		BlockState[] palette = { Blocks.STONE.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(),
			Blocks.GLASS.defaultBlockState() };

		Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
		for (int i = 0; i < count; i++)
			blocks.put(new BlockPos(i % 16, i / 256, (i / 16) % 16), palette[i % palette.length]);
		return blocks;
	}

	private static Map<BlockPos, BlockState> flatten(List<InstantPrintPacket> packets) {
		Map<BlockPos, BlockState> all = new HashMap<>();
		packets.forEach(packet -> all.putAll(packet.blocks()));
		return all;
	}

	@Test
	@DisplayName("the bound is positive, so a build can actually be split")
	void theBoundIsUsable() {
		assertTrue(MAX > 0, "MAX_BLOCKS_PER_PACKET must be positive");
	}

	@Test
	@DisplayName("an empty schematic still yields one packet")
	void emptySchematicYieldsOnePacket() {
		List<InstantPrintPacket> packets = InstantPrintPacket.sendSchematic(Map.of(), ANCHOR);

		assertEquals(1, packets.size());
		assertTrue(packets.get(0)
			.blocks()
			.isEmpty());
	}

	@Test
	@DisplayName("a schematic that fits stays in one packet")
	void schematicThatFitsStaysInOnePacket() {
		List<InstantPrintPacket> packets = InstantPrintPacket.sendSchematic(schematic(MAX), ANCHOR);

		assertEquals(1, packets.size());
		assertEquals(MAX, packets.get(0)
			.size());
	}

	@Test
	@DisplayName("one block past the bound needs a second packet")
	void oneBlockPastTheBoundSplits() {
		List<InstantPrintPacket> packets = InstantPrintPacket.sendSchematic(schematic(MAX + 1), ANCHOR);

		assertEquals(2, packets.size());
		assertEquals(MAX, packets.get(0)
			.size());
		assertEquals(1, packets.get(1)
			.size());
	}

	/**
	 * The bound is what the decoder enforces, so a sender that overshot it by one would have every
	 * such packet rejected on arrival - a print that silently does nothing.
	 */
	@TestFactory
	@DisplayName("no packet ever exceeds the bound the decoder enforces")
	Stream<DynamicTest> noPacketExceedsTheBound() {
		return Stream.of(0, 1, MAX - 1, MAX, MAX + 1, MAX * 3, MAX * 3 + 1, 1000)
			.map(count -> DynamicTest.dynamicTest(count + " blocks", () -> {
				List<InstantPrintPacket> packets = InstantPrintPacket.sendSchematic(schematic(count), ANCHOR);

				for (InstantPrintPacket packet : packets) {
					assertTrue(packet.size() <= MAX,
						() -> "a packet carried " + packet.size() + " blocks, past the limit of " + MAX);
					assertEquals(packet.blocks()
						.size(), packet.size(), "the declared size disagreed with the payload");
				}

				assertEquals(Math.max(1, (int) Math.ceil(count / (double) MAX)), packets.size(),
					"unexpected packet count for " + count + " blocks");
			}));
	}

	@Test
	@DisplayName("every block survives the split exactly once")
	void everyBlockSurvivesExactlyOnce() {
		Map<BlockPos, BlockState> source = schematic(MAX * 4 + 7);
		List<InstantPrintPacket> packets = InstantPrintPacket.sendSchematic(source, ANCHOR);

		int totalSent = packets.stream()
			.mapToInt(InstantPrintPacket::size)
			.sum();
		assertEquals(source.size(), totalSent, "the split lost or duplicated blocks");

		Map<BlockPos, BlockState> flattened = flatten(packets);
		assertEquals(source.size(), flattened.size(), "two packets carried the same position");

		source.forEach((local, state) -> assertEquals(state, flattened.get(local.offset(ANCHOR)),
			() -> "the block at " + local + " did not arrive at its anchored position"));
	}

	@Test
	@DisplayName("packets do not overlap")
	void packetsDoNotOverlap() {
		List<InstantPrintPacket> packets = InstantPrintPacket.sendSchematic(schematic(MAX * 3), ANCHOR);

		Set<BlockPos> seen = new HashSet<>();
		for (InstantPrintPacket packet : packets)
			for (BlockPos pos : packet.blocks()
				.keySet())
				assertTrue(seen.add(pos), () -> "position " + pos + " appeared in two packets");
	}

	@Test
	@DisplayName("the anchor is applied once, not twice")
	void anchorIsAppliedOnce() {
		Map<BlockPos, BlockState> source = Map.of(new BlockPos(1, 2, 3), Blocks.STONE.defaultBlockState());

		Map<BlockPos, BlockState> sent = flatten(InstantPrintPacket.sendSchematic(source, ANCHOR));

		assertTrue(sent.containsKey(new BlockPos(101, 66, -197)),
			() -> "expected the anchored position, got " + sent.keySet());
		assertFalse(sent.containsKey(new BlockPos(1, 2, 3)), "the block was sent at its local position");
	}

	@Test
	@DisplayName("a zero anchor leaves positions where they were")
	void zeroAnchorIsIdentity() {
		Map<BlockPos, BlockState> source = schematic(MAX + 5);

		assertEquals(source, flatten(InstantPrintPacket.sendSchematic(source, BlockPos.ZERO)));
	}

	@Test
	@DisplayName("sending does not mutate the schematic it was given")
	void doesNotMutateItsInput() {
		Map<BlockPos, BlockState> source = schematic(MAX * 2 + 3);
		Map<BlockPos, BlockState> before = new LinkedHashMap<>(source);

		InstantPrintPacket.sendSchematic(source, ANCHOR);

		assertEquals(before, source, "sendSchematic modified the caller's schematic");
	}
}
