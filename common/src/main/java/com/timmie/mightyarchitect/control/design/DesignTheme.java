package com.timmie.mightyarchitect.control.design;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.design.partials.Design;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

public class DesignTheme {

	private static final List<Integer> defaultHeightSequence = ImmutableList.of(2, 4);

	/**
	 * A theme's metadata file.
	 * <p>
	 * The field names are the ones the format has always used, spaces and all, so a theme.json
	 * written by any previous build reads unchanged - the schema was always expressible, it just
	 * was not written down anywhere a reader could check it against.
	 */
	public static final Codec<DesignTheme> CODEC = RecordCodecBuilder.create(instance -> instance
		.group(Codec.STRING.optionalFieldOf("Name", "")
			.forGetter(DesignTheme::getDisplayName),
			Codec.STRING.optionalFieldOf("Designer", "")
				.forGetter(DesignTheme::getDesigner),
			enumNames(DesignLayer.class).optionalFieldOf("Layers", List.of())
				.forGetter(theme -> orEmpty(theme.layers)),
			enumNames(DesignType.class).optionalFieldOf("Types", List.of())
				.forGetter(theme -> orEmpty(theme.types)),
			Codec.INT.optionalFieldOf("Maximum Room Height", 10)
				.forGetter(DesignTheme::getMaxFloorHeight))
		.apply(instance, DesignTheme::fromParts));

	private String filePath;
	private String displayName;
	private String designer;
	private DesignPicker designPicker;
	private boolean imported;
	private PaletteDefinition defaultPalette;
	private PaletteDefinition defaultSecondaryPalette;
	private ThemeStatistics statistics;
	private int maxFloorHeight;
	private List<Integer> heightSequence;

	private List<DesignLayer> roomLayers;
	private List<DesignLayer> layers;
	private List<DesignType> types;
	private Map<DesignLayer, Map<DesignType, Set<Design>>> designs;

	public DesignTheme(String displayName, String designer) {
		this.designer = designer;
		this.displayName = displayName;
		this.designPicker = new DesignPicker();
		this.designPicker.setTheme(this);
		imported = false;
		maxFloorHeight = 10;
		heightSequence = defaultHeightSequence;
	}

	public DesignTheme withLayers(DesignLayer... designLayers) {
		layers = ImmutableList.copyOf(designLayers);
		updateRoomLayers();
		return this;
	}

	public DesignTheme withHeightSequence(List<Integer> seq) {
		this.heightSequence = seq;
		return this;
	}

	protected void updateRoomLayers() {
		roomLayers = new ArrayList<>();
		roomLayers.addAll(layers);
		roomLayers.remove(DesignLayer.Roofing);
	}

	public DesignTheme withTypes(DesignType... designtypes) {
		types = ImmutableList.copyOf(designtypes);
		return this;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getDefaultHeightForFloor(int floor) {
		return heightSequence.size() <= floor ? -1 : heightSequence.get(floor);
	}

	public DesignPicker getDesignPicker() {
		return designPicker;
	}

	public List<DesignLayer> getLayers() {
		return layers;
	}

	public List<DesignType> getTypes() {
		return types;
	}

	public boolean isImported() {
		return imported;
	}

	public Set<Design> getDesigns(DesignLayer designLayer, DesignType designType) {
		if (designs == null) {
			initDesigns();
		}

		if (designs.containsKey(designLayer)) {
			Map<DesignType, Set<Design>> typeMap = designs.get(designLayer);

			if (typeMap.containsKey(designType)) {
				return typeMap.get(designType);
			}
		}

		return new HashSet<>();
	}

	protected void initDesigns() {
		designs = DesignResourceLoader.loadDesignsForTheme(this);
		statistics = ThemeStatistics.evaluate(this);
	}

	public ThemeStatistics getStatistics() {
		if (designs == null) {
			initDesigns();
		}
		return statistics;
	}

	public void clearDesigns() {
		designs = null;
	}

	public void setDesigner(String designer) {
		this.designer = designer;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDesigner() {
		return designer;
	}

	public void setLayers(List<DesignLayer> layers) {
		this.layers = layers;
		updateRoomLayers();
	}

	public void setTypes(List<DesignType> types) {
		this.types = types;
	}

	/** @return the theme as NBT, for the packed single-file export */
	public CompoundTag asTagCompound() {
		return JsonStorage.toNbt(CODEC, this)
			.orElseGet(CompoundTag::new);
	}

	/** @return the theme the compound describes, or null when it does not describe one */
	public static DesignTheme fromNBT(CompoundTag compound) {
		if (compound == null)
			return null;
		return JsonStorage.fromNbt(CODEC, compound, "a theme")
			.orElse(null);
	}

	private static DesignTheme fromParts(String name, String designer, List<DesignLayer> layers,
		List<DesignType> types, int maxFloorHeight) {
		DesignTheme theme = new DesignTheme(name, designer);
		theme.layers = new ArrayList<>(layers);
		theme.types = new ArrayList<>(types);
		theme.maxFloorHeight = maxFloorHeight;
		theme.updateRoomLayers();
		return theme;
	}

	private static <E> List<E> orEmpty(List<E> list) {
		return list == null ? List.of() : list;
	}

	/**
	 * A list of enum constants by name, skipping any this build does not have.
	 * <p>
	 * A theme written by a newer version of the mod, or simply by hand, can name a layer or type
	 * this build does not know. {@code valueOf} threw for it, and the throw escaped the single try
	 * block around the whole theme directory scan - so one such file emptied the theme list instead
	 * of costing that theme one layer.
	 */
	private static <E extends Enum<E>> Codec<List<E>> enumNames(Class<E> type) {
		return Codec.list(Codec.STRING)
			.xmap(names -> {
				List<E> known = new ArrayList<>(names.size());
				for (String name : names) {
					try {
						known.add(Enum.valueOf(type, name));
					} catch (IllegalArgumentException unknown) {
						TheMightyArchitect.logger.warn("Ignoring unknown {} '{}' in a theme", type.getSimpleName(),
							name);
					}
				}
				return known;
			}, constants -> {
				List<String> names = new ArrayList<>(constants.size());
				for (E constant : constants)
					names.add(constant.name());
				return names;
			});
	}

	public void setImported(boolean imported) {
		this.imported = imported;
	}

	public PaletteDefinition getDefaultPalette() {
		return defaultPalette;
	}

	public void setDefaultPalette(PaletteDefinition defaultPalette) {
		this.defaultPalette = defaultPalette;
	}

	public PaletteDefinition getDefaultSecondaryPalette() {
		return defaultSecondaryPalette;
	}

	public void setDefaultSecondaryPalette(PaletteDefinition defaultSecondaryPalette) {
		this.defaultSecondaryPalette = defaultSecondaryPalette;
	}

	public List<DesignLayer> getRoomLayers() {
		return roomLayers;
	}

	public int getMaxFloorHeight() {
		return maxFloorHeight;
	}

	public void setMaxFloorHeight(int maxFloorHeight) {
		this.maxFloorHeight = maxFloorHeight;
	}

}
