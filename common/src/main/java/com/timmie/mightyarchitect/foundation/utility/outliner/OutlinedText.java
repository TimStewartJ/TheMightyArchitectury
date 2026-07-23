package com.timmie.mightyarchitect.foundation.utility.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.foundation.utility.HudTextBuffer;
import com.timmie.mightyarchitect.foundation.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
		if (text == null || text.isEmpty())
			return;

		Minecraft mc = Minecraft.getInstance();
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Vec3 vec = VecHelper.lerp(prevLocation, location, pt);

		// 26.1 no longer renders immediate-mode world text into a custom buffer, so submit the label
		// to be drawn as HUD text (projected from this world position) during the overlay pass.
		HudTextBuffer.submit(vec, text, params.color);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
