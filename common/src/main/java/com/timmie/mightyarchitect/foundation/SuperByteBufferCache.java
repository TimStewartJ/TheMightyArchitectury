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
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
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
		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		ByteBufferBuilder byteBuffer = new ByteBufferBuilder(2097152);
		BufferBuilder builder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
		RandomSource random = RandomSource.create();
		
		// In 1.21.6, renderBatched takes List<BlockModelPart> instead of RandomSource
		BlockStateModel model = dispatcher.getBlockModel(referenceState);
		List<BlockModelPart> parts = model.collectParts(random);
		dispatcher.renderBatched(referenceState, BlockPos.ZERO.above(255), Minecraft.getInstance().level, ms,
				builder, true, parts);
		MeshData meshData = builder.build();

		SuperByteBuffer result = meshData != null ? new SuperByteBuffer(meshData) : SuperByteBuffer.empty();
		byteBuffer.close();
		return result;
	}

	public void invalidate() {
		cache.forEach((comp, cache) -> {
			cache.invalidateAll();
		});
	}

}
