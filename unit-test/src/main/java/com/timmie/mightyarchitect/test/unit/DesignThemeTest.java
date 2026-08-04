package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.design.DesignLayer;
import com.timmie.mightyarchitect.control.design.DesignTheme;
import com.timmie.mightyarchitect.control.design.DesignType;
import com.timmie.mightyarchitect.control.design.ThemeStorage;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme deserialization and creation. A theme file can be written by hand or by a newer build, so
 * it can name a layer or type this build does not have; {@code valueOf} threw for that, and the
 * throw escaped the single try block around the whole theme-directory scan - one such file emptied
 * the entire theme list instead of costing that theme one layer.
 */
@Bootstrapped
@DisplayName("DesignTheme")
class DesignThemeTest {

	private static CompoundTag themeTag(List<String> layers, List<String> types) {
		ListTag layerTags = new ListTag();
		layers.forEach(name -> layerTags.add(StringTag.valueOf(name)));
		ListTag typeTags = new ListTag();
		types.forEach(name -> typeTags.add(StringTag.valueOf(name)));

		CompoundTag tag = new CompoundTag();
		tag.putString("Name", "Unit Test Theme");
		tag.putString("Designer", "unit-test");
		tag.put("Layers", layerTags);
		tag.put("Types", typeTags);
		return tag;
	}

	@Nested
	@DisplayName("fromNBT")
	class FromNbt {

		@Test
		@DisplayName("reads the name, designer, layers and types")
		void readsTheBasics() {
			DesignTheme theme = DesignTheme.fromNBT(themeTag(List.of("Regular", "Roofing"), List.of("WALL", "ROOF")));

			assertEquals("Unit Test Theme", theme.getDisplayName());
			assertEquals("unit-test", theme.getDesigner());
			assertEquals(List.of(DesignLayer.Regular, DesignLayer.Roofing), theme.getLayers());
			assertEquals(List.of(DesignType.WALL, DesignType.ROOF), theme.getTypes());
		}

		@Test
		@DisplayName("skips an unknown layer instead of throwing")
		void skipsUnknownLayer() {
			DesignTheme theme = DesignTheme.fromNBT(
				themeTag(List.of("Regular", "NotALayerThisBuildKnows"), List.of()));

			assertEquals(List.of(DesignLayer.Regular), theme.getLayers());
		}

		@Test
		@DisplayName("skips an unknown type instead of throwing")
		void skipsUnknownType() {
			DesignTheme theme = DesignTheme.fromNBT(
				themeTag(List.of(), List.of("WALL", "PORTCULLIS_FROM_THE_FUTURE")));

			assertEquals(List.of(DesignType.WALL), theme.getTypes());
		}

		/** The regression itself: one bad name must cost one entry, not the whole list. */
		@Test
		@DisplayName("one unknown name does not empty the list")
		void oneUnknownNameDoesNotEmptyTheList() {
			DesignTheme theme = DesignTheme.fromNBT(themeTag(
				List.of("Foundation", "TypoLayer", "Regular", "Roofing"),
				List.of("WALL", "TypoType", "CORNER", "ROOF")));

			assertEquals(List.of(DesignLayer.Foundation, DesignLayer.Regular, DesignLayer.Roofing),
				theme.getLayers());
			assertEquals(List.of(DesignType.WALL, DesignType.CORNER, DesignType.ROOF), theme.getTypes());
		}

		@Test
		@DisplayName("a null tag yields null rather than throwing")
		void nullTagYieldsNull() {
			assertNull(DesignTheme.fromNBT(null));
		}

		@Test
		@DisplayName("an empty tag yields an empty but usable theme")
		void emptyTagYieldsAnEmptyTheme() {
			DesignTheme theme = DesignTheme.fromNBT(new CompoundTag());

			assertTrue(theme.getLayers()
				.isEmpty());
			assertTrue(theme.getTypes()
				.isEmpty());
			assertEquals(10, theme.getMaxFloorHeight(), "an absent room height should keep the default");
		}

