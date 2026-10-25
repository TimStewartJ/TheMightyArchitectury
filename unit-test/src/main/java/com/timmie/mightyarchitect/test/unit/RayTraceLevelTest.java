package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.foundation.utility.RaycastHelper;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A two-corner shape is drawn flat at its first corner's height, so its second corner is aimed on
 * that level rather than on the terrain under the crosshair.
 */
@Bootstrapped
@DisplayName("Aiming on a level")
class RayTraceLevelTest {

	@Test
	@DisplayName("a ray looking down lands where it crosses the level, whatever lies below")
	void obliqueRayCrossesLevel() {
		Vec3 eyes = new Vec3(0.5, 40, 0.5);
		Vec3 down45 = new Vec3(1, -1, 0);
		assertEquals(new BlockPos(30, 10, 0), RaycastHelper.rayTraceLevel(eyes, down45, 10, 75));
	}

	@Test
	@DisplayName("a ray from below reaches a level above it")
	void rayFromBelow() {
		assertEquals(new BlockPos(-4, 64, 7), RaycastHelper.rayTraceLevel(new Vec3(-2.5, 60, 7.5),
			new Vec3(-1, 4, 0), 64, 75));
	}

	@Test
	@DisplayName("no landing for a ray parallel to the level, pointing away, or too far")
	void misses() {
		Vec3 eyes = new Vec3(0, 40, 0);
		assertNull(RaycastHelper.rayTraceLevel(eyes, new Vec3(1, 0, 0), 10, 75));
		assertNull(RaycastHelper.rayTraceLevel(eyes, new Vec3(0, 1, 0), 10, 75));
		assertNull(RaycastHelper.rayTraceLevel(eyes, new Vec3(1, -0.1, 0), 10, 75));
	}
}
