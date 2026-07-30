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

import java.util.LinkedList;
import java.util.List;

public class PrintingToMultiplayer extends PhaseBase {

	static List<BlockPos> remaining;
	static boolean success;

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

		remaining = new LinkedList<>(getModel().getMaterializedSketch().getAllPositions());
		remaining.sort((o1, o2) -> Integer.compare(o1.getY(), o2.getY()));
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

		// print 10 blocks an update until completed
		for (int i = 0; i < 10; i++) {
			if (!remaining.isEmpty()) {
				BlockPos pos = remaining.get(0);
				remaining.remove(0);
				pos = pos.offset(getModel().getAnchor());
				BlockState state = getModel().getMaterializedSketch().getBlockState(pos);

				if (minecraft.level.getBlockState(pos) == state)
					continue;
				if (!minecraft.level.isUnobstructed(state, pos, CollisionContext.of(minecraft.player)))
					continue;

				String blockstring = state.toString().replaceFirst("Block\\{", "").replaceFirst("\\}", "");

				String cmd = "setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + blockstring;
				//? if >=1.21.6 {
				Minecraft.getInstance().player.connection.sendCommand(cmd);
				//?} else {
				/*Minecraft.getInstance().player.connection.sendUnsignedCommand(cmd);
				*///?}
			} else {
				ArchitectManager.unload();
				break;
			}
		}
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
			"Your server will report each block as it is placed.");
	}

}
