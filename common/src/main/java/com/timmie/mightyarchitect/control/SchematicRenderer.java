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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SchematicRenderer {

	private final Map<RenderType, SuperByteBuffer> bufferCache = new HashMap<>(getLayerCount());
	private final Set<RenderType> usedBlockRenderLayers = new HashSet<>(getLayerCount());
	private final Set<RenderType> startedBufferBuilders = new HashSet<>(getLayerCount());
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
		buffer.getBuffer(RenderType.solid());
		for (RenderType layer : RenderType.chunkBufferLayers()) {
			if (!usedBlockRenderLayers.contains(layer))
				continue;
			SuperByteBuffer superByteBuffer = bufferCache.get(layer);
			superByteBuffer.renderInto(ms, buffer.getBuffer(layer));
		}

		ms.popPose();
	}

	private void redraw(Minecraft minecraft) {
		usedBlockRenderLayers.clear();
		startedBufferBuilders.clear();

		final BlockAndTintGetter blockAccess = schematic.getMaterializedSketch();
		final BlockRenderDispatcher blockRendererDispatcher = minecraft.getBlockRenderer();

		Map<RenderType, ByteBufferBuilder> byteBuffers = new HashMap<>();
		Map<RenderType, BufferBuilder> buffers = new HashMap<>();
		PoseStack ms = new PoseStack();

		BlockPos.betweenClosedStream(schematic.getLocalBounds()
			.toMBB())
			.forEach(localPos -> {
				ms.pushPose();
				MatrixStacker.of(ms)
					.translate(localPos);
				BlockPos pos = localPos.offset(anchor);
				BlockState state = blockAccess.getBlockState(pos);

				for (RenderType blockRenderLayer : RenderType.chunkBufferLayers()) {
					if (blockRenderLayer != ItemBlockRenderTypes.getChunkRenderType(state))
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
						blockRendererDispatcher.renderBatched(state, pos, blockAccess, ms,
								bufferBuilder, true, minecraft.level.random);
						usedBlockRenderLayers.add(blockRenderLayer);
					}
				}

				ms.popPose();
			});

		// finishDrawing
		for (RenderType layer : RenderType.chunkBufferLayers()) {
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

	private static int getLayerCount() {
		return RenderType.chunkBufferLayers()
			.size();
	}

}
