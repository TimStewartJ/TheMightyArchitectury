//? if >=26.2 {
/*package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.SequencedMap;

// 26.2 removed MultiBufferSource and immediate-mode drawing: a RenderType no longer draws, it
// describes a pipeline, and geometry reaches the screen by being submitted to the frame's
// SubmitNodeCollector. The mod still emits vertices the old way, so each render type's vertices are
// recorded here and replayed inside a submitCustomGeometry callback when vanilla draws that phase.
public class SuperRenderTypeBuffer implements MightyBuffers {

	static SuperRenderTypeBuffer instance;

	public static SuperRenderTypeBuffer getInstance() {
		if (instance == null)
			instance = new SuperRenderTypeBuffer();
		return instance;
	}

	// The collector is only valid for the frame the loader hook handed it to us in.
	private static SubmitNodeCollector collector;
	// Recorded vertices are already transformed, so submissions carry an identity pose.
	private static final PoseStack IDENTITY = new PoseStack();

	public static void beginFrame(SubmitNodeCollector frameCollector) {
		collector = frameCollector;
	}

	private final Phase earlyBuffer = new Phase();
	private final Phase defaultBuffer = new Phase();
	private final Phase lateBuffer = new Phase();

	public VertexConsumer getEarlyBuffer(RenderType type) {
		return earlyBuffer.buffer(type);
	}

	@Override
	public VertexConsumer getBuffer(RenderType type) {
		return defaultBuffer.buffer(type);
	}

	public VertexConsumer getLateBuffer(RenderType type) {
		return lateBuffer.buffer(type);
	}

	public void draw() {
		earlyBuffer.submitAll(0);
		defaultBuffer.submitAll(1);
		lateBuffer.submitAll(2);
	}

	public void draw(RenderType type) {
		earlyBuffer.submitOne(0, type);
		defaultBuffer.submitOne(1, type);
		lateBuffer.submitOne(2, type);
	}

	private static class Phase {

		private final SequencedMap<RenderType, RecordedGeometry> recordings = new Object2ObjectLinkedOpenHashMap<>();

		VertexConsumer buffer(RenderType type) {
			return recordings.computeIfAbsent(type, ignored -> new RecordedGeometry());
		}

		void submitAll(int order) {
			recordings.forEach((type, geometry) -> submit(order, type, geometry));
			// The callbacks run after this returns, so drop the references rather than reusing them.
			recordings.clear();
		}

		void submitOne(int order, RenderType type) {
			RecordedGeometry geometry = recordings.remove(type);
			if (geometry != null)
				submit(order, type, geometry);
		}

		private void submit(int order, RenderType type, RecordedGeometry geometry) {
			if (collector == null || geometry.isEmpty())
				return;
			collector.order(order)
				.submitCustomGeometry(IDENTITY, type, (pose, consumer) -> geometry.replay(consumer));
		}
	}

	// Records the VertexConsumer calls made during the frame so they can be replayed later. Only the
	// eight non-default methods need recording; everything else on VertexConsumer funnels into them.
	private static class RecordedGeometry implements VertexConsumer {

		private static final int ADD_VERTEX = 0;
		private static final int SET_COLOR = 1;
		private static final int SET_COLOR_PACKED = 2;
		private static final int SET_UV = 3;
		private static final int SET_UV1 = 4;
		private static final int SET_UV2 = 5;
		private static final int SET_NORMAL = 6;
		private static final int SET_LINE_WIDTH = 7;

		// Ops and their arguments interleaved; floats are stored as raw bits.
		private final IntArrayList calls = new IntArrayList();

		boolean isEmpty() {
			return calls.isEmpty();
		}

		void replay(VertexConsumer consumer) {
			int i = 0;
			int size = calls.size();
			while (i < size) {
				int op = calls.getInt(i++);
				switch (op) {
					case ADD_VERTEX -> {
						consumer.addVertex(f(i), f(i + 1), f(i + 2));
						i += 3;
					}
					case SET_COLOR -> {
						consumer.setColor(calls.getInt(i), calls.getInt(i + 1), calls.getInt(i + 2), calls.getInt(i + 3));
						i += 4;
					}
					case SET_COLOR_PACKED -> {
						consumer.setColor(calls.getInt(i));
						i += 1;
					}
					case SET_UV -> {
						consumer.setUv(f(i), f(i + 1));
						i += 2;
					}
					case SET_UV1 -> {
						consumer.setUv1(calls.getInt(i), calls.getInt(i + 1));
						i += 2;
					}
					case SET_UV2 -> {
						consumer.setUv2(calls.getInt(i), calls.getInt(i + 1));
						i += 2;
					}
					case SET_NORMAL -> {
						consumer.setNormal(f(i), f(i + 1), f(i + 2));
						i += 3;
					}
					case SET_LINE_WIDTH -> {
						consumer.setLineWidth(f(i));
						i += 1;
					}
					default -> throw new IllegalStateException("Unknown recorded vertex op: " + op);
				}
			}
		}

		private float f(int index) {
			return Float.intBitsToFloat(calls.getInt(index));
		}

		private void push(int op, float... args) {
			calls.add(op);
			for (float arg : args)
				calls.add(Float.floatToRawIntBits(arg));
		}

		private void pushInts(int op, int... args) {
			calls.add(op);
			for (int arg : args)
				calls.add(arg);
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			push(ADD_VERTEX, x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			pushInts(SET_COLOR, red, green, blue, alpha);
			return this;
		}

		@Override
		public VertexConsumer setColor(int color) {
			pushInts(SET_COLOR_PACKED, color);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			push(SET_UV, u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			pushInts(SET_UV1, u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			pushInts(SET_UV2, u, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			push(SET_NORMAL, x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setLineWidth(float width) {
			push(SET_LINE_WIDTH, width);
			return this;
		}
	}
}
*///?} else {
package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
//? if >=1.21.11 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
*///?}
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelBakery;

