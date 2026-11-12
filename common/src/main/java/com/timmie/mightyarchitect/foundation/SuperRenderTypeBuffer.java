package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.timmie.mightyarchitect.foundation.compat.McCompat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
//? if >=1.21 {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
//?} else {
/*import com.mojang.blaze3d.vertex.BufferBuilder;
*///?}
//? if >=1.21.11 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
*///?}
// 26.2 removed MultiBufferSource along with immediate-mode drawing, and introduced the frame's
// SubmitNodeCollector in its place.
//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelBakery;

//? if >=1.21 {
import java.util.SequencedMap;
//?} else {
/*import java.util.SortedMap;
*///?}

/**
 * The mod's three-layer draw surface: an early, a default and a late phase, so world rendering can
 * order itself against vanilla geometry.
 *
 * <p>Two eras sit behind that. Up to 26.1 a phase owns vanilla immediate-mode buffers and flushes
 * them. 26.2 removed immediate-mode drawing entirely - a {@code RenderType} no longer draws, it
 * describes a pipeline, and geometry reaches the screen by being submitted to the frame's
 * {@code SubmitNodeCollector} - so a phase records the vertex calls and replays them inside a
 * {@code submitCustomGeometry} callback when vanilla draws that phase.
 *
 * <p>Only {@code Phase} differs between the two. The class shape, the singleton and the six
 * entry points below are shared, so adding a phase or changing draw order cannot be done to one
 * era and missed on the other.
 */
//? if >=26.2 {
/*public class SuperRenderTypeBuffer implements MightyBuffers {
*///?} else {
public class SuperRenderTypeBuffer implements MultiBufferSource, MightyBuffers {
//?}

	static SuperRenderTypeBuffer instance;

	public static SuperRenderTypeBuffer getInstance() {
		if (instance == null)
			instance = new SuperRenderTypeBuffer();
		return instance;
	}

	//? if >=26.2 {
	/*// The collector is only valid for the frame the loader hook handed it to us in.
	private static SubmitNodeCollector collector;
	// Recorded vertices are already transformed, so submissions carry an identity pose.
	private static final PoseStack IDENTITY = new PoseStack();

	public static void beginFrame(SubmitNodeCollector frameCollector) {
		collector = frameCollector;
		// Safe to recycle here but not at submit time: by the time the next collect hook runs,
		// vanilla has already replayed last frame's callbacks and dropped its references.
		getInstance().recycle();
	}
	*///?}

	// The argument is the phase's draw order within the frame, which only 26.2 has a use for.
	private final Phase earlyBuffer = new Phase(0);
	private final Phase defaultBuffer = new Phase(1);
	private final Phase lateBuffer = new Phase(2);

