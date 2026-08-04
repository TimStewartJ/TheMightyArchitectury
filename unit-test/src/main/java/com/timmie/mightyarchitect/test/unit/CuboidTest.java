package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.compose.Cuboid;
import com.timmie.mightyarchitect.control.compose.Room;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Cuboid} is the composer's geometry primitive - every room, stack and selection is one.
 * It is plain integer arithmetic over {@link BlockPos}, so all of it belongs here.
 */
@Bootstrapped
@DisplayName("Cuboid")
class CuboidTest {

	private static final BlockPos ORIGIN = new BlockPos(4, 5, 6);

	@Nested
	@DisplayName("normalization")
	class Normalization {

		@Test
		@DisplayName("a positive size is kept as given")
		void positiveSize() {
			Cuboid cuboid = new Cuboid(ORIGIN, 1, 2, 3);

			assertEquals(ORIGIN, cuboid.getOrigin());
			assertEquals(new BlockPos(1, 2, 3), cuboid.getSize());
		}

		/** Dragging a selection backwards produces negative extents; the origin has to follow. */
		@Test
		@DisplayName("a negative size moves the origin and is stored positive")
		void negativeSizeMovesTheOrigin() {
			Cuboid dragged = new Cuboid(ORIGIN, -2, -3, -4);

			assertEquals(new BlockPos(2, 2, 2), dragged.getOrigin());
			assertEquals(new BlockPos(2, 3, 4), dragged.getSize());
		}

		@Test
		@DisplayName("a mixed-sign size normalizes per axis")
		void mixedSignSize() {
			Cuboid dragged = new Cuboid(ORIGIN, -2, 3, -4);

			assertEquals(new BlockPos(2, 5, 2), dragged.getOrigin());
			assertEquals(new BlockPos(2, 3, 4), dragged.getSize());
		}

		@Test
		@DisplayName("the BlockPos constructor agrees with the int one")
		void blockPosConstructorAgrees() {
			assertEquals(new Cuboid(ORIGIN, 1, 2, 3), new Cuboid(ORIGIN, new BlockPos(1, 2, 3)));
		}
	}

	@Nested
	@DisplayName("equals/hashCode contract")
	class EqualsHashCode {

		@Test
		@DisplayName("equal cuboids hash equally")
		void equalCuboidsHashEqually() {
			Cuboid one = new Cuboid(ORIGIN, 1, 2, 3);
			Cuboid other = new Cuboid(ORIGIN, 1, 2, 3);

			assertEquals(one, other);
			assertEquals(one.hashCode(), other.hashCode(), "equal cuboids hashed differently");
		}

		/**
		 * The bug this guards: {@code equals} without {@code hashCode} makes every hash-based
		 * lookup miss, which is how the design cache silently stopped caching.
		 */
		@Test
		@DisplayName("an equal cuboid finds an entry keyed by another")
		void worksAsAHashMapKey() {
			Map<Cuboid, String> byBounds = new HashMap<>();
			byBounds.put(new Cuboid(ORIGIN, 1, 2, 3), "design");

			assertEquals("design", byBounds.get(new Cuboid(ORIGIN, 1, 2, 3)),
				"an equal key did not find its entry");
			assertEquals(1, new HashSet<>(java.util.List.of(new Cuboid(ORIGIN, 1, 2, 3),
				new Cuboid(ORIGIN, 1, 2, 3))).size(), "equal cuboids did not collapse in a Set");
		}

		@Test
		@DisplayName("each dimension participates in the hash")
		void everyDimensionParticipates() {
			Cuboid base = new Cuboid(BlockPos.ZERO, 1, 2, 3);
			Set<Integer> hashes = new HashSet<>();
			hashes.add(base.hashCode());
			hashes.add(new Cuboid(new BlockPos(1, 0, 0), 1, 2, 3).hashCode());
			hashes.add(new Cuboid(new BlockPos(0, 1, 0), 1, 2, 3).hashCode());
			hashes.add(new Cuboid(new BlockPos(0, 0, 1), 1, 2, 3).hashCode());
			hashes.add(new Cuboid(BlockPos.ZERO, 9, 2, 3).hashCode());
			hashes.add(new Cuboid(BlockPos.ZERO, 1, 9, 3).hashCode());
			hashes.add(new Cuboid(BlockPos.ZERO, 1, 2, 9).hashCode());

			assertEquals(7, hashes.size(), "two cuboids differing in one dimension shared a hash");
		}

