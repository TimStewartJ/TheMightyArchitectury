package com.timmie.mightyarchitect.test.mixin;

import com.timmie.mightyarchitect.control.compose.planner.RoomTool;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Marks a first corner without clicking, which would set the model's anchor and change what the
 * rest of the run sees.
 */
@Mixin(value = RoomTool.class, remap = false)
public interface RoomToolAccessor {

    @Accessor(value = "firstPosition", remap = false)
    BlockPos getFirstPosition();

    @Accessor(value = "firstPosition", remap = false)
    void setFirstPosition(BlockPos firstPosition);
}
