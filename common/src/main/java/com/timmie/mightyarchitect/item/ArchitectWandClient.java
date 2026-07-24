//? if >=26 {
package com.timmie.mightyarchitect.item;

import com.timmie.mightyarchitect.AllBlocks;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.phase.ArchitectPhases;
import com.timmie.mightyarchitect.control.phase.export.PhaseEditTheme;
import com.timmie.mightyarchitect.gui.DesignExporterScreen;
import com.timmie.mightyarchitect.gui.ScreenHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ArchitectWandClient {

	private ArchitectWandClient() {
	}

	public static void resetVisualization() {
		PhaseEditTheme.resetVisualization();
	}

	public static void handleUseOnDesignAnchor(Player player, Level world, BlockPos anchor, BlockState blockState) {
		if (AllBlocks.DESIGN_ANCHOR.typeOf(blockState)) {
			if (!ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
				return;

			String name = DesignExporter.exportDesign(world, anchor);
			if (!name.isEmpty())
				player.sendOverlayMessage(Component.literal(name));
		} else if (ArchitectManager.inPhase(ArchitectPhases.EditingThemes)) {
			resetVisualization();
		}
	}

	public static void handleRightClick(Player player) {
		if (!ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
			return;

		if (player.isShiftKeyDown())
			openGui();
		else
			resetVisualization();
	}

	public static void openGui() {
		if (ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
			ScreenHelper.open(new DesignExporterScreen());
	}
}
//?} else {
/*package com.timmie.mightyarchitect.item;

import com.timmie.mightyarchitect.AllBlocks;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.phase.ArchitectPhases;
import com.timmie.mightyarchitect.control.phase.export.PhaseEditTheme;
import com.timmie.mightyarchitect.gui.DesignExporterScreen;
import com.timmie.mightyarchitect.gui.ScreenHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ArchitectWandClient {

	private ArchitectWandClient() {
	}

	public static void resetVisualization() {
		PhaseEditTheme.resetVisualization();
	}

	public static void handleUseOnDesignAnchor(Player player, Level world, BlockPos anchor, BlockState blockState) {
		if (AllBlocks.DESIGN_ANCHOR.typeOf(blockState)) {
			if (!ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
				return;

			String name = DesignExporter.exportDesign(world, anchor);
			if (!name.isEmpty())
				player.displayClientMessage(Component.literal(name), true);
		} else if (ArchitectManager.inPhase(ArchitectPhases.EditingThemes)) {
			resetVisualization();
		}
	}

	public static void handleRightClick(Player player) {
		if (!ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
			return;

		if (player.isShiftKeyDown())
			openGui();
		else
			resetVisualization();
	}

	public static void openGui() {
		if (ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
			ScreenHelper.open(new DesignExporterScreen());
	}
}*///?}
