package com.timmie.mightyarchitect.foundation.utility.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=26 {
import com.timmie.mightyarchitect.foundation.utility.HudTextBuffer;
//?} else {
/*import com.timmie.mightyarchitect.foundation.RenderTypes;
*///?}
import com.timmie.mightyarchitect.foundation.utility.VecHelper;
//? if >=26 {
//?} else if >=1.21.10 {
/*import net.minecraft.client.Camera;
*///?} else {
/*
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.21.10 {
//?} else {
/*import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
*///?}
import net.minecraft.world.phys.Vec3;

public class OutlinedText extends Outline {

	private String text;
	Vec3 targetLocation;
	Vec3 location;
	Vec3 prevLocation;

	public OutlinedText() {
		setText("");
		targetLocation = Vec3.ZERO;
		location = Vec3.ZERO;
		prevLocation = Vec3.ZERO;
	}

	public void set(Vec3 location) {
		prevLocation = this.location = location;
	}

	public void target(Vec3 location) {
		targetLocation = location;
	}

	@Override
	public void tick() {
		super.tick();
		prevLocation = location;
		location = VecHelper.lerp(location, targetLocation, .5f);
	}

	@Override
	public void render(PoseStack ms, MultiBufferSource buffer) {
		//? if >=26 {
		if (text == null || text.isEmpty())
		//?} else {
		/*if (text == null)
		*///?}
			return;

		Minecraft mc = Minecraft.getInstance();
		//? if >=1.21.4 {
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		//?} else {
		/*float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
		*///?}
		Vec3 vec = VecHelper.lerp(prevLocation, location, pt);
		//? if >=26 {
		//?} else if >=1.21.10 {
		/*Camera camera = mc.gameRenderer.getMainCamera();
		float stringLength = mc.font.width(text);
		*///?} else {
		/*EntityRenderDispatcher renderManager = mc.getEntityRenderDispatcher();
		float stringLength = mc.font.width(text);
		*///?}

		//? if >=26 {
		// 26.1 no longer renders immediate-mode world text into a custom buffer, so submit the label
		// to be drawn as HUD text (projected from this world position) during the overlay pass.
		HudTextBuffer.submit(vec, text, params.color);
		//?} else if >=1.21.10 {
		/*ms.pushPose();
		ms.translate(vec.x, vec.y, vec.z);
		ms.mulPose(camera.rotation());

//		if (scalesUp) {
		double distance = mc.player.getEyePosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true))
			.distanceToSqr(vec);
		float scale = (float) (distance / 512f);
		ms.scale(2 + scale, 2 + scale, 2 + scale);
//		}

		float scaleMod = 0.025F;
		float f = -stringLength / 2;
		float h = mc.font.lineHeight;

		ms.pushPose();
		Vec3 v1 = new Vec3(-f + 2, -scaleMod * (h - 1), 0);
		Vec3 v2 = new Vec3(-f + 2, scaleMod, 0);
		Vec3 v3 = new Vec3(f - 2, scaleMod, 0);
		Vec3 v4 = new Vec3(f - 2, -scaleMod * (h - 1), 0);

		ms.pushPose();
		ms.scale(-scaleMod, 1, scaleMod);
		ms.translate(0, 0, .5f);

		putQuadUV(ms, buffer.getBuffer(RenderTypes.getOutlineSolid()), v1, v2, v3, v4,0, 0, 1, 1, null, true);

		ms.popPose();

		ms.scale(scaleMod, 1, 1);
		ms.translate(0, -2 * scaleMod, 0);
		renderCuboidLine(ms, buffer, v4, v1);
		ms.popPose();

		ms.pushPose();
		ms.scale(-scaleMod, -scaleMod, scaleMod);
		// Draw text using the font directly into the buffer source
		mc.font.drawInBatch(text, f, 0, params.color, false, ms.last().pose(), buffer, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
		ms.popPose();

		ms.popPose();
		*///?} else if >=1.21.6 {
		/*ms.pushPose();
		ms.translate(vec.x, vec.y, vec.z);
		ms.mulPose(renderManager.cameraOrientation());

//		if (scalesUp) {
		double distance = mc.player.getEyePosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true))
			.distanceToSqr(vec);
		float scale = (float) (distance / 512f);
		ms.scale(2 + scale, 2 + scale, 2 + scale);
//		}

		float scaleMod = 0.025F;
		float f = -stringLength / 2;
		float h = mc.font.lineHeight;

		ms.pushPose();
		Vec3 v1 = new Vec3(-f + 2, -scaleMod * (h - 1), 0);
		Vec3 v2 = new Vec3(-f + 2, scaleMod, 0);
		Vec3 v3 = new Vec3(f - 2, scaleMod, 0);
		Vec3 v4 = new Vec3(f - 2, -scaleMod * (h - 1), 0);

		ms.pushPose();
		ms.scale(-scaleMod, 1, scaleMod);
		ms.translate(0, 0, .5f);

		putQuadUV(ms, buffer.getBuffer(RenderTypes.getOutlineSolid()), v1, v2, v3, v4,0, 0, 1, 1, null, true);

		ms.popPose();

		ms.scale(scaleMod, 1, 1);
		ms.translate(0, -2 * scaleMod, 0);
		renderCuboidLine(ms, buffer, v4, v1);
		ms.popPose();

		ms.pushPose();
		ms.scale(-scaleMod, -scaleMod, scaleMod);
		// Draw text using the font directly into the buffer source
		mc.font.drawInBatch(text, f, 0, params.color, false, ms.last().pose(), buffer, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
		ms.popPose();

		ms.popPose();
		*///?} else if >=1.21.4 {
		/*ms.pushPose();
		ms.translate(vec.x, vec.y, vec.z);
		ms.mulPose(renderManager.cameraOrientation());

//		if (scalesUp) {
		double distance = mc.player.getEyePosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true))
			.distanceToSqr(vec);
		float scale = (float) (distance / 512f);
		ms.scale(2 + scale, 2 + scale, 2 + scale);
//		}

		float scaleMod = 0.025F;
		float f = -stringLength / 2;
		float h = mc.font.lineHeight;

		ms.pushPose();
		Vec3 v1 = new Vec3(-f + 2, -scaleMod * (h - 1), 0);
		Vec3 v2 = new Vec3(-f + 2, scaleMod, 0);
		Vec3 v3 = new Vec3(f - 2, scaleMod, 0);
		Vec3 v4 = new Vec3(f - 2, -scaleMod * (h - 1), 0);

		ms.pushPose();
		ms.scale(-scaleMod, 1, scaleMod);
		ms.translate(0, 0, .5f);

		putQuadUV(ms, buffer.getBuffer(RenderTypes.getOutlineSolid()), v1, v2, v3, v4,0, 0, 1, 1, null, true);

		ms.popPose();

		ms.scale(scaleMod, 1, 1);
		ms.translate(0, -2 * scaleMod, 0);
		renderCuboidLine(ms, buffer, v4, v1);
		ms.popPose();

		ms.pushPose();
		ms.scale(-scaleMod, -scaleMod, scaleMod);
		mc.font.drawInBatch(text, f, 0, params.color, false, ms.last().pose(), buffer, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
		ms.popPose();

		ms.popPose();
		*///?} else {
		/*ms.pushPose();
		ms.translate(vec.x, vec.y, vec.z);
		ms.mulPose(renderManager.cameraOrientation());

//		if (scalesUp) {
		double distance = mc.player.getEyePosition(mc.getTimer().getGameTimeDeltaPartialTick(true))
			.distanceToSqr(vec);
		float scale = (float) (distance / 512f);
		ms.scale(2 + scale, 2 + scale, 2 + scale);
//		}

		float scaleMod = 0.025F;
		float f = -stringLength / 2;
		float h = mc.font.lineHeight;

		ms.pushPose();
		Vec3 v1 = new Vec3(-f + 2, -scaleMod * (h - 1), 0);
		Vec3 v2 = new Vec3(-f + 2, scaleMod, 0);
		Vec3 v3 = new Vec3(f - 2, scaleMod, 0);
		Vec3 v4 = new Vec3(f - 2, -scaleMod * (h - 1), 0);

		ms.pushPose();
		ms.scale(-scaleMod, 1, scaleMod);
		ms.translate(0, 0, .5f);

		putQuadUV(ms, buffer.getBuffer(RenderTypes.getOutlineSolid()), v1, v2, v3, v4,0, 0, 1, 1, null, true);

		ms.popPose();

		ms.scale(scaleMod, 1, 1);
		ms.translate(0, -2 * scaleMod, 0);
		renderCuboidLine(ms, buffer, v4, v1);
		ms.popPose();

		ms.pushPose();
		ms.scale(-scaleMod, -scaleMod, scaleMod);
		mc.font.drawInBatch(text, f, 0, params.color, false, ms.last().pose(), buffer, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 0xF000F0);
		ms.popPose();

		ms.popPose();
		*///?}
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
