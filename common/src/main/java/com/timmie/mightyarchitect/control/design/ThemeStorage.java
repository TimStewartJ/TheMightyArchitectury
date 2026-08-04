package com.timmie.mightyarchitect.control.design;

import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.design.partials.DesignData;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.storage.ArchitectPaths;
import com.timmie.mightyarchitect.control.storage.ArchitectStorage;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import com.timmie.mightyarchitect.control.storage.PackedTheme;
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The themes available to the composer: the ones inside the mod, and the ones on disk.
 * <p>
 * This used to be entirely static, and {@link IncludedThemes} cached each built-in theme by
 * <i>writing it back into its own enum constant</i> - state with no owner and no way to drop it, so
 * nothing was reloadable and nothing was testable off-game. The state now lives on an instance that
 * {@link ArchitectStorage} owns and can discard, which is what makes F3+T reload the built-in
 * themes; the static methods are kept so the screens and phases that call them do not all have to
 * change in the same commit.
 */
public class ThemeStorage {

	/** The built-in themes, and the floor heights each was designed around. */
	public enum IncludedThemes {

		Medieval("medieval", 3, 5),
		Fallback("fallback_theme", 3, 4),
		Modern("modern", 2, 4),
		TownHouse("town_house", 4, 5),
		Cattingham("cattingham_palace", 7, 2, 6);

		public final String themeFolder;
		public final List<Integer> heights;

		private IncludedThemes(String themeFolder, Integer... floorHeights) {
			this.themeFolder = themeFolder;
			this.heights = Arrays.asList(floorHeights);
		}
	}

	private final Map<IncludedThemes, DesignTheme> included = new EnumMap<>(IncludedThemes.class);
	private List<DesignTheme> importedThemes;
	private List<DesignTheme> createdThemes;

	// ---------------------------------------------------------------- static facade

	public static List<DesignTheme> getAllThemes() {
		return ArchitectStorage.themes()
			.allThemes();
	}

	public static List<DesignTheme> getIncluded() {
		return ArchitectStorage.themes()
			.includedThemes();
	}

	public static List<DesignTheme> getImported() {
		return ArchitectStorage.themes()
			.imported();
	}

	public static List<DesignTheme> getCreated() {
		return ArchitectStorage.themes()
			.created();
	}

	public static DesignTheme getIncludedTheme(IncludedThemes which) {
		return ArchitectStorage.themes()
			.builtIn(which);
	}

	public static void reloadExternal() {
		ArchitectStorage.themes()
			.invalidateExternal();
	}

	public static void exportTheme(DesignTheme theme) {
		ArchitectStorage.themes()
			.save(theme);
	}

	public static String exportThemeFullyAsFile(DesignTheme theme, boolean compressed) {
		return ArchitectStorage.themes()
			.saveAsSingleFile(theme, compressed);
	}

	public static DesignTheme createTheme(String name) {
		return createTheme(name, Minecraft.getInstance().player.getName()
			.getString());
	}

	/**
	 * Builds a new empty theme with its own palettes.
	 * <p>
	 * The designer is a parameter rather than read from the client here so that the part of this
	 * that is pure - the name fallback, the file-path slug and the palette cloning - can be
	 * asserted without a running game. {@link #createTheme(String)} supplies the local player.
	 */
	public static DesignTheme createTheme(String name, String designer) {
		if (name.isEmpty())
			name = "My Theme";
		DesignTheme theme = new DesignTheme(name, designer);
		theme.setFilePath(FilesHelper.slugOr(name, "my_theme"));
		theme.setImported(true);
		// Clones: storing defaultPalette() itself handed the palette editor the shared default, so
		// recolouring one theme recoloured the starting palette of every theme made afterwards.
		theme.setDefaultPalette(PaletteDefinition.defaultPalette()
			.clone());
		theme.setDefaultSecondaryPalette(PaletteDefinition.defaultPalette()
			.clone());
		return theme.withLayers(DesignLayer.Regular, DesignLayer.Roofing, DesignLayer.Foundation)
			.withTypes(DesignType.WALL, DesignType.CORNER, DesignType.ROOF, DesignType.FACADE, DesignType.FLAT_ROOF);
	}

	// ---------------------------------------------------------------- instance

	public List<DesignTheme> allThemes() {
		List<DesignTheme> themes = new ArrayList<>(includedThemes());
		themes.addAll(imported());
		return themes;
	}

	public List<DesignTheme> includedThemes() {
		List<DesignTheme> themes = new ArrayList<>();
		for (IncludedThemes which : IncludedThemes.values()) {
			DesignTheme theme = builtIn(which);
			// The fallback exists to be picked from when a real theme has no matching design; it
			// is deliberately not offered as a theme of its own.
			if (which != IncludedThemes.Fallback)
				themes.add(theme);
		}
		return themes;
	}

	public DesignTheme builtIn(IncludedThemes which) {
		return included.computeIfAbsent(which,
			missing -> loadInternalTheme(missing.themeFolder).withHeightSequence(missing.heights));
	}

	public List<DesignTheme> imported() {
		if (importedThemes == null)
			importThemes();
		return importedThemes;
	}

	public List<DesignTheme> created() {
		if (createdThemes == null)
			importThemes();
		return createdThemes;
	}