	//? if >=26.2 {
	/*private void recycle() {
		earlyBuffer.recycle();
		defaultBuffer.recycle();
		lateBuffer.recycle();
	}

	// Draws geometry baked once, with the given pose applied as it is handed to vanilla. Used for
	// the schematic preview, which does not change from frame to frame: re-transforming and
	// re-recording every vertex each frame cost more than the rest of the frame on a large build.
	public void submitStatic(RenderType type, StaticGeometry geometry, PoseStack ms) {
		if (collector == null || geometry.isEmpty())
			return;
		collector.order(defaultBuffer.order)
			.submitCustomGeometry(ms, type, geometry::emit);
	}

	// Vertices captured once from any VertexConsumer writer, kept in flat arrays and written out in
	// one call per vertex. The single 11-argument addVertex is vanilla's fast path for block and
	// entity buffers; a shader-extended buffer falls back to the individual setters.
	public static final class StaticGeometry implements VertexConsumer {

		private static final int FLOATS = 8;
		private static final int INTS = 3;

		private float[] floats = new float[FLOATS * 64];
		private int[] ints = new int[INTS * 64];
		private int vertexCount;

		public boolean isEmpty() {
			return vertexCount == 0;
		}

		void emit(PoseStack.Pose pose, VertexConsumer consumer) {
			org.joml.Matrix4f matrix = pose.pose();
			org.joml.Matrix3f normalMatrix = pose.normal();
			org.joml.Vector3f position = new org.joml.Vector3f();
			org.joml.Vector3f normal = new org.joml.Vector3f();
			for (int v = 0; v < vertexCount; v++) {
				int f = v * FLOATS;
				int i = v * INTS;
				matrix.transformPosition(floats[f], floats[f + 1], floats[f + 2], position);
				normalMatrix.transform(floats[f + 5], floats[f + 6], floats[f + 7], normal);
				consumer.addVertex(position.x, position.y, position.z, ints[i], floats[f + 3], floats[f + 4],
					ints[i + 1], ints[i + 2], normal.x, normal.y, normal.z);
			}
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			if ((vertexCount + 1) * FLOATS > floats.length) {
				floats = java.util.Arrays.copyOf(floats, floats.length * 2);
				ints = java.util.Arrays.copyOf(ints, ints.length * 2);
			}
			int f = vertexCount * FLOATS;
			int i = vertexCount * INTS;
			floats[f] = x;
			floats[f + 1] = y;
			floats[f + 2] = z;
			ints[i] = -1;
			ints[i + 1] = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
			vertexCount++;
			return this;
		}

		private int f() {
			return (vertexCount - 1) * FLOATS;
		}

		private int i() {
			return (vertexCount - 1) * INTS;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			ints[i()] = (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
			return this;
		}

		@Override
		public VertexConsumer setColor(int color) {
			ints[i()] = color;
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			floats[f() + 3] = u;
			floats[f() + 4] = v;
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			ints[i() + 1] = u & 0xFFFF | v << 16;
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			ints[i() + 2] = u & 0xFFFF | v << 16;
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			floats[f() + 5] = x;
			floats[f() + 6] = y;
			floats[f() + 7] = z;
			return this;
		}

		@Override
		public VertexConsumer setLineWidth(float width) {
			return this;
		}

		// 26.3 only; no @Override for the same reason as RecordedGeometry.setUv3.
		public VertexConsumer setUv3(float u, float v) {
			return this;
		}
	}
	*///?}

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
		earlyBuffer.drawAll();
		defaultBuffer.drawAll();
		lateBuffer.drawAll();
	}

	public void draw(RenderType type) {
		earlyBuffer.drawOne(type);
		defaultBuffer.drawOne(type);
		lateBuffer.drawOne(type);
	}

	//? if >=26.2 {
	/*private static class Phase {

		// Vanilla replays submissions in this order.
		private final int order;

		// Kept across frames: the recordings are the per-frame scratch buffers, and reallocating
		// them every frame is what made this path allocation-bound on large schematics.
		private final SequencedMap<RenderType, RecordedGeometry> recordings = new Object2ObjectLinkedOpenHashMap<>();

		Phase(int order) {
			this.order = order;
		}

		VertexConsumer buffer(RenderType type) {
			return recordings.computeIfAbsent(type, ignored -> new RecordedGeometry());
		}

		void recycle() {
			recordings.values().forEach(RecordedGeometry::reset);
		}

		void drawAll() {
			recordings.forEach(this::submit);
		}

		void drawOne(RenderType type) {
			RecordedGeometry geometry = recordings.get(type);
			if (geometry != null)
				submit(type, geometry);
		}

		private void submit(RenderType type, RecordedGeometry geometry) {
			if (collector == null || geometry.isEmpty() || geometry.submitted)
				return;
			// Never reset the recording here: the callback replays it after this returns.
			geometry.submitted = true;
			collector.order(order)
				.submitCustomGeometry(IDENTITY, type, (pose, consumer) -> geometry.replay(consumer));
		}
	}

	// Records the VertexConsumer calls made during the frame so they can be replayed later. Only the
	// non-default methods need recording; everything else on VertexConsumer funnels into them.
	private static class RecordedGeometry implements VertexConsumer {

		private static final int ADD_VERTEX = 0;
		private static final int SET_COLOR = 1;
		private static final int SET_COLOR_PACKED = 2;
		private static final int SET_UV = 3;
		private static final int SET_UV1 = 4;
		private static final int SET_UV2 = 5;
		private static final int SET_NORMAL = 6;
		private static final int SET_LINE_WIDTH = 7;
		private static final int SET_UV3 = 8;

		// Ops and their arguments interleaved; floats are stored as raw bits.
		private final IntArrayList calls = new IntArrayList();
		private boolean submitted;

		boolean isEmpty() {
			return calls.isEmpty();
		}

		void reset() {
			// clear() keeps the backing array, so a steady-state frame allocates nothing here.
			calls.clear();
			submitted = false;
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
					case SET_UV3 -> {
						McCompat.setUv3(consumer, f(i), f(i + 1));
						i += 2;
					}
					default -> throw new IllegalStateException("Unknown recorded vertex op: " + op);
				}
			}
		}

		private float f(int index) {
			return Float.intBitsToFloat(calls.getInt(index));
		}

		// Written out per arity rather than with varargs: a varargs helper allocates an array on
		// every call, which at five calls per vertex dominated this path's allocation profile.
		private void push(int op, float a) {
			calls.add(op);
			calls.add(Float.floatToRawIntBits(a));
		}

		private void push(int op, float a, float b) {
			calls.add(op);
			calls.add(Float.floatToRawIntBits(a));
			calls.add(Float.floatToRawIntBits(b));
		}

		private void push(int op, float a, float b, float c) {
			calls.add(op);
			calls.add(Float.floatToRawIntBits(a));
			calls.add(Float.floatToRawIntBits(b));
			calls.add(Float.floatToRawIntBits(c));
		}

		private void pushInts(int op, int a) {
			calls.add(op);
			calls.add(a);
		}

		private void pushInts(int op, int a, int b) {
			calls.add(op);
			calls.add(a);
			calls.add(b);
		}

		private void pushInts(int op, int a, int b, int c, int d) {
			calls.add(op);
			calls.add(a);
			calls.add(b);
			calls.add(c);
			calls.add(d);
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

		// VertexConsumer gained this channel in 26.3. Deliberately no @Override: 26.2 compiles this
		// same arm against an interface without it, and a guard cannot nest in here.
		public VertexConsumer setUv3(float u, float v) {
			push(SET_UV3, u, v);
			return this;
		}
	}
	*///?} else {
	private static class Phase {

		// 1.21 swapped the immediate-mode buffer pool from BufferBuilder to ByteBufferBuilder and
		// the map type from fastutil's SortedMap view to SequencedMap.
		//? if >=1.21 {
		private final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers = Util.make(new Object2ObjectLinkedOpenHashMap<>(), map -> {
		//?} else {
		/*private final SortedMap<RenderType, BufferBuilder> fixedBuffers = Util.make(new Object2ObjectLinkedOpenHashMap<>(), map -> {
		*///?}
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
		//? if >=1.21 {
		private final MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediateWithBuffers(fixedBuffers, new ByteBufferBuilder(256));

		private static void put(Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> map, RenderType type) {
			map.put(type, new ByteBufferBuilder(type.bufferSize()));
		}
		//?} else {
		/*private final MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediateWithBuffers(fixedBuffers, new BufferBuilder(256));

		private static void put(Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> map, RenderType type) {
			map.put(type, new BufferBuilder(type.bufferSize()));
		}
		*///?}

		// The draw order only means something from 26.2; here the phases are flushed in call order.
		Phase(int order) {
		}

		VertexConsumer buffer(RenderType type) {
			return bufferSource.getBuffer(type);
		}

		void drawAll() {
			bufferSource.endBatch();
		}

		void drawOne(RenderType type) {
			bufferSource.endBatch(type);
		}
	}
	//?}

}
