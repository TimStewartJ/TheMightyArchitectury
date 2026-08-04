package com.timmie.mightyarchitect.control.design.partials;

import com.google.common.collect.ImmutableSet;
import com.timmie.mightyarchitect.control.design.DesignSlice;
import com.timmie.mightyarchitect.control.design.DesignSlice.DesignSliceTrait;
import com.timmie.mightyarchitect.control.design.DesignSlice.SliceData;
import com.timmie.mightyarchitect.control.palette.PaletteBlockInfo;
import net.minecraft.core.BlockPos;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class Design {

	/** Printed when a file declares more layers in {@code Size} than it actually carries. */
	private static final SliceData EMPTY_SLICE = new SliceData(DesignSliceTrait.Standard, "", Optional.empty());

	protected BlockPos size;
	protected DesignSlice[] slices;
	protected Set<Integer> heights;
	protected int defaultHeight;
	protected int defaultWidth;
	protected int yShift;

	public abstract Design fromData(DesignData data);

	protected void applyData(DesignData data) {
		size = data.size();
		defaultWidth = size.getX();
		slices = new DesignSlice[size.getY()];

		defaultHeight = 0;
		yShift = 0;
		heights = ImmutableSet.of(0);
		List<SliceData> sliceData = data.layers();

		for (int sliceIndex = 0; sliceIndex < slices.length; sliceIndex++) {
			// Size and Layers are independent fields, so a truncated file can declare more layers
			// than it carries. An empty slice prints nothing, which beats an index out of bounds
			// escaping into the theme scan.
			DesignSlice slice = DesignSlice
				.fromData(sliceIndex < sliceData.size() ? sliceData.get(sliceIndex) : EMPTY_SLICE);
			defaultHeight = slice.adjustDefaultHeight(defaultHeight);
			heights = slice.adjustHeigthsList(heights);
			slices[sliceIndex] = slice;

			if (slice.getTrait() == DesignSliceTrait.MaskBelow)
				yShift -= 1;
		}
	}

	public void getBlocks(DesignInstance instance, Map<BlockPos, PaletteBlockInfo> blocks) {
		getBlocksShifted(instance, blocks, BlockPos.ZERO);
	}

	protected void getBlocksShifted(DesignInstance instance, Map<BlockPos, PaletteBlockInfo> blocks, BlockPos localShift) {
		BlockPos position = instance.localAnchor;
		BlockPos totalShift = localShift.offset(0, yShift, 0);
		List<DesignSlice> toPrint = selectPrintedLayers(instance.height);

		for (int y = 0; y < toPrint.size(); y++) {
			DesignSlice layer = toPrint.get(y);
			for (int x = 0; x < size.getX(); x++) {
				for (int z = 0; z < size.getZ(); z++) {
					PaletteBlockInfo block = layer.getBlockAt(x, z, instance.rotationY);
					if (block == null)
						continue;
					BlockPos pos = rotateAroundZero(new BlockPos(x, y, z).offset(totalShift), instance.rotationY)
							.offset(position);
					putBlock(blocks, pos, block);
				}
			}
		}
	}

	protected List<DesignSlice> selectPrintedLayers(int targetHeight) {
		List<DesignSlice> toPrint = new LinkedList<>();
		int currentHeight = defaultHeight;
		for (DesignSlice slice : slices)
			currentHeight = slice.addToPrintedLayers(toPrint, currentHeight, targetHeight);
		return toPrint;
	}

	protected void putBlock(Map<BlockPos, PaletteBlockInfo> blocks, BlockPos pos, PaletteBlockInfo block) {
		if (!blocks.containsKey(pos) || !blocks.get(pos).palette.isPrefferedOver(block.palette)) {
			blocks.put(pos, block);
		}
	}

	public String toString() {
		String heights = "Heights ";
		for (Integer integer : this.heights) {
			heights += integer + " ";
		}
		return String.format("Design with ") + heights;
	}

	public boolean fitsHorizontally(int width) {
		return this.defaultWidth == width;
	}

	public boolean fitsVertically(int height) {
		return heights.contains(Integer.valueOf(height));
	}

	public BlockPos rotateAroundZero(BlockPos in, int rotation) {
		return rotateAround(in, rotation, new BlockPos(0, 0, 0));
	}

	public BlockPos rotateAround(BlockPos in, int rotation, BlockPos origin) {
		BlockPos local = in.subtract(origin);
		int x = (rotation == 180) ? -local.getX()
				: (rotation == 90) ? -local.getZ() : (rotation == -90) ? local.getZ() : local.getX();
		int z = (rotation == 180) ? -local.getZ()
				: (rotation == 90) ? local.getX() : (rotation == -90) ? -local.getX() : local.getZ();
		BlockPos rotated = new BlockPos(x, local.getY(), z);
		return rotated.offset(origin);
	}

	public DesignInstance create(BlockPos anchor, int rotation, int height) {
		return create(anchor, rotation, size.getX(), height);
	}

	public DesignInstance create(BlockPos anchor, int rotation, int width, int height) {
		return new DesignInstance(this, anchor, rotation, width, height, 0);
	}

	public class DesignInstance {

		BlockPos localAnchor;
		int rotationY, rotationZ;
		int width, height, depth;
		Design template;
		boolean flippedX;

		public DesignInstance(Design template, BlockPos anchor, int rotation, int width, int height, int depth) {
			this.template = template;
			this.localAnchor = anchor;
			this.height = height;
			this.rotationY = rotation;
			this.width = width;
			this.depth = depth;
		}

		public void getBlocks(Map<BlockPos, PaletteBlockInfo> blocks) {
			template.getBlocks(this, blocks);
		}

		public Design getTemplate() {
			return template;
		}


	}


}
