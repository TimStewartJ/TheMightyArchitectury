package com.timmie.mightyarchitect.control.design;

import com.timmie.mightyarchitect.AllBlocks;
import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.block.SliceMarkerBlock;
import com.timmie.mightyarchitect.control.compose.Cuboid;
import com.timmie.mightyarchitect.control.design.DesignSlice.DesignSliceTrait;
import com.timmie.mightyarchitect.control.design.DesignSlice.SliceData;
import com.timmie.mightyarchitect.control.design.partials.DesignData;
import com.timmie.mightyarchitect.control.design.partials.Wall.ExpandBehaviour;
import com.timmie.mightyarchitect.control.palette.BlockOrientation;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.phase.export.PhaseEditTheme;
import com.timmie.mightyarchitect.control.storage.ArchitectPaths;
import com.timmie.mightyarchitect.control.storage.ArchitectResources;
import com.timmie.mightyarchitect.control.storage.ArchitectStorage;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Scans a marked-out build in the world and writes it back as a design file.
 * <p>
 * The composer's export state - which theme, layer and type is being edited, and the palette being
 * scanned against - used to be public static fields with no owner, so it survived disconnecting and
 * reconnecting into a different world with a theme that no longer existed. It now lives on an
 * instance {@link ArchitectStorage} can discard; the static accessors below keep the call sites
 * unchanged in shape.
 */
public class DesignExporter {

	// A clone, not defaultPalette() itself: the scanning palette is edited in place by the theme
	// editor, and handing out the shared default would let that edit rewrite the palette every
	// other theme starts from.
	private PaletteDefinition scanningPalette = PaletteDefinition.defaultPalette()
		.clone();

	private DesignTheme theme;
	private DesignType type;
	private DesignLayer layer;

	private int designParameter;

	private boolean changed = true;

	private static DesignExporter get() {
		return ArchitectStorage.designExporter();
	}

	public static PaletteDefinition getScanningPalette() {
		return get().scanningPalette;
	}

	public static void setScanningPalette(PaletteDefinition palette) {
		get().scanningPalette = palette;
	}

	public static DesignTheme getTheme() {
		return get().theme;
	}

	public static DesignType getType() {
		return get().type;
	}

	public static void setType(DesignType type) {
		get().type = type;
	}

	public static DesignLayer getLayer() {
		return get().layer;
	}

	public static void setLayer(DesignLayer layer) {
		get().layer = layer;
	}

	public static int getDesignParameter() {
		return get().designParameter;
	}

	public static void setDesignParameter(int designParameter) {
		get().designParameter = designParameter;
	}

	public static void markChanged() {
		get().changed = true;
	}

	public static String exportDesign(Level worldIn, BlockPos anchor) {
		return get().export(worldIn, anchor);
	}

