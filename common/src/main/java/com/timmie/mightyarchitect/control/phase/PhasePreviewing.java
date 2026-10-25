package com.timmie.mightyarchitect.control.phase;

import com.timmie.mightyarchitect.foundation.compat.McCompat;
import com.timmie.mightyarchitect.foundation.utility.Mouse;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.control.PrintRange;
import com.timmie.mightyarchitect.control.compose.planner.Tools;
import com.timmie.mightyarchitect.gui.ToolSelectionScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
//?} else if >=1.21.10 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
*///?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;
*///?}
import com.timmie.mightyarchitect.foundation.MightyBuffers;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class PhasePreviewing extends PhaseBase implements IRenderGameOverlay {

	/** Ticks between print-range checks; they walk every block of the build. */
	private static final int RANGE_CHECK_INTERVAL = 20;

	private Tools activeTool;
	private ToolSelectionScreen toolSelection;
	private int rangeCheckTicks;
	private boolean warnedOutOfRange;

	@Override
	public void whenEntered() {
		rangeCheckTicks = 0;
		warnedOutOfRange = false;
		final Consumer<Tools> callback = tool -> {
			equipTool(tool);
		};

		activeTool = Tools.RerollAll;
		activeTool.getTool()
			.init();
		List<Tools> tools = Tools.getWallDecorationTools();
		toolSelection = new ToolSelectionScreen(tools, callback);

		MightyClient.renderer.display(getModel());
	}

	private void equipTool(Tools tool) {
		if (tool == activeTool)
			return;
		activeTool = tool;
		activeTool.getTool()
			.init();
	}

	@Override
	public void onClick(int button) {
		if (button == Mouse.RIGHT) {
			if (McCompat.currentScreen(Minecraft.getInstance()) == null) {
				String message = activeTool.getTool()
					.handleRightClick();
				sendStatusMessage(message);
			}
		}
	}

	@Override
	public void onKey(int key, boolean released) {
		//? if >=1.21.10 {
		KeyEvent keyEvent = new KeyEvent(key, 0, 0);
		if (MightyClient.TOOL_MENU.matches(keyEvent)) {
		//?} else {
		/*if (MightyClient.TOOL_MENU.matches(key, 0)) {
		*///?}
			if (released && toolSelection.focused) {
				toolSelection.focused = false;
				toolSelection.onClose();
			}

			if (!released && !toolSelection.focused)
				toolSelection.focused = true;

			return;
		}

		if (released)
			return;

		if (toolSelection.focused) {
			//? if >=1.21.10 {
			Optional<KeyMapping> mapping = Arrays.stream(Minecraft.getInstance().options.keyHotbarSlots).filter(keyMapping -> keyMapping.matches(keyEvent)).findFirst();
			//?} else {
			/*Optional<KeyMapping> mapping = Arrays.stream(Minecraft.getInstance().options.keyHotbarSlots).filter(keyMapping -> keyMapping.matches(key, 0)).findFirst();
			*///?}
			if (mapping.isEmpty())
				return;

			toolSelection.select(ArrayUtils.indexOf(Minecraft.getInstance().options.keyHotbarSlots, mapping.get()));

			return;
		}

		activeTool.getTool().handleKeyInput(key);
	}

	@Override
	public void update() {
		activeTool.getTool()
			.updateSelection();
		toolSelection.update();
		activeTool.getTool()
			.tickGroundPlanOutlines();
		activeTool.getTool()
			.tickToolOutlines();
		checkPrintRange();
	}

	/**
	 * Says so as soon as the build goes out of printing range, rather than only when print is
	 * pressed - drafting from a detached camera is exactly when the player is far from it. Once per
	 * crossing, so it does not keep overwriting the tools' own messages. Skipped while a palette
	 * change is still being applied over several ticks, which reading the blocks would force through.
	 */
	private void checkPrintRange() {
		if (++rangeCheckTicks < RANGE_CHECK_INTERVAL)
			return;
		rangeCheckTicks = 0;
		if (getModel().isMaterializing() || !PrintRange.mayPrint() || !PrintRange.usesPacketRoute())
			return;
		double reach = PrintRange.farthest(Minecraft.getInstance().player, getModel());
		boolean outOfReach = PrintRange.isOutOfReach(reach);
		if (outOfReach && !warnedOutOfRange)
			sendStatusMessage(PrintRange.outOfRangeMessage(reach));
		warnedOutOfRange = outOfReach;
	}

	@Override
	public boolean onScroll(int amount) {
		if (toolSelection.focused) {
			toolSelection.cycle(amount);
			return true;
		}

		return activeTool.getTool()
			.handleMouseWheel(amount);
	}

	@Override
	public void render(PoseStack ms, MightyBuffers buffer) {}

	@Override
	public void whenExited() {
		MightyClient.renderer.setActive(false);
	}

	@Override
	//? if >=26 {
	public void renderGameOverlay(GuiGraphicsExtractor ms, float partialTicks) {
	//?} else {
	/*public void renderGameOverlay(GuiGraphics ms, float partialTicks) {
	*///?}
		toolSelection.renderPassive(ms, partialTicks);
		activeTool.getTool()
			.renderOverlay(ms);
	}

	@Override
	public List<String> getToolTip() {
		return ImmutableList.of("Here is a preview of your new build.",
			"From here you can pick your materials in the palette picker [C]",
			"Once you are happy with what you see, save or build your structure.");
	}

}
