package com.timmie.mightyarchitect.control.phase;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.TemplateBlockAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
		if (!Minecraft.getInstance().player.hasPermissions(2)) {
			success = false;

			return;
		}

		success = true;

		Minecraft.getInstance().player
				.commandUnsigned("me is printing a structure created by the Mighty Architect.");
		Minecraft.getInstance().player.commandUnsigned("gamerule sendCommandFeedback false");
		Minecraft.getInstance().player.commandUnsigned("gamerule logAdminCommands false");

		remaining = new LinkedList<>(((TemplateBlockAccess) getModel().getMaterializedSketch()).getAllPositions());
		remaining.sort((o1, o2) -> Integer.compare(o1.getY(), o2.getY()));
	}

	@Override
	public void update() {
		// exit state if not successful
		if (!success) {
			Minecraft.getInstance().player.displayClientMessage(Component.literal(
							ChatFormatting.RED + "You do not have permission to print on this server."), false);
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
				
				Minecraft.getInstance().player.commandUnsigned("setblock " + pos.getX() + " " + pos.getY() + " "
						+ pos.getZ() + " " + blockstring);
			} else {
				ArchitectManager.unload();
				break;
			}
		}
	}

	@Override
	public void render(PoseStack ms, MultiBufferSource buffer) {
	}

	@Override
	public void whenExited() {
		if (success) {
			Minecraft.getInstance().player.displayClientMessage(Component.literal(ChatFormatting.GREEN + "Finished Printing, enjoy!"),
					false);
			Minecraft.getInstance().player.commandUnsigned("gamerule logAdminCommands true");
			Minecraft.getInstance().player.commandUnsigned("gamerule sendCommandFeedback true");
		}
	}

	@Override
	public List<String> getToolTip() {
		return ImmutableList.of("Please be patient while your building is being transferred.");
	}

}
