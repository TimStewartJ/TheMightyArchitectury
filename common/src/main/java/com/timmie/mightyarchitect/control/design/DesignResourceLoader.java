package com.timmie.mightyarchitect.control.design;

import com.timmie.mightyarchitect.control.design.partials.Design;
import com.timmie.mightyarchitect.control.design.partials.DesignData;
import com.timmie.mightyarchitect.control.storage.ArchitectPaths;
import com.timmie.mightyarchitect.control.storage.ArchitectResources;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import com.timmie.mightyarchitect.control.storage.PackedTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DesignResourceLoader {

	private static final String BASE_PATH = ArchitectPaths.THEMES;

	public static Map<DesignLayer, Map<DesignType, Set<Design>>> loadDesignsForTheme(DesignTheme theme) {
		final Map<DesignLayer, Map<DesignType, Set<Design>>> designMap = new HashMap<>();

		if (!theme.isImported())
			forEachSet(theme, (layer, type) -> add(designMap, layer, type,
				instantiate(type, loadBuiltInDesigns(theme, layer, type))));

		// Merged per design set, not per layer. putAll replaced whole layers, so as soon as a
		// folder existed under themes/<builtin>/ - which exporting a single design creates - every
		// built-in design in that layer disappeared behind the handful of exported ones.
		loadExternalDesignData(theme).forEach((layer, byType) -> byType
			.forEach((type, data) -> add(designMap, layer, type, instantiate(type, data))));

		return designMap;
	}

	public static Map<DesignLayer, Map<DesignType, Set<Design>>> loadExternalDesignsForTheme(DesignTheme theme) {
		final Map<DesignLayer, Map<DesignType, Set<Design>>> designMap = new HashMap<>();
		loadExternalDesignData(theme).forEach((layer, byType) -> byType
			.forEach((type, data) -> add(designMap, layer, type, instantiate(type, data))));
		return designMap;
	}

	/**
	 * Every design the user has, whether the theme is a folder or a single packed file.
	 * <p>
	 * Kept in decoded-but-not-instantiated form because that is what the single-file export writes
	 * back out: re-encoding a {@link Design} would mean a second serializer for the same schema,
	 * which is how the two export formats drifted apart in the first place.
	 */
	public static Map<DesignLayer, Map<DesignType, List<DesignData>>> loadExternalDesignData(DesignTheme theme) {
		return PackedTheme.isPackedName(theme.getFilePath()) ? loadFromPackedFile(theme) : loadThemeFromFolder(theme);
	}

	private static Map<DesignLayer, Map<DesignType, List<DesignData>>> loadFromPackedFile(DesignTheme theme) {
		final Map<DesignLayer, Map<DesignType, List<DesignData>>> designMap = new LinkedHashMap<>();

		Optional<PackedTheme> packed =
			PackedTheme.read(ArchitectPaths.findAcrossRoots(BASE_PATH, theme.getFilePath()));
		// A missing or malformed archive yields no designs rather than an exception that takes the
		// whole theme list down with it.
		if (!packed.isPresent())
			return designMap;

		forEachSet(theme, (layer, type) -> {
			List<DesignData> designs = packed.get()
				.designsFor(layer, type);
			if (!designs.isEmpty())
				designMap.computeIfAbsent(layer, missing -> new LinkedHashMap<>())
					.put(type, designs);
		});

		return designMap;
	}

	public static Map<DesignLayer, Map<DesignType, List<DesignData>>> loadThemeFromFolder(DesignTheme theme) {
		final Map<DesignLayer, Map<DesignType, List<DesignData>>> designMap = new LinkedHashMap<>();

		forEachSet(theme, (layer, type) -> {
			List<DesignData> designs = loadExternalDesigns(theme, layer, type);
			if (!designs.isEmpty())
				designMap.computeIfAbsent(layer, missing -> new LinkedHashMap<>())
					.put(type, designs);
		});

		return designMap;
	}

	/** The designs shipped inside the mod, which a resource pack may now override or extend. */
	private static List<DesignData> loadBuiltInDesigns(DesignTheme theme, DesignLayer layer, DesignType type) {
		List<DesignData> designs = new ArrayList<>();
		for (String path : ArchitectResources.list(designFolder(theme, layer, type),
			index -> "design" + (index == 0 ? "" : "_" + index) + ".json"))
			JsonStorage.readBuiltIn(path, DesignData.CODEC)
				.ifPresent(designs::add);
		return designs;
	}

	/** The designs the user exported, from the mod folder or the pre-migration one. */
	private static List<DesignData> loadExternalDesigns(DesignTheme theme, DesignLayer layer, DesignType type) {
		List<DesignData> designs = new ArrayList<>();
		for (Path file : ArchitectPaths.listAcrossRoots(designFolder(theme, layer, type)))
			if (Files.isRegularFile(file))
				JsonStorage.read(file, DesignData.CODEC)
					.ifPresent(designs::add);
		return designs;
	}

	/** The same relative path inside the jar and inside the mod folder, deliberately. */
	static String designFolder(DesignTheme theme, DesignLayer layer, DesignType type) {
		return BASE_PATH + "/" + theme.getFilePath() + "/" + layer.getFilePath() + "/" + type.getFilePath();
	}

	private static Set<Design> instantiate(DesignType type, List<DesignData> data) {
		Set<Design> designs = new HashSet<>();
		for (DesignData design : data)
			designs.add(type.getDesign()
				.fromData(design));
		return designs;
	}

	private static void add(Map<DesignLayer, Map<DesignType, Set<Design>>> designMap, DesignLayer layer,
		DesignType type, Set<Design> designs) {
		designMap.computeIfAbsent(layer, missing -> new HashMap<>())
			.computeIfAbsent(type, missing -> new HashSet<>())
			.addAll(designs);
	}

	private static void forEachSet(DesignTheme theme, DesignSetVisitor visitor) {
		for (DesignLayer layer : theme.getLayers())
			for (DesignType type : theme.getTypes())
				visitor.visit(layer, type);
	}

	@FunctionalInterface
	private interface DesignSetVisitor {
		void visit(DesignLayer layer, DesignType type);
	}

}
