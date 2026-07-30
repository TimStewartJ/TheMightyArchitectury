package com.timmie.mightyarchitect.gui;

import com.timmie.mightyarchitect.gui.widgets.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.10 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?} else {
/*
*///?}
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSimiScreen extends Screen {

	protected int sWidth, sHeight;
	protected int topLeftX, topLeftY;
	protected List<AbstractWidget> widgets;

	protected AbstractSimiScreen() {
		super(Component.literal(""));
		widgets = new ArrayList<>();
	}

	protected void setWindowSize(int width, int height) {
		sWidth = width;
		sHeight = height;
		topLeftX = (this.width - sWidth) / 2;
		topLeftY = (this.height - sHeight) / 2;
	}

	@Override
	//? if >=26 {
	public void extractRenderState(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
		// extractBackground draws a darkening overlay; only draw it for screens that opt in.
		if (shouldRenderDarkBackground()) {
			extractBackground(ms, mouseX, mouseY, partialTicks);
		}
	//?} else if >=1.21.6 {
	/*public void render(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		// In 1.21.6, renderBackground renders a dark overlay which blocks the game view
		// Only call it for screens that should have a darkened background
		if (shouldRenderDarkBackground()) {
			renderBackground(ms, mouseX, mouseY, partialTicks);
		}
	*///?} else if >=1.20.2 {
	/*public void render(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		renderBackground(ms, mouseX, mouseY, partialTicks);
	*///?} else if >=1.20 {
	/*public void render(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		renderBackground(ms);
	*///?} else {
	/*public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		GuiGraphics ms = new GuiGraphics(poseStack);
		renderBackground(poseStack);
	*///?}
		renderWindow(ms, mouseX, mouseY, partialTicks);
		for (AbstractWidget widget : widgets)
			//? if >=26 {
			widget.extractRenderState(ms, mouseX, mouseY, partialTicks);
			//?} else if >=1.20 {
			/*widget.render(ms, mouseX, mouseY, partialTicks);
			*///?} else {
			/*widget.render(ms.pose(), mouseX, mouseY, partialTicks);
			*///?}
		renderWindowForeground(ms, mouseX, mouseY, partialTicks);
	}

	//? if >=1.21.6 {
	//*
	 //* Override this to return true for screens that need a darkened/blurred background.
	 //* Default is false since these screens have their own texture backgrounds.
	 //
	protected boolean shouldRenderDarkBackground() {
		return false;
	}

	//?} else {
	/*
	*///?}
	@Override
	//? if >=1.21.10 {
	public boolean mouseClicked(MouseButtonEvent event, boolean flag) {
	//?} else {
	/*public boolean mouseClicked(double x, double y, int button) {
	*///?}
		boolean result = false;
		for (AbstractWidget widget : widgets) {
			//? if >=1.21.10 {
			if (widget.mouseClicked(event, flag))
			//?} else {
			/*if (widget.mouseClicked(x, y, button))
			*///?}
				result = true;
		}
		return result;
	}

	@Override
	//? if >=1.21.10 {
	public boolean keyPressed(KeyEvent event) {
	//?} else {
	/*public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
	*///?}
		for (AbstractWidget widget : widgets) {
			//? if >=1.21.10 {
			if (widget.keyPressed(event))
			//?} else {
			/*if (widget.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_))
			*///?}
				return true;
		}
		//? if >=1.21.10 {
		return super.keyPressed(event);
		//?} else {
		/*return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
		*///?}
	}

	@Override
	//? if >=1.21.10 {
	public boolean charTyped(CharacterEvent event) {
	//?} else {
	/*public boolean charTyped(char character, int code) {
	*///?}
		for (AbstractWidget widget : widgets) {
			//? if >=1.21.10 {
			if (widget.charTyped(event))
			//?} else {
			/*if (widget.charTyped(character, code))
			*///?}
				return true;
		}
		//? if >=1.21.10 {
		if ((char) event.codepoint() == 'e')
		//?} else {
		/*if (character == 'e')
		*///?}
			onClose();
		//? if >=1.21.10 {
		return super.charTyped(event);
		//?} else {
		/*return super.charTyped(character, code);
		*///?}
	}

	@Override
	//? if >=1.20.2 {
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		for (AbstractWidget widget : widgets) {
			if (widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount))
				return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	//?} else {
	/*public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
		for (AbstractWidget widget : widgets) {
			if (widget.mouseScrolled(mouseX, mouseY, verticalAmount))
				return true;
		}
		return super.mouseScrolled(mouseX, mouseY, verticalAmount);
	*///?}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	//? if >=26 {
	protected abstract void renderWindow(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks);
	//?} else {
	/*protected abstract void renderWindow(GuiGraphics ms, int mouseX, int mouseY, float partialTicks);
	*///?}

	//? if >=26 {
	protected void renderWindowForeground(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
	//?} else {
	/*protected void renderWindowForeground(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
	*///?}
		for (AbstractWidget widget : widgets) {
			if (!widget.isHoveredOrFocused())
				continue;
			if (widget instanceof AbstractSimiWidget && !((AbstractSimiWidget) widget).getToolTip()
				//? if >=26 {
				.isEmpty()) {
				// Convert the Component tooltip lines to FormattedCharSequence.
				java.util.List<net.minecraft.util.FormattedCharSequence> tooltipLines = ((AbstractSimiWidget) widget).getToolTip()
					.stream()
					.map(Component::getVisualOrderText)
					.collect(java.util.stream.Collectors.toList());
				ms.setTooltipForNextFrame(Minecraft.getInstance().font, tooltipLines, mouseX, mouseY);
			}
				//?} else if >=1.21.6 {
				/*.isEmpty()) {
				// In 1.21.6, convert Component list to FormattedCharSequence list
				java.util.List<net.minecraft.util.FormattedCharSequence> tooltipLines = ((AbstractSimiWidget) widget).getToolTip()
					.stream()
					.map(Component::getVisualOrderText)
					.collect(java.util.stream.Collectors.toList());
				ms.setTooltipForNextFrame(Minecraft.getInstance().font, tooltipLines, mouseX, mouseY);
			}
				*///?} else if >=1.20 {
				/*.isEmpty())
				ms.renderComponentTooltip(Minecraft.getInstance().font, ((AbstractSimiWidget) widget).getToolTip(), mouseX, mouseY);
				*///?} else {
				/*.isEmpty())
				renderComponentTooltip(ms.pose(), ((AbstractSimiWidget) widget).getToolTip(), mouseX, mouseY);
				*///?}
		}
	}

}
