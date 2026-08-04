package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The default palette is a shared mutable singleton every other palette starts from, so anything
 * that stores it has to clone first - otherwise recolouring one theme silently recolours the
 * starting palette of every theme made afterwards. That aliasing bug shipped for years.
 * <p>
 * Also covers the fix that let palettes load outside a world: reading blocks through
 * {@code Minecraft.getInstance().level} NPE'd on the title screen, so it now goes through
 * {@code BuiltInRegistries} - which is exactly why these assertions can run here at all.
 */
@Bootstrapped
@DisplayName("PaletteDefinition")
class PaletteDefinitionTest {

	@Test
	@DisplayName("the default palette is a single shared instance")
	void defaultPaletteIsShared() {
		assertSame(PaletteDefinition.defaultPalette(), PaletteDefinition.defaultPalette());
	}

	@Test
	@DisplayName("cloning yields a separate palette")
	void cloneIsSeparate() {
		PaletteDefinition shared = PaletteDefinition.defaultPalette();
		PaletteDefinition clone = shared.clone();

		assertNotSame(shared, clone);
		assertNotSame(shared.getDefinition(), clone.getDefinition(), "the clone aliased the same map");
	}

	@Test
	@DisplayName("editing a clone leaves the shared default alone")
	void editingACloneLeavesTheDefaultAlone() {
		PaletteDefinition shared = PaletteDefinition.defaultPalette();
		BlockState before = shared.get(Palette.ROOF_PRIMARY);

		PaletteDefinition clone = shared.clone();
		clone.put(Palette.ROOF_PRIMARY, Blocks.GOLD_BLOCK);

		assertEquals(before, shared.get(Palette.ROOF_PRIMARY),
			"editing a cloned palette rewrote the shared default");
		assertEquals(Blocks.GOLD_BLOCK.defaultBlockState(), clone.get(Palette.ROOF_PRIMARY),
			"the edit did not reach the clone");
	}

	@Test
	@DisplayName("two clones of the default do not alias each other")
	void clonesDoNotAliasEachOther() {
		PaletteDefinition primary = PaletteDefinition.defaultPalette()
			.clone();
		PaletteDefinition secondary = PaletteDefinition.defaultPalette()
			.clone();

		primary.put(Palette.FLOOR, Blocks.GOLD_BLOCK);

		assertNotSame(primary, secondary);
		assertEquals(PaletteDefinition.defaultPalette()
			.get(Palette.FLOOR), secondary.get(Palette.FLOOR),
			"editing one clone reached another");
	}

	@Test
	@DisplayName("a clone inherits every entry it does not override")
	void cloneInheritsEveryEntry() {
		PaletteDefinition clone = PaletteDefinition.defaultPalette()
			.clone();

		for (Palette key : Palette.values())
			assertEquals(PaletteDefinition.defaultPalette()
				.get(key), clone.get(key), "clone lost the entry for " + key);
	}

	@Test
	@DisplayName("an unset key falls back to air rather than null")
	void unsetKeyFallsBackToAir() {
		PaletteDefinition empty = new PaletteDefinition("empty");

		assertEquals(Blocks.AIR.defaultBlockState(), empty.get(Palette.ROOF_PRIMARY));
	}

	@Test
	@DisplayName("CLEAR is always the barrier, whatever the source said")
	void clearIsAlwaysBarrier() {
		PaletteDefinition clone = PaletteDefinition.defaultPalette()
			.clone();
		clone.put(Palette.CLEAR, Blocks.GOLD_BLOCK);

		assertEquals(Blocks.BARRIER.defaultBlockState(), clone.clone()
			.get(Palette.CLEAR), "a cloned palette kept a non-barrier CLEAR");
		assertEquals(Blocks.BARRIER.defaultBlockState(),
			PaletteDefinition.fromNBT(clone.writeToNBT(new CompoundTag()))
				.get(Palette.CLEAR),
			"a deserialized palette kept a non-barrier CLEAR");
	}

	@Test
	@DisplayName("survives an NBT round trip with its name and every entry")
	void survivesAnNbtRoundTrip() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.setName("Unit Test Palette");
		palette.put(Palette.ROOF_PRIMARY, Blocks.GOLD_BLOCK);

		PaletteDefinition roundTrip = PaletteDefinition.fromNBT(palette.writeToNBT(new CompoundTag()));

		assertEquals("Unit Test Palette", roundTrip.getName());
		for (Palette key : Palette.values())
			assertEquals(palette.get(key), roundTrip.get(key), "the round trip lost the entry for " + key);
	}

	/**
	 * Palette loading used to require being in a world, which is why it had to be deferred until
	 * joining one. Nothing here has a world, so this passing at all is the assertion.
	 */
	@Test
	@DisplayName("loads with no world, and tolerates missing or empty NBT")
	void loadsWithNoWorld() {
		assertNotNull(PaletteDefinition.fromNBT(null), "a null tag should still give a usable palette");
		assertNotNull(PaletteDefinition.fromNBT(new CompoundTag()),
			"an empty tag should still give a usable palette");
		assertEquals(PaletteDefinition.defaultPalette()
			.get(Palette.ROOF_PRIMARY),
			PaletteDefinition.fromNBT(null)
				.get(Palette.ROOF_PRIMARY),
			"a palette read from nothing should fall back to the default entries");
	}

	@Test
	@DisplayName("scan finds the key a blockstate is bound to")
	void scanFindsTheKey() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.put(Palette.ROOF_PRIMARY, Blocks.GOLD_BLOCK);

		assertSame(Palette.ROOF_PRIMARY, palette.scan(Blocks.GOLD_BLOCK.defaultBlockState()));
		assertNull(palette.scan(Blocks.AIR.defaultBlockState()), "air is not a palette entry");
	}
}
