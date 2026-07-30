package com.timmie.mightyarchitect.gui.widgets;

import com.google.common.collect.ImmutableList;
//? if >=26 {
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import com.timmie.mightyarchitect.gui.ScreenResources;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class Indicator extends AbstractSimiWidget {

	public enum State {
		OFF, ON,
		RED, YELLOW, GREEN;
	}

	public State state;

	public Indicator(int x, int y, String tooltip) {
		this(x, y, Component.literal(tooltip));
	}

	public Indicator(int x, int y, Component tooltip) {
		super(x, y, ScreenResources.INDICATOR.width, ScreenResources.INDICATOR.height);
		this.toolTip = ImmutableList.of(tooltip);
		this.state = State.OFF;
	}

	@Override
	//? if >=26 {
	public void extractWidgetRenderState(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks ) {
	//?} else if >=1.20 {
	/*public void renderWidget(GuiGraphics ms, int mouseX, int mouseY, float partialTicks ) {
	*///?} else {
	/*public void renderWidget(com.mojang.blaze3d.vertex.PoseStack poseStack, int mouseX, int mouseY, float partialTicks ) {
		GuiGraphics ms = new GuiGraphics(poseStack);
	*///?}
		ScreenResources toDraw;
		switch(state) {
			case ON: toDraw = ScreenResources.INDICATOR_WHITE; break;
			case OFF: toDraw = ScreenResources.INDICATOR; break;
			case RED: toDraw = ScreenResources.INDICATOR_RED; break;
			case YELLOW: toDraw = ScreenResources.INDICATOR_YELLOW; break;
			case GREEN: toDraw = ScreenResources.INDICATOR_GREEN; break;
			default: toDraw = ScreenResources.INDICATOR; break;
		}
		toDraw.draw(ms, getX(), getY());
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}

}
