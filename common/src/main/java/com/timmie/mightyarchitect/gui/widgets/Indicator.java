package com.timmie.mightyarchitect.gui.widgets;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.gui.ScreenResources;
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
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks ) {
		ScreenResources toDraw;
		switch(state) {
			case ON: toDraw = ScreenResources.INDICATOR_WHITE; break;
			case OFF: toDraw = ScreenResources.INDICATOR; break;
			case RED: toDraw = ScreenResources.INDICATOR_RED; break;
			case YELLOW: toDraw = ScreenResources.INDICATOR_YELLOW; break;
			case GREEN: toDraw = ScreenResources.INDICATOR_GREEN; break;
			default: toDraw = ScreenResources.INDICATOR; break;
		}
		toDraw.draw(matrixStack, this, x, y);
	}

	@Override
	public void renderWidget(PoseStack poseStack, int i, int j, float f) {

	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}

}
