package com.timmie.mightyarchitect.control.design.partials;

import com.timmie.mightyarchitect.control.palette.PaletteBlockInfo;
import net.minecraft.core.BlockPos;

import java.util.Map;

public class Tower extends Design {

	public int radius;

	@Override
	public Design fromData(DesignData data) {
		Tower tower = new Tower();
		tower.applyData(data);
		tower.radius = data.radius();
		tower.defaultWidth = tower.radius * 2 + 1;
		return tower;
	}

	public DesignInstance create(BlockPos anchor, int height) {
		return create(anchor, 0, size.getX(), height);
	}

	@Override
	public void getBlocks(DesignInstance instance, Map<BlockPos, PaletteBlockInfo> blocks) {
		int shift = (size.getX() - defaultWidth) / 2;
		getBlocksShifted(instance, blocks, new BlockPos(-shift, 0, -shift));
	}

	@Override
	public String toString() {
		return super.toString() + "\nRadius " + radius;
	}

	@Override
	public boolean fitsHorizontally(int width) {
		return width == defaultWidth;
	}




}
