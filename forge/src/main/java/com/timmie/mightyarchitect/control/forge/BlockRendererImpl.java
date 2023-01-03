package com.timmie.mightyarchitect.control.forge;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Map;

public class BlockRendererImpl {
    public static void batchRenderBlocks(Map<RenderType, BufferBuilder> buffers, RenderType blockRenderLayer, BlockState state, BlockAndTintGetter blockAccess,
             RandomSource random, BlockPos pos, PoseStack ms, BufferBuilder bufferBuilder, BlockRenderDispatcher blockRendererDispatcher)
    {
        //ForgeHooksClient.renderBlockOverlay(Minecraft.getInstance().player, ms, RenderBlockScreenEffectEvent.OverlayType.BLOCK, state, pos);
        blockRendererDispatcher.renderBatched(state, pos, blockAccess, ms, bufferBuilder, true, random, ModelData.EMPTY, blockRenderLayer);
    }
}