import java.util.SequencedMap;

public class SuperRenderTypeBuffer implements MultiBufferSource, MightyBuffers {

	static SuperRenderTypeBuffer instance;

	public static SuperRenderTypeBuffer getInstance() {
		if (instance == null)
			instance = new SuperRenderTypeBuffer();
		return instance;
	}

	SuperRenderTypeBufferPhase earlyBuffer;
	SuperRenderTypeBufferPhase defaultBuffer;
	SuperRenderTypeBufferPhase lateBuffer;

	public SuperRenderTypeBuffer() {
		earlyBuffer = new SuperRenderTypeBufferPhase();
		defaultBuffer = new SuperRenderTypeBufferPhase();
		lateBuffer = new SuperRenderTypeBufferPhase();
	}

	public VertexConsumer getEarlyBuffer(RenderType type) {
		return earlyBuffer.bufferSource.getBuffer(type);
	}

	@Override
	public VertexConsumer getBuffer(RenderType type) {
		return defaultBuffer.bufferSource.getBuffer(type);
	}

	public VertexConsumer getLateBuffer(RenderType type) {
		return lateBuffer.bufferSource.getBuffer(type);
	}

	public void draw() {
		earlyBuffer.bufferSource.endBatch();
		defaultBuffer.bufferSource.endBatch();
		lateBuffer.bufferSource.endBatch();
	}

	public void draw(RenderType type) {
		earlyBuffer.bufferSource.endBatch(type);
		defaultBuffer.bufferSource.endBatch(type);
		lateBuffer.bufferSource.endBatch(type);
	}

	private static class SuperRenderTypeBufferPhase {

		private final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers = Util.make(new Object2ObjectLinkedOpenHashMap<>(), map -> {
			//? if >=26 {
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.solidMovingBlock());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.cutoutMovingBlock());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock());
			//?} else {
			/*put(map, Sheets.solidBlockSheet());
			*///?}
			put(map, Sheets.cutoutBlockSheet());
			//? if >=26 {
			put(map, Sheets.cutoutBlockItemSheet());
			put(map, Sheets.translucentBlockItemSheet());
			put(map, Sheets.cutoutItemSheet());
			put(map, Sheets.translucentItemSheet());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.armorEntityGlint());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.glint());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.glintTranslucent());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.entityGlint());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.waterMask());
			//?} else if >=1.21.11 {
			/*put(map, Sheets.bannerSheet());
			put(map, Sheets.translucentItemSheet());
			put(map, Sheets.shieldSheet());
			put(map, Sheets.bedSheet());
			put(map, Sheets.shulkerBoxSheet());
			put(map, Sheets.signSheet());
			put(map, Sheets.hangingSignSheet());
			put(map, Sheets.chestSheet());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.armorEntityGlint());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.glint());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.glintTranslucent());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.entityGlint());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.waterMask());
			*///?} else if >=1.21.4 {
			/*put(map, Sheets.bannerSheet());
			put(map, Sheets.translucentItemSheet());
			put(map, Sheets.shieldSheet());
			put(map, Sheets.bedSheet());
			put(map, Sheets.shulkerBoxSheet());
			put(map, Sheets.signSheet());
			put(map, Sheets.hangingSignSheet());
			put(map, Sheets.chestSheet());
			put(map, RenderType.armorEntityGlint());
			put(map, RenderType.glint());
			put(map, RenderType.glintTranslucent());
			put(map, RenderType.entityGlint());
			put(map, RenderType.waterMask());
			*///?} else {
			/*put(map, Sheets.bannerSheet());
			put(map, Sheets.translucentCullBlockSheet());
			put(map, Sheets.shieldSheet());
			put(map, Sheets.bedSheet());
			put(map, Sheets.shulkerBoxSheet());
			put(map, Sheets.signSheet());
			put(map, Sheets.hangingSignSheet());
			put(map, Sheets.chestSheet());
			put(map, RenderType.armorEntityGlint());
			put(map, RenderType.glint());
			put(map, RenderType.glintTranslucent());
			put(map, RenderType.entityGlint());
			put(map, RenderType.entityGlintDirect());
			put(map, RenderType.waterMask());
			*///?}
			put(map, RenderTypes.getOutlineSolid());
			ModelBakery.DESTROY_TYPES.forEach((p_173062_) -> {
				put(map, p_173062_);
			});
		});
		private final BufferSource bufferSource = MultiBufferSource.immediateWithBuffers(fixedBuffers, new ByteBufferBuilder(256));

		private static void put(Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> map, RenderType type) {
			map.put(type, new ByteBufferBuilder(type.bufferSize()));
		}

	}

}
//?}
