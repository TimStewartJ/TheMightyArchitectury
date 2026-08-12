package com.timmie.mightyarchitect.control;

import com.timmie.mightyarchitect.foundation.compat.McCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.control.compose.GroundPlan;
import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.design.DesignTheme;
import com.timmie.mightyarchitect.control.design.ThemeStorage;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.palette.PaletteStorage;
import com.timmie.mightyarchitect.control.phase.ArchitectPhases;
import com.timmie.mightyarchitect.control.phase.IArchitectPhase;
import com.timmie.mightyarchitect.control.phase.IDrawBlockHighlights;
import com.timmie.mightyarchitect.control.phase.IRenderGameOverlay;
import com.timmie.mightyarchitect.control.storage.ArchitectPaths;
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import com.timmie.mightyarchitect.foundation.utility.Keyboard;
import com.timmie.mightyarchitect.gui.*;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import net.minecraft.ChatFormatting;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?} else {
/*
*///?}
import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;*///?} else {
/*import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;*///?}
import com.timmie.mightyarchitect.foundation.MightyBuffers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;

public class ArchitectManager {

	private static ArchitectPhases phase = ArchitectPhases.Empty;
	private static Schematic model = new Schematic();
	private static ArchitectMenuScreen menu = new ArchitectMenuScreen();

	public static boolean testRun = false;

	// Commands

	public static void compose() {
		enterPhase(ArchitectPhases.Composing);
	}

	public static void compose(DesignTheme theme) {
		if (getModel().isEmpty())
			getModel().setGroundPlan(new GroundPlan(theme));
		enterPhase(ArchitectPhases.Composing);
	}

	public static void pauseCompose() {
		status("Composer paused, use /compose to return.");
	}

	public static void unload() {
		if (!model.isEmpty())
			model.getTheme()
				.getDesignPicker()
				.reset();

		enterPhase(ArchitectPhases.Empty);
		resetSchematic();

		if (testRun) {
			testRun = false;
			editTheme(DesignExporter.getTheme());
			return;
		}

		menu.setVisible(false);
	}

	public static void design() {
		GroundPlan groundPlan = model.getGroundPlan();

		if (groundPlan.isEmpty()) {
			status("Draw some rooms before going to the next step!");
			return;
		}

		model.setSketch(groundPlan.theme.getDesignPicker()
			.assembleSketch(groundPlan, model.seed));
		enterPhase(ArchitectPhases.Previewing);
	}

	public static void reAssemble() {
		GroundPlan groundPlan = model.getGroundPlan();
		model.setSketch(groundPlan.theme.getDesignPicker()
			.assembleSketch(groundPlan, model.seed));
		MightyClient.renderer.update();
	}

	public static void createPalette(boolean primary) {
		getModel().startCreatingNewPalette(primary);
		enterPhase(ArchitectPhases.CreatingPalette);
	}

	public static void finishPalette(String name) {
		if (name.isEmpty())
			name = "My Palette";

		PaletteDefinition palette = getModel().getCreatedPalette();
		palette.setName(name);
		PaletteStorage.exportPalette(palette);
		PaletteStorage.loadAllPalettes();

		getModel().applyCreatedPalette();
		status("Your new palette has been saved.");
		enterPhase(ArchitectPhases.Previewing);
	}

	public static void print() {
		if (getModel().getSketch() == null)
			return;

		List<InstantPrintPacket> packets = getModel().getPackets();

		if (!packets.isEmpty() && AllPackets.canSendToServer(packets.get(0))) {
			if (!Minecraft.getInstance().gameMode.getPlayerMode().isCreative()
				&& !hasGameMasterPermission()) {
				reportPrintPermissionDenied();
				return;
			}
			for (InstantPrintPacket packet : packets)
				AllPackets.sendToServer(packet);
			MightyClient.renderer.setActive(false);
			status("Printed result into world.");
			unload();
			return;
		}

		if (!hasGameMasterPermission()) {
			reportPrintPermissionDenied();
			return;
		}
		enterPhase(ArchitectPhases.PrintingToMultiplayer);
	}

	private static boolean hasGameMasterPermission() {
		// Numeric permission levels were replaced by named permissions in 1.21.11.
		//? if >=1.21.11 {
		return Minecraft.getInstance().player.permissions()
			.hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
		//?} else {
		/*return Minecraft.getInstance().player.hasPermissions(2);
		*///?}
	}

	private static void reportPrintPermissionDenied() {
		Component message = Component.literal(
			ChatFormatting.RED + "You do not have permission to print on this server.");
		//? if >=26 {
		Minecraft.getInstance().player.sendSystemMessage(message);
		//?} else {
		/*Minecraft.getInstance().player.displayClientMessage(message, false);
		*///?}
	}

	public static void writeToFile(String name) {
		if (getModel().getSketch() == null)
			return;

		if (name.isEmpty())
			name = "My Build";

		Path folder = ArchitectPaths.schematics();

		FilesHelper.createFolderIfMissing(folder);
		String filename = FilesHelper.findFirstValidFilename(name, folder, "nbt");
		Path filepath = folder.resolve(filename);

		CompoundTag nbttagcompound = getModel().writeToTemplate()
			.save(new CompoundTag());
		if (!FilesHelper.writeAtomically(filepath, out -> NbtIo.writeCompressed(nbttagcompound, out))) {
			status("Could not save " + filepath);
			return;
		}
		status("Saved as " + filepath);

		BlockPos pos = model.getAnchor()
			.offset(((TemplateBlockAccess) model.getMaterializedSketch()).getBounds()
				.getOrigin());
		Component component = Component.literal("Deploy Schematic at: " + ChatFormatting.BLUE + "["
			+ pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]");
		//? if >=26 {
		Minecraft.getInstance().player.sendSystemMessage(component);
		//?} else {
		/*Minecraft.getInstance().player.displayClientMessage(component, false);*///?}
		unload();
	}

	public static void status(String message) {
		//? if >=26 {
		Minecraft.getInstance().player.sendOverlayMessage(Component.literal(message));
		//?} else {
		/*Minecraft.getInstance().player.displayClientMessage(Component.literal(message), true);*///?}
	}

	public static void pickPalette() {
		if (getModel().getSketch() == null)
			return;

		if (inPhase(ArchitectPhases.CreatingPalette)) {
			getModel().stopPalettePreview();
			enterPhase(ArchitectPhases.Previewing);
		}

		ScreenHelper.open(new PalettePickerScreen());
	}

	public static void pickScanPalette() {
		ScreenHelper.open(new PalettePickerScreen(true));
	}

	public static void manageThemes() {
		enterPhase(ArchitectPhases.ManagingThemes);
	}

	public static void createTheme() {
		TextInputPromptScreen gui = new TextInputPromptScreen(result -> {
			DesignExporter.setTheme(ThemeStorage.createTheme(result));
			ScreenHelper.open(new ThemeSettingsScreen());
		}, result -> {
		});
		gui.setButtonTextConfirm("Create");
		gui.setButtonTextAbort("Cancel");
		gui.setTitle("Enter a name for your Theme:");

		ScreenHelper.open(gui);
	}

	public static void editTheme(DesignTheme theme) {
		DesignExporter.setTheme(theme);
		enterPhase(ArchitectPhases.EditingThemes);
	}

	public static void changeExportedDesign() {
		ScreenHelper.open(new DesignExporterScreen());
	}

	// Phases

	public static boolean inPhase(ArchitectPhases phase) {
		return ArchitectManager.phase == phase;
	}

	public static void enterPhase(ArchitectPhases newPhase) {
		IArchitectPhase phaseHandler = phase.getPhaseHandler();
		phaseHandler.whenExited();
		phaseHandler = newPhase.getPhaseHandler();
		phaseHandler.whenEntered();
		phase = newPhase;
		menu.updateContents();
	}

	public static Schematic getModel() {
		return model;
	}

	public static ArchitectPhases getPhase() {
		return phase;
	}

	// Events
	// Registration lives on the loader side (shared Mixins on vanilla client classes); this class
	// only exposes the handlers they call.
	public static void onClientTick(Minecraft minecraft) {
		if (Minecraft.getInstance().level == null) {
			if (!inPhase(ArchitectPhases.Paused) && !model.isEmpty())
				enterPhase(ArchitectPhases.Paused);
			return;
		}

		phase.getPhaseHandler()
			.update();
		menu.onClientTick();

	}

	// Returns true when the scroll was consumed and vanilla must not also act on it.
	public static boolean onMouseScrolled(double horizontalAmount, double verticalAmount) {
		if (McCompat.currentScreen(Minecraft.getInstance()) != null)
			return false;
		if (phase.getPhaseHandler()
			.onScroll((int) Math.signum(verticalAmount)))
			return true;
		return false;
	}

	public static void render(PoseStack ms, MightyBuffers buffer) {
		if (Minecraft.getInstance().level != null)
			phase.getPhaseHandler()
				.render(ms, buffer);
	}

	public static void onClick(int button, int action) {
		if (McCompat.currentScreen(Minecraft.getInstance()) != null)
			return;
		if (action != Keyboard.PRESS)
			return;
		phase.getPhaseHandler()
			.onClick(button);
	}

	public static void onKeyTyped(int keyCode, int action) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && action == Keyboard.PRESS) {
			if (inPhase(ArchitectPhases.Composing) || inPhase(ArchitectPhases.Previewing)) {
				enterPhase(ArchitectPhases.Paused);
				menu.setVisible(false);
			}
			return;
		}
		if (McCompat.currentScreen(Minecraft.getInstance()) != null)
			return;
		if (MightyClient.COMPOSE.consumeClick()) {
			if (!menu.isFocused())
				openMenu();
			return;
		}

		boolean released = action == Keyboard.RELEASE;
		phase.getPhaseHandler()
			.onKey(keyCode, released);
	}

	public static void openMenu() {
		menu.updateContents();
		ScreenHelper.open(menu);
		menu.setFocused(true);
		menu.setVisible(true);
		return;
	}

	public static void tickBlockHighlightOutlines() {
		IArchitectPhase phaseHandler = phase.getPhaseHandler();
		if (phaseHandler instanceof IDrawBlockHighlights)
			((IDrawBlockHighlights) phaseHandler).tickHighlightOutlines();
	}

	// The HUD callback carried a raw partial-tick float until 1.21 replaced it with DeltaTracker.
	//? if >=26 {
	public static void onDrawGameOverlay(GuiGraphicsExtractor poseStack, DeltaTracker deltaTracker) {
		float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
	//?} else if >=1.21 {
	/*public static void onDrawGameOverlay(GuiGraphics poseStack, DeltaTracker deltaTracker) {
		float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
	*///?} else {
	/*public static void onDrawGameOverlay(GuiGraphics poseStack, float partialTicks) {
	*///?}
		IArchitectPhase phaseHandler = phase.getPhaseHandler();
		if (phaseHandler instanceof IRenderGameOverlay) {
			((IRenderGameOverlay) phaseHandler).renderGameOverlay(poseStack, partialTicks);
		}

		menu.drawPassive(poseStack, partialTicks);
		//? if <1.21.6 {
		/*com.mojang.blaze3d.systems.RenderSystem.enableBlend();*///?}

		// Draw world-space measurement labels submitted during the world render pass as HUD text.
		com.timmie.mightyarchitect.foundation.utility.HudTextBuffer.render(poseStack);
	}

	public static void resetSchematic() {
		model = new Schematic();
	}

}
