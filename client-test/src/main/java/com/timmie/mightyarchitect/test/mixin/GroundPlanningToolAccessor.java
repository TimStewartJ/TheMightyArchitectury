package com.timmie.mightyarchitect.test.mixin;

import com.timmie.mightyarchitect.control.compose.planner.GroundPlanningToolBase;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads where a ground-planning tool would act - the block its next right click marks - so the
 * camera-aim stage can assert what the tool targets without clicking and changing the model.
 */
@Mixin(value = GroundPlanningToolBase.class, remap = false)
public interface GroundPlanningToolAccessor {

    @Accessor(value = "selectedPosition", remap = false)
    BlockPos getSelectedPosition();
}
