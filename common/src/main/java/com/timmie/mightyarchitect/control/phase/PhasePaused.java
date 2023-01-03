package com.timmie.mightyarchitect.control.phase;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.MightyClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PhasePaused extends PhaseBase {

	@Override
	public void whenEntered() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return;
		player.displayClientMessage(Component.literal(
			"The Mighty Architect was " + ChatFormatting.BOLD + "Paused" + ChatFormatting.RESET + "."), false);
		player.displayClientMessage(Component.literal("You can continue composing with [" + ChatFormatting.AQUA
			+ MightyClient.COMPOSE.getTranslatedKeyMessage()
				.getString()
				.toUpperCase()
			+ ChatFormatting.WHITE + "]"), false);
	}

	@Override
	public void update() {

	}

	@Override
	public void render(PoseStack ms, MultiBufferSource buffer) {

	}

	@Override
	public void whenExited() {

	}

	@Override
	public List<String> getToolTip() {
		return ImmutableList.of("You have started a build earlier, would you like to continue where you left off?");
	}

}
