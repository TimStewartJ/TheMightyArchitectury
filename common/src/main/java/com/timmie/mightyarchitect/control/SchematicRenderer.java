package com.timmie.mightyarchitect.control;

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
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.control.compose.Cuboid;
import com.timmie.mightyarchitect.foundation.compat.McCompat;
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
import com.timmie.mightyarchitect.foundation.MightyBuffers;
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
import java.util.Iterator;
//? if >=1.21 {
import java.util.ArrayList;
import java.util.List;
//?} else {
/*
*///?}
import java.util.Map;
import java.util.Set;

public class SchematicRenderer {

	/**
	 * Blocks tesselated per client tick while a redraw is in flight.
	 * <p>
	 * Tesselating a whole build volume takes as long as it takes - a 50x20x50 house is over fifty
	 * thousand blocks - and it used to happen inside one tick, which is a visible freeze every time
	 * a palette changed. The previous geometry keeps rendering until the new geometry is complete,
	 * so spreading the work costs nothing visually.
	 */
	private static final int BLOCK_BUDGET_PER_TICK = 4096;

	/**
	 * Volumes at or below this are done in one go. Below the budget there is nothing to spread, and
	 * finishing immediately keeps small edits feeling instant.
	 */
	private static final int INLINE_VOLUME_LIMIT = BLOCK_BUDGET_PER_TICK;

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
	*///?} else {
	/*private static int getLayerCount() {
		return RenderType.chunkBufferLayers()
			.size();
	}
	*///?}

	// The layer type is the only thing that differs here, so these declarations are shared by the
	// three arms above rather than repeated inside each of them.
	//? if >=1.21.6 {
	/** Complete geometry, which is what {@link #render} draws. */
	private final Map<ChunkSectionLayer, SuperByteBuffer> bufferCache = new HashMap<>(CHUNK_SECTION_LAYERS.length);
	private final Set<ChunkSectionLayer> usedBlockRenderLayers = new HashSet<>(CHUNK_SECTION_LAYERS.length);

	/** Geometry for the redraw in flight. Swapped into the fields above only once it is complete. */
	private final Map<ChunkSectionLayer, BufferBuilder> pendingBuffers = new HashMap<>(CHUNK_SECTION_LAYERS.length);
	private final Set<ChunkSectionLayer> startedBufferBuilders = new HashSet<>(CHUNK_SECTION_LAYERS.length);
	private final Set<ChunkSectionLayer> pendingUsedLayers = new HashSet<>(CHUNK_SECTION_LAYERS.length);
	//?} else {
	/*private final Map<RenderType, SuperByteBuffer> bufferCache = new HashMap<>(getLayerCount());
	private final Set<RenderType> usedBlockRenderLayers = new HashSet<>(getLayerCount());

	private final Map<RenderType, BufferBuilder> pendingBuffers = new HashMap<>(getLayerCount());
	private final Set<RenderType> startedBufferBuilders = new HashSet<>(getLayerCount());
	private final Set<RenderType> pendingUsedLayers = new HashSet<>(getLayerCount());
	*///?}

	// Only ever closed as a group, never looked up by layer, so this needs no layer type - which
	// is what keeps it to one guard instead of one per layer-type arm.
	//? if >=1.21 {
	private final List<ByteBufferBuilder> pendingByteBuffers = new ArrayList<>();
	//?} else {
	/*
	*///?}

	private boolean active;
	private boolean changed;
	private Schematic schematic;
	private BlockPos anchor;

