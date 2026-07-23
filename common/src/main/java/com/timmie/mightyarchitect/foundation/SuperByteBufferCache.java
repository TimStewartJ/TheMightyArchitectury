package com.timmie.mightyarchitect.foundation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SuperByteBufferCache {

	public static class Compartment<T> {
	}

	public static final Compartment<BlockState> GENERIC_TILE = new Compartment<>();
	Map<Compartment<?>, Cache<Object, SuperByteBuffer>> cache;

	public SuperByteBufferCache() {
		cache = new HashMap<>();
		registerCompartment(GENERIC_TILE);
	}

	public SuperByteBuffer renderBlock(BlockState toRender) {
		return getGeneric(toRender, () -> standardBlockRender(toRender));
	}

	public SuperByteBuffer renderBlockIn(Compartment<BlockState> compartment, BlockState toRender) {
		return get(compartment, toRender, () -> standardBlockRender(toRender));
	}

	SuperByteBuffer getGeneric(BlockState key, Supplier<SuperByteBuffer> supplier) {
		return get(GENERIC_TILE, key, supplier);
	}

	public <T> SuperByteBuffer get(Compartment<T> compartment, T key, Supplier<SuperByteBuffer> supplier) {
		Cache<Object, SuperByteBuffer> compartmentCache = this.cache.get(compartment);
		try {
			return compartmentCache.get(key, supplier::get);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void registerCompartment(Compartment<?> instance) {
		cache.put(instance, CacheBuilder.newBuilder().build());
	}

	public void registerCompartment(Compartment<?> instance, long ticksTillExpired) {
		cache.put(instance,
				CacheBuilder.newBuilder().expireAfterAccess(ticksTillExpired * 50, TimeUnit.MILLISECONDS).build());
	}

	private SuperByteBuffer standardBlockRender(BlockState renderedState) {
		return standardBlockRender(renderedState, new PoseStack());
	}

	private SuperByteBuffer standardBlockRender(BlockState referenceState, PoseStack ms) {
		Minecraft minecraft = Minecraft.getInstance();
		ByteBufferBuilder byteBuffer = new ByteBufferBuilder(2097152);
		BufferBuilder builder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
		BlockPos pos = BlockPos.ZERO;
		BlockAndTintGetter blockAccess = singleBlockAccess(minecraft, referenceState, pos);
		ModelBlockRenderer renderer = new ModelBlockRenderer(minecraft.options.ambientOcclusion().get(), true, minecraft.getBlockColors());
		BlockStateModel model = minecraft.getModelManager()
			.getBlockStateModelSet()
			.get(referenceState);
		BlockQuadOutput output = (x, y, z, quad, instance) -> builder.putBlockBakedQuad(x, y, z, quad, instance);
		renderer.tesselateBlock(output, 0, 0, 0, blockAccess, pos, referenceState, model, referenceState.getSeed(pos));

		MeshData meshData = builder.build();

		SuperByteBuffer result = meshData != null ? new SuperByteBuffer(meshData) : SuperByteBuffer.empty();
		byteBuffer.close();
		return result;
	}

	private BlockAndTintGetter singleBlockAccess(Minecraft minecraft, BlockState state, BlockPos pos) {
		MovingBlockRenderState renderState = new MovingBlockRenderState();
		renderState.blockPos = pos;
		renderState.randomSeedPos = pos;
		renderState.blockState = state;
		if (minecraft.level != null) {
			renderState.biome = minecraft.level.getBiome(pos);
			renderState.cardinalLighting = minecraft.level.cardinalLighting();
			renderState.lightEngine = minecraft.level.getLightEngine();
		}
		return renderState;
	}

	public void invalidate() {
		cache.forEach((comp, cache) -> {
			cache.invalidateAll();
		});
	}

}