	/** Drops the themes read from disk, so the next request rescans. */
	public void invalidateExternal() {
		importedThemes = null;
		createdThemes = null;
	}

	/** Drops everything, including the built-in themes - what a resource reload has to do. */
	public void invalidate() {
		invalidateExternal();
		included.clear();
	}

	/** Writes a theme as a folder: its metadata and its two palettes. */
	public void save(DesignTheme theme) {
		Path folder = ArchitectPaths.themes()
			.resolve(theme.getFilePath());
		FilesHelper.createFolderIfMissing(folder);

		JsonStorage.write(folder.resolve("theme.json"), DesignTheme.CODEC, theme);
		JsonStorage.write(folder.resolve("palette.json"), PaletteDefinition.CODEC, theme.getDefaultPalette());
		JsonStorage.write(folder.resolve("palette2.json"), PaletteDefinition.CODEC,
			theme.getDefaultSecondaryPalette());
	}

	/**
	 * Writes a theme and every design in it as one shareable file.
	 *
	 * @return the filename written, so the caller can tell the user where it went
	 */
	public String saveAsSingleFile(DesignTheme theme, boolean compressed) {
		Path folder = ArchitectPaths.themeExports();
		FilesHelper.createFolderIfMissing(folder);

		Map<DesignLayer, Map<DesignType, List<DesignData>>> designs =
			DesignResourceLoader.loadThemeFromFolder(theme);
		PackedTheme packed = new PackedTheme(theme, theme.getDefaultPalette(),
			Optional.ofNullable(theme.getDefaultSecondaryPalette()), PackedTheme.group(theme, designs));

		String filename = theme.getFilePath()
			+ (compressed ? PackedTheme.COMPRESSED_EXTENSION : PackedTheme.JSON_EXTENSION);
		Path target = folder.resolve(filename);

		if (compressed)
			JsonStorage.toNbt(PackedTheme.CODEC, packed)
				.ifPresent(tag -> JsonStorage.writeCompressed(target, tag));
		else
			JsonStorage.writeCompact(target, PackedTheme.CODEC, packed);

		return filename;
	}

	private DesignTheme loadInternalTheme(String themeFolder) {
		String base = ArchitectPaths.THEMES + "/" + themeFolder + "/";
		DesignTheme theme = JsonStorage.readBuiltIn(base + "theme.json", DesignTheme.CODEC)
			.orElseThrow(() -> new IllegalStateException(
				"Built-in theme " + themeFolder + " is missing from the mod jar"));

		theme.setFilePath(themeFolder);
		theme.setImported(false);
		theme.setDefaultPalette(builtInPalette(base + "palette.json"));
		theme.setDefaultSecondaryPalette(builtInPalette(base + "palette2.json"));
		return theme;
	}

	private static PaletteDefinition builtInPalette(String resourcePath) {
		return JsonStorage.readBuiltIn(resourcePath, PaletteDefinition.CODEC)
			.orElseGet(() -> PaletteDefinition.defaultPalette()
				.clone());
	}

	private void importThemes() {
		importedThemes = new ArrayList<>();
		createdThemes = new ArrayList<>();

		for (Path entry : ArchitectPaths.listAcrossRoots(ArchitectPaths.THEMES)) {
			String name = entry.getFileName()
				.toString();

			if (name.equals("export"))
				continue;

			// Per theme, not per scan: an unknown layer name or a malformed file used to throw
			// out of the loop and drop every theme after it from the list.
			try {
				importTheme(entry, name);
			} catch (RuntimeException e) {
				TheMightyArchitect.logger.error("Skipping unreadable theme " + name, e);
			}
		}
	}

	private void importTheme(Path entry, String name) {
		DesignTheme theme;
		PaletteDefinition palette;
		PaletteDefinition secondaryPalette = null;

		boolean packedIntoOneFile = PackedTheme.isPackedName(name);

		if (packedIntoOneFile) {
			Optional<PackedTheme> packed = PackedTheme.read(entry);
			if (!packed.isPresent())
				return;
			theme = packed.get()
				.theme();
			palette = packed.get()
				.palette();
			secondaryPalette = packed.get()
				.secondaryPalette()
				.orElse(null);

		} else {
			if (!Files.isDirectory(entry))
				return;
			Optional<DesignTheme> read = JsonStorage.read(entry.resolve("theme.json"), DesignTheme.CODEC);
			if (!read.isPresent())
				return;
			theme = read.get();
			palette = JsonStorage.read(entry.resolve("palette.json"), PaletteDefinition.CODEC)
				.orElseGet(() -> PaletteDefinition.defaultPalette()
					.clone());
			secondaryPalette = JsonStorage.read(entry.resolve("palette2.json"), PaletteDefinition.CODEC)
				.orElse(null);
		}

		theme.setFilePath(name);
		theme.setImported(true);
		theme.setDefaultPalette(palette);

		if (secondaryPalette != null)
			theme.setDefaultSecondaryPalette(secondaryPalette);
		else
			// Cloned, so that editing the secondary palette does not also edit the primary one.
			theme.setDefaultSecondaryPalette(palette.clone());

		importedThemes.add(theme);
		if (!packedIntoOneFile)
			createdThemes.add(theme);
	}
}
