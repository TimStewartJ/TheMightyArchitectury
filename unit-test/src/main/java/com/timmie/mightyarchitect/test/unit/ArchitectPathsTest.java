package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.storage.ArchitectPaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mod's three folders used to be resolved with {@code Paths.get("themes/")}, which is relative
 * to the process working directory rather than to the game. They now hang off the game directory
 * and share one mod folder.
 * <p>
 * That move is the dangerous part of this change, not the paths themselves: the theme screen has
 * been telling people to drop files into the old folder for years, so real data exists there. The
 * rules the migration has to hold to are asserted here one at a time - <b>copy, never overwrite,
 * never delete, run once</b>, and keep reading the old place afterwards.
 */
@DisplayName("ArchitectPaths")
class ArchitectPathsTest {

	@AfterEach
	void restoreRealPaths() {
		ArchitectPaths.setRootsForTesting(null, null);
	}

	private static void write(Path file, String content) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

	@Nested
	@DisplayName("layout")
	class Layout {

		@Test
		@DisplayName("every folder sits below one mod folder")
		void everyFolderSitsBelowTheModFolder(@TempDir Path root, @TempDir Path legacy) {
			ArchitectPaths.setRootsForTesting(root, legacy);

			assertEquals(root, ArchitectPaths.root());
			assertEquals(root.resolve("themes"), ArchitectPaths.themes());
			assertEquals(root.resolve("palettes"), ArchitectPaths.palettes());
			assertEquals(root.resolve("schematics"), ArchitectPaths.schematics());
			assertEquals(root.resolve("themes")
				.resolve("export"), ArchitectPaths.themeExports());
		}

		@Test
		@DisplayName("the mod folder is named after the mod, under the game directory")
		void theModFolderIsNamedAfterTheMod() {
			// No client here, so the game directory falls back to the working directory - which is
			// the property that matters: nothing resolves against a hardcoded relative name any
			// more.
			assertEquals(ArchitectPaths.gameDirectory()
				.resolve(ArchitectPaths.FOLDER), ArchitectPaths.root());
			assertEquals("mightyarchitect", ArchitectPaths.FOLDER);
		}
	}

	@Nested
	@DisplayName("migration")
	class Migration {

		@Test
		@DisplayName("copies the previous folders and leaves the originals alone")
		void copiesAndLeavesTheOriginals(@TempDir Path root, @TempDir Path legacy) throws IOException {
			write(legacy.resolve("themes/castle/theme.json"), "{\"Name\":\"Castle\"}");
			write(legacy.resolve("themes/castle/regular/wall/design.json"), "{}");
			write(legacy.resolve("palettes/mine.json"), "{}");
			write(legacy.resolve("schematics/keep.nbt"), "x");

			ArchitectPaths.setRootsForTesting(root, legacy);
			ArchitectPaths.migrateLegacyData();

			assertEquals("{\"Name\":\"Castle\"}", Files.readString(root.resolve("themes/castle/theme.json")));
			assertTrue(Files.exists(root.resolve("themes/castle/regular/wall/design.json")),
				"the copy did not recurse into a theme's design folders");
			assertTrue(Files.exists(root.resolve("palettes/mine.json")));
			assertTrue(Files.exists(root.resolve("schematics/keep.nbt")));

			assertTrue(Files.exists(legacy.resolve("themes/castle/theme.json")),
				"the migration deleted the original, which is the failure it exists to prevent");
			assertTrue(Files.exists(legacy.resolve("palettes/mine.json")));
			assertTrue(Files.exists(legacy.resolve("schematics/keep.nbt")));
		}

		@Test
		@DisplayName("never writes over a file the new folder already has")
		void neverOverwrites(@TempDir Path root, @TempDir Path legacy) throws IOException {
			write(legacy.resolve("palettes/mine.json"), "old");
			write(root.resolve("palettes/mine.json"), "new");

			ArchitectPaths.setRootsForTesting(root, legacy);
			ArchitectPaths.migrateLegacyData();

			assertEquals("new", Files.readString(root.resolve("palettes/mine.json")));
		}

		@Test
		@DisplayName("runs once, so a deleted theme does not come back next launch")
		void runsOnce(@TempDir Path root, @TempDir Path legacy) throws IOException {
			write(legacy.resolve("palettes/mine.json"), "old");

			ArchitectPaths.setRootsForTesting(root, legacy);
			ArchitectPaths.migrateLegacyData();
			Files.delete(root.resolve("palettes/mine.json"));

			// A second launch: same folders, fresh process state.
			ArchitectPaths.setRootsForTesting(root, legacy);
			ArchitectPaths.migrateLegacyData();

			assertFalse(Files.exists(root.resolve("palettes/mine.json")),
				"the migration resurrected a file the user had deleted");
		}

		@Test
		@DisplayName("does nothing when there is nothing to copy")
		void doesNothingWithNoLegacyData(@TempDir Path root, @TempDir Path legacy) {
			ArchitectPaths.setRootsForTesting(root, legacy);
			ArchitectPaths.migrateLegacyData();

			assertFalse(Files.exists(root.resolve("themes")), "it created folders it had nothing to put in");
		}
	}

	@Nested
	@DisplayName("reading both locations")
	class ReadingBoth {

		@Test
		@DisplayName("a file left in the old folder is still found")
		void stillReadsTheOldFolder(@TempDir Path root, @TempDir Path legacy) throws IOException {
			ArchitectPaths.setRootsForTesting(root, legacy);
			ArchitectPaths.migrateLegacyData();

			// Dropped in afterwards, the way someone following an old tutorial would.
			write(legacy.resolve("palettes/late.json"), "{}");

			List<Path> found = ArchitectPaths.listAcrossRoots("palettes");
			assertEquals(1, found.size(), "a palette added to the old folder after the migration was invisible");
			assertEquals("late.json", found.get(0)
				.getFileName()
				.toString());
		}

		@Test
		@DisplayName("the mod folder wins when both have the same name")
		void theModFolderWins(@TempDir Path root, @TempDir Path legacy) throws IOException {
			write(legacy.resolve("palettes/mine.json"), "old");
			write(root.resolve("palettes/mine.json"), "new");

			ArchitectPaths.setRootsForTesting(root, legacy);

			List<Path> found = ArchitectPaths.listAcrossRoots("palettes");
			assertEquals(1, found.size(), "the same palette was offered twice");
			assertEquals("new", Files.readString(found.get(0)));
			assertEquals("new", Files.readString(ArchitectPaths.findAcrossRoots("palettes", "mine.json")));
		}

		@Test
		@DisplayName("a name in neither folder resolves under the mod folder, ready to be written")
		void unknownNamesResolveUnderTheModFolder(@TempDir Path root, @TempDir Path legacy) {
			ArchitectPaths.setRootsForTesting(root, legacy);

			assertEquals(root.resolve("palettes")
				.resolve("absent.json"), ArchitectPaths.findAcrossRoots("palettes", "absent.json"));
		}
	}
}
