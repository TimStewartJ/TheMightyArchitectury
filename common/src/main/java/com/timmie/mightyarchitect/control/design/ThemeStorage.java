package com.timmie.mightyarchitect.control.design;

import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

public class ThemeStorage {

	public enum IncludedThemes {

		Medieval("medieval", 3, 5),
		Fallback("fallback_theme", 3, 4),
		Modern("modern", 2, 4),
		TownHouse("town_house", 4, 5),
		Cattingham("cattingham_palace", 7, 2, 6);

		public DesignTheme theme;
		public String themeFolder;
		public List<Integer> heights;

		private IncludedThemes(String themeFolder, Integer... floorHeights) {
			this.themeFolder = themeFolder;
			this.heights = Arrays.asList(floorHeights);
		}
	}

	private static List<DesignTheme> importedThemes;
	private static List<DesignTheme> createdThemes;

	public static List<DesignTheme> getAllThemes() {
		List<DesignTheme> themes = new ArrayList<>(getIncluded());
		themes.addAll(getImported());
		return themes;
	}

	public static List<DesignTheme> getIncluded() {
		List<DesignTheme> included = new ArrayList<>();
		for (IncludedThemes theme : IncludedThemes.values()) {

			if (theme.theme == null)
				theme.theme = loadInternalTheme(theme.themeFolder).withHeightSequence(theme.heights);

			if (theme == IncludedThemes.Fallback)
				continue;

			included.add(theme.theme);
		}
		return included;
	}

	public static List<DesignTheme> getImported() {
		if (importedThemes == null)
			importThemes();

		return importedThemes;
	}

	public static List<DesignTheme> getCreated() {
		if (createdThemes == null)
			importThemes();

		return createdThemes;
	}

	public static void reloadExternal() {
		importedThemes = null;
		createdThemes = null;
	}

