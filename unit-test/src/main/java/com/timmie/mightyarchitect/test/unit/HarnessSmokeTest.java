package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.compose.Cuboid;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Proves the harness itself works before anything relies on it: the mod's classes load, and
 * Minecraft's registries are usable.
 */
@Bootstrapped
@DisplayName("harness")
class HarnessSmokeTest {

	@Test
	@DisplayName("Minecraft's block registry is bootstrapped")
	void registriesAreUsable() {
		assertNotNull(Blocks.STONE.defaultBlockState(), "Blocks.STONE has no default state");
	}

	@Test
	@DisplayName("the mod's own classes load")
	void modClassesLoad() {
		assertEquals(new BlockPos(1, 2, 3), new Cuboid(new BlockPos(1, 2, 3), 1, 1, 1).getOrigin());
	}
}
