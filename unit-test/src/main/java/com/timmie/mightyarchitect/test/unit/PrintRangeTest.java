package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.PrintRange;
import com.timmie.mightyarchitect.networking.ServerBuildGuard;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Printing refuses a build that reaches past the server's placement range. The client measures with
 * the server's own rule, so the two cannot disagree about which blocks would be dropped.
 */
@Bootstrapped
@DisplayName("Print range")
class PrintRangeTest {

	private static final int LIMIT = ServerBuildGuard.MAX_BUILD_DISTANCE;

	@Test
	@DisplayName("the farthest block is measured to its centre, after the anchor offset")
	void farthestUsesAnchorAndBlockCentres() {
		List<BlockPos> local = List.of(BlockPos.ZERO, new BlockPos(9, 0, 0), new BlockPos(2, 0, 0));
		double farthest = ServerBuildGuard.farthestDistance(0.5, 0.5, 0.5, local, new BlockPos(100, 0, 0));
		assertEquals(109.0, farthest, 1e-9);
	}

	@Test
	@DisplayName("no blocks is no distance")
	void emptyBuildIsInRange() {
		assertEquals(0.0, ServerBuildGuard.farthestDistance(5, 5, 5, List.of(), BlockPos.ZERO));
	}

	@Test
	@DisplayName("a block exactly at the limit can still be printed, one past it cannot")
	void boundaryMatchesTheServer() {
		BlockPos atLimit = new BlockPos(LIMIT, 0, 0);
		BlockPos pastLimit = new BlockPos(LIMIT + 1, 0, 0);

		double at = ServerBuildGuard.farthestDistance(0.5, 0.5, 0.5, List.of(atLimit), BlockPos.ZERO);
		double past = ServerBuildGuard.farthestDistance(0.5, 0.5, 0.5, List.of(pastLimit), BlockPos.ZERO);

		assertFalse(PrintRange.isOutOfReach(at));
		assertTrue(ServerBuildGuard.withinReach(0.5, 0.5, 0.5, atLimit));
		assertTrue(PrintRange.isOutOfReach(past));
		assertFalse(ServerBuildGuard.withinReach(0.5, 0.5, 0.5, pastLimit));
	}

	@Test
	@DisplayName("the message names the distance, rounded up, and the limit")
	void messageNamesDistanceAndLimit() {
		assertEquals("This build reaches 312 blocks from you. Printing only works within " + LIMIT
			+ " - move closer and print again.", PrintRange.outOfRangeMessage(311.2));
	}
}