	private String export(Level worldIn, BlockPos anchor) {
		BlockPos layerDefAnchor = anchor;
		boolean found = false;
		for (int range = 1; range < 100 && !found; range++) {
			for (int i = 0; i <= range; i++) {
				if (isMarker(worldIn, anchor.offset(range, 0, i))) {
					layerDefAnchor = anchor.offset(range, 0, i);
					found = true;
					break;
				} else if (isMarker(worldIn, anchor.offset(i, 0, range))) {
					layerDefAnchor = anchor.offset(i, 0, range);
					found = true;
					break;
				}
			}
		}

		if (!found) {
			return "";
		}

		// Collect information
		int height = 0;
		int effectiveHeight = 0;
		for (BlockPos pos = layerDefAnchor; isMarker(worldIn, pos); pos = pos.above()) {
			height++;
			if (DesignSliceTrait.values()[markerValueAt(worldIn, pos)] != DesignSliceTrait.MaskAbove)
				effectiveHeight++;
		}

		if (effectiveHeight != PhaseEditTheme.effectiveHeight) {
			PhaseEditTheme.effectiveHeight = effectiveHeight;
			changed = true;
		}

		BlockPos size = layerDefAnchor.west()
			.subtract(anchor.east())
			.offset(1, height, 1);

		boolean visualizing = PhaseEditTheme.isVisualizing();
		Cuboid bounds = new Cuboid(anchor.east(), size);
		boolean boundsChanged = visualizing && !PhaseEditTheme.selectedDesign.equals(bounds) || changed;

		changed = false;

		if (!visualizing || boundsChanged) {
			PhaseEditTheme.setVisualization(bounds);
			return "Design traits visualized, click again to confirm.";
		}

		PhaseEditTheme.resetVisualization();

		// Assemble the design
		List<SliceData> layers = new ArrayList<>();

		for (int y = 0; y < size.getY(); y++) {
			DesignSliceTrait trait = DesignSliceTrait.values()[markerValueAt(worldIn, layerDefAnchor.above(y))];

			StringBuilder data = new StringBuilder();
			for (int z = 0; z < size.getZ(); z++) {
				for (int x = 0; x < size.getX(); x++) {
					BlockPos pos = anchor.east()
						.offset(x, y, z);
					BlockState blockState = worldIn.getBlockState(pos);
					Palette block = scanningPalette.scan(blockState);

					if (block == null && blockState.getBlock() != Blocks.AIR) {
						//? if >=26 {
						Minecraft.getInstance().player.sendSystemMessage(Component.literal(blockState.getBlock()
						//?} else {
						/*Minecraft.getInstance().player.displayClientMessage(
							Component.literal(blockState.getBlock()
						*///?}
							.getDescriptionId() + " @" + pos.getX() + "," + pos.getY() + "," + pos.getZ()
							//? if >=26 {
							+ " does not belong to the Scanner Palette"));
							//?} else {
							/*+ " does not belong to the Scanner Palette"), false);
							*///?}
						return "Export failed";
					}

					data.append(block != null ? block.asChar() : ' ');
				}
				if (z < size.getZ() - 1)
					data.append(",");
			}

			StringBuilder orientationStrip = new StringBuilder();
			for (int z = 0; z < size.getZ(); z++) {
				for (int x = 0; x < size.getX(); x++) {
					BlockOrientation orientation = BlockOrientation.byState(worldIn.getBlockState(anchor.east()
						.offset(x, y, z)));
					orientationStrip.append(orientation.asChar());
				}
				if (z < size.getZ() - 1)
					orientationStrip.append(",");
			}

			layers.add(new SliceData(trait, data.toString(), Optional.of(orientationStrip.toString())));
		}

		// Additional data. Only the field this design type uses is set; the rest stay at the
		// defaults the codec omits, so the file gains no keys it does not mean.
		int data = designParameter;
		int roofspan = 0;
		int margin = 0;
		int radius = 0;
		ExpandBehaviour expandBehaviour = ExpandBehaviour.None;

		switch (type) {
		case ROOF:
			roofspan = data;
			break;
		case FLAT_ROOF:
			margin = data;
			break;
		case WALL:
			if (data == -1)
				return "Revisit the Design settings.";
			expandBehaviour = ExpandBehaviour.values()[data];
			if (size.getX() == 1 && expandBehaviour == ExpandBehaviour.MergedRepeat)
				return "Can't merge Walls of length 1. Use 'Repeat' instead.";
			break;
		case TOWER_FLAT_ROOF:
		case TOWER_ROOF:
		case TOWER:
			radius = data;
			break;
		default:
			break;
		}

		DesignData design = new DesignData(size, layers, roofspan, margin, radius, expandBehaviour);

		// Write it out
		String relativeFolder = ArchitectPaths.THEMES + "/" + theme.getFilePath() + "/" + layer.getFilePath() + "/"
			+ type.getFilePath();
		Path folder = ArchitectPaths.resolve(relativeFolder);
		FilesHelper.createFolderIfMissing(folder);

		String filename = "";
		BlockPos signPos = anchor.above();
		if (worldIn.getBlockState(signPos)
			.getBlock() == Blocks.SPRUCE_SIGN && worldIn.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
			//? if >=1.20 {
			String signedName = sign.getFrontText().getMessage(1, false).getString();
			//?} else {
			/*String signedName = sign.getMessage(1, false).getString();
			*///?}
			filename = designFilename(signedName);
		}

		if (filename.isEmpty())
			filename = nextFreeDesignName(relativeFolder, folder);

		AllPackets.sendToServer(new PlaceSignPacket(layer.getDisplayName()
			.substring(0, 1) + ". " + type.getDisplayName(), filename, signPos));

		Path target = folder.resolve(filename);
		JsonStorage.write(target, DesignData.CODEC, design);
		return target.toString();
	}

	/**
	 * @return the first {@code design_N.json} that neither the mod nor the user's folder already
	 *         has - the built-in check matters because an exported design has to sit alongside the
	 *         theme's shipped ones without shadowing one of them
	 */
	private static String nextFreeDesignName(String relativeFolder, Path folder) {
		for (int index = 0; index < 2048; index++) {
			String candidate = "design" + ((index == 0) ? "" : "_" + index) + ".json";
			if (!ArchitectResources.exists(relativeFolder + "/" + candidate)
				&& !Files.exists(folder.resolve(candidate)))
				return candidate;
		}
		return "design.json";
	}

	/**
	 * Turns the name written on line two of a design's sign into the file that design is saved as.
	 * <p>
	 * The name is echoed back onto the sign after every export, so this has to be idempotent: an
	 * already-suffixed name keeps its single {@code .json} instead of growing another one, which is
	 * what makes re-exporting overwrite the previous file rather than leaving an orphan behind that
	 * keeps loading forever.
	 *
	 * @return the filename, or empty when the sign carries no usable name
	 */
	public static String designFilename(String signedName) {
		String base = signedName.trim();
		if (base.toLowerCase(Locale.ROOT)
			.endsWith(".json"))
			base = base.substring(0, base.length() - ".json".length());

		String slug = FilesHelper.slug(base);
		return slug.isEmpty() ? "" : slug + ".json";
	}

	public static void setTheme(DesignTheme theme) {
		get().applyTheme(theme);
	}

	private void applyTheme(DesignTheme theme) {
		this.theme = theme;
		scanningPalette = theme.getDefaultPalette();
		if (layer == null || !theme.getLayers()
			.contains(layer))
			layer = DesignLayer.Regular;
		if (type == null || !theme.getTypes()
			.contains(type))
			type = DesignType.WALL;
		changed = true;
	}

	private static boolean isMarker(Level worldIn, BlockPos pos) {
		return AllBlocks.SLICE_MARKER.typeOf(worldIn.getBlockState(pos));
	}

	private static int markerValueAt(Level worldIn, BlockPos pos) {
		return worldIn.getBlockState(pos)
			.getValue(SliceMarkerBlock.VARIANT)
			.ordinal();
	}

}