	/** Positions still to tesselate, or null when no redraw is in flight. */
	private Iterator<BlockPos> pendingPositions;
	/** The sketch the redraw in flight is reading, pinned so it cannot change under the job. */
	private TemplateBlockAccess pendingSketch;
	private int redrawBudget;
	private boolean wasMaterializing;
	/** Last value of {@link McCompat#modelGeneration}, to spot a resource reload. */
	private Object modelGeneration;

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
		if (!active && pendingPositions != null) {
			// A paused job would sit on its buffers indefinitely, so drop it and record that the
			// geometry is owed instead - going inactive and back is always followed by a display()
			// or an update() today, but this does not depend on that staying true.
			abandonRedraw();
			changed = true;
		}
	}

	public void update() {
		changed = true;
	}

	public void tick() {
		if (!active)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null)
			return;

		// F3+T rebakes every model and re-stitches the block atlas, which moves the sprite
		// coordinates already baked into the geometry below. Nothing notifies a mod, so notice.
		Object generation = McCompat.modelGeneration(mc);
		if (modelGeneration != null && modelGeneration != generation)
			changed = true;
		modelGeneration = generation;

		// The palette is applied to the volume before it can be tesselated, and both are budgeted,
		// so let one finish before starting the other.
		boolean materializing = schematic != null && schematic.advanceMaterialization();
		if (wasMaterializing && !materializing)
			changed = true;
		wasMaterializing = materializing;
		if (materializing)
			return;

		if (changed) {
			startRedraw();
			changed = false;
		}

		if (pendingPositions != null)
			advanceRedraw(mc, redrawBudget);
	}

	public void render(PoseStack ms, MightyBuffers buffer) {
		if (!active)
			return;

		ms.pushPose();
		ms.translate(anchor.getX(), anchor.getY(), anchor.getZ());
		//? if >=26.2 {
		/*// No ordering hint here: 26.2 sorts submitted geometry itself, and seeding an empty buffer
		// would only create a recording that never gets submitted.
		for (ChunkSectionLayer layer : CHUNK_SECTION_LAYERS) {
		*///?} else if >=26 {
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
			// A used layer always has a buffer, but a null here would take the whole frame down
			// rather than one layer of one schematic.
			if (superByteBuffer == null)
				continue;
			//? if >=1.21.6 {
			superByteBuffer.renderInto(ms, buffer.getBuffer(layerToRenderType(layer)));
			//?} else {
			/*superByteBuffer.renderInto(ms, buffer.getBuffer(layer));
			*///?}
		}

		ms.popPose();
	}

	/**
	 * Begins a fresh redraw, discarding whatever a previous one had staged.
	 * <p>
	 * The sketch is pinned here rather than read per slice, so a materialization finishing mid-job
	 * cannot make one half of the geometry disagree with the other. That materialization sets
	 * {@link #changed}, which restarts this from the top on the next tick.
	 */
	private void startRedraw() {
		abandonRedraw();

		if (schematic == null)
			return;
		TemplateBlockAccess sketch = schematic.getMaterializedSketch();
		Cuboid localBounds = schematic.getLocalBounds();
		if (sketch == null || localBounds == null)
			return;

		pendingSketch = sketch;
		pendingPositions = BlockPos.betweenClosedStream(localBounds.toMBB())
			.iterator();
		// betweenClosedStream walks corner to corner inclusive, so the volume is one more than the
		// size on each axis.
		long volume = (long) (localBounds.width + 1) * (localBounds.height + 1) * (localBounds.length + 1);
		redrawBudget = volume <= INLINE_VOLUME_LIMIT ? Integer.MAX_VALUE : BLOCK_BUDGET_PER_TICK;
	}

	private void advanceRedraw(Minecraft minecraft, int budget) {
		final BlockAndTintGetter blockAccess = pendingSketch;
		//? if >=26 {
		final ModelBlockRenderer blockRenderer = new ModelBlockRenderer(
			minecraft.options.ambientOcclusion().get(), true, minecraft.getBlockColors());
		final boolean cutoutLeaves = minecraft.options.cutoutLeaves().get();
		//?} else if >=1.21.6 {
		/*final BlockRenderDispatcher blockRendererDispatcher = minecraft.getBlockRenderer();
		final RandomSource random = RandomSource.create();
		PoseStack ms = new PoseStack();
		*///?} else {
		/*final BlockRenderDispatcher blockRendererDispatcher = minecraft.getBlockRenderer();
		PoseStack ms = new PoseStack();
		*///?}

		int remaining = budget;
		while (remaining > 0 && pendingPositions.hasNext()) {
			remaining--;
			final BlockPos localPos = pendingPositions.next();
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
					if (!pendingBuffers.containsKey(blockRenderLayer)) {
						int bufferSize = MightyClient.iris_presence ? 262144 : 2097152;
						ByteBufferBuilder byteBuffer = new ByteBufferBuilder(bufferSize);
						pendingByteBuffers.add(byteBuffer);
						pendingBuffers.put(blockRenderLayer, com.timmie.mightyarchitect.foundation.compat.McCompat.quadBuffer(byteBuffer, DefaultVertexFormat.BLOCK));
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
					pendingBuffers.get(blockRenderLayer).putBlockBakedQuad(x, y, z, quad, instance);
					pendingUsedLayers.add(blockRenderLayer);
				};
					//?} else if >=1.21 {
					/*if (!pendingBuffers.containsKey(blockRenderLayer))
				{
					int bufferSize = MightyClient.iris_presence ? 262144 : 2097152;
					ByteBufferBuilder byteBuffer = new ByteBufferBuilder(bufferSize);
					pendingByteBuffers.add(byteBuffer);
					pendingBuffers.put(blockRenderLayer, com.timmie.mightyarchitect.foundation.compat.McCompat.quadBuffer(byteBuffer, DefaultVertexFormat.BLOCK));
					startedBufferBuilders.add(blockRenderLayer);
				}
					*///?} else {
					/*if (!pendingBuffers.containsKey(blockRenderLayer))
				{
					int bufferSize = MightyClient.iris_presence ? 262144 : 2097152;
					pendingBuffers.put(blockRenderLayer, new BufferBuilder(bufferSize));
				}
				if (startedBufferBuilders.add(blockRenderLayer))
					pendingBuffers.get(blockRenderLayer).begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
					*///?}

				//? if >=26 {
				BlockStateModel model = minecraft.getModelManager()
					.getBlockStateModelSet()
					.get(state);
				blockRenderer.tesselateBlock(output, localPos.getX(), localPos.getY(), localPos.getZ(),
					blockAccess, pos, state, model, state.getSeed(pos));
				//?} else if >=1.21.6 {
				/*BufferBuilder bufferBuilder = pendingBuffers.get(blockRenderLayer);

				if (state.getRenderShape() == RenderShape.MODEL)
				{
					// In 1.21.6, renderBatched takes List<BlockModelPart> instead of RandomSource
					BlockStateModel model = blockRendererDispatcher.getBlockModel(state);
					List<BlockModelPart> parts = model.collectParts(random);
					blockRendererDispatcher.renderBatched(state, pos, blockAccess, ms,
							bufferBuilder, true, parts);
					pendingUsedLayers.add(blockRenderLayer);
				}
				*///?} else {
				/*BufferBuilder bufferBuilder = pendingBuffers.get(blockRenderLayer);

				if (state.getRenderShape() == RenderShape.MODEL)
				{
					blockRendererDispatcher.renderBatched(state, pos, blockAccess, ms,
							bufferBuilder, true, minecraft.level.random);
					pendingUsedLayers.add(blockRenderLayer);
				}
				*///?}
			}
			//? if >=26 {
			//?} else {
			/*
			ms.popPose();
			*///?}
		}

		if (!pendingPositions.hasNext())
			finishRedraw();
	}

	/**
	 * Publishes the staged geometry.
	 * <p>
	 * The cache is emptied first rather than written over, so a layer that stopped being used - the
	 * glass in a palette swapped for stone, say - stops being drawn instead of staying behind as
	 * whatever it was last time.
	 */
	private void finishRedraw() {
		bufferCache.clear();
		usedBlockRenderLayers.clear();

		// finishDrawing
		//? if >=1.21.6 {
		for (ChunkSectionLayer layer : CHUNK_SECTION_LAYERS) {
		//?} else {
		/*for (RenderType layer : RenderType.chunkBufferLayers()) {
		*///?}
			if (!startedBufferBuilders.contains(layer))
				continue;
			BufferBuilder buf = pendingBuffers.get(layer);
			//? if >=1.21 {
			MeshData meshData = buf.build();
			if (meshData != null) {
				bufferCache.put(layer, new SuperByteBuffer(meshData));
			}
			//?} else {
			/*var renderedBuffer = buf.end();
			if (renderedBuffer != null) {
				bufferCache.put(layer, new SuperByteBuffer(renderedBuffer));
			}
			*///?}
		}

		usedBlockRenderLayers.addAll(pendingUsedLayers);
		clearPending();
	}

	/** Drops a redraw that was superseded or is no longer wanted, without publishing it. */
	private void abandonRedraw() {
		if (pendingPositions == null)
			return;
		clearPending();
	}

	private void clearPending() {
		pendingPositions = null;
		pendingSketch = null;
		pendingBuffers.clear();
		startedBufferBuilders.clear();
		pendingUsedLayers.clear();
		//? if >=1.21 {
		// Native memory, so it is freed here rather than left to the collector - and here rather
		// than only on completion, because an abandoned redraw has the same buffers to release.
		for (ByteBufferBuilder byteBuffer : pendingByteBuffers)
			byteBuffer.close();
		pendingByteBuffers.clear();
		//?} else {
		/*
		*///?}
	}
}
