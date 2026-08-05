package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code scan} asks which palette slot a block belongs to, and it is asked that once per block when
 * a build is scanned. It used to answer by rebuilding a map of the entire palette every single
 * time; that map is now built once and kept.
 * <p>
 * Which turns a cheap lookup into a cache, and a cache into a staleness bug waiting to happen: the
 * palette editor rewrites slots while the same palette object stays in place, so an index that does
 * not notice a write would keep naming the block that used to be there. Everything below is about
 * that.
 */
@Bootstrapped
@DisplayName("PaletteDefinition.scan")
class PaletteScanCacheTest {

	@Test
	@DisplayName("finds the slot a block is bound to")
	void findsTheSlot() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.put(Palette.ROOF_PRIMARY, Blocks.GOLD_BLOCK);

		assertSame(Palette.ROOF_PRIMARY, palette.scan(Blocks.GOLD_BLOCK.defaultBlockState()));
	}

	@Test
	@DisplayName("answers the same way when asked twice")
	void isStableAcrossCalls() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();

		assertSame(palette.scan(Blocks.GRANITE.defaultBlockState()),
			palette.scan(Blocks.GRANITE.defaultBlockState()));
	}

	/**
	 * A state that names a block in the palette but is not one of the stored states verbatim, so
	 * answering it has to go through the reverse index rather than the linear search in front of
	 * it. This is the only shape of query the index is actually consulted for.
	 */
	private static BlockState rotated(net.minecraft.world.level.block.Block block, Direction facing) {
		return block.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
			.setValue(BlockStateProperties.OPEN, true);
	}

	/** The one that matters: a write after a read has to be visible to the next read. */
	@Test
	@DisplayName("a slot rewritten after the index was built is picked up by the next scan")
	void writesAfterAReadAreVisible() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();

		// Builds the index: the standard palette holds a south-facing oak trapdoor, and a
		// north-facing one can only be resolved through it.
		assertSame(Palette.OUTER_FLAT, palette.scan(rotated(Blocks.OAK_TRAPDOOR, Direction.NORTH)),
			"the index did not resolve a rotated entry in the first place");
		assertNull(palette.scan(rotated(Blocks.ACACIA_TRAPDOOR, Direction.NORTH)),
			"acacia is not in the standard palette to begin with");

		palette.put(Palette.ROOF_PRIMARY, Blocks.ACACIA_TRAPDOOR);

		assertSame(Palette.ROOF_PRIMARY, palette.scan(rotated(Blocks.ACACIA_TRAPDOOR, Direction.SOUTH)),
			"the scan answered from an index built before the slot was written");
	}

	@Test
	@DisplayName("a block that was replaced stops being found")
	void replacedBlocksStopBeingFound() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.put(Palette.ROOF_PRIMARY, Blocks.ACACIA_TRAPDOOR);
		assertSame(Palette.ROOF_PRIMARY, palette.scan(rotated(Blocks.ACACIA_TRAPDOOR, Direction.SOUTH)));

		palette.put(Palette.ROOF_PRIMARY, Blocks.DIAMOND_BLOCK);

		assertNull(palette.scan(rotated(Blocks.ACACIA_TRAPDOOR, Direction.SOUTH)),
			"a block that is no longer in the palette was still named");
		assertSame(Palette.ROOF_PRIMARY, palette.scan(Blocks.DIAMOND_BLOCK.defaultBlockState()));
	}

	@Test
	@DisplayName("one palette's writes do not reach another's answers")
	void palettesDoNotShareTheIndex() {
		PaletteDefinition one = PaletteDefinition.defaultPalette()
			.clone();
		PaletteDefinition other = PaletteDefinition.defaultPalette()
			.clone();

		one.put(Palette.ROOF_PRIMARY, Blocks.ACACIA_TRAPDOOR);
		assertSame(Palette.ROOF_PRIMARY, one.scan(rotated(Blocks.ACACIA_TRAPDOOR, Direction.SOUTH)));

		assertNull(other.scan(rotated(Blocks.ACACIA_TRAPDOOR, Direction.SOUTH)),
			"a palette answered with another palette's entry");
	}

	@Test
	@DisplayName("a clone answers from its own entries, not the palette it came from")
	void clonesAnswerFromTheirOwnEntries() {
		PaletteDefinition original = PaletteDefinition.defaultPalette()
			.clone();
		original.put(Palette.ROOF_PRIMARY, Blocks.ACACIA_TRAPDOOR);
		assertSame(Palette.ROOF_PRIMARY, original.scan(rotated(Blocks.ACACIA_TRAPDOOR, Direction.SOUTH)));

		PaletteDefinition clone = original.clone();
		clone.put(Palette.ROOF_PRIMARY, Blocks.DIAMOND_BLOCK);

		assertSame(Palette.ROOF_PRIMARY, clone.scan(Blocks.DIAMOND_BLOCK.defaultBlockState()));
		assertSame(Palette.ROOF_PRIMARY, original.scan(rotated(Blocks.ACACIA_TRAPDOOR, Direction.SOUTH)),
			"editing the clone changed what the original scanned to");
	}

	@Test
	@DisplayName("air is never a palette entry")
	void airIsNeverAnEntry() {
		assertNull(PaletteDefinition.defaultPalette()
			.clone()
			.scan(Blocks.AIR.defaultBlockState()));
	}

	/**
	 * The reason the index is keyed on {@code Block} rather than {@code BlockState}: a wall in a
	 * build is the same palette entry as the wall in the palette even though the two states differ
	 * by whatever their neighbours made of them.
	 */
	@Test
	@DisplayName("a rotated or reshaped state still resolves to its slot")
	void rotationIsIgnored() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.put(Palette.OUTER_FLAT, Blocks.OAK_TRAPDOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
			.setValue(BlockStateProperties.OPEN, true));

		assertSame(Palette.OUTER_FLAT, palette.scan(Blocks.OAK_TRAPDOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
			.setValue(BlockStateProperties.OPEN, true)),
			"a trapdoor facing the other way was not recognised as the same palette entry");
	}

	@Test
	@DisplayName("duplicate detection follows the entries as they change")
	void duplicateDetectionFollowsWrites() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.put(Palette.ROOF_PRIMARY, Blocks.GOLD_BLOCK);
		palette.put(Palette.FLOOR, Blocks.DIAMOND_BLOCK);
		assertFalse(palette.hasDuplicates(), "two different blocks were reported as duplicates");

		palette.put(Palette.FLOOR, Blocks.GOLD_BLOCK);

		assertTrue(palette.hasDuplicates(), "two slots holding the same block were not reported");
		assertFalse(palette.getDuplicates()
			.isEmpty(), "getDuplicates said nothing while hasDuplicates said yes");
	}
}