		@Test
		@DisplayName("room layers exclude roofing")
		void roomLayersExcludeRoofing() {
			DesignTheme theme = DesignTheme.fromNBT(
				themeTag(List.of("Regular", "Roofing", "Foundation"), List.of()));

			assertEquals(List.of(DesignLayer.Regular, DesignLayer.Foundation), theme.getRoomLayers());
		}

		@Test
		@DisplayName("survives a round trip through asTagCompound")
		void survivesARoundTrip() {
			DesignTheme original = DesignTheme.fromNBT(
				themeTag(List.of("Regular", "Roofing"), List.of("WALL", "ROOF")));
			original.setMaxFloorHeight(7);

			DesignTheme roundTrip = DesignTheme.fromNBT(original.asTagCompound());

			assertEquals(original.getDisplayName(), roundTrip.getDisplayName());
			assertEquals(original.getDesigner(), roundTrip.getDesigner());
			assertEquals(original.getLayers(), roundTrip.getLayers());
			assertEquals(original.getTypes(), roundTrip.getTypes());
			assertEquals(7, roundTrip.getMaxFloorHeight());
		}
	}

	@Nested
	@DisplayName("ThemeStorage.createTheme")
	class CreateTheme {

		@Test
		@DisplayName("gives every new theme its own two palettes")
		void newThemesGetTheirOwnPalettes() {
			PaletteDefinition shared = PaletteDefinition.defaultPalette();
			DesignTheme created = ThemeStorage.createTheme("Unit Test Theme", "unit-test");

			assertNotSame(shared, created.getDefaultPalette(),
				"a new theme aliased the shared default palette");
			assertNotSame(shared, created.getDefaultSecondaryPalette(),
				"a new theme aliased the shared default palette as its secondary");
			assertNotSame(created.getDefaultPalette(), created.getDefaultSecondaryPalette(),
				"a new theme's two palettes are the same object");
		}

		@Test
		@DisplayName("editing a new theme's palette leaves the default alone")
		void editingLeavesTheDefaultAlone() {
			PaletteDefinition shared = PaletteDefinition.defaultPalette();
			BlockState before = shared.get(Palette.ROOF_PRIMARY);

			DesignTheme created = ThemeStorage.createTheme("Unit Test Theme", "unit-test");
			created.getDefaultPalette()
				.put(Palette.ROOF_PRIMARY, Blocks.GOLD_BLOCK);

			assertEquals(before, shared.get(Palette.ROOF_PRIMARY),
				"editing a theme palette rewrote the shared default");
			assertEquals(before, created.getDefaultSecondaryPalette()
				.get(Palette.ROOF_PRIMARY), "editing the primary palette reached the secondary");
		}

		@Test
		@DisplayName("the file path is one sanitized folder name")
		void filePathIsOneSanitizedFolderName() {
			DesignTheme created = ThemeStorage.createTheme("../../Evil Theme", "unit-test");
			String path = created.getFilePath();

			assertFalse(path.isEmpty());
			assertFalse(path.contains("/"), () -> "theme folder kept a slash: " + path);
			assertFalse(path.contains("\\"), () -> "theme folder kept a backslash: " + path);
			assertFalse(path.contains(".."), () -> "theme folder kept a parent reference: " + path);
		}

		@Test
		@DisplayName("a name that sanitizes away still gets a usable folder")
		void unusableNameStillGetsAFolder() {
			assertEquals("my_theme", ThemeStorage.createTheme("///", "unit-test")
				.getFilePath());
			assertEquals("my_theme", ThemeStorage.createTheme("", "unit-test")
				.getFilePath(), "an empty name should become the default theme name");
		}

		@Test
		@DisplayName("a new theme is marked imported and starts with the standard layers and types")
		void newThemeShape() {
			DesignTheme created = ThemeStorage.createTheme("Unit Test Theme", "unit-test");

			assertTrue(created.isImported());
			assertEquals("unit-test", created.getDesigner());
			assertEquals(List.of(DesignLayer.Regular, DesignLayer.Roofing, DesignLayer.Foundation),
				created.getLayers());
			assertEquals(List.of(DesignType.WALL, DesignType.CORNER, DesignType.ROOF, DesignType.FACADE,
				DesignType.FLAT_ROOF), created.getTypes());
		}
	}
}
