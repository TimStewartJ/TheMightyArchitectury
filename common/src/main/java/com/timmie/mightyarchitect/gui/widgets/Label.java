package com.timmie.mightyarchitect.gui.widgets;

//? if >=1.21.6 {
//?} else {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class Label extends AbstractSimiWidget {

	public Component text;
	public String suffix;
	protected boolean hasShadow;
	protected int color;
	protected Font font;

	public Label(int x, int y, String tooltip) {
		this(x, y, Component.literal(tooltip));
	}

	public Label(int x, int y, Component text) {
		super(x, y, Minecraft.getInstance().font.width(text), 10);
		font = Minecraft.getInstance().font;
		this.text = Component.literal("Label");
		color = 0xFFFFFF;
		hasShadow = false;
		suffix = "";
	}

	public Label colored(int color) {
		this.color = color;
		return this;
	}

	public Label withShadow() {
		this.hasShadow = true;
		return this;
	}

	public Label withSuffix(String s) {
		suffix = s;
		return this;
	}

	public void setText(String text) {
		this.text = Component.literal(text);
	}

	public void setTextAndTrim(Component newText, boolean trimFront, int maxWidthPx) {
		Font fontRenderer = Minecraft.getInstance().font;

		if (fontRenderer.width(newText) <= maxWidthPx) {
			text = newText;
			return;
		}

		String trim = "...";
		int trimWidth = fontRenderer.width(trim);

		String raw = newText.getString();
		StringBuilder builder = new StringBuilder(raw);
		int startIndex = trimFront ? 0 : raw.length() - 1;
		int endIndex = !trimFront ? 0 : raw.length() - 1;
		int step = (int) Math.signum(endIndex - startIndex);

		for (int i = startIndex; i != endIndex; i += step) {
			String sub = builder.substring(trimFront ? i : startIndex, trimFront ? endIndex + 1 : i + 1);
			if (fontRenderer.width(Component.literal(sub).setStyle(newText.getStyle())) + trimWidth <= maxWidthPx) {
				text = Component.literal(trimFront ? trim + sub : sub + trim).setStyle(newText.getStyle());
				return;
			}
		}

	}

	@Override
	//? if >=26 {
	public void extractWidgetRenderState(GuiGraphicsExtractor matrixStack, int mouseX, int mouseY, float partialTicks) {
	//?} else if >=1.20 {
	/*public void renderWidget(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
	*///?} else {
	/*public void renderWidget(com.mojang.blaze3d.vertex.PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		GuiGraphics matrixStack = new GuiGraphics(poseStack);
	*///?}
		if (!visible)
			return;
		if (text == null || text.getString().isEmpty())
			return;

		//? if >=1.21.6 {
		//?} else {
		/*RenderSystem.setShaderColor(1, 1, 1, 1);
		*///?}
		MutableComponent copy = text.plainCopy();
		if (suffix != null && !suffix.isEmpty())
			copy.append(suffix);

		//? if >=26 {
		matrixStack.text(font, copy, getX(), getY(), color, hasShadow);
		//?} else {
		/*matrixStack.drawString(font, copy, getX(), getY(), color, hasShadow);
		*///?}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}

}