	public static DesignTheme createTheme(String name) {
		if (name.isEmpty())
			name = "My Theme";
		DesignTheme theme = new DesignTheme(name, Minecraft.getInstance().player.getName()
			.getString());
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

	public static void exportTheme(DesignTheme theme) {
		String folderPath = "themes";
		FilesHelper.createFolderIfMissing(folderPath);

		String foldername = theme.getFilePath();
		FilesHelper.createFolderIfMissing(folderPath + "/" + foldername);

		String filepath = folderPath + "/" + foldername + "/theme.json";
		FilesHelper.saveTagCompoundAsJson(theme.asTagCompound(), filepath);

		String palettePath = folderPath + "/" + foldername + "/palette.json";
		FilesHelper.saveTagCompoundAsJson(theme.getDefaultPalette()
			.writeToNBT(new CompoundTag()), palettePath);

		String palette2Path = folderPath + "/" + foldername + "/palette2.json";
		FilesHelper.saveTagCompoundAsJson(theme.getDefaultSecondaryPalette()
			.writeToNBT(new CompoundTag()), palette2Path);
	}

	public static String exportThemeFullyAsFile(DesignTheme theme, boolean compressed) {
		String folderPath = "themes/export";
		FilesHelper.createFolderIfMissing(folderPath);
		CompoundTag massiveThemeTag = new CompoundTag();

		massiveThemeTag.put("Theme", theme.asTagCompound());
		massiveThemeTag.put("Palette", theme.getDefaultPalette()
			.writeToNBT(new CompoundTag()));
		massiveThemeTag.put("SecondaryPalette", theme.getDefaultSecondaryPalette()
			.writeToNBT(new CompoundTag()));

		Map<DesignLayer, Map<DesignType, Set<CompoundTag>>> designFiles =
			DesignResourceLoader.loadThemeFromFolder(theme);

		CompoundTag layers = new CompoundTag();
		for (DesignLayer layer : theme.getLayers()) {
			if (!designFiles.containsKey(layer))
				continue;

			CompoundTag types = new CompoundTag();
			for (DesignType type : theme.getTypes()) {
				if (!designFiles.get(layer)
					.containsKey(type))
					continue;

				ListTag designs = new ListTag();
				for (CompoundTag tag : designFiles.get(layer)
					.get(type))
					designs.add(tag);
				types.put(type.name(), designs);
			}
			layers.put(layer.name(), types);
		}
		massiveThemeTag.put("Designs", layers);

		if (compressed) {
			Path path = Paths.get(folderPath + "/" + theme.getFilePath() + ".theme");
			FilesHelper.writeAtomically(path, out -> NbtIo.writeCompressed(massiveThemeTag, out));
		} else {
			FilesHelper.saveTagCompoundAsJsonCompact(massiveThemeTag, folderPath + "/" + theme.getFilePath() + ".json");
		}

		return theme.getFilePath() + (compressed ? ".theme" : ".json");
	}

	public static DesignTheme importThemeFullyFromFile(String path) {
		return null;
	}

	private static DesignTheme loadInternalTheme(String themeFolder) {
		CompoundTag themeCompound = FilesHelper.loadJsonResourceAsNBT("themes/" + themeFolder + "/theme.json");
		CompoundTag paletteCompound = FilesHelper.loadJsonResourceAsNBT("themes/" + themeFolder + "/palette.json");
		CompoundTag palette2Compound = FilesHelper.loadJsonResourceAsNBT("themes/" + themeFolder + "/palette2.json");
		DesignTheme theme = DesignTheme.fromNBT(themeCompound);
		if (theme == null)
			throw new IllegalStateException("Built-in theme " + themeFolder + " is missing from the mod jar");
		theme.setFilePath(themeFolder);
		theme.setImported(false);
		theme.setDefaultPalette(PaletteDefinition.fromNBT(paletteCompound));
		theme.setDefaultSecondaryPalette(PaletteDefinition.fromNBT(palette2Compound));
		return theme;
	}

	private static void importThemes() {
		importedThemes = new ArrayList<>();
		createdThemes = new ArrayList<>();
		String folderPath = "themes";

		FilesHelper.createFolderIfMissing(folderPath);

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(folderPath))) {
			for (Path path : directoryStream) {
				String themeFolder = path.getFileName()
					.toString();

				if (themeFolder.equals("export"))
					continue;

				// Per theme, not per scan: an unknown layer name or a malformed file used to throw
				// out of the loop and drop every theme after it from the list.
				try {
					importTheme(folderPath, themeFolder);
				} catch (RuntimeException e) {
					TheMightyArchitect.logger.error("Skipping unreadable theme " + themeFolder, e);
				}
			}
		} catch (IOException e) {
			TheMightyArchitect.logger.error("Could not list themes in " + folderPath, e);
		}
	}

	private static void importTheme(String folderPath, String themeFolder) {
		CompoundTag themeCompound;
		CompoundTag paletteCompound;
		CompoundTag secondaryPaletteCompound = null;

		boolean packedIntoOneFile = themeFolder.endsWith(".theme") || themeFolder.endsWith(".json");

		if (packedIntoOneFile) {
			CompoundTag themeFile = themeFolder.endsWith(".theme")
				? readCompressedTheme(folderPath + "/" + themeFolder)
				: FilesHelper.loadJsonAsNBT(folderPath + "/" + themeFolder);

			if (themeFile == null)
				return;

			//? if >=1.21.6 {
			themeCompound = themeFile.getCompound("Theme").orElse(new CompoundTag());
			//?} else {
			/*themeCompound = themeFile.getCompound("Theme");*///?}
			//? if >=1.21.6 {
			paletteCompound = themeFile.getCompound("Palette").orElse(new CompoundTag());
			//?} else {
			/*paletteCompound = themeFile.getCompound("Palette");*///?}
			if (themeFile.contains("SecondaryPalette"))
				//? if >=1.21.6 {
				secondaryPaletteCompound = themeFile.getCompound("SecondaryPalette").orElse(new CompoundTag());
				//?} else {
				/*secondaryPaletteCompound = themeFile.getCompound("SecondaryPalette");*///?}

		} else {
			themeCompound = FilesHelper.loadJsonAsNBT(folderPath + "/" + themeFolder + "/theme.json");
			paletteCompound = FilesHelper.loadJsonAsNBT(folderPath + "/" + themeFolder + "/palette.json");
			secondaryPaletteCompound = FilesHelper.loadJsonAsNBT(folderPath + "/" + themeFolder + "/palette2.json");
		}

		if (themeCompound == null)
			return;

		DesignTheme theme = DesignTheme.fromNBT(themeCompound);
		theme.setFilePath(themeFolder);
		theme.setImported(true);
		theme.setDefaultPalette(PaletteDefinition.fromNBT(paletteCompound));

		if (secondaryPaletteCompound != null)
			theme.setDefaultSecondaryPalette(PaletteDefinition.fromNBT(secondaryPaletteCompound));
		else
			// Cloned, so that editing the secondary palette does not also edit the primary one.
			theme.setDefaultSecondaryPalette(theme.getDefaultPalette()
				.clone());

		importedThemes.add(theme);
		if (!packedIntoOneFile)
			createdThemes.add(theme);
	}

	private static CompoundTag readCompressedTheme(String path) {
		try (InputStream inputStream = Files.newInputStream(Paths.get(path), StandardOpenOption.READ)) {
			//? if >=1.20.3 {
			return NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
			//?} else {
			/*return NbtIo.readCompressed(inputStream);
			*///?}
		} catch (IOException e) {
			TheMightyArchitect.logger.error("Could not read theme " + path, e);
			return null;
		}
	}
}
