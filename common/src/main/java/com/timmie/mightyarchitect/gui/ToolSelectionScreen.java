package com.timmie.mightyarchitect.gui;

import com.timmie.mightyarchitect.foundation.compat.McCompat;
import com.mojang.blaze3d.platform.Window;
//? if >=1.21.6 {
//?} else {
/*import com.mojang.blaze3d.systems.RenderSystem;
*///?}
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.control.compose.planner.Tools;
import com.timmie.mightyarchitect.foundation.utility.LerpedFloat;
import com.timmie.mightyarchitect.foundation.utility.LerpedFloat.Chaser;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.6 {
//?} else if >=1.21.4 {
/*import net.minecraft.client.renderer.RenderType;
*///?} else {
/*
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Consumer;

public class ToolSelectionScreen extends Screen {

	protected List<Tools> tools;
	protected Consumer<Tools> callback;
	public boolean focused;
	private LerpedFloat yOffset;
	protected int selection;

	protected int w;
	protected int h;

	public ToolSelectionScreen(List<Tools> tools, Consumer<Tools> callback) {
		//? if >=1.21.11 {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Tool Selection"));
		//?} else {
		/*super(Component.literal("Tool Selection"));
		this.minecraft = Minecraft.getInstance();
		*///?}
		this.tools = tools;
		this.callback = callback;
		focused = false;
		yOffset = LerpedFloat.linear()
			.startWithValue(0)
			.chase(0, .1f, Chaser.EXP);
		selection = 0;

		w = tools.size() * 50 + 30;
		h = 30;
	}

	public void cycle(int direction) {
		selection += (direction < 0) ? 1 : -1;
		selection = (selection + tools.size()) % tools.size();
	}

	public void select(int index) {
		selection = Mth.clamp(index, 0, tools.size() - 1);
	}

	//? if >=26 {
	private void draw(GuiGraphicsExtractor graphics, float partialTicks) {
	//?} else if >=1.21.6 {
	/*private void draw(GuiGraphics graphics, float partialTicks) {
	*///?} else {
	/*private void draw(GuiGraphics ms, float partialTicks) {
	*///?}
		Window mainWindow = Minecraft.getInstance()
			.getWindow();
		Font font = minecraft.font;

		int x = (mainWindow.getGuiScaledWidth() - w) / 2 + 15;
		int y = 15;

		//? if >=1.21.6 {
		//?} else {
		/*ms.pose().pushPose();
		ms.pose().translate(0, 0, focused ? 100 : 0);

		*///?}
		ScreenResources gray = ScreenResources.GRAY;
		//? if >=1.21.6 {
		//?} else {
		/*RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1, 1, 1, focused ? 7 / 8f : 1 / 2f);
		RenderSystem.setShaderTexture(0, gray.location);
		*///?}
		float toolTipAlpha = yOffset.getValue(partialTicks) / 10;

		//? if >=1.21.6 {
		// render main box using tiled texture background with dark overlay
		int bgAlpha = focused ? 0xB0 : 0x60;
		gray.drawTiledWithBackground(graphics, x - 15, y, w, h, (bgAlpha << 24) | 0x202030);
		//?} else if >=1.21.4 {
		/*// render main box
		ms.blit(RenderType::guiTextured, gray.location, x - 15, y, gray.startX, gray.startY, w, h, gray.width, gray.height);
		*///?} else {
		/*// render main box
		ms.blit(gray.location, x - 15, y, gray.startX, gray.startY, w, h, gray.width, gray.height);
		*///?}

		// render tools
		List<String> toolTip = tools.get(selection)
			.getDescription();
		int stringAlphaComponent = ((int) (toolTipAlpha * 0xFF)) << 24;

		if (toolTipAlpha > 0.25f) {
			//? if >=1.21.6 {
			int tooltipBgAlpha = (int)(toolTipAlpha * 0xB0);
			gray.drawTiledWithBackground(graphics, x - 15, y + 30, w, h + 22, (tooltipBgAlpha << 24) | 0x202030);
			//?} else if >=1.21.4 {
			/*RenderSystem.setShaderTexture(0, gray.location);
			RenderSystem.setShaderColor(.7f, .7f, .8f, toolTipAlpha);
			ms.blit(RenderType::guiTextured, gray.location, x - 15, y + 30, gray.startX, gray.startY, w, h + 22, gray.width, gray.height);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			*///?} else {
			/*RenderSystem.setShaderTexture(0, gray.location);
			RenderSystem.setShaderColor(.7f, .7f, .8f, toolTipAlpha);
			ms.blit(gray.location, x - 15, y + 30, gray.startX, gray.startY, w, h + 22, gray.width, gray.height);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			*///?}

			if (toolTip.size() > 0)
				//? if >=26 {
				graphics.text(font, toolTip.get(0), x - 10, y + 35, 0xEEEEEE + stringAlphaComponent);
				//?} else if >=1.21.6 {
				/*graphics.drawString(font, toolTip.get(0), x - 10, y + 35, 0xEEEEEE + stringAlphaComponent);
				*///?} else {
				/*ms.drawString(font, toolTip.get(0), x - 10, y + 35, 0xEEEEEE + stringAlphaComponent);
				*///?}
			if (toolTip.size() > 1)
				//? if >=26 {
				graphics.text(font, toolTip.get(1), x - 10, y + 47, 0xCCDDFF + stringAlphaComponent);
				//?} else if >=1.21.6 {
				/*graphics.drawString(font, toolTip.get(1), x - 10, y + 47, 0xCCDDFF + stringAlphaComponent);
				*///?} else {
				/*ms.drawString(font, toolTip.get(1), x - 10, y + 47, 0xCCDDFF + stringAlphaComponent);
				*///?}
			if (toolTip.size() > 2)
				//? if >=26 {
				graphics.text(font, toolTip.get(2), x - 10, y + 57, 0xCCDDFF + stringAlphaComponent);
				//?} else if >=1.21.6 {
				/*graphics.drawString(font, toolTip.get(2), x - 10, y + 57, 0xCCDDFF + stringAlphaComponent);
				*///?} else {
				/*ms.drawString(font, toolTip.get(2), x - 10, y + 57, 0xCCDDFF + stringAlphaComponent);
				*///?}
			if (toolTip.size() > 3)
				//? if >=26 {
				graphics.text(font, toolTip.get(3), x - 10, y + 69, 0xCCCCDD + stringAlphaComponent);
				//?} else if >=1.21.6 {
				/*graphics.drawString(font, toolTip.get(3), x - 10, y + 69, 0xCCCCDD + stringAlphaComponent);
				*///?} else {
				/*ms.drawString(font, toolTip.get(3), x - 10, y + 69, 0xCCCCDD + stringAlphaComponent);
				*///?}
		}

		//? if >=1.21.6 {
		//?} else {
		/*RenderSystem.setShaderColor(1, 1, 1, 1);
		*///?}
		String translationKey = MightyClient.TOOL_MENU.getTranslatedKeyMessage()
			.getString()
			.toUpperCase();
		int width = minecraft.getWindow()
			.getGuiScaledWidth();
		if (!focused)
			//? if >=26 {
			graphics.centeredText(minecraft.font, "Hold [" + translationKey + "] to focus", width / 2, y - 10,
				0xFFCCDDFF);
			//?} else if >=1.21.6 {
			/*graphics.drawCenteredString(minecraft.font, "Hold [" + translationKey + "] to focus", width / 2, y - 10,
				0xFFCCDDFF);
			*///?} else if >=1.21.4 {
			/*ms.drawCenteredString(minecraft.font, "Hold [" + translationKey + "] to focus", width / 2, y - 10,
				0xFFCCDDFF);
			*///?} else {
			/*ms.drawCenteredString(minecraft.font, "Hold [" + translationKey + "] to focus", width / 2, y - 10,
				0xCCDDFF);
			*///?}
		else
			//? if >=26 {
			graphics.centeredText(minecraft.font, "[SCROLL] to Cycle", width / 2, y - 10, 0xFFCCDDFF);
			//?} else if >=1.21.6 {
			/*graphics.drawCenteredString(minecraft.font, "[SCROLL] to Cycle", width / 2, y - 10, 0xFFCCDDFF);
			*///?} else if >=1.21.4 {
			/*ms.drawCenteredString(minecraft.font, "[SCROLL] to Cycle", width / 2, y - 10, 0xFFCCDDFF);
			*///?} else {
			/*ms.drawCenteredString(minecraft.font, "[SCROLL] to Cycle", width / 2, y - 10, 0xCCDDFF);
			*///?}

		for (int i = 0; i < tools.size(); i++) {
			//? if >=1.21.6 {
			float alpha = focused ? 1 : .2f;
			int yToolOffset = 0;
			//?} else {
			/*ms.pose().pushPose();
			*///?}

			//? if >=1.21.6 {
			//?} else if >=1.21.4 {
			/*float alpha = focused ? 1 : .2f;

			*///?} else {
			/*float alpha = focused ? 1 : .2f;
			*///?}
			if (i == selection) {
				//? if >=26 {
				yToolOffset = -10;
				graphics.centeredText(minecraft.font, tools.get(i)
					.getDisplayName(), x + i * 50 + 24, y + 20, 0xFFCCDDFF);
				//?} else if >=1.21.6 {
				/*yToolOffset = -10;
				graphics.drawCenteredString(minecraft.font, tools.get(i)
					.getDisplayName(), x + i * 50 + 24, y + 20, 0xFFCCDDFF);
				*///?} else if >=1.21.4 {
				/*ms.pose().translate(0, -10, 0);
				ms.drawCenteredString(minecraft.font, tools.get(i)
					.getDisplayName(), x + i * 50 + 24, y + 28, 0xFFCCDDFF);
				*///?} else {
				/*ms.pose().translate(0, -10, 0);
				ms.drawCenteredString(minecraft.font, tools.get(i)
					.getDisplayName(), x + i * 50 + 24, y + 28, 0xCCDDFF);
				*///?}
				alpha = 1;
			}
			//? if >=1.21.4 {
			int alphaInt = (int) (alpha * 255);
			int shadowColor = alphaInt << 24;  // black with alpha
			int iconColor = (alphaInt << 24) | 0xFFFFFF;  // white with alpha
			//?} else {
			/*RenderSystem.setShaderColor(0, 0, 0, alpha);
			*///?}
			tools.get(i)
				.getIcon()
				//? if >=1.21.6 {
				.draw(graphics, x + i * 50 + 16, y + 12 + yToolOffset, shadowColor);
				//?} else if >=1.21.4 {
				/*.draw(ms, x + i * 50 + 16, y + 12, shadowColor);
				*///?} else {
				/*.draw(ms, x + i * 50 + 16, y + 12);
			RenderSystem.setShaderColor(1, 1, 1, alpha);
				*///?}
			tools.get(i)
				.getIcon()
				//? if >=1.21.6 {
				.draw(graphics, x + i * 50 + 16, y + 11 + yToolOffset, iconColor);
				//?} else if >=1.21.4 {
				/*.draw(ms, x + i * 50 + 16, y + 11, iconColor);
				*///?} else {
				/*.draw(ms, x + i * 50 + 16, y + 11);
				*///?}

			if (focused && i != selection) {
				KeyMapping keyMapping = minecraft.options.keyHotbarSlots[i];
				//? if >=26 {
				graphics.centeredText(minecraft.font, "[" + keyMapping.getTranslatedKeyMessage()
					.getString() + "]", x + i * 50 + 24, y + 3, 0xFFCCDDFF);
				//?} else if >=1.21.6 {
				/*graphics.drawCenteredString(minecraft.font, "[" + keyMapping.getTranslatedKeyMessage()
					.getString() + "]", x + i * 50 + 24, y + 3, 0xFFCCDDFF);
				*///?} else if >=1.21.4 {
				/*ms.drawCenteredString(minecraft.font, "[" + keyMapping.getTranslatedKeyMessage()
					.getString() + "]", x + i * 50 + 24, y + 3, 0xFFCCDDFF);
				*///?} else {
				/*ms.drawCenteredString(minecraft.font, "[" + keyMapping.getTranslatedKeyMessage()
					.getString() + "]", x + i * 50 + 24, y + 3, 0xCCDDFF);
				*///?}
			}
			//? if >=1.21.6 {
			//?} else {
			/*
			ms.pose().popPose();
			*///?}
		}
		//? if >=1.21.6 {
		//?} else {
		/*
		// The per-tool loop above ends with setShaderColor(1, 1, 1, alpha), where alpha is 0.2
		// while the bar is unfocused, and the loop's last tool is never the selected one. That
		// is global GL state rather than part of the pose stack, so popPose does not restore
		// it and the composer's tool-mode text, drawn next, inherits the alpha. Only 1.21.1
		// reaches here dirty: 1.21.4 resets unconditionally further up and then stops using
		// shader colour, and 1.21.6+ dropped the API from this path entirely.
		RenderSystem.setShaderColor(1, 1, 1, 1);
		ms.pose().popPose();
		*///?}
	}

	public void update() {
		yOffset.updateChaseTarget(focused ? 10 : 0);
		yOffset.tickChaser();
	}

	//? if >=26 {
	public void renderPassive(GuiGraphicsExtractor ms, float partialTicks) {
	//?} else {
	/*public void renderPassive(GuiGraphics ms, float partialTicks) {
	*///?}
		if (McCompat.currentScreen(Minecraft.getInstance()) != null)
			return;
		draw(ms, partialTicks);
	}

	@Override
	public void onClose() {
		callback.accept(tools.get(selection));
	}

}
