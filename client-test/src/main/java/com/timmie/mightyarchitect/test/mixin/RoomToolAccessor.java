package com.timmie.mightyarchitect.test.mixin;

import com.timmie.mightyarchitect.control.compose.planner.RoomTool;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Marks a room's first corner the way a right-click on the ground would, without depending on
 * where the test player happens to be looking.
 */
@Mixin(value = RoomTool.class, remap = false)
public interface RoomToolAccessor {

    @Accessor(value = "firstPosition", remap = false)
    BlockPos getFirstPosition();

    @Accessor(value = "firstPosition", remap = false)
    void setFirstPosition(BlockPos position);
}
