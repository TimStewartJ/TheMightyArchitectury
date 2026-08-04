package com.timmie.mightyarchitect.control.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.timmie.mightyarchitect.control.design.DesignLayer;
import com.timmie.mightyarchitect.control.design.DesignTheme;
import com.timmie.mightyarchitect.control.design.DesignType;
import com.timmie.mightyarchitect.control.design.partials.DesignData;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A whole theme in one file - the {@code .theme} and single-file {@code .json} exports.
 * <p>
 * Both spellings carry the same tree; the only difference is that one is gzipped NBT and the other
 * is JSON, which is precisely the split a codec exists to erase. Before this the two were written
 * by different code paths against different serializers, and the JSON one went out through the SNBT
 * detour, so a design that survived the compressed export could still come back with its layer
 * counts turned into strings from the uncompressed one.
 * <p>
 * The layer and type keys stay strings rather than becoming enums in the schema: an archive written
 * by a newer build can name a layer this one has never heard of, and that has to cost the archive
 * one layer rather than the whole import.
 */
public record PackedTheme(DesignTheme theme, PaletteDefinition palette,
	Optional<PaletteDefinition> secondaryPalette, Map<String, Map<String, List<DesignData>>> designs) {

	public static final String COMPRESSED_EXTENSION = ".theme";
	public static final String JSON_EXTENSION = ".json";

	private static final Codec<Map<String, Map<String, List<DesignData>>>> DESIGNS =
		Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.list(DesignData.CODEC)));

	public static final Codec<PackedTheme> CODEC = RecordCodecBuilder.create(instance -> instance
		.group(DesignTheme.CODEC.fieldOf("Theme")
			.forGetter(PackedTheme::theme),
			// Both palettes are stored as whole palette files, so they keep their own outer
			// "Palette" object here - which is what PaletteDefinition.CODEC already expects.
			PaletteDefinition.CODEC.fieldOf("Palette")
				.forGetter(PackedTheme::palette),
			PaletteDefinition.CODEC.optionalFieldOf("SecondaryPalette")
				.forGetter(PackedTheme::secondaryPalette),
			DESIGNS.optionalFieldOf("Designs", Map.of())
				.forGetter(PackedTheme::designs))
		.apply(instance, PackedTheme::new));

	/** @return whether that filename is a packed theme rather than a theme folder */
	public static boolean isPackedName(String name) {
		return name.endsWith(COMPRESSED_EXTENSION) || name.endsWith(JSON_EXTENSION);
	}

	/**
	 * Reads a packed theme, picking the format from the extension.
	 *
	 * @return the archive, or empty when it is missing, unreadable or not a theme
	 */
	public static Optional<PackedTheme> read(Path path) {
		String name = path.getFileName()
			.toString();
		if (name.endsWith(COMPRESSED_EXTENSION))
			return JsonStorage.readCompressed(path)
				.flatMap(tag -> JsonStorage.fromNbt(CODEC, tag, path.toString()));
		return JsonStorage.read(path, CODEC);
	}

	/** @return the designs of one layer and type, or an empty list when the archive has none */
	public List<DesignData> designsFor(DesignLayer layer, DesignType type) {
		Map<String, List<DesignData>> byType = designs.get(layer.name());
		if (byType == null)
			return List.of();
		List<DesignData> found = byType.get(type.name());
		return found == null ? List.of() : found;
	}

	/** Groups designs by layer and type name, in the order the theme declares them. */
	public static Map<String, Map<String, List<DesignData>>> group(DesignTheme theme,
		Map<DesignLayer, Map<DesignType, List<DesignData>>> designs) {
		Map<String, Map<String, List<DesignData>>> grouped = new LinkedHashMap<>();

		for (DesignLayer layer : theme.getLayers()) {
			Map<DesignType, List<DesignData>> byType = designs.get(layer);
			if (byType == null)
				continue;

			Map<String, List<DesignData>> types = new LinkedHashMap<>();
			for (DesignType type : theme.getTypes()) {
				List<DesignData> found = byType.get(type);
				if (found != null)
					types.put(type.name(), found);
			}
			grouped.put(layer.name(), types);
		}

		return grouped;
	}
}
