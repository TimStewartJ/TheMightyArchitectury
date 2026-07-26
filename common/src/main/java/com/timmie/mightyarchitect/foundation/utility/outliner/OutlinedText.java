package com.timmie.mightyarchitect.foundation.utility.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.foundation.utility.HudTextBuffer;
import com.timmie.mightyarchitect.foundation.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public class OutlinedText extends Outline {

	//*
	 //* Outline colours are written as plain RGB (the default is 0xFFFFFF), so the alpha byte is
	 //* zero. Font.drawInBatch takes the alpha at face value and renders nothing, unlike the GUI
	 //* text helpers which promote an unset alpha to opaque. Do the same promotion here so the
	 //* label is actually visible.
	 //
	private static int opaque(int color) {
		return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
	}

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
		//? if >=1.21.4 {
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		//?} else {
		/*float pt = mc.getTimer().getGameTimeDeltaPartialTick(true);
		*///?}
		Vec3 vec = VecHelper.lerp(prevLocation, location, pt);

		// Drawing this label as world geometry does not work: immediate-mode glyphs submitted to a
		// custom buffer never reach the screen, so the label's backing quad appears without any
		// text. Submit the anchor instead and let the overlay pass draw it as HUD text.
		HudTextBuffer.submit(vec, text, opaque(params.color));
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
