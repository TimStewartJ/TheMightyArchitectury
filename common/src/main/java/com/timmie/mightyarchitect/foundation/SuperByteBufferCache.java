package com.timmie.mightyarchitect.foundation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
//? if >=1.21 {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
//?} else {
/*
*///?}
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//? if >=1.21 {
import com.mojang.blaze3d.vertex.MeshData;
//?} else {
/*
*///?}
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
//?} else if >=1.21.6 {
/*import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
*///?} else {
/*import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
*///?}
import net.minecraft.core.BlockPos;
//? if >=26 {
//?} else {
/*import net.minecraft.util.RandomSource;
*///?}
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
//? if >=26 {
//?} else if >=1.21.6 {
/*import java.util.List;
*///?} else {
/*
*///?}
import java.util.Map;
//? if >=1.21.6 {
//?} else {
/*import java.util.Random;
*///?}
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
		//? if >=1.21.6 {
		return standardBlockRender(renderedState, new PoseStack());
		//?} else {
		/*BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		return standardModelRender(dispatcher.getBlockModel(renderedState), renderedState);
		*///?}
	}

	//? if >=26 {
	private SuperByteBuffer standardBlockRender(BlockState referenceState, PoseStack ms) {
		Minecraft minecraft = Minecraft.getInstance();
	//?} else if >=1.21.6 {
	/*private SuperByteBuffer standardBlockRender(BlockState referenceState, PoseStack ms) {
		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
	*///?} else {
	/*private SuperByteBuffer standardModelRender(BakedModel model, BlockState referenceState) {
		return standardModelRender(model, referenceState, new PoseStack());
	}

	private SuperByteBuffer standardModelRender(BakedModel model, BlockState referenceState, PoseStack ms) {
		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		ModelBlockRenderer blockRenderer = dispatcher.getModelRenderer();
	*///?}
		//? if >=1.21 {
		ByteBufferBuilder byteBuffer = new ByteBufferBuilder(2097152);
		BufferBuilder builder = com.timmie.mightyarchitect.foundation.compat.McCompat.quadBuffer(byteBuffer, DefaultVertexFormat.BLOCK);
		//?} else {
		/*BufferBuilder builder = new BufferBuilder(DefaultVertexFormat.BLOCK.getIntegerSize());
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
		*///?}
		//? if >=26 {
		BlockPos pos = BlockPos.ZERO;
		BlockAndTintGetter blockAccess = singleBlockAccess(minecraft, referenceState, pos);
		ModelBlockRenderer renderer = new ModelBlockRenderer(minecraft.options.ambientOcclusion().get(), true, minecraft.getBlockColors());
		BlockStateModel model = minecraft.getModelManager()
			.getBlockStateModelSet()
			.get(referenceState);
		BlockQuadOutput output = (x, y, z, quad, instance) -> builder.putBlockBakedQuad(x, y, z, quad, instance);
		renderer.tesselateBlock(output, 0, 0, 0, blockAccess, pos, referenceState, model, referenceState.getSeed(pos));

		//?} else if >=1.21.6 {
		/*RandomSource random = RandomSource.create();

		// In 1.21.6, renderBatched takes List<BlockModelPart> instead of RandomSource
		BlockStateModel model = dispatcher.getBlockModel(referenceState);
		List<BlockModelPart> parts = model.collectParts(random);
		dispatcher.renderBatched(referenceState, BlockPos.ZERO.above(255), Minecraft.getInstance().level, ms,
				builder, true, parts);
		*///?} else {
		/*RandomSource random = RandomSource.create();
		blockRenderer.tesselateBlock(Minecraft.getInstance().level, model, referenceState, BlockPos.ZERO.above(255), ms,
				builder, true, random, 42, OverlayTexture.NO_OVERLAY);
		*///?}
		//? if >=1.21 {
		MeshData meshData = builder.build();

		SuperByteBuffer result = meshData != null ? new SuperByteBuffer(meshData) : SuperByteBuffer.empty();
		byteBuffer.close();
		return result;
		//?} else {
		/*var renderedBuffer = builder.end();

		return renderedBuffer != null ? new SuperByteBuffer(renderedBuffer) : SuperByteBuffer.empty();
		*///?}
	}

	//? if >=26 {
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

	//?} else {
	/*
	*///?}
	public void invalidate() {
		cache.forEach((comp, cache) -> {
			cache.invalidateAll();
		});
	}

}
