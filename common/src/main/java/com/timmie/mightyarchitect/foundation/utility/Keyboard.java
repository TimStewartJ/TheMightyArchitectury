package com.timmie.mightyarchitect.foundation.utility;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/**
 * Key and action codes as the running Minecraft numbers them. They are taken from InputConstants
 * rather than written out because the numbering is not stable: it was GLFW's (G = 71, Escape = 256)
 * until 26.3 moved the game to SDL scancodes (G = 10, Escape = 41). These stay compile-time
 * constants, so each version's jar simply carries its own numbers.
 */
public class Keyboard {

	public static final int PRESS = InputConstants.PRESS;
	public static final int HOLD = InputConstants.REPEAT;
	public static final int RELEASE = InputConstants.RELEASE;

	public static final int LSHIFT = InputConstants.KEY_LSHIFT;
	public static final int RSHIFT = InputConstants.KEY_RSHIFT;
	public static final int LCONTROL = InputConstants.KEY_LCONTROL;
	public static final int LALT = InputConstants.KEY_LALT;
	public static final int RETURN = InputConstants.KEY_RETURN;
	public static final int ESCAPE = InputConstants.KEY_ESCAPE;

	public static final int DOWN = InputConstants.KEY_DOWN;
	public static final int LEFT = InputConstants.KEY_LEFT;
	public static final int RIGHT = InputConstants.KEY_RIGHT;
	public static final int UP = InputConstants.KEY_UP;

	public static final int G = InputConstants.KEY_G;

	public static boolean isKeyDown(int key) {
		//? if >=26.3 {
		/*return InputConstants.isKeyDown(key);
		*///?} else if >=1.21.10 {
		return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
		//?} else {
		/*return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
		*///?}
	}

}
