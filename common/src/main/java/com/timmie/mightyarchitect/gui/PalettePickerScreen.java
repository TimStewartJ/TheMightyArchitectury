package com.timmie.mightyarchitect.gui;

//? if >=1.21.10 {
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import com.timmie.mightyarchitect.foundation.compat.McCompat;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.palette.PaletteStorage;
import com.timmie.mightyarchitect.control.storage.ArchitectPaths;
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import com.timmie.mightyarchitect.gui.widgets.IconButton;
import net.minecraft.ChatFormatting;
//? if >=1.21.11 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}
import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.21.10 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else if >=1.20 {
/*import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import net.minecraft.client.gui.Gui;
import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.10 {
import net.minecraft.client.input.MouseButtonEvent;
//?} else {
/*
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;

public class PalettePickerScreen extends AbstractSimiScreen {

	private PaletteButton primary, secondary;
	private IconButton buttonAddPalette;
	private IconButton buttonOpenFolder;
	private IconButton buttonRefresh;
	private final boolean scanPicker;

	public PalettePickerScreen() {
		this(false);
	}

	public PalettePickerScreen(boolean scanPicker) {
		super();

		//? if >=1.21.11 {
		//?} else {
		/*minecraft = Minecraft.getInstance();
		*///?}
		this.scanPicker = scanPicker;

	}

	@Override
	public void init() {
		super.init();
		setWindowSize(256, 236);
		clearWidgets();

		// selected
		updateSelected();

		// resource palettes
		int id = 2;
		int x = topLeftX + 10;
		int y = topLeftY + 68;
		for (String paletteName : PaletteStorage.getResourcePaletteNames()) {
			addWidget(new PaletteButton(PaletteStorage.getPalette(paletteName), this, id, x + ((id - 2) % 5) * 23,
				y + ((id - 2) / 5) * 23));
			id++;
		}

		// my palettes
		int i = 0;
		x = topLeftX + 135;
		y = topLeftY + 68;
		for (String paletteName : PaletteStorage.getPaletteNames()) {
			addWidget(new PaletteButton(PaletteStorage.getPalette(paletteName), this, id + i, x + (i % 5) * 23,
				y + (i / 5) * 23));
			i++;
		}

		// create
		if (!scanPicker) {
			buttonAddPalette = new IconButton(x + (i % 5) * 23, y + (i / 5) * 23, ScreenResources.ICON_ADD);
			buttonAddPalette.setToolTip(Component.literal("Create Palette"));
			buttonAddPalette.getToolTip()
				.add(Component.literal("Will use currently selected").withStyle(ChatFormatting.GRAY));
			buttonAddPalette.getToolTip()
				.add(Component.literal("Palette as the template.").withStyle(ChatFormatting.GRAY));
			i++;
			addWidget(buttonAddPalette);
		}

		buttonOpenFolder = new IconButton(x + (i % 5) * 23, y + (i / 5) * 23, ScreenResources.ICON_FOLDER);
		buttonOpenFolder.setToolTip(Component.literal("Open Palette Folder"));
		addWidget(buttonOpenFolder);
		i++;

		buttonRefresh = new IconButton(x + (i % 5) * 23, y + (i / 5) * 23, ScreenResources.ICON_REFRESH);
		buttonRefresh.setToolTip(Component.literal("Refresh Imported Palettes"));
		addWidget(buttonRefresh);
	}

	@Override
	public void removed() {
		super.removed();

		if (scanPicker) {
			if (primary.palette.hasDuplicates())
				//? if >=26 {
				minecraft.player.sendSystemMessage(Component.literal(ChatFormatting.RED + "Warning: Ambiguous Scanner Palette "
						+ ChatFormatting.WHITE + "( " + primary.palette.getDuplicates() + " )"));
				//?} else {
				/*minecraft.player.displayClientMessage(
					Component.literal(ChatFormatting.RED + "Warning: Ambiguous Scanner Palette "
						+ ChatFormatting.WHITE + "( " + primary.palette.getDuplicates() + " )"),
					false);
				*///?}

			//? if >=26 {
			minecraft.player.sendOverlayMessage(Component.literal("Updated Default Palette"));
			//?} else {
			/*minecraft.player.displayClientMessage(Component.literal("Updated Default Palette"), true);
			*///?}
			DesignExporter.getTheme().setDefaultPalette(primary.palette);
			DesignExporter.getTheme().setDefaultSecondaryPalette(secondary.palette);
		}
	}

	private void updateSelected() {
		if (primary != null)
			removeWidget(primary);
		if (secondary != null)
			removeWidget(secondary);

		if (scanPicker) {
			primary = new PaletteButton(DesignExporter.getScanningPalette(), this, 0, topLeftX + 135, topLeftY + 8);
			primary.active = false;
			secondary = new PaletteButton(DesignExporter.getTheme().getDefaultSecondaryPalette(), this, 1, topLeftX + 192,
				topLeftY + 8);
			secondary.active = false;
			addWidget(primary);
			addWidget(secondary);
			return;
		}

		primary = new PaletteButton(ArchitectManager.getModel()
			.getPrimary(), this, 0, topLeftX + 135, topLeftY + 8);
		primary.active = false;
		secondary = new PaletteButton(ArchitectManager.getModel()
			.getSecondary(), this, 1, topLeftX + 192, topLeftY + 8);
		secondary.active = false;
		addWidget(primary);
		addWidget(secondary);
	}

	@Override
	//? if >=26 {
	public void renderWindow(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
	//?} else {
	/*public void renderWindow(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
	*///?}
		ScreenResources.PALETTES.draw(ms, topLeftX, topLeftY);

		int color = ScreenResources.FONT_COLOR;

		if (scanPicker) {
			//? if >=26 {
			ms.text(font, "Choose a palette for", topLeftX + 8, topLeftY + 10, color);
			ms.text(font, "your theme.", topLeftX + 8, topLeftY + 18, color);
			//?} else {
			/*ms.drawString(font, "Choose a palette for", topLeftX + 8, topLeftY + 10, color);
			ms.drawString(font, "your theme.", topLeftX + 8, topLeftY + 18, color);
			*///?}

		} else {
			//? if >=26 {
			ms.text(font, "Palette Picker", topLeftX + 8, topLeftY + 10, color);
			ms.text(font, "Primary", topLeftX + 134, topLeftY + 30, color);
			ms.text(font, "Secondary", topLeftX + 191, topLeftY + 30, color);
			//?} else {
			/*ms.drawString(font, "Palette Picker", topLeftX + 8, topLeftY + 10, color);
			ms.drawString(font, "Primary", topLeftX + 134, topLeftY + 30, color);
			ms.drawString(font, "Secondary", topLeftX + 191, topLeftY + 30, color);
			*///?}

		}

		//? if >=26 {
		ms.text(font, "Included Palettes", topLeftX + 8, topLeftY + 53, color);
		ms.text(font, "My Palettes", topLeftX + 134, topLeftY + 53, color);
		//?} else {
		/*ms.drawString(font, "Included Palettes", topLeftX + 8, topLeftY + 53, color);
		ms.drawString(font, "My Palettes", topLeftX + 134, topLeftY + 53, color);
		*///?}
	}

	@Override
	//? if >=1.21.10 {
	public boolean mouseClicked(MouseButtonEvent event, boolean flag) {
		double mouseX = event.x();
		double mouseY = event.y();
		int mouseButton = event.button();
	//?} else {
	/*public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
	*///?}
		// A copy: buttonClicked can rebuild the screen, which clears the live child list.
		for (GuiEventListener child : new ArrayList<>(children())) {
			if (!(child instanceof AbstractWidget guibutton))
				continue;

			if (guibutton.isMouseOver(mouseX, mouseY)) {
				guibutton.playDownSound(this.minecraft.getSoundManager());
				if (mouseButton == 0)
					this.buttonClicked(guibutton);
				if (mouseButton == 1)
					this.buttonRightClicked(guibutton);
				return true;
			}
		}
		//? if >=1.21.10 {
		return super.mouseClicked(event, flag);
		//?} else {
		/*return super.mouseClicked(mouseX, mouseY, mouseButton);
		*///?}
	}

	protected void buttonClicked(AbstractWidget button) {
		if (button == buttonOpenFolder) {
			Path folder = ArchitectPaths.palettes();
			FilesHelper.createFolderIfMissing(folder);
			Util.getPlatform()
				.openFile(folder.toFile());
		}

		if (button == buttonRefresh) {
			PaletteStorage.loadAllPalettes();
			init();
		}

		if (scanPicker) {
			if (button instanceof PaletteButton)
				DesignExporter.setScanningPalette(((PaletteButton) button).palette);
			updateSelected();
			return;
		}

		if (!(button instanceof PaletteButton)) {
			if (button == buttonAddPalette) {
				ArchitectManager.createPalette(true);
				McCompat.setScreen(minecraft, null);
			}
		} else {
			ArchitectManager.getModel()
				.swapPrimaryPalette(((PaletteButton) button).palette);
			updateSelected();
			MightyClient.renderer.update();
		}
	}

	protected void buttonRightClicked(AbstractWidget button) {
		if (scanPicker) {
			if (button instanceof PaletteButton)
				DesignExporter.getTheme().setDefaultSecondaryPalette(((PaletteButton) button).palette);
			updateSelected();
			return;
		}

		if (!(button instanceof PaletteButton)) {
			ArchitectManager.createPalette(false);
			McCompat.setScreen(minecraft, null);
		} else {
			ArchitectManager.getModel()
				.swapSecondaryPalette(((PaletteButton) button).palette);
			updateSelected();
			MightyClient.renderer.update();
		}
	}

	class PaletteButton extends IconButton {
		Screen parent;
		PaletteDefinition palette;

		public PaletteButton(PaletteDefinition palette, Screen parent, int buttonId, int x, int y) {
			super(x, y, ScreenResources.ICON_NONE);
			this.parent = parent;
			this.palette = palette;
			visible = true;
			active = true;
			// The mod's own tooltip, not vanilla's: vanilla shows a Tooltip when the widget is
			// hovered *or* focused-by-keyboard, and these buttons became focusable when the screen
			// started registering its widgets with vanilla. renderWindowForeground gates on hover.
			setToolTip(Component.literal(palette.getName()));
		}

		//? if >=26 {
		private void preview(GuiGraphicsExtractor ms, Minecraft mc) {
			// pose() is a Matrix3x2fStack (2D); positioning is passed to GuiGameElement.
			float baseX = getX() + 1;
			float baseY = getY() + 9;
			float baseZ = 100;
			float scale = 1 + 1/64f;
			renderBlock(ms, mc, new BlockPos(0, 1, 0), Palette.INNER_PRIMARY, baseX, baseY, baseZ, scale);
			renderBlock(ms, mc, new BlockPos(1, 1, 0), Palette.INNER_DETAIL, baseX, baseY, baseZ, scale);
			renderBlock(ms, mc, new BlockPos(0, 0, 0), Palette.HEAVY_PRIMARY, baseX, baseY, baseZ, scale);
			renderBlock(ms, mc, new BlockPos(1, 0, 0), Palette.ROOF_PRIMARY, baseX, baseY, baseZ, scale);
		//?} else if >=1.21.6 {
		/*private void preview(GuiGraphics ms, Minecraft mc) {
			// In 1.21.6, GuiGraphics.pose() returns Matrix3x2fStack (2D).
			// We pass positioning to GuiGameElement instead
			float baseX = getX() + 1;
			float baseY = getY() + 9;
			float baseZ = 100;
			float scale = 1 + 1/64f;
			renderBlock(ms, mc, new BlockPos(0, 1, 0), Palette.INNER_PRIMARY, baseX, baseY, baseZ, scale);
			renderBlock(ms, mc, new BlockPos(1, 1, 0), Palette.INNER_DETAIL, baseX, baseY, baseZ, scale);
			renderBlock(ms, mc, new BlockPos(0, 0, 0), Palette.HEAVY_PRIMARY, baseX, baseY, baseZ, scale);
			renderBlock(ms, mc, new BlockPos(1, 0, 0), Palette.ROOF_PRIMARY, baseX, baseY, baseZ, scale);
		*///?} else {
		/*private void preview(GuiGraphics ms, Minecraft mc) {
			ms.pose().pushPose();
			ms.pose().translate(getX() + 1, getY() + 9, 100);
			ms.pose().scale(1 + 1/64f, 1 + 1/64f, 1);
			renderBlock(ms, mc, new BlockPos(0, 1, 0), Palette.INNER_PRIMARY);
			renderBlock(ms, mc, new BlockPos(1, 1, 0), Palette.INNER_DETAIL);
			renderBlock(ms, mc, new BlockPos(0, 0, 0), Palette.HEAVY_PRIMARY);
			renderBlock(ms, mc, new BlockPos(1, 0, 0), Palette.ROOF_PRIMARY);
			ms.pose().popPose();
		*///?}
		}

		//? if >=26 {
		protected void renderBlock(GuiGraphicsExtractor ms, Minecraft mc, BlockPos pos, Palette key, float baseX, float baseY, float baseZ, float baseScale) {
		//?} else if >=1.21.6 {
		/*protected void renderBlock(GuiGraphics ms, Minecraft mc, BlockPos pos, Palette key, float baseX, float baseY, float baseZ, float baseScale) {
		*///?} else {
		/*protected void renderBlock(GuiGraphics ms, Minecraft mc, BlockPos pos, Palette key) {
			ms.pose().pushPose();

		*///?}
			GuiGameElement.of(palette.get(key))
				//? if >=1.21.6 {
				.at(baseX, baseY, baseZ)
				.atLocal(pos.getX() * baseScale, pos.getY() * baseScale, pos.getZ() * baseScale)
				.scale(7.9f * baseScale)
				//?} else {
				/*.atLocal(pos.getX(), pos.getY(), pos.getZ())
				.scale(7.9f)
				*///?}
				.render(ms);
			//? if >=1.21.6 {
			//?} else {
			/*
			ms.pose().popPose();
			*///?}
		}

		@Override
		//? if >=26 {
		public void extractWidgetRenderState(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
			super.extractWidgetRenderState(ms, mouseX, mouseY, partialTicks);
		//?} else if >=1.20 {
		/*public void renderWidget(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
			super.renderWidget(ms, mouseX, mouseY, partialTicks);
		*///?} else {
		/*public void renderWidget(com.mojang.blaze3d.vertex.PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
			super.renderWidget(poseStack, mouseX, mouseY, partialTicks);
			GuiGraphics ms = new GuiGraphics(poseStack);
		*///?}
			preview(ms, minecraft);
		}
	}
}
