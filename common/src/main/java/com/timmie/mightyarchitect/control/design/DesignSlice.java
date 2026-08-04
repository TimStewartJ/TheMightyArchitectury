package com.timmie.mightyarchitect.control.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.palette.BlockOrientation;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteBlockInfo;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DesignSlice {

	public enum DesignSliceTrait implements StringRepresentable {
		Standard("-> Use this slice once"),
		CloneOnce("-> Duplicate this slice if necessary"),
		CloneThrice("-> Duplicate up to 3 times"),
		Optional("-> Ignore slice if necessary"),
		MaskAbove("-> Slice does not count towards effective Height"),
		MaskBelow("-> Add this slice onto lower layers");

		private String description;

		private DesignSliceTrait(String description) {
			this.description = description;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}

		public String getDescription() {
			return description;
		}

		public DesignSliceTrait cycle(int amount) {
			DesignSliceTrait[] values = values();
			return values[(this.ordinal() + amount + values.length) % values.length];
		}
	}

	private DesignSliceTrait trait;
	private Palette[][] blocks;
	private BlockOrientation[][] orientations;

	/**
	 * A trait name, tolerating one this build does not have.
	 * <p>
	 * {@code valueOf} threw, and the throw escaped the single try block around the whole theme
	 * scan, so a design written by a newer build emptied the theme list rather than costing itself
	 * one layer.
	 */
	private static final Codec<DesignSliceTrait> TRAIT_CODEC = Codec.STRING.xmap(name -> {
		try {
			return DesignSliceTrait.valueOf(name);
		} catch (IllegalArgumentException unknown) {
			TheMightyArchitect.logger.warn("Ignoring unknown design slice trait '{}'", name);
			return DesignSliceTrait.Standard;
		}
	}, DesignSliceTrait::name);

	/**
	 * One layer of a design, exactly as the file carries it.
	 * <p>
	 * The two grids are strings on disk - one character per block, rows separated by commas - so
	 * the schema is three fields rather than anything structural. Keeping the decoded form separate
	 * from {@link DesignSlice} is what lets the codec be the only thing that knows the file layout;
	 * the slice itself only ever sees a parsed record.
	 */
	public record SliceData(DesignSliceTrait trait, String blocks, Optional<String> facing) {

		public static final Codec<SliceData> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(TRAIT_CODEC.optionalFieldOf("Trait", DesignSliceTrait.Standard)
				.forGetter(SliceData::trait),
				Codec.STRING.optionalFieldOf("Blocks", "")
					.forGetter(SliceData::blocks),
				Codec.STRING.optionalFieldOf("Facing")
					.forGetter(SliceData::facing))
			.apply(instance, SliceData::new));
	}

	public static DesignSlice fromData(SliceData data) {
		DesignSlice slice = new DesignSlice();
		slice.trait = data.trait();

		String[] strips = data.blocks()
			.split(",");
		int width = strips[0].length();
		int length = strips.length;
		slice.blocks = new Palette[length][width];

		for (int z = 0; z < length; z++) {
			String strip = strips[z];
			for (int x = 0; x < width && x < strip.length(); x++) {
				char charAt = strip.charAt(x);
				if (charAt != ' ')
					slice.blocks[z][x] = Palette.getByChar(charAt);
			}
		}

		slice.orientations = new BlockOrientation[length][width];
		for (int z = 0; z < length; z++)
			Arrays.fill(slice.orientations[z], BlockOrientation.NONE);

		if (data.facing()
			.isPresent()) {
			String[] facingStrips = data.facing()
				.get()
				.split(",");

			for (int z = 0; z < length && z < facingStrips.length; z++) {
				String strip = facingStrips[z];
				// Bounded by the strip too: a hand-edited design whose Facing row is shorter than
				// its Blocks row used to throw out of the whole theme scan.
				for (int x = 0; x < width && x < strip.length(); x++)
					slice.orientations[z][x] = BlockOrientation.valueOf(strip.charAt(x));
			}
		}

		return slice;
	}

	public PaletteBlockInfo getBlockAt(int x, int z, int rotation) {
		return getBlockAt(x, z, rotation, false);
	}

	public PaletteBlockInfo getBlockAt(int x, int z, int rotation, boolean mirrorX) {
		Palette palette = blocks[z][x];
		if (palette == null)
			return null;

		BlockOrientation blockOrientation = orientations[z][x];
		if (!blockOrientation.hasFacing())
			blockOrientation = BlockOrientation.valueOf(blockOrientation.getHalf(), Direction.NORTH);

		PaletteBlockInfo paletteBlockInfo = new PaletteBlockInfo(palette, blockOrientation);
		paletteBlockInfo.afterPosition = BlockOrientation.NORTH.withRotation(rotation);

		if (orientations[z][x].hasFacing() && orientations[z][x].getFacing().getAxis() != Axis.Y)
			paletteBlockInfo.forceAxis = true;

		if (rotation % 180 == 0)
			paletteBlockInfo.mirrorZ = mirrorX;
		else
			paletteBlockInfo.mirrorX = mirrorX;
		return paletteBlockInfo;
	}

	public DesignSliceTrait getTrait() {
		return trait;
	}

	public Set<Integer> adjustHeigthsList(Set<Integer> heightsList) {
		Set<Integer> newHeights = new HashSet<>();
		for (Integer integer : heightsList) {
			switch (trait) {
			case Standard:
				newHeights.add(integer + 1);
				break;
			case CloneOnce:
				newHeights.add(integer + 1);
				newHeights.add(integer + 2);
				break;
			case CloneThrice:
				newHeights.add(integer + 1);
				newHeights.add(integer + 2);
				newHeights.add(integer + 3);
				newHeights.add(integer + 4);
				break;
			case Optional:
				newHeights.add(integer);
				newHeights.add(integer + 1);
				break;
			case MaskAbove:
			case MaskBelow:
				newHeights.add(integer);
				break;
			}
		}
		return newHeights;
	}

	public int adjustDefaultHeight(int defaultHeight) {
		switch (trait) {
		case MaskAbove:
		case MaskBelow:
			return defaultHeight;
		default:
			return defaultHeight + 1;
		}
	}

	public int addToPrintedLayers(List<DesignSlice> toPrint, int currentHeight, int targetHeight) {
		switch (trait) {
		case MaskAbove:
		case MaskBelow:
		case Standard:
			toPrint.add(this);
			return currentHeight;
		case Optional:
			if (currentHeight > targetHeight) {
				return currentHeight - 1;
			} else {
				toPrint.add(this);
				return currentHeight;
			}
		case CloneOnce:
			toPrint.add(this);
			if (currentHeight < targetHeight) {
				toPrint.add(this);
				return currentHeight + 1;
			}
			return currentHeight;
		case CloneThrice:
			toPrint.add(this);
			int i = 0;
			for (; i < 3 && currentHeight + i < targetHeight; i++) {
				toPrint.add(this);
			}
			return currentHeight + i;
		default:
			return currentHeight;
		}
	}

}
