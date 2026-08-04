package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code FilesHelper.slug} is the mod's only defence against a user-supplied name reaching the
 * filesystem: theme names, palette names and design sign text are all pasted straight into a path.
 * It is pure {@code String} arithmetic, so it belongs here rather than costing a game boot on 25
 * targets to check.
 */
@Bootstrapped
@DisplayName("FilesHelper.slug")
class FilesHelperSlugTest {

	/**
	 * Names that must not be able to address anything outside the folder they are joined to. Sign
	 * text is attacker-adjacent - it is whatever another player wrote on a sign in a shared world.
	 */
	private static final List<String> HOSTILE_NAMES = List.of(
		"../../evil name",
		"..",
		"../",
		"..\\..\\evil",
		"/etc/passwd",
		"C:\\Windows\\System32",
		"nested/path/name",
		"name\u0000truncated",
		"....//....//x",
		"~/.ssh/authorized_keys");

	@Test
	@DisplayName("lower-cases and keeps the safe alphabet")
	void keepsSafeCharacters() {
		assertEquals("medieval_tower2", FilesHelper.slug("Medieval_Tower2"));
	}

	@Test
	@DisplayName("spaces, dashes and dots become underscores")
	void convertsSeparators() {
		assertEquals("town_house", FilesHelper.slug("Town House"));
		assertEquals("town_house", FilesHelper.slug("Town-House"));
		assertEquals("town_house", FilesHelper.slug("Town.House"));
	}

	@Test
	@DisplayName("drops everything else instead of escaping it")
	void dropsUnsafeCharacters() {
		assertEquals("cattinghams_palace", FilesHelper.slug("Cattingham's Palace!"));
		// Only space, dash and dot map to an underscore; a drive colon and a backslash are simply
		// gone, which is why the result runs together rather than keeping a separator.
		assertEquals("cwindows", FilesHelper.slug("C:\\Windows"));
	}

	/**
	 * The property that matters, stated as a property rather than as expected output: whatever
	 * comes out is one path element that resolves inside the folder it is joined to.
	 */
	@TestFactory
	@DisplayName("cannot escape its folder")
	Stream<DynamicTest> cannotEscapeItsFolder() {
		Path base = Paths.get("themes")
			.toAbsolutePath()
			.normalize();

		return HOSTILE_NAMES.stream()
			.map(hostile -> DynamicTest.dynamicTest(quoted(hostile), () -> {
				String slug = FilesHelper.slug(hostile);

				assertFalse(slug.contains("/"), () -> "slug kept a forward slash: " + slug);
				assertFalse(slug.contains("\\"), () -> "slug kept a backslash: " + slug);
				assertFalse(slug.contains(".."), () -> "slug kept a parent reference: " + slug);
				assertFalse(slug.contains("\u0000"), () -> "slug kept a NUL byte: " + slug);

				Path resolved = base.resolve(slug.isEmpty() ? "unnamed" : slug)
					.normalize();
				assertTrue(resolved.startsWith(base),
					() -> quoted(hostile) + " resolved outside the folder: " + resolved);
				assertEquals(1, base.relativize(resolved)
					.getNameCount(),
					() -> quoted(hostile) + " produced more than one path element: " + resolved);
			}));
	}

	@Test
	@DisplayName("sanitizes away to empty rather than to a dangerous default")
	void sanitizesAwayToEmpty() {
		assertEquals("", FilesHelper.slug("   "));
		assertEquals("", FilesHelper.slug("..."));
		assertEquals("", FilesHelper.slug("!!!"));
		assertEquals("", FilesHelper.slug(""));
	}

	@Test
	@DisplayName("trims leading and trailing underscores")
	void trimsUnderscores() {
		assertEquals("name", FilesHelper.slug("  name  "));
		assertEquals("name", FilesHelper.slug("___name___"));
		assertEquals("a_b", FilesHelper.slug("-a b-"));
	}

	@Test
	@DisplayName("caps the length so a long name cannot overflow a path")
	void capsLength() {
		assertEquals(64, FilesHelper.slug("a".repeat(500))
			.length());
	}

	/** Windows refuses these as filenames with or without an extension, so they get a suffix. */
	@TestFactory
	@DisplayName("escapes Windows reserved device names")
	Stream<DynamicTest> escapesWindowsReservedNames() {
		return Stream.of("con", "PRN", "aux", "NUL", "com1", "lpt9")
			.map(reserved -> DynamicTest.dynamicTest(reserved,
				() -> assertEquals(reserved.toLowerCase(Locale.ROOT) + "_", FilesHelper.slug(reserved))));
	}

	@Test
	@DisplayName("slugOr substitutes the fallback only when nothing survived")
	void slugOrFallsBack() {
		assertEquals("unnamed", FilesHelper.slugOr("///", "unnamed"));
		assertEquals("my_theme", FilesHelper.slugOr("", "my_theme"));
		assertEquals("kept", FilesHelper.slugOr("Kept", "my_theme"));
	}

	private static String quoted(String value) {
		return "\"" + value.replace("\u0000", "\\0") + "\"";
	}
}
