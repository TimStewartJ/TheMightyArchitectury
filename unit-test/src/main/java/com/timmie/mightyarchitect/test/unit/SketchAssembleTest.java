package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.compose.Room;
import com.timmie.mightyarchitect.control.design.DesignLayer;
import com.timmie.mightyarchitect.control.design.Sketch;
import com.timmie.mightyarchitect.control.design.partials.Design;
import com.timmie.mightyarchitect.control.palette.BlockOrientation;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteBlockInfo;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Sketch.assemble} flattens the placed designs into the two palette layers the printer
 * consumes, then cleans and floors them. It is the last step before anything becomes blocks, and
 * it is pure map arithmetic over {@link BlockPos} - no world, no rendering.
 * <p>
 * The two rules it enforces are easy to get silently wrong: {@code CLEAR} marks a hole that must
 * be punched through <em>both</em> layers, and a room's interior must be emptied unless its layer
 * is an exterior one. Both are "something is missing" bugs, which is the kind a screenshot test
 * cannot see.
 */
@Bootstrapped
@DisplayName("Sketch.assemble")
class SketchAssembleTest {

	private static final int PRIMARY = 0;
	private static final int SECONDARY = 1;

	/**
	 * A design that contributes exactly the blocks it was handed. The real subclasses read their
	 * geometry out of NBT slices; none of that is what {@code assemble} does with the result.
	 */
	private static Design fixedDesign(Map<BlockPos, PaletteBlockInfo> fixed) {
		return new Design() {
			@Override
			public Design fromNBT(CompoundTag compound) {
				return this;
			}

			@Override
			public void getBlocks(DesignInstance instance, Map<BlockPos, PaletteBlockInfo> blocks) {
				blocks.putAll(fixed);
			}
		};
	}

	private static Design.DesignInstance instanceOf(Map<BlockPos, PaletteBlockInfo> fixed) {
		return fixedDesign(fixed).create(BlockPos.ZERO, 0, 1, 1);
	}

	private static Map<BlockPos, PaletteBlockInfo> blocks(Object... posThenPalette) {
		Map<BlockPos, PaletteBlockInfo> map = new HashMap<>();
		for (int i = 0; i < posThenPalette.length; i += 2)
			map.put((BlockPos) posThenPalette[i],
				new PaletteBlockInfo((Palette) posThenPalette[i + 1], BlockOrientation.NONE));
		return map;
	}

	@Test
	@DisplayName("returns the primary layer first and the secondary second")
	void returnsBothLayersInOrder() {
		Sketch sketch = new Sketch();
		sketch.primary.add(instanceOf(blocks(new BlockPos(1, 0, 0), Palette.INNER_PRIMARY)));
		sketch.secondary.add(instanceOf(blocks(new BlockPos(2, 0, 0), Palette.ROOF_PRIMARY)));

		Vector<Map<BlockPos, PaletteBlockInfo>> assembled = sketch.assemble();

		assertEquals(2, assembled.size());
		assertSame(Palette.INNER_PRIMARY, assembled.get(PRIMARY)
			.get(new BlockPos(1, 0, 0)).palette);
		assertSame(Palette.ROOF_PRIMARY, assembled.get(SECONDARY)
			.get(new BlockPos(2, 0, 0)).palette);
		assertFalse(assembled.get(PRIMARY)
			.containsKey(new BlockPos(2, 0, 0)), "the secondary layer leaked into the primary");
	}

	@Test
	@DisplayName("an empty sketch assembles to two empty layers, not to null")
	void emptySketchAssembles() {
		Vector<Map<BlockPos, PaletteBlockInfo>> assembled = new Sketch().assemble();

		assertEquals(2, assembled.size());
		assertTrue(assembled.get(PRIMARY)
			.isEmpty());
		assertTrue(assembled.get(SECONDARY)
			.isEmpty());
	}

	@Test
	@DisplayName("CLEAR punches a hole through both layers")
	void clearRemovesFromBothLayers() {
		BlockPos shared = new BlockPos(1, 0, 0);

		Sketch sketch = new Sketch();
		sketch.primary.add(instanceOf(blocks(shared, Palette.CLEAR)));
		sketch.secondary.add(instanceOf(blocks(shared, Palette.ROOF_PRIMARY)));

		Vector<Map<BlockPos, PaletteBlockInfo>> assembled = sketch.assemble();

		assertFalse(assembled.get(PRIMARY)
			.containsKey(shared), "CLEAR did not remove its own layer");
		assertFalse(assembled.get(SECONDARY)
			.containsKey(shared), "CLEAR did not punch through the other layer");
	}

