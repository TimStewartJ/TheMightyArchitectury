package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.design.DesignTheme;
import com.timmie.mightyarchitect.control.design.ThemeStorage;
import com.timmie.mightyarchitect.control.storage.ArchitectResources;
import com.timmie.mightyarchitect.control.storage.ArchitectStorage;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A resource pack can now ship a whole theme, not only override one of the five in the jar.
 * <p>
 * The theme list used to be a walk over a five-constant enum, so a pack could replace
 * {@code medieval}'s files but had no way to add a sixth theme - the mod would never look for it.
 * It is discovered from the resource stack now, which is what moving the content into
 * {@code assets/mightyarchitect/} was for.
 * <p>
 * These drive {@link ArchitectResources.ResourceIndex}, the mod's own stand-in for the stack,
 * rather than a real {@code ResourceManager}: this module is not Stonecutter-processed, and
 * {@code ResourceManager}'s signatures moved from {@code ResourceLocation} to {@code Identifier} at
 * 1.21.11, so naming it here would not compile on every node. What that costs in fidelity the
 * client matrix covers - it asserts {@code ThemeStorage.getIncluded()} on a real client, through
 * the real manager, on all 25 targets. What it buys is the discovery rules themselves, including
 * the ordering, which is load-bearing: the composer binds themes to number keys by list position,
 * so a pack adding a theme must not renumber the five people already know.
 */
@Bootstrapped
@DisplayName("Resource-pack themes")
class PackThemeDiscoveryTest {

	/** The five the mod ships, in the order the menu has always listed them. */
	private static final List<String> SHIPPED =
		List.of("medieval", "fallback_theme", "modern", "town_house", "cattingham_palace");

	@AfterEach
	void restoreRealResources() {
		ArchitectResources.setIndexForTesting(null);
		ArchitectStorage.reset();
	}

	private static String themeJson(String name, String heights) {
		return "{\"Name\":\"" + name + "\",\"Designer\":\"a pack\","
			+ "\"Layers\":[\"Regular\"],\"Types\":[\"WALL\"]"
			+ (heights == null ? "" : ",\"HeightSequence\":" + heights) + "}";
	}

	/**
	 * Installs a resource stack: everything the jar ships, with the given entries layered over it,
	 * which is what a resource pack is from the mod's point of view.
	 */
	private static void installPackWith(Map<String, String> overlay) {
		ArchitectResources.setIndexForTesting(new OverlayIndex(overlay));
	}

	@Test
	@DisplayName("a theme only a pack provides shows up in the list")
	void packThemeAppears() {
		Map<String, String> pack = new LinkedHashMap<>();
		pack.put("themes/zeppelin/theme.json", themeJson("Zeppelin Yard", null));
		pack.put("themes/zeppelin/palette.json", "{\"Palette\":{\"Name\":\"Zeppelin\"}}");
		installPackWith(pack);

		List<String> names = new ArrayList<>();
		for (DesignTheme theme : ArchitectStorage.themes()
			.includedThemes())
			names.add(theme.getDisplayName());

		assertTrue(names.contains("Zeppelin Yard"), "a pack-supplied theme was not discovered: " + names);
		assertTrue(names.contains("Medieval"), "adding a pack theme lost the shipped ones: " + names);
	}

	@Test
	@DisplayName("the five shipped themes keep their positions, so number keys do not shuffle")
	void shippedThemesKeepTheirOrder() {
		// Deliberately sorts before "medieval", which is where a naive sort would put it first.
		installPackWith(Map.of("themes/aardvark/theme.json", themeJson("Aardvark Hall", null)));

		List<String> paths = new ArrayList<>();
		for (DesignTheme theme : ArchitectStorage.themes()
			.includedThemes())
			paths.add(theme.getFilePath());

		assertEquals(List.of("medieval", "modern", "town_house", "cattingham_palace", "aardvark"), paths,
			"a pack theme reordered the shipped themes instead of being appended");
	}

	@Test
	@DisplayName("the fallback theme is still not offered as a theme of its own")
	void fallbackStaysHidden() {
		installPackWith(Map.of());

		for (DesignTheme theme : ArchitectStorage.themes()
			.includedThemes())
			assertFalse("fallback_theme".equals(theme.getFilePath()),
				"the fallback theme was offered to the player");
	}

