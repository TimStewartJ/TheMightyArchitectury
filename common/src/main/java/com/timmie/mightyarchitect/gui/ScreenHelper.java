package com.timmie.mightyarchitect.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ScreenHelper {

	private static Screen openedGuiNextTick;

	public static void onClientTick(Minecraft minecraft) {
		if (openedGuiNextTick != null) {
			Minecraft.getInstance().setScreen(openedGuiNextTick);
			openedGuiNextTick = null;
		}
	}
	
	public static void open(Screen gui) {
		openedGuiNextTick = gui;
	}
	
}
