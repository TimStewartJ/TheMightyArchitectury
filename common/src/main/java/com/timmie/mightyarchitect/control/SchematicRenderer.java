package com.timmie.mightyarchitect.control;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.foundation.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SchematicRenderer {

	// In 1.21.6, ChunkSectionLayer enum replaces chunk render layer RenderTypes
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
		buffer.getBuffer(RenderTypes.solidMovingBlock());
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
		final ModelBlockRenderer blockRenderer = new ModelBlockRenderer(
			minecraft.options.ambientOcclusion().get(), true, minecraft.getBlockColors());
		final boolean cutoutLeaves = minecraft.options.cutoutLeaves().get();

		Map<ChunkSectionLayer, ByteBufferBuilder> byteBuffers = new HashMap<>();
		Map<ChunkSectionLayer, BufferBuilder> buffers = new HashMap<>();

		BlockPos.betweenClosedStream(schematic.getLocalBounds()
			.toMBB())
			.forEach(localPos -> {
				BlockPos pos = localPos.offset(anchor);
				BlockState state = blockAccess.getBlockState(pos);

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

						buffers.get(blockRenderLayer).putBlockBakedQuad(x, y, z, quad, instance);
						usedBlockRenderLayers.add(blockRenderLayer);
					};

					BlockStateModel model = minecraft.getModelManager()
						.getBlockStateModelSet()
						.get(state);
					blockRenderer.tesselateBlock(output, localPos.getX(), localPos.getY(), localPos.getZ(),
						blockAccess, pos, state, model, state.getSeed(pos));
				}
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