	/**
	 * The heights table is keyed by folder name, so a theme the mod has never heard of has no
	 * entry in it. Declaring the sequence in the file is how a pack theme gets sensible floors.
	 */
	@Test
	@DisplayName("a pack theme can declare its own floor heights")
	void packThemeDeclaresItsHeights() {
		installPackWith(Map.of("themes/zeppelin/theme.json", themeJson("Zeppelin Yard", "[6,3,9]")));

		DesignTheme theme = ArchitectStorage.themes()
			.builtIn("zeppelin");

		assertEquals(6, theme.getDefaultHeightForFloor(0));
		assertEquals(3, theme.getDefaultHeightForFloor(1));
		assertEquals(9, theme.getDefaultHeightForFloor(2));
	}

	@Test
	@DisplayName("a pack theme with no heights still gets usable floors")
	void packThemeWithoutHeightsFallsBack() {
		installPackWith(Map.of("themes/zeppelin/theme.json", themeJson("Zeppelin Yard", null)));

		assertTrue(ArchitectStorage.themes()
			.builtIn("zeppelin")
			.getDefaultHeightForFloor(0) > 0, "a pack theme with no HeightSequence had no ground floor");
	}

	/**
	 * The heights of the shipped themes live in a table because their files predate the field. A
	 * pack overriding one of those files and stating its own floors has to win, or the override is
	 * only half applied.
	 */
	@Test
	@DisplayName("a pack overriding a shipped theme overrides its floor heights too")
	void packOverridesShippedHeights() {
		installPackWith(Map.of());
		assertEquals(3, ArchitectStorage.themes()
			.builtIn("medieval")
			.getDefaultHeightForFloor(0), "the shipped heights table stopped being applied");

		ArchitectStorage.reset();
		installPackWith(Map.of("themes/medieval/theme.json", themeJson("Medieval", "[8]")));

		assertEquals(8, ArchitectStorage.themes()
			.builtIn("medieval")
			.getDefaultHeightForFloor(0), "the table beat a pack that stated its own floors");
	}

	@Test
	@DisplayName("only a folder with a theme.json directly in it counts as a theme")
	void onlyRealThemeFoldersCount() {
		Map<String, String> pack = new LinkedHashMap<>();
		pack.put("themes/zeppelin/theme.json", themeJson("Zeppelin Yard", null));
		// A design sits two levels down. Its folders must not become themes.
		pack.put("themes/zeppelin/regular/wall/design.json", "{\"Size\":[1,1,1],\"Layers\":[]}");
		// Nor may a theme.json buried deeper than one level.
		pack.put("themes/zeppelin/regular/theme.json", themeJson("Not A Theme", null));
		installPackWith(pack);

		List<String> discovered = ArchitectResources.listFoldersContaining("themes", "theme.json", SHIPPED);

		assertTrue(discovered.contains("zeppelin"));
		// A theme is one folder deep, so nothing discovered may be a path. Checked as a property
		// rather than by naming the one bad entry, because a depth bug spells it "zeppelin/regular"
		// rather than "regular" and an assertion naming the latter would never see it.
		for (String folder : discovered)
			assertFalse(folder.contains("/"), "a nested folder was mistaken for a theme: " + folder);
		assertEquals(SHIPPED.size() + 1, discovered.size(),
			"the discovered set is not exactly the shipped themes plus the pack's one: " + discovered);
	}

	@Test
	@DisplayName("a folder with no theme.json is not a theme")
	void foldersWithoutAThemeFileAreSkipped() {
		installPackWith(Map.of("themes/notatheme/palette.json", "{\"Palette\":{\"Name\":\"Stray\"}}"));

		assertFalse(ArchitectResources.listFoldersContaining("themes", "theme.json", SHIPPED)
			.contains("notatheme"), "a folder with no theme.json was listed as a theme");
	}

