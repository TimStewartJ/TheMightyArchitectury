package com.timmie.mightyarchitect.control.design.partials;

import com.timmie.mightyarchitect.control.design.DesignSlice;
import com.timmie.mightyarchitect.control.palette.PaletteBlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Map;

public class Corner extends Design {
	
	@Override
	public Design fromNBT(CompoundTag compound) {
		Corner corner = new Corner();
		corner.applyNBT(compound);
		return corner;
	}
	
	
	public DesignInstance create(BlockPos anchor, int rotation, int height, boolean flipX) {
		DesignInstance create = create(anchor, rotation, size.getX(), height);
		create.flippedX = flipX;
		return create;
	}
	
	@Override
	protected void getBlocksShifted(DesignInstance instance, Map<BlockPos, PaletteBlockInfo> blocks, BlockPos localShift) {
		BlockPos position = instance.localAnchor;
		BlockPos totalShift = localShift.offset(0, yShift, 0);
		List<DesignSlice> toPrint = selectPrintedLayers(instance.height);

		for (int y = 0; y < toPrint.size(); y++) {
			DesignSlice layer = toPrint.get(y);
			for (int x = 0; x < size.getX(); x++) {
				for (int z = 0; z < size.getZ(); z++) {
					PaletteBlockInfo block = layer.getBlockAt(x, z, instance.rotationY, instance.flippedX);
					if (block == null)
						continue;
					BlockPos pos = rotateAroundZero(new BlockPos(instance.flippedX? - x : x, y, instance.flippedX? z : z).offset(totalShift), instance.rotationY)
							.offset(position);
					putBlock(blocks, pos, block);
				}
			}
		}
	}

}
