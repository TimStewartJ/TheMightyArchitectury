package com.timmie.mightyarchitect.control;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.control.design.DesignSlice.DesignSliceTrait;
import com.timmie.mightyarchitect.control.design.ExportTools;
import com.timmie.mightyarchitect.control.phase.ArchitectPhases;
import com.timmie.mightyarchitect.control.phase.MultiplayerPrintCommands;
import com.timmie.mightyarchitect.foundation.compat.McCompat;
import com.timmie.mightyarchitect.item.ArchitectWandClient;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;

/**
 * What right-clicking does while a theme is being edited.
 * <p>
 * The exporter's tools are plain vanilla items and blocks (see {@link ExportTools}), so nothing on
 * the server knows what they are for. Their behaviour lives here instead, reached from a client
 * mixin on {@code MultiPlayerGameMode} before vanilla sends anything: a click the mod handles never
 * reaches the server, and every other click is left exactly as vanilla would treat it. Outside the
 * theme editor these hooks do nothing at all.
 */
public final class ExportInteractions {

	private ExportInteractions() {
	}

	/**
	 * @return the result to report instead of vanilla's, or {@code null} to let vanilla handle the
	 *         click
	 */
	public static InteractionResult useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
		if (!ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
			return null;

		Level level = Minecraft.getInstance().level;
		if (level == null)
			return null;
		BlockPos pos = hit.getBlockPos();
		if (ExportTools.isWand(player.getItemInHand(hand))) {
			if (!wandReady(level))
				return InteractionResult.SUCCESS;
			if (player.isShiftKeyDown())
				ArchitectWandClient.openGui();
			else
				ArchitectWandClient.handleUseOnDesignAnchor(player, level, pos, level.getBlockState(pos));
			return InteractionResult.SUCCESS;
		}

		// Holding the exporter in the other hand means this click is about the exporter, not the
		// marker; vanilla calls the off hand next and the branch above takes it.
		if (hand != InteractionHand.MAIN_HAND || holdsWand(player))
			return null;
		// The top and bottom faces stay free for stacking the next marker onto a column.
		if (hit.getDirection()
			.getAxis() == Direction.Axis.Y)
			return null;
		BlockState state = level.getBlockState(pos);
		if (!ExportTools.isMarker(state))
			return null;
		// Sneaking with something in hand is vanilla's "place against it", same as for any block.
		ItemStack held = player.getItemInHand(hand);
		if (!held.isEmpty() && player.isShiftKeyDown())
			return null;

		cycleMarker(player, pos, state, player.isShiftKeyDown());
		return InteractionResult.SUCCESS;
	}

	public static InteractionResult useItem(Player player, InteractionHand hand) {
		if (!ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
			return null;
		if (!ExportTools.isWand(player.getItemInHand(hand)))
			return null;
		Level level = Minecraft.getInstance().level;
		if (level != null && wandReady(level))
			ArchitectWandClient.handleRightClick(player);
		return InteractionResult.SUCCESS;
	}

	/**
	 * Vanilla repeats a held right-click every four ticks, and the exporter's first click only
	 * shows a design's outline - the second one writes it. The old wand item sat on a five-tick
	 * cooldown so a slightly long press could not do both; this is that cooldown.
	 */
	static final int WAND_COOLDOWN_TICKS = 5;
	private static long lastWandUse = Long.MIN_VALUE;

	private static boolean wandReady(Level level) {
		long now = level.getGameTime();
		if (lastWandUse != Long.MIN_VALUE && now >= lastWandUse && now - lastWandUse < WAND_COOLDOWN_TICKS)
			return false;
		lastWandUse = now;
		return true;
	}

	/**
	 * Swaps a layer marker for the next trait's block.
	 *
	 * @return whether a change was sent
	 */
	public static boolean cycleMarker(LocalPlayer player, BlockPos pos, BlockState current, boolean backwards) {
		BlockState next = ExportTools.cycledMarker(current, backwards);
		if (!placeBlock(player, pos, next))
			return false;
		DesignSliceTrait trait = ExportTools.traitOf(next);
		ArchitectManager.status(ExportTools.label(trait) + " " + trait.getDescription());
		return true;
	}

	/**
	 * Places one block through the same two routes printing uses: the mod's packet when the server
	 * has the mod, otherwise a {@code /setblock}. Both need what the server would ask for anyway -
	 * creative or operator for the packet ({@code ServerBuildGuard.mayBuild}), operator for the
	 * command.
	 */
	static boolean placeBlock(LocalPlayer player, BlockPos pos, BlockState state) {
		Map<BlockPos, BlockState> block = Map.of(pos, state);
		InstantPrintPacket packet = InstantPrintPacket.sendSchematic(block, BlockPos.ZERO)
			.get(0);
		boolean operator = ArchitectManager.hasGameMasterPermission();

		if (AllPackets.canSendToServer(packet)) {
			if (!isCreative() && !operator)
				return deny(player);
			AllPackets.sendToServer(packet);
			return true;
		}

		if (!operator)
			return deny(player);
		McCompat.sendCommand(player, MultiplayerPrintCommands.plan(block)
			.get(0)
			.text());
		return true;
	}

	private static boolean holdsWand(Player player) {
		return ExportTools.isWand(player.getMainHandItem()) || ExportTools.isWand(player.getOffhandItem());
	}

	private static boolean isCreative() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.gameMode != null && minecraft.gameMode.getPlayerMode()
			.isCreative();
	}

	private static boolean deny(LocalPlayer player) {
		Component message = Component.literal(ChatFormatting.RED
			+ "Changing layer markers needs creative mode or operator permission here.");
		//? if >=26 {
		player.sendOverlayMessage(message);
		//?} else {
		/*player.displayClientMessage(message, true);
		*///?}
		return false;
	}
}