	@Test
	@DisplayName("a room's interior is emptied out of both layers")
	void interiorIsCleared() {
		BlockPos inside = new BlockPos(2, 1, 2);
		BlockPos outside = new BlockPos(9, 1, 9);

		Sketch sketch = new Sketch();
		sketch.primary.add(instanceOf(blocks(inside, Palette.INNER_PRIMARY, outside, Palette.INNER_PRIMARY)));
		sketch.interior.add(room(new BlockPos(0, 0, 0), 6, 4, 6, DesignLayer.Regular));

		Vector<Map<BlockPos, PaletteBlockInfo>> assembled = sketch.assemble();

		assertFalse(assembled.get(PRIMARY)
			.containsKey(inside), "a block inside the room survived cleaning");
		assertTrue(assembled.get(PRIMARY)
			.containsKey(outside), "a block outside the room was cleaned away");
	}

	/** Exterior layers are the outside of the building, so their volume must not be hollowed. */
	@Test
	@DisplayName("an exterior room's interior is left alone")
	void exteriorRoomIsNotCleared() {
		BlockPos inside = new BlockPos(2, 1, 2);

		Sketch sketch = new Sketch();
		sketch.primary.add(instanceOf(blocks(inside, Palette.INNER_PRIMARY)));
		sketch.interior.add(room(new BlockPos(0, 0, 0), 6, 4, 6, DesignLayer.Open));

		assertTrue(sketch.assemble()
			.get(PRIMARY)
			.containsKey(inside), "an exterior room hollowed itself out");
	}

	@Test
	@DisplayName("every room gets a floor at its top layer")
	void roomsGetAFloor() {
		Sketch sketch = new Sketch();
		sketch.interior.add(room(new BlockPos(0, 0, 0), 3, 4, 2, DesignLayer.Regular));

		Map<BlockPos, PaletteBlockInfo> primary = sketch.assemble()
			.get(PRIMARY);

		assertEquals(3 * 2, primary.size(), "the floor did not cover the room's footprint exactly");
		for (int x = 0; x < 3; x++)
			for (int z = 0; z < 2; z++) {
				BlockPos pos = new BlockPos(x, 3, z);
				assertTrue(primary.containsKey(pos), () -> "no floor at " + pos);
				assertSame(Palette.FLOOR, primary.get(pos).palette);
				assertSame(BlockOrientation.TOP_UP, primary.get(pos).afterPosition,
					"the floor was not laid on top of its layer");
			}
	}

	@Test
	@DisplayName("a room on the secondary palette floors the secondary layer")
	void secondaryPaletteRoomFloorsTheSecondaryLayer() {
		Room room = room(new BlockPos(0, 0, 0), 2, 2, 2, DesignLayer.Regular);
		room.secondaryPalette = true;

		Sketch sketch = new Sketch();
		sketch.interior.add(room);

		Vector<Map<BlockPos, PaletteBlockInfo>> assembled = sketch.assemble();

		assertTrue(assembled.get(PRIMARY)
			.isEmpty(), "a secondary-palette room floored the primary layer");
		assertEquals(4, assembled.get(SECONDARY)
			.size());
	}

	/** A room with a one-tall room sitting directly on it is a floor for that room, not a ceiling. */
	@Test
	@DisplayName("a covering trim room suppresses the floor below it")
	void trimAboveSuppressesTheFloor() {
		Sketch sketch = new Sketch();
		sketch.interior.add(room(new BlockPos(0, 0, 0), 4, 3, 4, DesignLayer.Regular));
		sketch.interior.add(room(new BlockPos(0, 3, 0), 4, 1, 4, DesignLayer.Regular));

		Map<BlockPos, PaletteBlockInfo> primary = sketch.assemble()
			.get(PRIMARY);

		assertEquals(4 * 4, primary.size(),
			"the covered room floored itself as well as the trim above it");
		assertTrue(primary.containsKey(new BlockPos(0, 3, 0)));
	}

	@Test
	@DisplayName("a smaller overlapping room takes precedence over the floor above it")
	void overlappingSmallerRoomKeepsItsSpace() {
		Sketch sketch = new Sketch();
		sketch.interior.add(room(new BlockPos(0, 0, 0), 4, 2, 4, DesignLayer.Regular));
		// Occupies the same y as the big room's floor layer, and is smaller, so it wins there.
		sketch.interior.add(room(new BlockPos(0, 0, 0), 2, 2, 2, DesignLayer.Regular));

		Map<BlockPos, PaletteBlockInfo> primary = sketch.assemble()
			.get(PRIMARY);

		assertFalse(primary.isEmpty(), "no floor was laid at all");
		assertTrue(primary.containsKey(new BlockPos(3, 1, 3)),
			"the larger room lost the part of its floor nothing overlapped");
	}

	private static Room room(BlockPos origin, int width, int height, int length, DesignLayer layer) {
		Room room = new Room(origin, width, height, length);
		room.designLayer = layer;
		return room;
	}
}
