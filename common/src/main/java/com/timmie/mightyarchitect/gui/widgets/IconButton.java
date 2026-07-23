package com.timmie.mightyarchitect.gui.widgets;

import com.timmie.mightyarchitect.gui.ScreenResources;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public class IconButton extends AbstractSimiWidget {

	private ScreenResources icon;
	protected boolean pressed;

	public IconButton(int x, int y, ScreenResources icon) {
		super(x, y, 18, 18);
		this.icon = icon;
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
		if (this.visible) {
			this.isHovered =
				mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

			ScreenResources button = (pressed || !active) ? button = ScreenResources.BUTTON_DOWN
				: (isHovered) ? ScreenResources.BUTTON_HOVER : ScreenResources.BUTTON;

			// In 1.21.6, use RenderPipelines.GUI_TEXTURED for GUI texture rendering
			ms.blit(RenderPipelines.GUI_TEXTURED, ScreenResources.BUTTON.location, getX(), getY(), (float) button.startX, (float) button.startY, button.width, button.height, 256, 256);
			icon.draw(ms, getX() + 1, getY() + 1);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean flag) {
		super.onClick(event, flag);
		this.pressed = true;
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		super.onRelease(event);
		this.pressed = false;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}

	public void setToolTip(String text) {
		setToolTip(Component.literal(text));
	}

	public void setToolTip(Component text) {
		toolTip.clear();
		toolTip.add(text);
	}

}