package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.design.DesignLayer;
import com.timmie.mightyarchitect.control.design.DesignTheme;
import com.timmie.mightyarchitect.control.design.DesignType;
import com.timmie.mightyarchitect.control.design.ThemeStorage;
import com.timmie.mightyarchitect.control.design.partials.DesignData;
import com.timmie.mightyarchitect.control.design.partials.Wall.ExpandBehaviour;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.storage.ArchitectPaths;
import com.timmie.mightyarchitect.control.storage.ArchitectStorage;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import com.timmie.mightyarchitect.control.storage.PackedTheme;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A whole theme in one file. The compressed {@code .theme} and the plain {@code .json} carry the
 * same tree, but before this they were produced by two different code paths against two different
 * serializers - and the JSON one went through the SNBT detour, so the two could and did disagree
 * about what a field meant.
 * <p>
 * The point of these assertions is that the archive is now defined once and rendered by the same
 * codec into both, so a theme exported either way comes back the same.
 */
@Bootstrapped
@DisplayName("PackedTheme")
class PackedThemeTest {

	private static PackedTheme sampleArchive() {
		DesignTheme theme = ThemeStorage.createTheme("Packed Theme", "unit-test");

		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.setName("Packed Primary");
		palette.put(Palette.ROOF_PRIMARY, Blocks.GOLD_BLOCK);

		PaletteDefinition secondary = PaletteDefinition.defaultPalette()
			.clone();
		secondary.setName("Packed Secondary");

		DesignData wall = new DesignData(new BlockPos(3, 2, 1),
			List.of(new com.timmie.mightyarchitect.control.design.DesignSlice.SliceData(
				com.timmie.mightyarchitect.control.design.DesignSlice.DesignSliceTrait.Standard, "GDA",
				Optional.of("AAA"))),
			0, 0, 0, ExpandBehaviour.Repeat);

		Map<DesignLayer, Map<DesignType, List<DesignData>>> designs =
			Map.of(DesignLayer.Regular, Map.of(DesignType.WALL, List.of(wall)));

		return new PackedTheme(theme, palette, Optional.of(secondary), PackedTheme.group(theme, designs));
	}

	private static void assertMatchesSample(PackedTheme roundTrip) {
		assertEquals("Packed Theme", roundTrip.theme()
			.getDisplayName());
		assertEquals("unit-test", roundTrip.theme()
			.getDesigner());
		assertEquals("Packed Primary", roundTrip.palette()
			.getName());
		assertEquals(Blocks.GOLD_BLOCK.defaultBlockState(), roundTrip.palette()
			.get(Palette.ROOF_PRIMARY));
		assertTrue(roundTrip.secondaryPalette()
			.isPresent());
		assertEquals("Packed Secondary", roundTrip.secondaryPalette()
			.get()
			.getName());

		List<DesignData> walls = roundTrip.designsFor(DesignLayer.Regular, DesignType.WALL);
		assertEquals(1, walls.size(), "the archive lost its designs");
		assertEquals(new BlockPos(3, 2, 1), walls.get(0)
			.size());
		assertEquals(ExpandBehaviour.Repeat, walls.get(0)
			.expandBehaviour());
		assertEquals("GDA", walls.get(0)
			.layers()
			.get(0)
			.blocks());
	}

	@Test
	@DisplayName("survives the compressed round trip")
	void survivesTheNbtRoundTrip() {
		CompoundTag encoded = JsonStorage.toNbt(PackedTheme.CODEC, sampleArchive())
			.orElseThrow();

		assertMatchesSample(JsonStorage.fromNbt(PackedTheme.CODEC, encoded, "a packed theme")
			.orElseThrow());
	}

