package com.timmie.mightyarchitect.control.phase;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.control.ArchitectManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import com.timmie.mightyarchitect.foundation.MightyBuffers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Deque;

public class PrintingToMultiplayer extends PhaseBase {

	private static final int COMMANDS_PER_TICK = 1;
	private static final int POSITION_CHECKS_PER_TICK = 8_192;

	private Deque<MultiplayerPrintCommands.Command> remaining;
	private boolean success;

	@Override
	public void whenEntered() {
		// check for permissions for the setblock command
		//? if >=1.21.11 {
		if (!Minecraft.getInstance().player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
		//?} else {
		/*if (!Minecraft.getInstance().player.hasPermissions(2)) {
		*///?}
			success = false;

			return;
		}

		success = true;

		// Printing deliberately does NOT touch the server's gamerules. It used to turn off
		// sendCommandFeedback and logAdminCommands and turn them back on afterwards, which
		// silently disabled the server's admin-command audit log for everyone, and left it
		// disabled for good if the print was interrupted by a crash, kick or disconnect. The
		// resulting command feedback in chat is the honest cost of building with commands.

		Map<BlockPos, BlockState> blocks = new HashMap<>();
		for (BlockPos localPos : getModel().getMaterializedSketch().getAllPositions()) {
			BlockPos worldPos = localPos.offset(getModel().getAnchor());
			BlockState state = getModel().getMaterializedSketch().getBlockState(worldPos);
			blocks.put(worldPos, state);
		}
		remaining = new ArrayDeque<>(MultiplayerPrintCommands.plan(blocks));
	}

	@Override
	public void update() {
		// exit state if not successful
		if (!success) {
			//? if >=26 {
			Minecraft.getInstance().player.sendSystemMessage(Component.literal(
							ChatFormatting.RED + "You do not have permission to print on this server."));
			//?} else {
			/*Minecraft.getInstance().player.displayClientMessage(Component.literal(
							ChatFormatting.RED + "You do not have permission to print on this server."), false);
			*///?}
			ArchitectManager.enterPhase(ArchitectPhases.Previewing);
			return;
		}

		int sent = 0;
		int checked = 0;
		while (sent < COMMANDS_PER_TICK && checked < POSITION_CHECKS_PER_TICK) {
			MultiplayerPrintCommands.Command queued = remaining.pollFirst();
			if (queued == null) {
				ArchitectManager.unload();
				break;
			}
			checked += queued.blockCount();

			List<MultiplayerPrintCommands.Command> current = MultiplayerPrintCommands.replan(queued,
				pos -> isCurrentlyPlaceable(pos, queued.state()));
			for (int i = current.size() - 1; i > 0; i--)
				remaining.addFirst(current.get(i));
			if (current.isEmpty())
				continue;

			//? if >=1.21.6 {
			Minecraft.getInstance().player.connection.sendCommand(current.get(0).text());
			//?} else {
			/*Minecraft.getInstance().player.connection.sendUnsignedCommand(current.get(0).text());
			*///?}
			sent++;
		}
	}

	private boolean isCurrentlyPlaceable(BlockPos pos, BlockState state) {
		// A matching block is left untouched, including any block-entity data already there.
		return minecraft.level.isInWorldBounds(pos)
			&& minecraft.level.isLoaded(pos)
			&& !minecraft.level.getBlockState(pos).equals(state)
			&& minecraft.level.isUnobstructed(state, pos, CollisionContext.of(minecraft.player));
	}

	@Override
	public void render(PoseStack ms, MightyBuffers buffer) {
	}

	@Override
	public void whenExited() {
		if (success) {
			//? if >=26 {
			Minecraft.getInstance().player.sendSystemMessage(Component.literal(ChatFormatting.GREEN + "Finished Printing, enjoy!"));
			//?} else {
			/*Minecraft.getInstance().player.displayClientMessage(Component.literal(ChatFormatting.GREEN + "Finished Printing, enjoy!"),
					false);
			*///?}
		}
	}

	@Override
	public List<String> getToolTip() {
		return ImmutableList.of("Please be patient while your building is being transferred.",
			"Your server will report each command batch as it is placed.");
	}

}
