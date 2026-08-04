package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.design.DesignLayer;
import com.timmie.mightyarchitect.control.design.DesignTheme;
import com.timmie.mightyarchitect.control.design.DesignType;
import com.timmie.mightyarchitect.control.design.ThemeStorage;
import com.timmie.mightyarchitect.control.design.partials.DesignData;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.storage.ArchitectResources;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The five built-in themes and twenty-one built-in palettes moved out of the jar root and under
 * {@code assets/mightyarchitect/}, so a resource pack can override them and no other mod's jar can
 * shadow them by declaring a top-level {@code themes/} of its own.
 * <p>
 * These read the real shipped files rather than fixtures, which makes them the assertion that the
 * move landed: a stale path, a file left behind, or a codec that cannot read the shape those 695
 * files are actually written in all fail here rather than on somebody's title screen. There is no
 * client in this suite, so this also covers the classpath fallback the resource manager is not
 * available for.
 */
@Bootstrapped
@DisplayName("Built-in content")
class BuiltInResourcesTest {

	@Test
	@DisplayName("every built-in theme loads, with its palettes")
	void everyBuiltInThemeLoads() {
		for (ThemeStorage.IncludedThemes which : ThemeStorage.IncludedThemes.values()) {
			DesignTheme theme = ThemeStorage.getIncludedTheme(which);

			assertNotNull(theme, which + " did not load");
			assertFalse(theme.getDisplayName()
				.isEmpty(), which + " loaded with no name");
			assertFalse(theme.getLayers()
				.isEmpty(), which + " loaded with no layers");
			assertFalse(theme.getTypes()
				.isEmpty(), which + " loaded with no types");
			assertNotNull(theme.getDefaultPalette(), which + " loaded with no palette");
			assertNotNull(theme.getDefaultSecondaryPalette(), which + " loaded with no secondary palette");
			assertEquals(which.themeFolder, theme.getFilePath());
			assertFalse(theme.isImported(), which + " was treated as a user theme");
		}
	}

	@Test
	@DisplayName("the shipped theme metadata still reads")
	void shippedThemeMetadataReads() {
		DesignTheme medieval = JsonStorage.readBuiltIn("themes/medieval/theme.json", DesignTheme.CODEC)
			.orElseThrow();

		assertEquals("Medieval", medieval.getDisplayName());
		assertEquals("simibubi", medieval.getDesigner());
		assertTrue(medieval.getLayers()
			.contains(DesignLayer.Regular));
		assertTrue(medieval.getTypes()
			.contains(DesignType.WALL));
		// Not written by any shipped theme, so this is the default the codec supplies.
		assertEquals(10, medieval.getMaxFloorHeight());
	}

	@Test
	@DisplayName("all twenty-one shipped palettes are found and read")
	void everyShippedPaletteLoads() {
		List<String> paths = ArchitectResources.list("palettes", index -> "p" + index + ".json");

		assertEquals(21, paths.size(), "the palette folder did not come across intact");
		for (String path : paths) {
			PaletteDefinition palette = JsonStorage.readBuiltIn(path, PaletteDefinition.CODEC)
				.orElseThrow(() -> new AssertionError(path + " did not read"));
			assertFalse(palette.getName()
				.isEmpty(), path + " read with no name");
		}
	}

	@Test
	@DisplayName("a shipped design reads, with its geometry")
	void aShippedDesignReads() {
		DesignData design =
			JsonStorage.readBuiltIn("themes/medieval/foundation/corner/design.json", DesignData.CODEC)
				.orElseThrow();

		assertEquals(2, design.size()
			.getX());
		assertEquals(5, design.size()
			.getY());
		assertEquals(2, design.size()
			.getZ());
		assertEquals(5, design.layers()
			.size(), "the design lost its slices");
		assertEquals("GD,DA", design.layers()
			.get(0)
			.blocks());
	}

	@Test
	@DisplayName("a built-in theme's designs are found and instantiated")
	void builtInDesignsAreFound() {
		DesignTheme medieval = ThemeStorage.getIncludedTheme(ThemeStorage.IncludedThemes.Medieval);

		assertFalse(medieval.getDesigns(DesignLayer.Regular, DesignType.WALL)
			.isEmpty(), "the medieval theme found no regular walls");
		assertFalse(medieval.getDesigns(DesignLayer.Roofing, DesignType.ROOF)
			.isEmpty(), "the medieval theme found no roofs");
	}

	@Test
	@DisplayName("nothing is left at the jar root any more")
	void nothingIsLeftAtTheJarRoot() {
		assertNull(BuiltInResourcesTest.class.getClassLoader()
			.getResource("themes/medieval/theme.json"),
			"a theme is still at the jar root, where another mod's jar can shadow it");
		assertNull(BuiltInResourcesTest.class.getClassLoader()
			.getResource("palettes/p0.json"),
			"a palette is still at the jar root, where another mod's jar can shadow it");
	}

	@Test
	@DisplayName("built-in paths are listed in natural order, so p2 comes before p10")
	void listedInNaturalOrder() {
		assertTrue(ArchitectResources.compareNaturally("palettes/p2.json", "palettes/p10.json") < 0);
		assertTrue(ArchitectResources.compareNaturally("palettes/p10.json", "palettes/p2.json") > 0);
		assertEquals(0, ArchitectResources.compareNaturally("palettes/p7.json", "palettes/p7.json"));
		assertTrue(ArchitectResources.compareNaturally("design.json", "design_1.json") < 0);
	}

	/**
	 * An imported theme's folder is named whatever the user called it on disk, and a resource
	 * location only accepts {@code [a-z0-9/._-]}. Asking whether such a path has a built-in file
	 * has to answer "no" rather than throw: the design exporter asks exactly that before choosing
	 * a filename, on the client thread, inside a right-click - so an exception there is a crash
	 * report on the first export into any theme somebody downloaded.
	 */
	@Test
	@DisplayName("a theme folder a resource location cannot name is answered, not thrown at")
	void unaddressablePathsAreAnswered() {
		assertTrue(ArchitectResources.isAddressable("themes/medieval/regular/wall/design.json"));
		assertFalse(ArchitectResources.isAddressable("themes/Nordic Village/regular/wall/design.json"),
			"a folder name with a space and capitals was treated as a valid resource path");
		assertFalse(ArchitectResources.isAddressable("themes/Gothic/regular/wall/design.json"));

		for (String hostile : new String[] { "themes/Nordic Village/regular/wall/design.json",
			"themes/Gothic/regular/wall/design.json", "themes/thème/regular/wall/design.json" }) {
			assertFalse(ArchitectResources.exists(hostile), hostile + " reported a built-in file");
			assertFalse(ArchitectResources.open(hostile)
				.isPresent(), hostile + " opened something");
		}
	}
}
