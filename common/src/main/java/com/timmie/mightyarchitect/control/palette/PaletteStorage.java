package com.timmie.mightyarchitect.control.palette;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class PaletteStorage {

	private static Map<String, PaletteDefinition> palettes;
	private static Map<String, PaletteDefinition> resourcePalettes;

	public static PaletteDefinition getRandomPalette() {
		if (palettes == null)
			loadAllPalettes();
		Random random = new Random();
		List<String> names = new ArrayList<>(palettes.keySet());
		PaletteDefinition palette = palettes.get(names.get(random.nextInt(names.size())));
		return palette;
	}

	public static PaletteDefinition getPalette(String name) {
		if (palettes == null)
			loadAllPalettes();
		if (palettes.containsKey(name))
			return palettes.get(name);
		else
			return resourcePalettes.get(name);
	}

	public static List<String> getPaletteNames() {
		if (palettes == null)
			loadAllPalettes();
		return new ArrayList<>(palettes.keySet());
	}

	public static List<String> getResourcePaletteNames() {
		if (resourcePalettes == null)
			loadAllPalettes();
		return new ArrayList<>(resourcePalettes.keySet());
	}

	public static void exportPalette(PaletteDefinition palette) {
		String folderPath = "palettes";
		FilesHelper.createFolderIfMissing(folderPath);
		String filename = FilesHelper.findFirstValidFilename(palette.getName(), folderPath, "json");
		String filepath = folderPath + "/" + filename;
		FilesHelper.saveTagCompoundAsJson(palette.writeToNBT(new CompoundTag()), filepath);
	}

	public static PaletteDefinition importPalette(Path path) {
		try (JsonReader reader = new JsonReader(Files.newBufferedReader(path))) {
			reader.setLenient(true);
			JsonElement element = Streams.parse(reader);
			//? if >=1.21.6 {
			return PaletteDefinition.fromNBT(TagParser.create(net.minecraft.nbt.NbtOps.INSTANCE).parseCompoundFully(element.toString()));
			//?} else {
			/*return PaletteDefinition.fromNBT(TagParser.parseTag(element.toString()));
			*///?}
		} catch (Exception e) {
			// Exception rather than the exact types: the parser throws a checked
			// CommandSyntaxException before 1.21.6 and an unchecked parse error after it.
			TheMightyArchitect.logger.error("Could not read palette " + path, e);
		}
		return null;
	}

	public static void loadAllPalettes() {
		palettes = new HashMap<>();
		resourcePalettes = new HashMap<>();
		loadResourcePalettes();
		try (Stream<Path> files = Files.list(Paths.get("palettes/"))) {
			files.forEach(PaletteStorage::loadPalette);
		} catch (NoSuchFileException e) {
			// No palettes created yet
		} catch (IOException e) {
			TheMightyArchitect.logger.error("Could not list palettes", e);
		}
	}

	public static void loadPalette(Path path) {
		PaletteDefinition palette = importPalette(path);
		// One unreadable palette file used to NPE here and abort loading all the others.
		if (palette == null)
			return;
		palettes.put(palette.getName(), palette);
	}

	public static void loadResourcePalettes() {
		int index = 0;
		while (index < 2048) {
			String path = "palettes/p" + index + ".json";
			if (TheMightyArchitect.class.getClassLoader().getResource(path) == null)
				break;
			CompoundTag tag = FilesHelper.loadJsonResourceAsNBT(path);
			if (tag != null) {
				PaletteDefinition paletteDefinition = PaletteDefinition.fromNBT(tag);
				resourcePalettes.put(paletteDefinition.getName(), paletteDefinition);
			}
			index++;
		}
	}

}