		@Test
		@DisplayName("differing cuboids are not equal")
		void differingCuboidsAreNotEqual() {
			Cuboid base = new Cuboid(ORIGIN, 1, 2, 3);

			assertFalse(base.equals(new Cuboid(ORIGIN, 1, 2, 4)));
			assertFalse(base.equals(new Cuboid(BlockPos.ZERO, 1, 2, 3)));
			assertFalse(base.equals(null));
			assertFalse(base.equals("not a cuboid"));
		}

		/**
		 * A Cuboid is mutable and moved in place by the planner tools, so its hash moves with it -
		 * which is why anything keying a map on one is keyed on identity. Pinning that here so the
		 * constraint is visible rather than folklore.
		 */
		@Test
		@DisplayName("moving one changes its hash, so map keys are identity keys")
		void mutationChangesTheHash() {
			Cuboid cuboid = new Cuboid(ORIGIN, 1, 2, 3);
			int before = cuboid.hashCode();
			cuboid.move(1, 0, 0);

			assertFalse(before == cuboid.hashCode(), "moving a cuboid left its hash unchanged");
		}
	}

	@Nested
	@DisplayName("containment and intersection")
	class Geometry {

		@Test
		@DisplayName("contains is inclusive at the origin and exclusive at the far corner")
		void containsIsHalfOpen() {
			Cuboid cuboid = new Cuboid(BlockPos.ZERO, 2, 2, 2);

			assertTrue(cuboid.contains(BlockPos.ZERO));
			assertTrue(cuboid.contains(new BlockPos(1, 1, 1)));
			assertFalse(cuboid.contains(new BlockPos(2, 1, 1)));
			assertFalse(cuboid.contains(new BlockPos(1, 2, 1)));
			assertFalse(cuboid.contains(new BlockPos(1, 1, 2)));
			assertFalse(cuboid.contains(new BlockPos(-1, 0, 0)));
		}

		/** Intersection is deliberately horizontal only - stacks are allowed to sit on each other. */
		@Test
		@DisplayName("intersects ignores height")
		void intersectsIgnoresHeight() {
			Cuboid ground = new Cuboid(BlockPos.ZERO, 4, 4, 4);
			Cuboid above = new Cuboid(new BlockPos(0, 100, 0), 4, 4, 4);
			Cuboid beside = new Cuboid(new BlockPos(4, 0, 0), 4, 4, 4);
			Cuboid overlapping = new Cuboid(new BlockPos(3, 0, 3), 4, 4, 4);

			assertTrue(ground.intersects(above), "vertically separated cuboids should still intersect");
			assertFalse(ground.intersects(beside), "cuboids sharing only a face should not intersect");
			assertTrue(ground.intersects(overlapping));
			assertTrue(overlapping.intersects(ground), "intersects is not symmetric");
		}

		@Test
		@DisplayName("getCenter offsets by half the size")
		void centre() {
			assertEquals(new BlockPos(1, 2, 3), new Cuboid(BlockPos.ZERO, 2, 4, 6).getCenter());
			assertEquals(new BlockPos(5, 7, 9), new Cuboid(ORIGIN, 3, 5, 7).getCenter());
		}

		@Test
		@DisplayName("centerHorizontallyOn keeps the y of the target")
		void centerHorizontally() {
			Cuboid cuboid = new Cuboid(BlockPos.ZERO, 4, 3, 6);
			cuboid.centerHorizontallyOn(new BlockPos(10, 20, 30));

			assertEquals(new BlockPos(8, 20, 27), cuboid.getOrigin());
			assertEquals(new BlockPos(4, 3, 6), cuboid.getSize(), "centering changed the size");
		}

		@Test
		@DisplayName("move translates without resizing")
		void move() {
			Cuboid cuboid = new Cuboid(ORIGIN, 1, 2, 3);
			cuboid.move(-1, 2, -3);

			assertEquals(new BlockPos(3, 7, 3), cuboid.getOrigin());
			assertEquals(new BlockPos(1, 2, 3), cuboid.getSize());
		}

		@Test
		@DisplayName("clone is an equal but separate cuboid")
		void cloneIsIndependent() {
			Cuboid cuboid = new Cuboid(ORIGIN, 1, 2, 3);
			Cuboid clone = cuboid.clone();

			assertEquals(cuboid, clone);
			assertNotSame(cuboid, clone);

			clone.move(100, 0, 0);
			assertEquals(ORIGIN, cuboid.getOrigin(), "moving the clone moved the original");
		}
	}

	@Nested
	@DisplayName("moveToAttach")
	class Attachment {

		private Room neighbour() {
			return new Room(new BlockPos(0, 0, 0), 6, 4, 8);
		}

		@Test
		@DisplayName("WEST puts this cuboid on the neighbour's far x face")
		void west() {
			Cuboid attached = new Cuboid(BlockPos.ZERO, 2, 2, 2);
			attached.moveToAttach(neighbour(), Direction.WEST, 0);

			assertEquals(6, attached.getOrigin()
				.getX());
			assertEquals(3, attached.getOrigin()
				.getZ(), "attaching sideways should centre on the neighbour's z");
		}

