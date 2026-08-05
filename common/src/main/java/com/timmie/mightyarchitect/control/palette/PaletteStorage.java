package com.timmie.mightyarchitect.control.palette;

import com.timmie.mightyarchitect.control.storage.ArchitectPaths;
import com.timmie.mightyarchitect.control.storage.ArchitectResources;
import com.timmie.mightyarchitect.control.storage.ArchitectStorage;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The palettes available to the composer: the ones inside the mod, and the ones on disk.
 * <p>
 * The built-in set used to be found by probing {@code palettes/p0.json}, {@code p1.json} and so on
 * until one was missing, which is what reading them off the raw classpath forced - a classloader
 * cannot list a directory. Going through the resource system means asking for the folder, so a
 * resource pack can now add a palette rather than only replace one that already exists.
 */
public class PaletteStorage {

	private static final String BUILT_IN_FOLDER = ArchitectPaths.PALETTES;

	private Map<String, PaletteDefinition> palettes;
	private Map<String, PaletteDefinition> resourcePalettes;

	// ---------------------------------------------------------------- static facade

	public static PaletteDefinition getRandomPalette() {
		return ArchitectStorage.palettes()
			.randomUserPalette();
	}

	public static PaletteDefinition getPalette(String name) {
		return ArchitectStorage.palettes()
			.byName(name);
	}

	public static List<String> getPaletteNames() {
		return ArchitectStorage.palettes()
			.userPaletteNames();
	}

	public static List<String> getResourcePaletteNames() {
		return ArchitectStorage.palettes()
			.builtInPaletteNames();
	}

	public static void exportPalette(PaletteDefinition palette) {
		ArchitectStorage.palettes()
			.save(palette);
	}

	public static void loadAllPalettes() {
		ArchitectStorage.palettes()
			.invalidate();
	}

	// ---------------------------------------------------------------- instance

	/** Drops both sets, so the next request rereads them. */
	public void invalidate() {
		palettes = null;
		resourcePalettes = null;
	}

	public PaletteDefinition randomUserPalette() {
		List<String> names = userPaletteNames();
		if (names.isEmpty())
			return PaletteDefinition.defaultPalette()
				.clone();
		return palettes.get(names.get(new Random().nextInt(names.size())));
	}

	/** @return the palette under that name, from either set, or null when there is none */
	public PaletteDefinition byName(String name) {
		load();
		PaletteDefinition palette = palettes.get(name);
		return palette != null ? palette : resourcePalettes.get(name);
	}

	public List<String> userPaletteNames() {
		load();
		return new ArrayList<>(palettes.keySet());
	}

	public List<String> builtInPaletteNames() {
		load();
		return new ArrayList<>(resourcePalettes.keySet());
	}

	/** Writes a palette into the mod folder under a filename derived from its name. */
	public void save(PaletteDefinition palette) {
		Path folder = ArchitectPaths.palettes();
		FilesHelper.createFolderIfMissing(folder);
		String filename = FilesHelper.findFirstValidFilename(palette.getName(), folder, "json");
		JsonStorage.write(folder.resolve(filename), PaletteDefinition.CODEC, palette);
	}

	private void load() {
		if (palettes != null)
			return;

		// Insertion-ordered: the picker renders these in list order, and a HashMap moved the
		// buttons around between launches.
		resourcePalettes = new LinkedHashMap<>();
		for (String path : ArchitectResources.list(BUILT_IN_FOLDER, index -> "p" + index + ".json"))
			JsonStorage.readBuiltIn(path, PaletteDefinition.CODEC)
				.ifPresent(palette -> resourcePalettes.put(palette.getName(), palette));

		palettes = new LinkedHashMap<>();
		for (Path file : ArchitectPaths.listAcrossRoots(ArchitectPaths.PALETTES))
			if (Files.isRegularFile(file))
				// One unreadable palette file used to NPE here and abort loading all the others.
				JsonStorage.read(file, PaletteDefinition.CODEC)
					.ifPresent(palette -> palettes.put(palette.getName(), palette));
	}

}
