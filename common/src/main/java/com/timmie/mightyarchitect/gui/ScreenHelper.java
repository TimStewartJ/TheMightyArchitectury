package com.timmie.mightyarchitect.gui;

import com.timmie.mightyarchitect.foundation.compat.McCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ScreenHelper {

	private static Screen openedGuiNextTick;

	public static void onClientTick(Minecraft minecraft) {
		if (openedGuiNextTick != null) {
			McCompat.setScreen(Minecraft.getInstance(), openedGuiNextTick);
			openedGuiNextTick = null;
		}
	}
	
	public static void open(Screen gui) {
		openedGuiNextTick = gui;
	}
	
}