		@Test
		@DisplayName("EAST puts this cuboid before the neighbour's origin")
		void east() {
			Cuboid attached = new Cuboid(BlockPos.ZERO, 2, 2, 2);
			attached.moveToAttach(neighbour(), Direction.EAST, 0);

			assertEquals(-2, attached.getOrigin()
				.getX());
		}

		@Test
		@DisplayName("NORTH and SOUTH attach along z and centre along x")
		void northSouth() {
			Cuboid north = new Cuboid(BlockPos.ZERO, 2, 2, 2);
			north.moveToAttach(neighbour(), Direction.NORTH, 0);
			assertEquals(-2, north.getOrigin()
				.getZ());
			assertEquals(2, north.getOrigin()
				.getX(), "attaching along z should centre on the neighbour's x");

			Cuboid south = new Cuboid(BlockPos.ZERO, 2, 2, 2);
			south.moveToAttach(neighbour(), Direction.SOUTH, 0);
			assertEquals(8, south.getOrigin()
				.getZ());
		}

		@Test
		@DisplayName("UP and DOWN stack vertically and centre on both horizontal axes")
		void upDown() {
			Cuboid up = new Cuboid(BlockPos.ZERO, 2, 2, 2);
			up.moveToAttach(neighbour(), Direction.UP, 0);
			assertEquals(new BlockPos(2, 4, 3), up.getOrigin());

			Cuboid down = new Cuboid(BlockPos.ZERO, 2, 2, 2);
			down.moveToAttach(neighbour(), Direction.DOWN, 0);
			assertEquals(new BlockPos(2, -2, 3), down.getOrigin());
		}

		@Test
		@DisplayName("the shift argument slides the centred axes")
		void shiftSlidesTheCentredAxes() {
			Cuboid unshifted = new Cuboid(BlockPos.ZERO, 2, 2, 2);
			unshifted.moveToAttach(neighbour(), Direction.WEST, 0);

			Cuboid shifted = new Cuboid(BlockPos.ZERO, 2, 2, 2);
			shifted.moveToAttach(neighbour(), Direction.WEST, 3);

			assertEquals(unshifted.getOrigin()
				.getZ() + 3,
				shifted.getOrigin()
					.getZ());
			assertEquals(unshifted.getOrigin()
				.getX(),
				shifted.getOrigin()
					.getX(), "the attached axis should not be shifted");
		}
	}

	@Nested
	@DisplayName("Room")
	class RoomBehaviour {

		@Test
		@DisplayName("getInterior insets by one on x and z and keeps the height")
		void interiorIsInsetByOne() {
			Room room = new Room(new BlockPos(0, 0, 0), 6, 4, 8);
			Room interior = room.getInterior();

			assertEquals(new BlockPos(1, 0, 1), interior.getOrigin());
			assertEquals(new BlockPos(4, 4, 6), interior.getSize());
		}

		@Test
		@DisplayName("stack sits on top and clears the roof of the room below")
		void stackSitsOnTop() {
			// Square on purpose: quadFacadeRoof is true only for a square footprint, so a
			// rectangular fixture would assert that stack() cleared a flag that was never set.
			Room room = new Room(new BlockPos(0, 0, 0), 6, 4, 6);
			Room stacked = room.stack(false);

			assertEquals(4, stacked.getOrigin()
				.getY());
			assertEquals(com.timmie.mightyarchitect.control.design.DesignType.NONE, room.roofType,
				"stacking should clear the lower room's roof");
			assertFalse(room.quadFacadeRoof, "stacking should clear the lower room's quad facade roof");
		}

		@Test
		@DisplayName("a non-exact stack is at least four blocks tall")
		void stackHasAMinimumHeight() {
			Room shallow = new Room(new BlockPos(0, 0, 0), 6, 2, 8);

			assertEquals(4, shallow.stack(false).height);
			assertEquals(2, new Room(new BlockPos(0, 0, 0), 6, 2, 8).stack(true).height);
		}

		@Test
		@DisplayName("orientation follows the longer horizontal axis")
		void orientation() {
			assertSame(Direction.Axis.X, new Room(BlockPos.ZERO, 8, 4, 4).getOrientation());
			assertSame(Direction.Axis.Z, new Room(BlockPos.ZERO, 4, 4, 8).getOrientation());
			assertSame(Direction.Axis.Z, new Room(BlockPos.ZERO, 4, 4, 4).getOrientation(),
				"a square room should not flip orientation between builds");
		}
	}
}
