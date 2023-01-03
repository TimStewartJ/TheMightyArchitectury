package com.timmie.mightyarchitect.gui.widgets;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SelectionScrollInput extends ScrollInput {

	protected List<String> options;
	protected Component scrollToSelect = Component.literal("Scroll to Select");

	public SelectionScrollInput(int xIn, int yIn, int widthIn, int heightIn) {
		super(xIn, yIn, widthIn, heightIn);
		options = new ArrayList<>();
	}

	public ScrollInput forOptions(List<String> options) {
		this.options = options;
		this.max = options.size();
		updateTooltip();
		return this;
	}

	@Override
	protected void writeToLabel() {
		displayLabel.text = Component.literal(options.get(state));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		return super.mouseScrolled(mouseX, mouseY, -delta);
	}

	@Override
	protected void updateTooltip() {
		toolTip.clear();
		toolTip.add(title.plainCopy()
			.withStyle(ChatFormatting.BLUE));
		for (int i = min; i < max; i++) {
			if (i == state)
				toolTip.add(Component.empty().plainCopy()
					.append("-> ")
					.append(options.get(i))
					.withStyle(ChatFormatting.WHITE));
			else
				toolTip.add(Component.empty().plainCopy()
					.append("> ")
					.append(options.get(i))
					.withStyle(ChatFormatting.GRAY));
		}
		toolTip.add(Component.empty().plainCopy()
			.append(scrollToSelect)
			.withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
	}

}
