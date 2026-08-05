package com.timmie.mightyarchitect.control.design;

import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.design.partials.DesignData;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.storage.ArchitectPaths;
import com.timmie.mightyarchitect.control.storage.ArchitectResources;
import com.timmie.mightyarchitect.control.storage.ArchitectStorage;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import com.timmie.mightyarchitect.control.storage.PackedTheme;
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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

	/**
	 * The themes that ship inside the mod, and the floor heights each was designed around.
	 * <p>
	 * This is no longer the list of built-in themes - {@link #builtInFolders()} discovers those
	 * from the resource stack, so a pack can add one. What is left here is the heights table for
	 * the five the mod ships, whose files predate {@code HeightSequence} and so cannot state their
	 * own, plus the name of the fallback, which is referred to by identity elsewhere.
	 */
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

		/** @return the entry for that folder, or empty for a theme the mod does not ship */
		static Optional<IncludedThemes> byFolder(String folder) {
			for (IncludedThemes which : values())
				if (which.themeFolder.equals(folder))
					return Optional.of(which);
			return Optional.empty();
		}
	}

	/** The marker that makes a folder under {@code themes/} a theme. */
	private static final String THEME_FILE = "theme.json";

	private final Map<String, DesignTheme> included = new LinkedHashMap<>();
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

	/** @return where the theme folder for editing lives, for the "open folder" affordances */
	public static Path themeFolder() {
		return ArchitectPaths.themes();
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

	/**
	 * Every theme available without the user installing anything.
	 * <p>
	 * Discovered from the resource stack rather than read off {@link IncludedThemes}, so a
	 * resource pack that ships {@code assets/mightyarchitect/themes/<name>/theme.json} appears
	 * here alongside the five in the jar. One unreadable theme costs itself rather than the list.
	 */
	public List<DesignTheme> includedThemes() {
		List<DesignTheme> themes = new ArrayList<>();
		for (String folder : discoverBuiltInFolders()) {
			// The fallback exists to be picked from when a real theme has no matching design; it
			// is deliberately not offered as a theme of its own.
			if (IncludedThemes.Fallback.themeFolder.equals(folder))
				continue;
			try {
				themes.add(builtIn(folder));
			} catch (RuntimeException e) {
				TheMightyArchitect.logger.error("Skipping unreadable built-in theme " + folder, e);
			}
		}
		return themes;
	}

	/**
	 * @return the theme folders to offer, falling back to the ones in the jar if discovery itself
	 *         fails - this list feeds the composer menu, so failing here would mean the menu
	 *         cannot be opened at all, which is a far worse outcome than ignoring a bad pack
	 */
	private List<String> discoverBuiltInFolders() {
		try {
			return builtInFolders();
		} catch (RuntimeException e) {
			TheMightyArchitect.logger.error("Could not discover themes; using the ones in the mod jar", e);
			return shippedFolders();
		}
	}

	/**
	 * The theme folders the resource stack provides, best order first.
	 * <p>
	 * The ones the mod ships keep their existing order, because the composer menu binds them to
	 * number keys by list position and a pack adding a theme should not renumber the five people
	 * already know. Anything a pack adds is appended.
	 */
	private List<String> builtInFolders() {
		List<String> shipped = shippedFolders();
		List<String> discovered = ArchitectResources.listFoldersContaining(ArchitectPaths.THEMES, THEME_FILE, shipped);

		List<String> ordered = new ArrayList<>();
		for (String folder : shipped)
			if (discovered.contains(folder))
				ordered.add(folder);
		for (String folder : discovered)
			if (!shipped.contains(folder))
				ordered.add(folder);

		return ordered;
	}

	/** The theme folders the mod itself ships, in the order the menu has always listed them. */
	private static List<String> shippedFolders() {
		List<String> shipped = new ArrayList<>();
		for (IncludedThemes which : IncludedThemes.values())
			shipped.add(which.themeFolder);
		return shipped;
	}

	public DesignTheme builtIn(IncludedThemes which) {
		return builtIn(which.themeFolder);
	}

	public DesignTheme builtIn(String folder) {
		return included.computeIfAbsent(folder, ThemeStorage::loadInternalTheme);
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
	 * <p>
	 * This goes to {@code themes/export/} rather than alongside the theme folders on purpose: the
	 * packed file is a copy for handing to someone else, and the theme scanner skips that folder
	 * so your own theme does not appear twice in the list - once live and editable, once as a
	 * frozen snapshot under an identical name.
	 *
	 * @return where the file went, so the caller can tell the user somewhere they can act on
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

		return ArchitectPaths.describe(target);
	}

	private static DesignTheme loadInternalTheme(String themeFolder) {
		String base = ArchitectPaths.THEMES + "/" + themeFolder + "/";
		DesignTheme theme = JsonStorage.readBuiltIn(base + THEME_FILE, DesignTheme.CODEC)
			.orElseThrow(() -> new IllegalStateException(
				"Built-in theme " + themeFolder + " is missing from the mod jar"));

		// The table only speaks for a theme that did not speak for itself: the five shipped files
		// predate HeightSequence, but a pack overriding one of them and declaring its own floors
		// has to win.
		if (!theme.declaresHeightSequence())
			IncludedThemes.byFolder(themeFolder)
				.ifPresent(which -> theme.withHeightSequence(which.heights));

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
