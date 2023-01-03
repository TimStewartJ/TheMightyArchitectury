package com.timmie.mightyarchitect.foundation.utility;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class Keyboard {
	
	public static final int PRESS = 1;
	public static final int HOLD = 2;
	public static final int RELEASE = 0;
	
	public static final int LSHIFT = 340;
	public static final int LALT = 342;
	public static final int RETURN = 257;
	
	public static final int DOWN = 264;
	public static final int LEFT = 263;
	public static final int RIGHT = 262;
	public static final int UP = 265;
	
	public static final int G = 71;

	public static boolean isKeyDown(int key) {
		return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
	}
	
}