	/**
	 * A pack author's most likely first mistake is putting {@code theme.json} one directory too
	 * high. The prefix {@code themes/} and the suffix {@code /theme.json} share their slash, so
	 * {@code themes/theme.json} satisfies both and used to slice a substring backwards - which
	 * threw out of discovery, and discovery feeds the composer menu.
	 */
	@Test
	@DisplayName("a theme.json directly in themes/ is ignored rather than throwing")
	void aThemeFileOneLevelTooHighIsIgnored() {
		Map<String, String> pack = new LinkedHashMap<>();
		pack.put("themes/theme.json", themeJson("Too High", null));
		pack.put("themes/zeppelin/theme.json", themeJson("Zeppelin Yard", null));
		installPackWith(pack);

		List<String> discovered =
			assertDoesNotThrow(() -> ArchitectResources.listFoldersContaining("themes", "theme.json", SHIPPED),
				"a theme.json one level too high threw out of discovery");

		assertTrue(discovered.contains("zeppelin"), "the misplaced file also lost the valid theme beside it");
		for (String folder : discovered)
			assertFalse(folder.isEmpty(), "the misplaced file was listed as a theme with no name");
	}

	/**
	 * Discovery feeds {@code getAllThemes()}, which the composer menu builds its keybind list
	 * from. A throw there does not cost a theme, it costs the menu - so it degrades to the themes
	 * in the jar instead.
	 */
	@Test
	@DisplayName("if discovery fails outright, the shipped themes still load")
	void discoveryFailureDegradesToShipped() {
		ArchitectResources.setIndexForTesting(new ArchitectResources.ResourceIndex() {

			@Override
			public Optional<byte[]> read(String path) {
				return new OverlayIndex(Map.of()).read(path);
			}

			@Override
			public Optional<List<String>> listAll(String folder) {
				throw new IllegalStateException("a pack broke enumeration");
			}
		});

		List<DesignTheme> themes = assertDoesNotThrow(() -> ArchitectStorage.themes()
			.includedThemes(), "a broken resource stack took the whole composer menu down");

		assertFalse(themes.isEmpty(), "discovery failed closed instead of falling back to the jar");
		assertTrue(themes.stream()
			.anyMatch(theme -> "medieval".equals(theme.getFilePath())),
			"the fallback did not produce the shipped themes");
	}

	@Test
	@DisplayName("with nothing to enumerate, discovery falls back to the themes in the jar")
	void offGameFallsBackToShipped() {
		// No index installed and no client: this is the unit-suite path.
		ArchitectResources.setIndexForTesting(null);

		List<String> discovered = ArchitectResources.listFoldersContaining("themes", "theme.json", SHIPPED);

		assertEquals(SHIPPED.size(), discovered.size(), "the shipped themes were not all found off-game");
		for (String folder : SHIPPED)
			assertTrue(discovered.contains(folder), folder + " was not found off-game");
	}

	@Test
	@DisplayName("a name that is not really there is not reported off-game")
	void offGameValidatesTheNamesItIsGiven() {
		ArchitectResources.setIndexForTesting(null);

		assertFalse(ArchitectResources.listFoldersContaining("themes", "theme.json", List.of("no_such_theme"))
			.contains("no_such_theme"), "a theme that does not exist was reported as present");
	}

	/** The jar's own content, with a pack's entries layered over it. */
	private static final class OverlayIndex implements ArchitectResources.ResourceIndex {

		private final Map<String, String> overlay;

		private OverlayIndex(Map<String, String> overlay) {
			this.overlay = overlay;
		}

		@Override
		public Optional<byte[]> read(String path) {
			if (overlay.containsKey(path))
				return Optional.of(overlay.get(path)
					.getBytes(StandardCharsets.UTF_8));

			try (InputStream in = TheMightyArchitect.class.getClassLoader()
				.getResourceAsStream(ArchitectResources.CLASSPATH_ROOT + path)) {
				return in == null ? Optional.empty() : Optional.of(in.readAllBytes());
			} catch (IOException e) {
				return Optional.empty();
			}
		}

		/**
		 * The overlay's paths plus the jar's theme files.
		 * <p>
		 * A classloader cannot walk a jar directory, so the shipped side is enumerated from the
		 * names the mod knows - which is enough, because what is under test is the rule applied to
		 * an enumeration, not the enumeration itself.
		 */
		@Override
		public Optional<List<String>> listAll(String folder) {
			List<String> paths = new ArrayList<>();
			String prefix = folder + "/";

			for (String path : overlay.keySet())
				if (path.startsWith(prefix))
					paths.add(path);

			for (String shipped : SHIPPED) {
				String path = "themes/" + shipped + "/theme.json";
				if (path.startsWith(prefix) && !paths.contains(path) && read(path).isPresent())
					paths.add(path);
			}

			return Optional.of(paths);
		}
	}
}
