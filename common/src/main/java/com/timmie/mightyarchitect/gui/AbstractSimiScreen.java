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
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.10 {
import net.minecraft.client.input.CharacterEvent;
//?} else {
/*
*///?}
import net.minecraft.network.chat.Component;

/**
 * Base class for the mod's window-style screens.
 * <p>
 * Widgets are registered with vanilla through {@code Screen.addWidget}, so {@code children()} is
 * the single widget list and {@code Screen} / {@code ContainerEventHandler} do the dispatching for
 * clicks, keys, characters, scroll and focus. Re-implementing that dispatch is what used to make
 * this the mod's most version-sensitive file: five signatures to track by hand, every one of which
 * moved again in 1.21.10. Vanilla absorbs those changes for free.
 * <p>
 * Drawing is still done here, deliberately. The window art has to appear between the background and
 * the widgets, and {@code Screen.render} draws the background itself on 1.20.2, 1.20.4, 1.20.6 and
 * 1.21.4 but not on any other supported version, so {@code super.render} cannot be used to draw the
 * widgets without double-darkening those four.
 * <p>
 * Purely decorative widgets ({@code Label}, {@code Indicator}) are inactive, which keeps them out of
 * vanilla's {@code getChildAt} hit-testing - several of them are registered before, and overlap, a
 * {@code ScrollInput}.
 */
public abstract class AbstractSimiScreen extends Screen {

	protected int sWidth, sHeight;
	protected int topLeftX, topLeftY;

	protected AbstractSimiScreen() {
		super(Component.literal(""));
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
		renderWidgets(ms, mouseX, mouseY, partialTicks);
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
	// The one place the widget draw call has to be spelled per epoch. Everything else about
	// widgets - storage, ordering, hit-testing, focus - is vanilla's.
	//? if >=26 {
	private void renderWidgets(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
		for (GuiEventListener child : children())
			if (child instanceof AbstractWidget widget)
				widget.extractRenderState(ms, mouseX, mouseY, partialTicks);
	}
	//?} else if >=1.20 {
	/*private void renderWidgets(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		for (GuiEventListener child : children())
			if (child instanceof AbstractWidget widget)
				widget.render(ms, mouseX, mouseY, partialTicks);
	}
	*///?} else {
	/*private void renderWidgets(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		for (GuiEventListener child : children())
			if (child instanceof AbstractWidget widget)
				widget.render(ms.pose(), mouseX, mouseY, partialTicks);
	}
	*///?}

	/**
	 * Closes on the inventory letter, the way an in-game container screen does. Vanilla offers the
	 * character to the focused widget first, so a text field can still contain that letter.
	 */
	@Override
	//? if >=1.21.10 {
	public boolean charTyped(CharacterEvent event) {
		if (super.charTyped(event))
			return true;
		return closeOn((char) event.codepoint());
	//?} else {
	/*public boolean charTyped(char character, int code) {
		if (super.charTyped(character, code))
			return true;
		return closeOn(character);
	*///?}
	}

	private boolean closeOn(char character) {
		if (character != 'e')
			return false;
		onClose();
		return true;
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
		for (GuiEventListener child : children()) {
			if (!(child instanceof AbstractSimiWidget widget))
				continue;
			// Hover, not isHoveredOrFocused: widgets are focusable now that they are registered
			// with vanilla, and Screen.setInitialFocus focuses one of them on open from 1.20.6.
			// Keying tooltips off focus would leave one permanently on screen.
			if (!widget.isHovered() || widget.getToolTip()
				.isEmpty())
				continue;
			//? if >=1.21.6 {
			ms.setTooltipForNextFrame(Minecraft.getInstance().font, widget.getToolTip()
				.stream()
				.map(Component::getVisualOrderText)
				.toList(), mouseX, mouseY);
			//?} else if >=1.20 {
			/*ms.renderComponentTooltip(Minecraft.getInstance().font, widget.getToolTip(), mouseX, mouseY);
			*///?} else {
			/*renderComponentTooltip(ms.pose(), widget.getToolTip(), mouseX, mouseY);
			*///?}
		}
	}

}
