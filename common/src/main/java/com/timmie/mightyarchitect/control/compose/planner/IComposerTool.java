package com.timmie.mightyarchitect.control.compose.planner;

import com.timmie.mightyarchitect.foundation.utility.Keyboard;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Optional;

public interface IComposerTool {
	
	Object toolOutlineKey = new Object();

	String handleRightClick();
	boolean handleMouseWheel(int scroll);

	default void handleKeyInput(int key) {
		if (!numberInputSimulatesScrolls())
			return;

		KeyEvent keyEvent = new KeyEvent(key, 0, 0);
		Optional<KeyMapping> mapping = Arrays.stream(Minecraft.getInstance().options.keyHotbarSlots).filter(keyMapping -> keyMapping.matches(keyEvent)).findFirst();
		if (mapping.isEmpty())
			return;

		int number = ArrayUtils.indexOf(Minecraft.getInstance().options.keyHotbarSlots, mapping.get()) + 1;
		if (Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
			number = number * -1;
		}

		handleMouseWheel(number);

	}
	default boolean numberInputSimulatesScrolls() {
		return false;
	}

	void tickToolOutlines();
	void tickGroundPlanOutlines();
	
	void updateSelection();
	void renderOverlay(GuiGraphicsExtractor ms);
	void init();
}
