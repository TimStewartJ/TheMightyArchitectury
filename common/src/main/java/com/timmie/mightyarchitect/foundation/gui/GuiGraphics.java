//? if <1.20 {
/*package com.timmie.mightyarchitect.foundation.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class GuiGraphics {

	private final PoseStack pose;

	public GuiGraphics(PoseStack pose) {
		this.pose = pose;
	}

	public PoseStack pose() {
		return pose;
	}

	public int guiWidth() {
		return Minecraft.getInstance().getWindow().getGuiScaledWidth();
	}

	public int guiHeight() {
		return Minecraft.getInstance().getWindow().getGuiScaledHeight();
	}

	public int drawString(Font font, String text, int x, int y, int color) {
		return drawString(font, text, x, y, color, true);
	}

	public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
		return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
	}

	public int drawString(Font font, Component text, int x, int y, int color) {
		return drawString(font, text, x, y, color, true);
	}

	public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
		return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
	}

	public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
		return drawString(font, text, x, y, color, true);
	}

	public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
		return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
	}

	public void drawCenteredString(Font font, String text, int x, int y, int color) {
		GuiComponent.drawCenteredString(pose, font, text, x, y, color);
	}

	public void drawCenteredString(Font font, Component text, int x, int y, int color) {
		GuiComponent.drawCenteredString(pose, font, text, x, y, color);
	}

	public void drawCenteredString(Font font, FormattedCharSequence text, int x, int y, int color) {
		GuiComponent.drawCenteredString(pose, font, text, x, y, color);
	}

	public void fill(int minX, int minY, int maxX, int maxY, int color) {
		GuiComponent.fill(pose, minX, minY, maxX, maxY, color);
	}

	public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
		blit(texture, x, y, u, v, width, height, 256, 256);
	}

	public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height,
		int textureWidth, int textureHeight) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, texture);
		GuiComponent.blit(pose, x, y, (float) u, (float) v, width, height, textureWidth, textureHeight);
	}
}
*///?}
