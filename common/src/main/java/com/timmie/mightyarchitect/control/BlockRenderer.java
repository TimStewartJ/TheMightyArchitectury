package com.timmie.mightyarchitect.control;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class BlockRenderer {
    @ExpectPlatform
    public static void batchRenderBlocks(Map<RenderType, BufferBuilder> buffers, RenderType blockRenderLayer, BlockState state,  BlockAndTintGetter blockAccess,
        RandomSource random, BlockPos pos, PoseStack ms, BufferBuilder bufferBuilder, BlockRenderDispatcher blockRendererDispatcher)
    {
        throw new AssertionError();
    }
}
