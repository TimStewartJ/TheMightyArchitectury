package com.timmie.mightyarchitect.foundation.utility;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Mouse buttons as the running Minecraft numbers them: GLFW's left 0 / right 1 until 26.3, SDL's
 * left 1 / right 3 from it. See {@link Keyboard}.
 */
public class Mouse {

	public static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	public static final int RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;

}
