package com.timmie.mightyarchitect.foundation.utility;

import net.minecraft.client.Minecraft;

public class AnimationTickHolder {

	public static int ticks;

	public static void tick() {
		ticks++;
	}

	public static float getRenderTick() {
		//? if >=1.21.4 {
		return ticks + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		//?} else {
		/*return ticks + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
		*///?}
	}

}