	@Test
	@DisplayName("survives the JSON round trip")
	void survivesTheJsonRoundTrip() {
		String encoded = JsonStorage.encode(PackedTheme.CODEC, sampleArchive())
			.orElseThrow()
			.toString();

		assertMatchesSample(JsonStorage
			.read(new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8)), PackedTheme.CODEC,
				"a packed theme")
			.orElseThrow());
	}

	/** The two export buttons have to produce the same theme, which is the whole point. */
	@Test
	@DisplayName("both formats describe the same archive")
	void bothFormatsAgree() {
		PackedTheme archive = sampleArchive();

		PackedTheme viaNbt = JsonStorage
			.fromNbt(PackedTheme.CODEC, JsonStorage.toNbt(PackedTheme.CODEC, archive)
				.orElseThrow(), "a packed theme")
			.orElseThrow();
		PackedTheme viaJson = JsonStorage.read(new ByteArrayInputStream(JsonStorage.encode(PackedTheme.CODEC, archive)
			.orElseThrow()
			.toString()
			.getBytes(StandardCharsets.UTF_8)), PackedTheme.CODEC, "a packed theme")
			.orElseThrow();

		assertEquals(viaNbt.theme()
			.getDisplayName(),
			viaJson.theme()
				.getDisplayName());
		assertEquals(viaNbt.designs(), viaJson.designs(),
			"the compressed and plain exports disagree about the designs");
		for (Palette key : Palette.values())
			assertEquals(viaNbt.palette()
				.get(key),
				viaJson.palette()
					.get(key),
				"the two formats disagree on palette slot " + key);
	}

	@Test
	@DisplayName("an archive with no secondary palette still reads")
	void secondaryPaletteIsOptional() {
		PackedTheme archive = sampleArchive();
		PackedTheme withoutSecondary =
			new PackedTheme(archive.theme(), archive.palette(), Optional.empty(), archive.designs());

		PackedTheme roundTrip = JsonStorage
			.fromNbt(PackedTheme.CODEC, JsonStorage.toNbt(PackedTheme.CODEC, withoutSecondary)
				.orElseThrow(), "a packed theme")
			.orElseThrow();

		assertFalse(roundTrip.secondaryPalette()
			.isPresent());
	}

	@Test
	@DisplayName("both file extensions are recognised as packed themes")
	void recognisesBothExtensions() {
		assertTrue(PackedTheme.isPackedName("castle.theme"));
		assertTrue(PackedTheme.isPackedName("castle.json"));
		assertFalse(PackedTheme.isPackedName("castle"), "a theme folder was mistaken for a packed file");
	}

	/**
	 * The export used to report a bare filename - "Exported Theme as my_theme.theme" - which does
	 * not say where to look. It reports the path relative to the game directory now, which is both
	 * actionable and short enough for the composer's status line.
	 */
	@Test
	@DisplayName("exporting reports where the file went, not just its name")
	void exportReportsThePath(@TempDir Path root) {
		ArchitectPaths.setRootsForTesting(root, root.resolve("instance"));
		try {
			DesignTheme theme = ThemeStorage.createTheme("Packed Theme", "unit-test");
			theme.setDefaultPalette(PaletteDefinition.defaultPalette()
				.clone());
			theme.setDefaultSecondaryPalette(PaletteDefinition.defaultPalette()
				.clone());

			String reported = ArchitectStorage.themes()
				.saveAsSingleFile(theme, true);

			assertTrue(reported.contains("packed_theme.theme"), "the filename went missing: " + reported);
			assertTrue(reported.contains("export"),
				"the report does not say which folder the file went to: " + reported);
			assertTrue(Files.exists(root.resolve("themes")
				.resolve("export")
				.resolve("packed_theme.theme")), "the export did not land where it said it did");
		} finally {
			ArchitectPaths.setRootsForTesting(null, null);
			ArchitectStorage.reset();
		}
	}

	/**
	 * A theme's live folder and its packed copy carry the same display name, so scanning the
	 * export folder would list every exported theme twice - once editable, once as a frozen
	 * snapshot under an identical label.
	 */
	@Test
	@DisplayName("an exported copy does not become a second theme in the list")
	void exportedCopiesAreNotRescanned(@TempDir Path root) {
		ArchitectPaths.setRootsForTesting(root, root.resolve("instance"));
		try {
			DesignTheme theme = ThemeStorage.createTheme("Packed Theme", "unit-test");
			theme.setDefaultPalette(PaletteDefinition.defaultPalette()
				.clone());
			theme.setDefaultSecondaryPalette(PaletteDefinition.defaultPalette()
				.clone());

			ArchitectStorage.themes()
				.save(theme);
			ArchitectStorage.themes()
				.saveAsSingleFile(theme, true);
			ArchitectStorage.themes()
				.invalidateExternal();

			long matching = ArchitectStorage.themes()
				.imported()
				.stream()
				.filter(found -> "Packed Theme".equals(found.getDisplayName()))
				.count();

			assertEquals(1, matching, "the exported copy was listed as a second theme with the same name");
		} finally {
			ArchitectPaths.setRootsForTesting(null, null);
			ArchitectStorage.reset();
		}
	}

	@Test
	@DisplayName("asking for a layer or type the archive lacks gives nothing, not an exception")
	void missingSetsAreEmpty() {
		PackedTheme archive = sampleArchive();

		assertTrue(archive.designsFor(DesignLayer.Foundation, DesignType.WALL)
			.isEmpty());
		assertTrue(archive.designsFor(DesignLayer.Regular, DesignType.TOWER)
			.isEmpty());
	}
}
