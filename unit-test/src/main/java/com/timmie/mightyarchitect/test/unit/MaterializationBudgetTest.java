package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.Schematic;
import com.timmie.mightyarchitect.control.Schematic.Materialization;
import com.timmie.mightyarchitect.control.compose.Cuboid;
import com.timmie.mightyarchitect.control.palette.BlockOrientation;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteBlockInfo;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applying a palette to a build volume used to happen inside whichever input handler asked for it,
 * whole, every time - so a palette swap on a large build stalled the client for as long as the
 * build was big. It is now spread across ticks.
 * <p>
 * That is only safe if slicing the work cannot change the answer, and "cannot change the answer" is
 * exactly what a screenshot test is bad at: a subtly wrong block deep inside a wall looks like a
 * building either way. So the property asserted here is the equivalence itself - the same inputs
 * through any sequence of budgets land on the same blocks and the same bounding box as one pass.
 */
@Bootstrapped
@DisplayName("Schematic.Materialization")
class MaterializationBudgetTest {

	/**
	 * {@code afterPosition} is filled in by {@code Sketch.assemble} in the real pipeline, and
	 * {@link PaletteDefinition#get(PaletteBlockInfo)} dereferences it, so it has to be set here.
	 */
	private static PaletteBlockInfo info(Palette palette) {
		PaletteBlockInfo info = new PaletteBlockInfo(palette, BlockOrientation.NONE);
		info.afterPosition = BlockOrientation.NONE;
		return info;
	}

	/** Insertion-ordered so the two runs being compared walk the entries in the same order. */
	private static Map<BlockPos, PaletteBlockInfo> layer(Object... posThenPalette) {
		Map<BlockPos, PaletteBlockInfo> layer = new LinkedHashMap<>();
		for (int i = 0; i < posThenPalette.length; i += 2)
			layer.put((BlockPos) posThenPalette[i], info((Palette) posThenPalette[i + 1]));
		return layer;
	}

	private static Map<BlockPos, PaletteBlockInfo> gridLayer(int side, Palette palette) {
		Map<BlockPos, PaletteBlockInfo> layer = new LinkedHashMap<>();
		for (int x = 0; x < side; x++)
			for (int y = 0; y < side; y++)
				for (int z = 0; z < side; z++)
					layer.put(new BlockPos(x, y, z), info(palette));
		return layer;
	}

	private static Materialization job(Map<BlockPos, PaletteBlockInfo> primary,
		Map<BlockPos, PaletteBlockInfo> secondary) {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		return new Materialization(palette, palette, primary, secondary);
	}

	private static Materialization runWithBudget(Map<BlockPos, PaletteBlockInfo> primary,
		Map<BlockPos, PaletteBlockInfo> secondary, int budget) {
		Materialization materialization = job(primary, secondary);
		int guard = 0;
		while (!materialization.advance(budget))
			if (++guard > 10_000)
				throw new AssertionError("materialization never finished at budget " + budget);
		return materialization;
	}

	@Test
	@DisplayName("a budget of one still finishes, one entry at a time")
	void aBudgetOfOneFinishes() {
		Map<BlockPos, PaletteBlockInfo> primary = layer(BlockPos.ZERO, Palette.HEAVY_PRIMARY,
			new BlockPos(1, 0, 0), Palette.ROOF_PRIMARY);
		Map<BlockPos, PaletteBlockInfo> secondary = layer(new BlockPos(0, 1, 0), Palette.WINDOW);

		Materialization materialization = job(primary, secondary);

		assertEquals(3, materialization.total);
		assertFalse(materialization.advance(1), "one entry of three should not finish the job");
		assertFalse(materialization.advance(1), "two entries of three should not finish the job");
		assertTrue(materialization.advance(1), "the third entry should finish the job");
		assertEquals(3, materialization.blockMap.size());
	}

	@Test
	@DisplayName("nothing is published early: the first slice leaves the rest unapplied")
	void slicesDoNotLeakPartialResults() {
		Materialization materialization = job(gridLayer(4, Palette.HEAVY_PRIMARY), Map.of());

		assertFalse(materialization.advance(10));
		assertEquals(10, materialization.blockMap.size(), "a slice applied more than its budget");
	}

	@Test
	@DisplayName("every budget lands on exactly the same blocks and bounds as one pass")
	void budgetingCannotChangeTheAnswer() {
		Map<BlockPos, PaletteBlockInfo> primary = gridLayer(5, Palette.HEAVY_PRIMARY);
		Map<BlockPos, PaletteBlockInfo> secondary = gridLayer(6, Palette.WINDOW);

		Materialization whole = runWithBudget(primary, secondary, Integer.MAX_VALUE);

		for (int budget : new int[] { 1, 2, 7, 64, 300 }) {
			Materialization sliced = runWithBudget(primary, secondary, budget);

			assertEquals(whole.blockMap, sliced.blockMap, "budget " + budget + " produced different blocks");
			assertEquals(whole.bounds.getOrigin(), sliced.bounds.getOrigin(),
				"budget " + budget + " produced a different origin");
			assertEquals(whole.bounds.getSize(), sliced.bounds.getSize(),
				"budget " + budget + " produced a different size");
		}
	}

