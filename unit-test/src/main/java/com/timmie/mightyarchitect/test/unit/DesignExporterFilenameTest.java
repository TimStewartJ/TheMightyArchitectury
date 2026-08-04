package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The name on line two of a design's sign becomes the file that design is saved as. Before the
 * P1 fixes this returned a Java object dump ({@code SignText@6f1a2b}) on every version from 1.20
 * onwards, which meant sign-naming was dead, the garbage name was written back to the sign, and
 * re-exporting a design orphaned the old file instead of overwriting it.
 * <p>
 * Idempotence is the property that makes re-export overwrite rather than orphan, and it is pure
 * string arithmetic - exactly what should not need a game boot to check.
 */
@Bootstrapped
@DisplayName("DesignExporter.designFilename")
class DesignExporterFilenameTest {

	@Test
	@DisplayName("derives a filename from sign text")
	void derivesFromSignText() {
		assertEquals("my_design.json", DesignExporter.designFilename("My Design"));
	}

	@Test
	@DisplayName("is idempotent, so re-export overwrites rather than orphaning")
	void isIdempotent() {
		String once = DesignExporter.designFilename("My Design");
		String twice = DesignExporter.designFilename(once);
		String thrice = DesignExporter.designFilename(twice);

		assertEquals("my_design.json", once);
		assertEquals(once, twice, "a second pass grew another suffix");
		assertEquals(once, thrice, "a third pass grew another suffix");
	}

	@Test
	@DisplayName("strips the extension case-insensitively")
	void stripsExtensionCaseInsensitively() {
		assertEquals("my_design.json", DesignExporter.designFilename("My Design.JSON"));
		assertEquals("my_design.json", DesignExporter.designFilename("my_design.Json"));
	}

	@Test
	@DisplayName("blank sign text yields nothing, so the caller falls back to an indexed name")
	void blankSignTextYieldsNothing() {
		assertTrue(DesignExporter.designFilename("   ")
			.isEmpty());
		assertTrue(DesignExporter.designFilename("")
			.isEmpty());
		assertTrue(DesignExporter.designFilename(".json")
			.isEmpty());
		assertTrue(DesignExporter.designFilename("!!!")
			.isEmpty());
	}

	@TestFactory
	@DisplayName("cannot escape the design folder")
	Stream<DynamicTest> cannotEscapeItsFolder() {
		List<String> hostile = List.of("../../evil name", "..", "../../../design.json", "/tmp/design",
			"C:\\design.json", "a/b/c");

		return hostile.stream()
			.map(name -> DynamicTest.dynamicTest("\"" + name + "\"", () -> {
				String filename = DesignExporter.designFilename(name);

				assertFalse(filename.contains("/"), () -> "kept a forward slash: " + filename);
				assertFalse(filename.contains("\\"), () -> "kept a backslash: " + filename);
				assertFalse(filename.contains(".."), () -> "kept a parent reference: " + filename);
				assertTrue(filename.isEmpty() || filename.endsWith(".json"),
					() -> "produced something that is not a design file: " + filename);
			}));
	}

	/**
	 * The exported name is echoed back onto the sign, so whatever comes out has to survive being
	 * read back in. A name that changed on every pass is what orphaned the previous file.
	 */
	@Test
	@DisplayName("a derived name survives a round trip through the sign")
	void survivesTheSignRoundTrip() {
		for (String signed : List.of("Gothic Window", "gothic_window.json", "  Gothic-Window  ", "Roof 2")) {
			String filename = DesignExporter.designFilename(signed);
			assertEquals(filename, DesignExporter.designFilename(filename),
				"\"" + signed + "\" did not survive being written back to the sign");
		}
	}
}
