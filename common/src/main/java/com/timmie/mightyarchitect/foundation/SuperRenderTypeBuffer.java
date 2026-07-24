//? if >=26 {
package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelBakery;

import java.util.SequencedMap;

public class SuperRenderTypeBuffer implements MultiBufferSource {

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
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.solidMovingBlock());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.cutoutMovingBlock());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock());
			put(map, Sheets.cutoutBlockSheet());
			put(map, Sheets.cutoutBlockItemSheet());
			put(map, Sheets.translucentBlockItemSheet());
			put(map, Sheets.cutoutItemSheet());
			put(map, Sheets.translucentItemSheet());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.armorEntityGlint());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.glint());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.glintTranslucent());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.entityGlint());
			put(map, net.minecraft.client.renderer.rendertype.RenderTypes.waterMask());
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
//?} else if >=1.21.11 {
/*package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelBakery;

import java.util.SequencedMap;

public class SuperRenderTypeBuffer implements MultiBufferSource {

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
			put(map, Sheets.solidBlockSheet());
			put(map, Sheets.cutoutBlockSheet());
			put(map, Sheets.bannerSheet());
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

}*/
//?} else if >=1.21.4 {
/*package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelBakery;

import java.util.SequencedMap;

public class SuperRenderTypeBuffer implements MultiBufferSource {

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
			put(map, Sheets.solidBlockSheet());
			put(map, Sheets.cutoutBlockSheet());
			put(map, Sheets.bannerSheet());
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

}*/
//?} else {
/*package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelBakery;

import java.util.SequencedMap;

public class SuperRenderTypeBuffer implements MultiBufferSource {

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
			put(map, Sheets.solidBlockSheet());
			put(map, Sheets.cutoutBlockSheet());
			put(map, Sheets.bannerSheet());
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

}*///?}
