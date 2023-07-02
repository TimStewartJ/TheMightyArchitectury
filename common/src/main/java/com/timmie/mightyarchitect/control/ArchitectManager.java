package com.timmie.mightyarchitect.control;

import com.mojang.blaze3d.systems.RenderSystem;
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
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import com.timmie.mightyarchitect.foundation.utility.Keyboard;
import com.timmie.mightyarchitect.gui.*;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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
			editTheme(DesignExporter.theme);
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

		Minecraft mc = Minecraft.getInstance();

		if (mc.hasSingleplayerServer()) {
			for (InstantPrintPacket packet : getModel().getPackets())
				AllPackets.channel.sendToServer(packet);
			MightyClient.renderer.setActive(false);
			status("Printed result into world.");
			unload();
			return;
		}

		enterPhase(ArchitectPhases.PrintingToMultiplayer);
	}

	public static void writeToFile(String name) {
		if (getModel().getSketch() == null)
			return;

		if (name.isEmpty())
			name = "My Build";

		String folderPath = "schematics";

		FilesHelper.createFolderIfMissing(folderPath);
		String filename = FilesHelper.findFirstValidFilename(name, folderPath, "nbt");
		String filepath = folderPath + "/" + filename;

		OutputStream outputStream = null;
		try {
			outputStream = Files.newOutputStream(Paths.get(filepath), StandardOpenOption.CREATE);
			CompoundTag nbttagcompound = getModel().writeToTemplate()
				.save(new CompoundTag());
			NbtIo.writeCompressed(nbttagcompound, outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (outputStream != null)
				IOUtils.closeQuietly(outputStream);
		}
		status("Saved as " + filepath);

		BlockPos pos = model.getAnchor()
			.offset(((TemplateBlockAccess) model.getMaterializedSketch()).getBounds()
				.getOrigin());
		Component component = Component.literal("Deploy Schematic at: " + ChatFormatting.BLUE + "["
			+ pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]");
		Minecraft.getInstance().player.displayClientMessage(component, false);
		unload();
	}

	public static void status(String message) {
		Minecraft.getInstance().player.displayClientMessage(Component.literal(message), true);
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
	public static void registerAllEvents()
	{
		ClientTickEvent.CLIENT_POST.register(ArchitectManager::onClientTick);
		ClientGuiEvent.RENDER_HUD.register(ArchitectManager::onDrawGameOverlay);

		ClientRawInputEvent.MOUSE_SCROLLED.register(ArchitectManager::onMouseScrolled);
		ClientRawInputEvent.MOUSE_CLICKED_PRE.register(ArchitectManager::onClick);
		ClientRawInputEvent.KEY_PRESSED.register(ArchitectManager::onKeyTyped);
		InteractionEvent.RIGHT_CLICK_BLOCK.register(ArchitectManager::onItemRightClick);
	}

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

	public static EventResult onMouseScrolled(Minecraft minecraft, double v) {
		if (Minecraft.getInstance().screen != null)
			return EventResult.pass();
		if (phase.getPhaseHandler()
			.onScroll((int) Math.signum(v)))
			return EventResult.interruptTrue();
		return EventResult.pass();
	}

	public static void render(PoseStack ms, MultiBufferSource buffer) {
		if (Minecraft.getInstance().level != null)
			phase.getPhaseHandler()
				.render(ms, buffer);
	}

	public static EventResult onClick(Minecraft minecraft, int button, int action, int modifiers) {
		if (Minecraft.getInstance().screen != null)
			return EventResult.pass();
		if (action != Keyboard.PRESS)
			return EventResult.pass();
		phase.getPhaseHandler()
			.onClick(button);
		return EventResult.pass();
	}

	public static EventResult onKeyTyped(Minecraft minecraft, int keyCode, int scanCode, int action, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && action == Keyboard.PRESS) {
			if (inPhase(ArchitectPhases.Composing) || inPhase(ArchitectPhases.Previewing)) {
				enterPhase(ArchitectPhases.Paused);
				menu.setVisible(false);
			}
			return EventResult.pass();
		}
		if (Minecraft.getInstance().screen != null)
			return EventResult.pass();
		if (MightyClient.COMPOSE.consumeClick()) {
			if (!menu.isFocused())
				openMenu();
			return EventResult.pass();
		}

		boolean released = action == Keyboard.RELEASE;
		phase.getPhaseHandler()
			.onKey(keyCode, released);
		return EventResult.pass();
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

	public static void onDrawGameOverlay(GuiGraphics poseStack, float partialTicks) {
		IArchitectPhase phaseHandler = phase.getPhaseHandler();
		if (phaseHandler instanceof IRenderGameOverlay) {
			((IRenderGameOverlay) phaseHandler).renderGameOverlay(poseStack, partialTicks);
		}

		menu.drawPassive();
		RenderSystem.enableBlend();
	}

	public static EventResult onItemRightClick(Player player, InteractionHand interactionHand, BlockPos blockPos, Direction direction) { return EventResult.pass(); }

	public static void resetSchematic() {
		model = new Schematic();
	}

}