	/**
	 * The rule the second layer exists for. A slice boundary falling between the primary entry and
	 * the secondary one that overlaps it must not change who wins.
	 */
	@Test
	@DisplayName("the primary layer keeps winning where it is preferred, whatever the budget")
	void primaryStillWinsAcrossSliceBoundaries() {
		BlockPos contested = new BlockPos(2, 2, 2);
		Map<BlockPos, PaletteBlockInfo> primary = layer(BlockPos.ZERO, Palette.HEAVY_PRIMARY, contested,
			Palette.HEAVY_PRIMARY);
		Map<BlockPos, PaletteBlockInfo> secondary = layer(contested, Palette.WINDOW, new BlockPos(9, 0, 0),
			Palette.WINDOW);

		BlockState expected = runWithBudget(primary, secondary, Integer.MAX_VALUE).blockMap.get(contested);

		for (int budget = 1; budget <= 4; budget++)
			assertEquals(expected, runWithBudget(primary, secondary, budget).blockMap.get(contested),
				"budget " + budget + " changed which layer won the contested position");
	}

	@Test
	@DisplayName("the bounding box covers both layers, not just the first")
	void boundsCoverBothLayers() {
		Map<BlockPos, PaletteBlockInfo> primary = layer(BlockPos.ZERO, Palette.HEAVY_PRIMARY);
		Map<BlockPos, PaletteBlockInfo> secondary = layer(new BlockPos(4, 5, 6), Palette.WINDOW);

		Cuboid bounds = runWithBudget(primary, secondary, 1).bounds;

		assertEquals(BlockPos.ZERO, bounds.getOrigin());
		assertEquals(new BlockPos(5, 6, 7), bounds.getSize());
	}

	@Test
	@DisplayName("an empty sketch finishes immediately with no bounds")
	void emptySketchFinishesImmediately() {
		Materialization materialization = job(new HashMap<>(), new HashMap<>());

		assertTrue(materialization.advance(1), "an empty job should be finished the first time it is asked");
		assertTrue(materialization.blockMap.isEmpty());
		assertNull(materialization.bounds, "an empty job should not invent a bounding box");
	}

	/**
	 * {@code growToInclude} is shared with the one-pass path, so this is really checking that the
	 * job grows the box over the positions it actually applied rather than every position it saw -
	 * a position the primary layer wins must not stretch the box twice.
	 */
	@Test
	@DisplayName("a position the secondary layer loses does not grow the box again")
	void losingPositionsDoNotGrowTheBox() {
		BlockPos contested = new BlockPos(3, 3, 3);
		Map<BlockPos, PaletteBlockInfo> primary = layer(BlockPos.ZERO, Palette.HEAVY_PRIMARY, contested,
			Palette.HEAVY_PRIMARY);
		Map<BlockPos, PaletteBlockInfo> secondary = layer(contested, Palette.WINDOW);

		Cuboid bounds = runWithBudget(primary, secondary, 1).bounds;

		assertEquals(BlockPos.ZERO, bounds.getOrigin());
		assertEquals(new BlockPos(4, 4, 4), bounds.getSize());
	}

	@Test
	@DisplayName("growToInclude is what the job uses, so the two agree")
	void agreesWithGrowToInclude() {
		Map<BlockPos, PaletteBlockInfo> primary = gridLayer(3, Palette.HEAVY_PRIMARY);

		Cuboid expected = null;
		for (BlockPos pos : primary.keySet())
			expected = Schematic.growToInclude(expected, pos);

		Cuboid actual = runWithBudget(primary, Map.of(), 2).bounds;

		assertEquals(expected.getOrigin(), actual.getOrigin());
		assertEquals(expected.getSize(), actual.getSize());
	}

	@Test
	@DisplayName("the blocks applied are the ones the palette names")
	void appliesThePaletteEntries() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.put(Palette.ROOF_PRIMARY, Blocks.GOLD_BLOCK);

		Materialization materialization = new Materialization(palette, palette,
			layer(BlockPos.ZERO, Palette.ROOF_PRIMARY), Map.of());
		assertTrue(materialization.advance(Integer.MAX_VALUE));

		assertEquals(Blocks.GOLD_BLOCK.defaultBlockState(), materialization.blockMap.get(BlockPos.ZERO));
	}
}
