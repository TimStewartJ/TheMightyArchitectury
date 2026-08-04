package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.Schematic;
import com.timmie.mightyarchitect.control.compose.Cuboid;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bounding box a materialized sketch reports is accumulated one block at a time as the palette
 * is applied, and everything downstream sizes itself from it - the structure template written to
 * disk, the renderer's chunk, the outline drawn in the world. An off-by-one here crops the printed
 * building rather than throwing.
 * <p>
 * This is the whole of {@code materializeSketch} that can run outside a game: the rest of it ends
 * in a {@code TemplateBlockAccess}, which reads the client's level.
 */
@Bootstrapped
@DisplayName("Schematic.growToInclude")
class SchematicBoundsTest {

	private static Cuboid boundsOf(BlockPos... positions) {
		Cuboid bounds = null;
		for (BlockPos pos : positions)
			bounds = Schematic.growToInclude(bounds, pos);
		return bounds;
	}

	@Test
	@DisplayName("the first position gives a one-block box at that position")
	void firstPositionGivesAUnitBox() {
		Cuboid bounds = boundsOf(new BlockPos(3, 4, 5));

		assertEquals(new BlockPos(3, 4, 5), bounds.getOrigin());
		assertEquals(new BlockPos(1, 1, 1), bounds.getSize());
	}

	@Test
	@DisplayName("repeating a position does not grow the box")
	void repeatingAPositionIsIdempotent() {
		BlockPos pos = new BlockPos(3, 4, 5);

		assertEquals(new BlockPos(1, 1, 1), boundsOf(pos, pos, pos)
			.getSize());
	}

	@Test
	@DisplayName("grows forwards to include a position past the far corner")
	void growsForwards() {
		Cuboid bounds = boundsOf(BlockPos.ZERO, new BlockPos(2, 3, 4));

		assertEquals(BlockPos.ZERO, bounds.getOrigin());
		assertEquals(new BlockPos(3, 4, 5), bounds.getSize());
	}

	@Test
	@DisplayName("grows backwards by moving the origin")
	void growsBackwards() {
		Cuboid bounds = boundsOf(BlockPos.ZERO, new BlockPos(-2, -3, -4));

		assertEquals(new BlockPos(-2, -3, -4), bounds.getOrigin());
		assertEquals(new BlockPos(3, 4, 5), bounds.getSize());
	}

	@Test
	@DisplayName("grows in both directions on the same axis")
	void growsBothWays() {
		Cuboid bounds = boundsOf(BlockPos.ZERO, new BlockPos(5, 0, 0), new BlockPos(-5, 0, 0));

		assertEquals(-5, bounds.getOrigin()
			.getX());
		assertEquals(11, bounds.getSize()
			.getX());
	}

	/**
	 * The property, rather than a hand-computed number: whatever order the positions arrive in -
	 * and they arrive in {@code HashMap} order, so it really is arbitrary - the box has to contain
	 * every one of them and be no larger than it has to be.
	 */
	@Test
	@DisplayName("contains every position it was grown over, however they were ordered")
	void containsEveryPositionInAnyOrder() {
		List<BlockPos> positions = List.of(new BlockPos(4, 1, -7), new BlockPos(-3, 9, 2), new BlockPos(0, 0, 0),
			new BlockPos(12, -4, 5), new BlockPos(-8, 2, 11));

		Cuboid forwards = boundsOf(positions.toArray(BlockPos[]::new));
		List<BlockPos> reversed = new java.util.ArrayList<>(positions);
		java.util.Collections.reverse(reversed);
		Cuboid backwards = boundsOf(reversed.toArray(BlockPos[]::new));

		assertEquals(forwards.getOrigin(), backwards.getOrigin(), "the box depended on insertion order");
		assertEquals(forwards.getSize(), backwards.getSize(), "the box depended on insertion order");

		for (BlockPos pos : positions)
			assertTrue(forwards.contains(pos), () -> "the box excluded " + pos);

		assertEquals(new BlockPos(-8, -4, -7), forwards.getOrigin());
		assertEquals(new BlockPos(21, 14, 19), forwards.getSize());
	}

	@Test
	@DisplayName("a single-plane sketch still has one block of depth")
	void flatSketchIsOneDeep() {
		Cuboid bounds = boundsOf(new BlockPos(0, 0, 0), new BlockPos(3, 0, 3));

		assertEquals(1, bounds.getSize()
			.getY(), "a flat sketch collapsed to zero height");
	}

	@Test
	@DisplayName("grows the box in place rather than replacing it")
	void growsInPlace() {
		Cuboid bounds = Schematic.growToInclude(null, BlockPos.ZERO);

		assertSame(bounds, Schematic.growToInclude(bounds, new BlockPos(9, 9, 9)));
	}
}
