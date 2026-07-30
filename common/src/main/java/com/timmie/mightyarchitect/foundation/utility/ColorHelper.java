package com.timmie.mightyarchitect.foundation.utility;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ColorHelper {

	public static int rainbowColor(int timeStep) {
		int localTimeStep = timeStep % 1536;
		int timeStepInPhase = localTimeStep % 256;
		int phaseBlue = localTimeStep / 256;
		int red = colorInPhase(phaseBlue + 4, timeStepInPhase);
		int green = colorInPhase(phaseBlue + 2, timeStepInPhase);
		int blue = colorInPhase(phaseBlue, timeStepInPhase);
		return (red << 16) + (green << 8) + (blue);
	}

	private static int colorInPhase(int phase, int progress) {
		phase = phase % 6;
		if (phase <= 1)
			return 0;
		if (phase == 2)
			return progress;
		if (phase <= 4)
			return 255;
		else
			return 255 - progress;
	}

	public static int mixColors(int color1, int color2, float w) {
		int r1 = (color1 >> 16);
		int g1 = (color1 >> 8) & 0xFF;
		int b1 = color1 & 0xFF;
		int r2 = (color2 >> 16);
		int g2 = (color2 >> 8) & 0xFF;
		int b2 = color2 & 0xFF;

		int color = ((int) (r1 + (r2 - r1) * w) << 16) + ((int) (g1 + (g2 - g1) * w) << 8) + (int) (b1 + (b2 - b1) * w);

		return color;
	}

	public static Vec3 getRGB(int color) {
		int r = (color >> 16);
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		return new Vec3(r, g, b).scale(1 / 256d);
	}

	/**
	 * Picks a backdrop for HUD text that its glyphs stay readable against. Callers pick a text
	 * colour without knowing what will sit behind it, so dark text gets a light plate and light
	 * text the darker default.
	 */
	public static int labelBackdrop(int color) {
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		int luma = (r * 299 + g * 587 + b * 114) / 1000;
		return luma < 128 ? 0xC0F0F0F0 : 0xC0101018;
	}

	public static int colorFromUUID(UUID uuid) {
		if (uuid == null)
			return 0x333333;
		return colorFromLong(uuid.getLeastSignificantBits());
	}

	public static int colorFromLong(long l) {
		int rainbowColor = ColorHelper.rainbowColor(String.valueOf(l).hashCode());
		return ColorHelper.mixColors(rainbowColor, 0xFFFFFF, .5f);
	}

}
