package com.timmie.mightyarchitect.control.compose.planner;

import com.mojang.blaze3d.platform.Window;
//? if >=1.21.6 {
//?} else {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.Schematic;
import com.timmie.mightyarchitect.control.compose.GroundPlan;
import com.timmie.mightyarchitect.control.compose.Room;
import com.timmie.mightyarchitect.control.compose.Stack;
import com.timmie.mightyarchitect.foundation.utility.Keyboard;
import com.timmie.mightyarchitect.foundation.utility.RaycastHelper;
import com.timmie.mightyarchitect.foundation.utility.RaycastHelper.PredicateTraceResult;
import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public abstract class ComposerToolBase implements IComposerTool {

	protected String toolModeNoCtrl = null;
	protected String toolModeCtrl = null;
	protected float toolModeYOffset = 0;
	protected float lastToolModeYOffset = 0;

	public static Stack selectedStack;
	public static Room selectedRoom;
	public static Direction selectedFace;
	public static BlockPos selectedPos;

	protected Schematic model;

	@Override
	public void init() {
		model = ArchitectManager.getModel();
		deselect();
	}

	protected void deselect() {
		selectedStack = null;
		selectedFace = null;
		selectedRoom = null;
		selectedPos = null;
	}

	@Override
	public void updateSelection() {
		updateOverlay();
		updateSelectedRooms();
	}

	protected void updateSelectedRooms() {
		final GroundPlan groundPlan = ArchitectManager.getModel().getGroundPlan();
		final BlockPos anchor = ArchitectManager.getModel().getAnchor();

		if (groundPlan.isEmpty()) {
			deselect();
			return;
		}

		LocalPlayer player = Minecraft.getInstance().player;

		PredicateTraceResult result = RaycastHelper.rayTraceUntil(player, 70, position -> {
			return groundPlan.getRoomAtPos(position.subtract(anchor)) != null;
		});

		if (result.missed()) {
			deselect();
			return;
		}

		selectedPos = result.getPos().subtract(anchor);
		selectedRoom = groundPlan.getRoomAtPos(selectedPos);
		selectedStack = groundPlan.getStackAtPos(selectedPos);
		selectedFace = result.getFacing();
	}

	protected void updateOverlay() {
		lastToolModeYOffset = toolModeYOffset;
		if (Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL))
			toolModeYOffset += (12 - toolModeYOffset) * .2f;
		else
			toolModeYOffset *= .8f;
	}

	@Override
	//? if >=26 {
	public void renderOverlay(GuiGraphicsExtractor graphics) {
	//?} else if >=1.21.6 {
	/*public void renderOverlay(GuiGraphics graphics) {
	*///?} else {
	/*public void renderOverlay(GuiGraphics ms) {
		ms.pose().pushPose();
	*///?}
		Minecraft mc = Minecraft.getInstance();
		Window mainWindow = mc.getWindow();
		//? if >=1.21.6 {
		//?} else if >=1.21.4 {
		/*ms.pose().translate(mainWindow.getGuiScaledWidth() / 2, mainWindow.getGuiScaledHeight() / 2 - 3, 0);
		ms.pose().translate(25,
				-Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), lastToolModeYOffset, toolModeYOffset),
				0);
		*///?} else {
		/*ms.pose().translate(mainWindow.getGuiScaledWidth() / 2, mainWindow.getGuiScaledHeight() / 2 - 3, 0);
		ms.pose().translate(25,
				-Mth.lerp(mc.getTimer().getGameTimeDeltaPartialTick(true), lastToolModeYOffset, toolModeYOffset),
				0);
		*///?}

		//? if >=1.21.6 {
		int baseX = mainWindow.getGuiScaledWidth() / 2 + 25;
		int baseY = (int)(mainWindow.getGuiScaledHeight() / 2 - 3 -
				Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), lastToolModeYOffset, toolModeYOffset));

		//?} else {
		/*
		*///?}
		if (toolModeNoCtrl != null) {
			int color = 0xFFFFFFFF;
			if (Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL))
				color = 0x66AACCFF;
			//? if >=26 {
			graphics.text(mc.font, toolModeNoCtrl, baseX, baseY, color);
			//?} else if >=1.21.6 {
			/*graphics.drawString(mc.font, toolModeNoCtrl, baseX, baseY, color);
			*///?} else {
			/*ms.drawString(mc.font, toolModeNoCtrl, 0, 0, color);
			*///?}
		}
		if (toolModeCtrl != null) {
			int color = 0xFFFFFFFF;
			if (!Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL))
				color = 0x66AACCFF;
			//? if >=26 {
			graphics.text(mc.font, toolModeCtrl, baseX, baseY + 12, color);
			//?} else if >=1.21.6 {
			/*graphics.drawString(mc.font, toolModeCtrl, baseX, baseY + 12, color);
			*///?} else {
			/*ms.drawString(mc.font, toolModeCtrl, 0, 12, color);
			*///?}
		}
		//? if >=1.21.6 {
		//?} else {
		/*
		RenderSystem.setShaderColor(1, 1, 1, 1);
		ms.pose().popPose();
		*///?}
	}

	protected void status(String message) {
		//? if >=26 {
		Minecraft.getInstance().player.sendOverlayMessage(Component.literal(message));
		//?} else {
		/*Minecraft.getInstance().player.displayClientMessage(Component.literal(message), true);
		*///?}
	}

}
