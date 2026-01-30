package com.timmie.mightyarchitect.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.palette.PaletteStorage;
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import com.timmie.mightyarchitect.gui.widgets.IconButton;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.nio.file.Paths;

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
		minecraft = Minecraft.getInstance();
		this.scanPicker = scanPicker;

	}

	@Override
	public void init() {
		super.init();
		setWindowSize(256, 236);
		widgets.clear();

		// selected
		updateSelected();

		// resource palettes
		int id = 2;
		int x = topLeftX + 10;
		int y = topLeftY + 68;
		for (String paletteName : PaletteStorage.getResourcePaletteNames()) {
			widgets.add(new PaletteButton(PaletteStorage.getPalette(paletteName), this, id, x + ((id - 2) % 5) * 23,
				y + ((id - 2) / 5) * 23));
			id++;
		}

		// my palettes
		int i = 0;
		x = topLeftX + 135;
		y = topLeftY + 68;
		for (String paletteName : PaletteStorage.getPaletteNames()) {
			widgets.add(new PaletteButton(PaletteStorage.getPalette(paletteName), this, id + i, x + (i % 5) * 23,
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
			widgets.add(buttonAddPalette);
		}

		buttonOpenFolder = new IconButton(x + (i % 5) * 23, y + (i / 5) * 23, ScreenResources.ICON_FOLDER);
		buttonOpenFolder.setToolTip(Component.literal("Open Palette Folder"));
		widgets.add(buttonOpenFolder);
		i++;

		buttonRefresh = new IconButton(x + (i % 5) * 23, y + (i / 5) * 23, ScreenResources.ICON_REFRESH);
		buttonRefresh.setToolTip(Component.literal("Refresh Imported Palettes"));
		widgets.add(buttonRefresh);
	}

	@Override
	public void removed() {
		super.removed();

		if (scanPicker) {
			if (primary.palette.hasDuplicates())
				minecraft.player.displayClientMessage(
					Component.literal(ChatFormatting.RED + "Warning: Ambiguous Scanner Palette "
						+ ChatFormatting.WHITE + "( " + primary.palette.getDuplicates() + " )"),
					false);

			minecraft.player.displayClientMessage(Component.literal("Updated Default Palette"), true);
			DesignExporter.theme.setDefaultPalette(primary.palette);
			DesignExporter.theme.setDefaultSecondaryPalette(secondary.palette);
		}
	}

	private void updateSelected() {
		widgets.remove(primary);
		widgets.remove(secondary);

		if (scanPicker) {
			primary = new PaletteButton(DesignExporter.scanningPalette, this, 0, topLeftX + 135, topLeftY + 8);
			primary.active = false;
			secondary = new PaletteButton(DesignExporter.theme.getDefaultSecondaryPalette(), this, 1, topLeftX + 192,
				topLeftY + 8);
			secondary.active = false;
			widgets.add(primary);
			widgets.add(secondary);
			return;
		}

		primary = new PaletteButton(ArchitectManager.getModel()
			.getPrimary(), this, 0, topLeftX + 135, topLeftY + 8);
		primary.active = false;
		secondary = new PaletteButton(ArchitectManager.getModel()
			.getSecondary(), this, 1, topLeftX + 192, topLeftY + 8);
		secondary.active = false;
		widgets.add(primary);
		widgets.add(secondary);
	}

	@Override
	public void renderWindow(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		ScreenResources.PALETTES.draw(ms, topLeftX, topLeftY);

		int color = ScreenResources.FONT_COLOR;

		if (scanPicker) {
			ms.drawString(font, "Choose a palette for", topLeftX + 8, topLeftY + 10, color);
			ms.drawString(font, "your theme.", topLeftX + 8, topLeftY + 18, color);

		} else {
			ms.drawString(font, "Palette Picker", topLeftX + 8, topLeftY + 10, color);
			ms.drawString(font, "Primary", topLeftX + 134, topLeftY + 30, color);
			ms.drawString(font, "Secondary", topLeftX + 191, topLeftY + 30, color);

		}

		ms.drawString(font, "Included Palettes", topLeftX + 8, topLeftY + 53, color);
		ms.drawString(font, "My Palettes", topLeftX + 134, topLeftY + 53, color);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		for (int i = 0; i < this.widgets.size(); ++i) {
			AbstractWidget guibutton = this.widgets.get(i);

			if (guibutton.isMouseOver(mouseX, mouseY)) {
				guibutton.playDownSound(this.minecraft.getSoundManager());
				if (mouseButton == 0)
					this.buttonClicked(guibutton);
				if (mouseButton == 1)
					this.buttonRightClicked(guibutton);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	protected void buttonClicked(AbstractWidget button) {
		if (button == buttonOpenFolder) {
			FilesHelper.createFolderIfMissing("palettes");
			Util.getPlatform()
				.openFile(Paths.get("palettes/")
					.toFile());
		}

		if (button == buttonRefresh) {
			PaletteStorage.loadAllPalettes();
			init();
		}

		if (scanPicker) {
			if (button instanceof PaletteButton)
				DesignExporter.scanningPalette = ((PaletteButton) button).palette;
			updateSelected();
			return;
		}

		if (!(button instanceof PaletteButton)) {
			if (button == buttonAddPalette) {
				ArchitectManager.createPalette(true);
				minecraft.setScreen(null);
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
				DesignExporter.theme.setDefaultSecondaryPalette(((PaletteButton) button).palette);
			updateSelected();
			return;
		}

		if (!(button instanceof PaletteButton)) {
			ArchitectManager.createPalette(false);
			minecraft.setScreen(null);
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
			var tooltipText = Component.literal(palette.getName());
			this.setTooltip(Tooltip.create(tooltipText));
		}

		private void preview(GuiGraphics ms, Minecraft mc) {
			// In 1.21.6, GuiGraphics.pose() returns Matrix3x2fStack (2D).
			// We pass positioning to GuiGameElement instead
			float baseX = x + 1;
			float baseY = y + 9;
			float baseZ = 100;
			float scale = 1 + 1/64f;
			renderBlock(ms, mc, new BlockPos(0, 1, 0), Palette.INNER_PRIMARY, baseX, baseY, baseZ, scale);
			renderBlock(ms, mc, new BlockPos(1, 1, 0), Palette.INNER_DETAIL, baseX, baseY, baseZ, scale);
			renderBlock(ms, mc, new BlockPos(0, 0, 0), Palette.HEAVY_PRIMARY, baseX, baseY, baseZ, scale);
			renderBlock(ms, mc, new BlockPos(1, 0, 0), Palette.ROOF_PRIMARY, baseX, baseY, baseZ, scale);
		}

		protected void renderBlock(GuiGraphics ms, Minecraft mc, BlockPos pos, Palette key, float baseX, float baseY, float baseZ, float baseScale) {
			GuiGameElement.of(palette.get(key))
				.at(baseX, baseY, baseZ)
				.atLocal(pos.getX() * baseScale, pos.getY() * baseScale, pos.getZ() * baseScale)
				.scale(7.9f * baseScale)
				.render(ms);
		}

		@Override
		public void renderWidget(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
			super.renderWidget(ms, mouseX, mouseY, partialTicks);
			preview(ms, minecraft);
		}
	}
}
