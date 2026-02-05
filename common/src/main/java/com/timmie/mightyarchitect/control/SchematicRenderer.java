package com.timmie.mightyarchitect.control;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.foundation.MatrixStacker;
import com.timmie.mightyarchitect.foundation.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchematicRenderer {

	// In 1.21.6, ChunkSectionLayer enum replaces chunk render layer RenderTypes
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
		buffer.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.solidMovingBlock());
		for (ChunkSectionLayer layer : CHUNK_SECTION_LAYERS) {
			if (!usedBlockRenderLayers.contains(layer))
				continue;
			SuperByteBuffer superByteBuffer = bufferCache.get(layer);
			superByteBuffer.renderInto(ms, buffer.getBuffer(layerToRenderType(layer)));
		}

		ms.popPose();
	}

	private void redraw(Minecraft minecraft) {
		usedBlockRenderLayers.clear();
		startedBufferBuilders.clear();

		final BlockAndTintGetter blockAccess = schematic.getMaterializedSketch();
		final BlockRenderDispatcher blockRendererDispatcher = minecraft.getBlockRenderer();
		final RandomSource random = RandomSource.create();

		Map<ChunkSectionLayer, ByteBufferBuilder> byteBuffers = new HashMap<>();
		Map<ChunkSectionLayer, BufferBuilder> buffers = new HashMap<>();
		PoseStack ms = new PoseStack();

		BlockPos.betweenClosedStream(schematic.getLocalBounds()
			.toMBB())
			.forEach(localPos -> {
				ms.pushPose();
				MatrixStacker.of(ms)
					.translate(localPos);
				BlockPos pos = localPos.offset(anchor);
				BlockState state = blockAccess.getBlockState(pos);

				ChunkSectionLayer stateLayer = ItemBlockRenderTypes.getChunkRenderType(state);
				for (ChunkSectionLayer blockRenderLayer : CHUNK_SECTION_LAYERS) {
					if (blockRenderLayer != stateLayer)
						continue;

					if (!buffers.containsKey(blockRenderLayer))
					{
						int bufferSize = MightyClient.iris_presence ? 262144 : 2097152;
						ByteBufferBuilder byteBuffer = new ByteBufferBuilder(bufferSize);
						byteBuffers.put(blockRenderLayer, byteBuffer);
						buffers.put(blockRenderLayer, new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK));
						startedBufferBuilders.add(blockRenderLayer);
					}

					BufferBuilder bufferBuilder = buffers.get(blockRenderLayer);

					if (state.getRenderShape() == RenderShape.MODEL)
					{
						// In 1.21.6, renderBatched takes List<BlockModelPart> instead of RandomSource
						BlockStateModel model = blockRendererDispatcher.getBlockModel(state);
						List<BlockModelPart> parts = model.collectParts(random);
						blockRendererDispatcher.renderBatched(state, pos, blockAccess, ms,
								bufferBuilder, true, parts);
						usedBlockRenderLayers.add(blockRenderLayer);
					}
				}

				ms.popPose();
			});

		// finishDrawing
		for (ChunkSectionLayer layer : CHUNK_SECTION_LAYERS) {
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

}
