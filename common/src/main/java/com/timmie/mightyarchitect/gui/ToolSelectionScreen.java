package com.timmie.mightyarchitect.gui;

import com.mojang.blaze3d.platform.Window;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.control.compose.planner.Tools;
import com.timmie.mightyarchitect.foundation.utility.LerpedFloat;
import com.timmie.mightyarchitect.foundation.utility.LerpedFloat.Chaser;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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
		super(Component.literal("Tool Selection"));
		this.minecraft = Minecraft.getInstance();
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

	private void draw(GuiGraphics graphics, float partialTicks) {
		Window mainWindow = Minecraft.getInstance()
			.getWindow();
		Font font = minecraft.font;

		int x = (mainWindow.getGuiScaledWidth() - w) / 2 + 15;
		int y = 15;

		ScreenResources gray = ScreenResources.GRAY;
		float toolTipAlpha = yOffset.getValue(partialTicks) / 10;

		// render main box using fill for background
		int bgAlpha = focused ? 0xE0 : 0x80;
		graphics.fill(x - 15, y, x - 15 + w, y + h, (bgAlpha << 24));

		// render tools
		List<String> toolTip = tools.get(selection)
			.getDescription();
		int stringAlphaComponent = ((int) (toolTipAlpha * 0xFF)) << 24;

		if (toolTipAlpha > 0.25f) {
			int tooltipBgAlpha = (int)(toolTipAlpha * 0xE0);
			graphics.fill(x - 15, y + 30, x - 15 + w, y + 30 + h + 22, (tooltipBgAlpha << 24) | 0x333344);

			if (toolTip.size() > 0)
				graphics.drawString(font, toolTip.get(0), x - 10, y + 35, 0xEEEEEE + stringAlphaComponent);
			if (toolTip.size() > 1)
				graphics.drawString(font, toolTip.get(1), x - 10, y + 47, 0xCCDDFF + stringAlphaComponent);
			if (toolTip.size() > 2)
				graphics.drawString(font, toolTip.get(2), x - 10, y + 57, 0xCCDDFF + stringAlphaComponent);
			if (toolTip.size() > 3)
				graphics.drawString(font, toolTip.get(3), x - 10, y + 69, 0xCCCCDD + stringAlphaComponent);
		}

		String translationKey = MightyClient.TOOL_MENU.getTranslatedKeyMessage()
			.getString()
			.toUpperCase();
		int width = minecraft.getWindow()
			.getGuiScaledWidth();
		if (!focused)
			graphics.drawCenteredString(minecraft.font, "Hold [" + translationKey + "] to focus", width / 2, y - 10,
				0xFFCCDDFF);
		else
			graphics.drawCenteredString(minecraft.font, "[SCROLL] to Cycle", width / 2, y - 10, 0xFFCCDDFF);

		for (int i = 0; i < tools.size(); i++) {
			float alpha = focused ? 1 : .2f;
			int yToolOffset = 0;

			if (i == selection) {
				yToolOffset = -10;
				graphics.drawCenteredString(minecraft.font, tools.get(i)
					.getDisplayName(), x + i * 50 + 24, y + 28, 0xFFCCDDFF);
				alpha = 1;
			}
			int alphaInt = (int) (alpha * 255);
			int shadowColor = alphaInt << 24;  // black with alpha
			int iconColor = (alphaInt << 24) | 0xFFFFFF;  // white with alpha
			tools.get(i)
				.getIcon()
				.draw(graphics, x + i * 50 + 16, y + 12 + yToolOffset, shadowColor);
			tools.get(i)
				.getIcon()
				.draw(graphics, x + i * 50 + 16, y + 11 + yToolOffset, iconColor);

			if (focused && i != selection) {
				KeyMapping keyMapping = minecraft.options.keyHotbarSlots[i];
				graphics.drawCenteredString(minecraft.font, "[" + keyMapping.getTranslatedKeyMessage()
					.getString() + "]", x + i * 50 + 24, y + 3, 0xFFCCDDFF);
			}
		}
	}

	public void update() {
		yOffset.updateChaseTarget(focused ? 10 : 0);
		yOffset.tickChaser();
	}

	public void renderPassive(GuiGraphics ms, float partialTicks) {
		if (Minecraft.getInstance().screen != null)
			return;
		draw(ms, partialTicks);
	}

	@Override
	public void onClose() {
		callback.accept(tools.get(selection));
	}

}
