package com.timmie.mightyarchitect.control;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.timmie.mightyarchitect.MightyClient;
//? if >=26 {
//?} else {
/*import com.timmie.mightyarchitect.foundation.MatrixStacker;
*///?}
import com.timmie.mightyarchitect.foundation.SuperByteBuffer;
import net.minecraft.client.Minecraft;
//? if >=26 {
//?} else {
/*import net.minecraft.client.renderer.ItemBlockRenderTypes;
*///?}
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=26 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else if >=1.21.11 {
/*import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
*///?} else if >=1.21.6 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
*///?} else {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
*///?}
import net.minecraft.core.BlockPos;
//? if >=26 {
//?} else if >=1.21.6 {
/*import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
*///?} else {
/*import net.minecraft.world.level.BlockAndTintGetter;
*///?}
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
//? if >=26 {
//?} else if >=1.21.6 {
/*import java.util.List;
*///?} else {
/*
*///?}
import java.util.Map;
import java.util.Set;

public class SchematicRenderer {

	//? if >=26 {
	private static final ChunkSectionLayer[] CHUNK_SECTION_LAYERS = ChunkSectionLayer.values();

	// Map ChunkSectionLayer to RenderType for buffer rendering
	private static RenderType layerToRenderType(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> RenderTypes.solidMovingBlock();
			case CUTOUT -> RenderTypes.cutoutMovingBlock();
			case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
		};
	}

	private final Map<ChunkSectionLayer, SuperByteBuffer> bufferCache = new HashMap<>(CHUNK_SECTION_LAYERS.length);
	private final Set<ChunkSectionLayer> usedBlockRenderLayers = new HashSet<>(CHUNK_SECTION_LAYERS.length);
	private final Set<ChunkSectionLayer> startedBufferBuilders = new HashSet<>(CHUNK_SECTION_LAYERS.length);
	//?} else if >=1.21.11 {
	/*// In 1.21.6, ChunkSectionLayer enum replaces chunk render layer RenderTypes
	private static final ChunkSectionLayer[] CHUNK_SECTION_LAYERS = ChunkSectionLayer.values();

	// Map ChunkSectionLayer to RenderType for buffer rendering
	private static RenderType layerToRenderType(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> net.minecraft.client.renderer.rendertype.RenderTypes.solidMovingBlock();
			case CUTOUT -> net.minecraft.client.renderer.rendertype.RenderTypes.cutoutMovingBlock();
			case TRANSLUCENT -> net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock();
			case TRIPWIRE -> net.minecraft.client.renderer.rendertype.RenderTypes.tripwireMovingBlock();
		};
	}

	private final Map<ChunkSectionLayer, SuperByteBuffer> bufferCache = new HashMap<>(CHUNK_SECTION_LAYERS.length);
	private final Set<ChunkSectionLayer> usedBlockRenderLayers = new HashSet<>(CHUNK_SECTION_LAYERS.length);
	private final Set<ChunkSectionLayer> startedBufferBuilders = new HashSet<>(CHUNK_SECTION_LAYERS.length);
	*///?} else if >=1.21.6 {
	/*// In 1.21.6, ChunkSectionLayer enum replaces chunk render layer RenderTypes
	private static final ChunkSectionLayer[] CHUNK_SECTION_LAYERS = ChunkSectionLayer.values();

	// Map ChunkSectionLayer to RenderType for buffer rendering
	private static RenderType layerToRenderType(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> RenderType.solid();
			case CUTOUT -> RenderType.cutout();
			case CUTOUT_MIPPED -> RenderType.cutoutMipped();
			case TRANSLUCENT -> RenderType.tripwire(); // translucent() removed, use tripwire()
			case TRIPWIRE -> RenderType.tripwire();
		};
	}

	private final Map<ChunkSectionLayer, SuperByteBuffer> bufferCache = new HashMap<>(CHUNK_SECTION_LAYERS.length);
	private final Set<ChunkSectionLayer> usedBlockRenderLayers = new HashSet<>(CHUNK_SECTION_LAYERS.length);
	private final Set<ChunkSectionLayer> startedBufferBuilders = new HashSet<>(CHUNK_SECTION_LAYERS.length);
	*///?} else {
	/*private final Map<RenderType, SuperByteBuffer> bufferCache = new HashMap<>(getLayerCount());
	private final Set<RenderType> usedBlockRenderLayers = new HashSet<>(getLayerCount());
	private final Set<RenderType> startedBufferBuilders = new HashSet<>(getLayerCount());
	*///?}
	private boolean active;
	private boolean changed;
	private Schematic schematic;
	private BlockPos anchor;

	public SchematicRenderer() {
		changed = false;
	}

	public void display(Schematic schematic) {
		this.anchor = schematic.getAnchor();
		this.schematic = schematic;
		this.active = true;
		this.changed = true;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void update() {
		changed = true;
	}

	public void tick() {
		if (!active)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || !changed)
			return;

		redraw(mc);
		changed = false;
	}

	public void render(PoseStack ms, MultiBufferSource buffer) {
		if (!active)
			return;

		ms.pushPose();
		ms.translate(anchor.getX(), anchor.getY(), anchor.getZ());
		//? if >=26 {
		buffer.getBuffer(RenderTypes.solidMovingBlock());
		for (ChunkSectionLayer layer : CHUNK_SECTION_LAYERS) {
		//?} else if >=1.21.11 {
		/*buffer.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.solidMovingBlock());
		for (ChunkSectionLayer layer : CHUNK_SECTION_LAYERS) {
		*///?} else if >=1.21.6 {
		/*buffer.getBuffer(RenderType.solid());
		for (ChunkSectionLayer layer : CHUNK_SECTION_LAYERS) {
		*///?} else {
		/*buffer.getBuffer(RenderType.solid());
		for (RenderType layer : RenderType.chunkBufferLayers()) {
		*///?}
			if (!usedBlockRenderLayers.contains(layer))
				continue;
			SuperByteBuffer superByteBuffer = bufferCache.get(layer);
			//? if >=1.21.6 {
			superByteBuffer.renderInto(ms, buffer.getBuffer(layerToRenderType(layer)));
			//?} else {
			/*superByteBuffer.renderInto(ms, buffer.getBuffer(layer));
			*///?}
		}

		ms.popPose();
	}

	private void redraw(Minecraft minecraft) {
		usedBlockRenderLayers.clear();
		startedBufferBuilders.clear();

		final BlockAndTintGetter blockAccess = schematic.getMaterializedSketch();
		//? if >=26 {
		final ModelBlockRenderer blockRenderer = new ModelBlockRenderer(
			minecraft.options.ambientOcclusion().get(), true, minecraft.getBlockColors());
		final boolean cutoutLeaves = minecraft.options.cutoutLeaves().get();
		//?} else if >=1.21.6 {
		/*final BlockRenderDispatcher blockRendererDispatcher = minecraft.getBlockRenderer();
		final RandomSource random = RandomSource.create();
		*///?} else {
		/*final BlockRenderDispatcher blockRendererDispatcher = minecraft.getBlockRenderer();
		*///?}

		//? if >=26 {
		Map<ChunkSectionLayer, ByteBufferBuilder> byteBuffers = new HashMap<>();
		Map<ChunkSectionLayer, BufferBuilder> buffers = new HashMap<>();
		//?} else if >=1.21.6 {
		/*Map<ChunkSectionLayer, ByteBufferBuilder> byteBuffers = new HashMap<>();
		Map<ChunkSectionLayer, BufferBuilder> buffers = new HashMap<>();
		PoseStack ms = new PoseStack();
		*///?} else {
		/*Map<RenderType, ByteBufferBuilder> byteBuffers = new HashMap<>();
		Map<RenderType, BufferBuilder> buffers = new HashMap<>();
		PoseStack ms = new PoseStack();
		*///?}

		BlockPos.betweenClosedStream(schematic.getLocalBounds()
			.toMBB())
			.forEach(localPos -> {
				//? if >=26 {
				//?} else {
				/*ms.pushPose();
				MatrixStacker.of(ms)
					.translate(localPos);
				*///?}
				BlockPos pos = localPos.offset(anchor);
				BlockState state = blockAccess.getBlockState(pos);

				//? if >=26 {
				if (state.getRenderShape() == RenderShape.MODEL) {
					boolean forceOpaque = ModelBlockRenderer.forceOpaque(cutoutLeaves, state);
					BlockQuadOutput output = (x, y, z, quad, instance) -> {
						ChunkSectionLayer blockRenderLayer = forceOpaque ? ChunkSectionLayer.SOLID : quad.materialInfo().layer();
						if (!buffers.containsKey(blockRenderLayer)) {
							int bufferSize = MightyClient.iris_presence ? 262144 : 2097152;
							ByteBufferBuilder byteBuffer = new ByteBufferBuilder(bufferSize);
							byteBuffers.put(blockRenderLayer, byteBuffer);
							buffers.put(blockRenderLayer, new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK));
							startedBufferBuilders.add(blockRenderLayer);
						}
				//?} else if >=1.21.6 {
				/*ChunkSectionLayer stateLayer = ItemBlockRenderTypes.getChunkRenderType(state);
				for (ChunkSectionLayer blockRenderLayer : CHUNK_SECTION_LAYERS) {
					if (blockRenderLayer != stateLayer)
						continue;
				*///?} else {
				/*for (RenderType blockRenderLayer : RenderType.chunkBufferLayers()) {
					if (blockRenderLayer != ItemBlockRenderTypes.getChunkRenderType(state))
						continue;
				*///?}

						//? if >=26 {
						buffers.get(blockRenderLayer).putBlockBakedQuad(x, y, z, quad, instance);
						usedBlockRenderLayers.add(blockRenderLayer);
					};
						//?} else {
						/*if (!buffers.containsKey(blockRenderLayer))
					{
						int bufferSize = MightyClient.iris_presence ? 262144 : 2097152;
						ByteBufferBuilder byteBuffer = new ByteBufferBuilder(bufferSize);
						byteBuffers.put(blockRenderLayer, byteBuffer);
						buffers.put(blockRenderLayer, new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK));
						startedBufferBuilders.add(blockRenderLayer);
					}
						*///?}

					//? if >=26 {
					BlockStateModel model = minecraft.getModelManager()
						.getBlockStateModelSet()
						.get(state);
					blockRenderer.tesselateBlock(output, localPos.getX(), localPos.getY(), localPos.getZ(),
						blockAccess, pos, state, model, state.getSeed(pos));
					//?} else if >=1.21.6 {
					/*BufferBuilder bufferBuilder = buffers.get(blockRenderLayer);

					if (state.getRenderShape() == RenderShape.MODEL)
					{
						// In 1.21.6, renderBatched takes List<BlockModelPart> instead of RandomSource
						BlockStateModel model = blockRendererDispatcher.getBlockModel(state);
						List<BlockModelPart> parts = model.collectParts(random);
						blockRendererDispatcher.renderBatched(state, pos, blockAccess, ms,
								bufferBuilder, true, parts);
						usedBlockRenderLayers.add(blockRenderLayer);
					}
					*///?} else {
					/*BufferBuilder bufferBuilder = buffers.get(blockRenderLayer);

					if (state.getRenderShape() == RenderShape.MODEL)
					{
						blockRendererDispatcher.renderBatched(state, pos, blockAccess, ms,
								bufferBuilder, true, minecraft.level.random);
						usedBlockRenderLayers.add(blockRenderLayer);
					}
					*///?}
				}
				//? if >=26 {
				//?} else {
				/*
				ms.popPose();
				*///?}
			});

		// finishDrawing
		//? if >=1.21.6 {
		for (ChunkSectionLayer layer : CHUNK_SECTION_LAYERS) {
		//?} else {
		/*for (RenderType layer : RenderType.chunkBufferLayers()) {
		*///?}
			if (!startedBufferBuilders.contains(layer))
				continue;
			BufferBuilder buf = buffers.get(layer);
			MeshData meshData = buf.build();
			if (meshData != null) {
				bufferCache.put(layer, new SuperByteBuffer(meshData));
			}
			// Close the ByteBufferBuilder to free memory
			ByteBufferBuilder byteBuffer = byteBuffers.get(layer);
			if (byteBuffer != null) {
				byteBuffer.close();
			}
		}
	}

	//? if >=1.21.6 {
	//?} else {
	/*private static int getLayerCount() {
		return RenderType.chunkBufferLayers()
			.size();
	}

	*///?}
}
