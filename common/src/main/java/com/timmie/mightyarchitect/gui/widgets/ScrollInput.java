package com.timmie.mightyarchitect.gui.widgets;

//? if >=1.21.10 {
import com.timmie.mightyarchitect.foundation.utility.Keyboard;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import net.minecraft.ChatFormatting;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if >=1.21.10 {
//?} else {
/*import net.minecraft.client.gui.screens.Screen;
*///?}
import net.minecraft.network.chat.Component;
//? if >=1.21.10 {
import org.lwjgl.glfw.GLFW;
//?} else {
/*
*///?}

import java.util.function.Consumer;

public class ScrollInput extends AbstractSimiWidget {

	protected Consumer<Integer> onScroll;
	protected int state;
	protected Label displayLabel;
	protected Component title = Component.literal("Choose an Option");
	protected Component scrollToModify = Component.literal("Scroll to Modify");
	protected int min, max;
	protected int shiftStep;

	public ScrollInput(int xIn, int yIn, int widthIn, int heightIn) {
		super(xIn, yIn, widthIn, heightIn);
		state = 0;
		min = 0;
		max = 1;
		shiftStep = 5;
	}

	public ScrollInput withRange(int min, int max) {
		this.min = min;
		this.max = max;
		return this;
	}

	public ScrollInput calling(Consumer<Integer> onScroll) {
		this.onScroll = onScroll;
		return this;
	}

	public ScrollInput removeCallback() {
		this.onScroll = null;
		return this;
	}

	public ScrollInput titled(String title) {
		this.title = Component.literal(title);
		updateTooltip();
		return this;
	}

	public ScrollInput writingTo(Label label) {
		this.displayLabel = label;
		writeToLabel();
		return this;
	}

	public int getState() {
		return state;
	}

	public ScrollInput setState(int state) {
		this.state = state;
		clampState();
		updateTooltip();
		if (displayLabel != null)
			writeToLabel();
		return this;
	}

	public ScrollInput withShiftStep(int step) {
		shiftStep = step;
		return this;
	}

	@Override
	//? if >=1.20.2 {
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
	//?} else {
	/*public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
	*///?}
		if (!isHovered)
			return false;

		int priorState = state;
		//? if >=1.21.10 {
		boolean shifted = Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
		//?} else {
		/*boolean shifted = Screen.hasShiftDown();
		*///?}
		int step = (int) Math.signum(verticalAmount);

		state += step;
		if (shifted)
			state -= state % shiftStep;

		clampState();

		if (priorState != state)
			onChanged();

		return priorState != state;
	}

	protected void clampState() {
		if (state >= max)
			state = max - 1;
		if (state < min)
			state = min;
	}

	public void onChanged() {
		if (displayLabel != null)
			writeToLabel();
		if (onScroll != null)
			onScroll.accept(state);
		updateTooltip();
	}

	protected void writeToLabel() {
		displayLabel.text = Component.literal(String.valueOf(state));
	}

	protected void updateTooltip() {
		toolTip.clear();
		toolTip.add(title.plainCopy()
			.withStyle(ChatFormatting.BLUE));
		toolTip.add(scrollToModify.plainCopy()
			.withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
	}

	@Override
	//? if >=26 {
	public void extractWidgetRenderState(GuiGraphicsExtractor poseStack, int i, int j, float f) {
	//?} else if >=1.20 {
	/*public void renderWidget(GuiGraphics poseStack, int i, int j, float f) {
	*///?} else {
	/*public void renderWidget(com.mojang.blaze3d.vertex.PoseStack poseStack, int i, int j, float f) {
	*///?}

	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}

}
