//? if >=26 {
package com.timmie.mightyarchitect.gui;

import com.timmie.mightyarchitect.gui.widgets.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
	public void extractRenderState(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
		// extractBackground draws a darkening overlay; only draw it for screens that opt in.
		if (shouldRenderDarkBackground()) {
			extractBackground(ms, mouseX, mouseY, partialTicks);
		}
		renderWindow(ms, mouseX, mouseY, partialTicks);
		for (AbstractWidget widget : widgets)
			widget.extractRenderState(ms, mouseX, mouseY, partialTicks);
		renderWindowForeground(ms, mouseX, mouseY, partialTicks);
	}

	//*
	 //* Override this to return true for screens that need a darkened/blurred background.
	 //* Default is false since these screens have their own texture backgrounds.
	 //
	protected boolean shouldRenderDarkBackground() {
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean flag) {
		boolean result = false;
		for (AbstractWidget widget : widgets) {
			if (widget.mouseClicked(event, flag))
				result = true;
		}
		return result;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		for (AbstractWidget widget : widgets) {
			if (widget.keyPressed(event))
				return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		for (AbstractWidget widget : widgets) {
			if (widget.charTyped(event))
				return true;
		}
		if ((char) event.codepoint() == 'e')
			onClose();
		return super.charTyped(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		for (AbstractWidget widget : widgets) {
			if (widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount))
				return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected abstract void renderWindow(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks);

	protected void renderWindowForeground(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
		for (AbstractWidget widget : widgets) {
			if (!widget.isHoveredOrFocused())
				continue;
			if (widget instanceof AbstractSimiWidget && !((AbstractSimiWidget) widget).getToolTip()
				.isEmpty()) {
				// Convert the Component tooltip lines to FormattedCharSequence.
				java.util.List<net.minecraft.util.FormattedCharSequence> tooltipLines = ((AbstractSimiWidget) widget).getToolTip()
					.stream()
					.map(Component::getVisualOrderText)
					.collect(java.util.stream.Collectors.toList());
				ms.setTooltipForNextFrame(Minecraft.getInstance().font, tooltipLines, mouseX, mouseY);
			}
		}
	}

}
//?} else if >=1.21.6 {
/*package com.timmie.mightyarchitect.gui;

import com.timmie.mightyarchitect.gui.widgets.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
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
	public void render(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		// In 1.21.6, renderBackground renders a dark overlay which blocks the game view
		// Only call it for screens that should have a darkened background
		if (shouldRenderDarkBackground()) {
			renderBackground(ms, mouseX, mouseY, partialTicks);
		}
		renderWindow(ms, mouseX, mouseY, partialTicks);
		for (AbstractWidget widget : widgets)
			widget.render(ms, mouseX, mouseY, partialTicks);
		renderWindowForeground(ms, mouseX, mouseY, partialTicks);
	}

	//*
	 //* Override this to return true for screens that need a darkened/blurred background.
	 //* Default is false since these screens have their own texture backgrounds.
	 //
	protected boolean shouldRenderDarkBackground() {
		return false;
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		boolean result = false;
		for (AbstractWidget widget : widgets) {
			if (widget.mouseClicked(x, y, button))
				result = true;
		}
		return result;
	}

	@Override
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		for (AbstractWidget widget : widgets) {
			if (widget.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_))
				return true;
		}
		return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
	}

	@Override
	public boolean charTyped(char character, int code) {
		for (AbstractWidget widget : widgets) {
			if (widget.charTyped(character, code))
				return true;
		}
		if (character == 'e')
			onClose();
		return super.charTyped(character, code);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		for (AbstractWidget widget : widgets) {
			if (widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount))
				return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected abstract void renderWindow(GuiGraphics ms, int mouseX, int mouseY, float partialTicks);

	protected void renderWindowForeground(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		for (AbstractWidget widget : widgets) {
			if (!widget.isHoveredOrFocused())
				continue;
			if (widget instanceof AbstractSimiWidget && !((AbstractSimiWidget) widget).getToolTip()
				.isEmpty()) {
				// In 1.21.6, convert Component list to FormattedCharSequence list
				java.util.List<net.minecraft.util.FormattedCharSequence> tooltipLines = ((AbstractSimiWidget) widget).getToolTip()
					.stream()
					.map(Component::getVisualOrderText)
					.collect(java.util.stream.Collectors.toList());
				ms.setTooltipForNextFrame(Minecraft.getInstance().font, tooltipLines, mouseX, mouseY);
			}
		}
	}

}*/
//?} else {
/*package com.timmie.mightyarchitect.gui;

import com.timmie.mightyarchitect.gui.widgets.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
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
	public void render(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		renderBackground(ms, mouseX, mouseY, partialTicks);
		renderWindow(ms, mouseX, mouseY, partialTicks);
		for (AbstractWidget widget : widgets)
			widget.render(ms, mouseX, mouseY, partialTicks);
		renderWindowForeground(ms, mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		boolean result = false;
		for (AbstractWidget widget : widgets) {
			if (widget.mouseClicked(x, y, button))
				result = true;
		}
		return result;
	}

	@Override
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		for (AbstractWidget widget : widgets) {
			if (widget.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_))
				return true;
		}
		return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
	}

	@Override
	public boolean charTyped(char character, int code) {
		for (AbstractWidget widget : widgets) {
			if (widget.charTyped(character, code))
				return true;
		}
		if (character == 'e')
			onClose();
		return super.charTyped(character, code);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		for (AbstractWidget widget : widgets) {
			if (widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount))
				return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected abstract void renderWindow(GuiGraphics ms, int mouseX, int mouseY, float partialTicks);

	protected void renderWindowForeground(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		for (AbstractWidget widget : widgets) {
			if (!widget.isHoveredOrFocused())
				continue;
			if (widget instanceof AbstractSimiWidget && !((AbstractSimiWidget) widget).getToolTip()
				.isEmpty())
				ms.renderComponentTooltip(Minecraft.getInstance().font, ((AbstractSimiWidget) widget).getToolTip(), mouseX, mouseY);
		}
	}

}*///?}
