package com.timmie.mightyarchitect.control.phase;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.TemplateBlockAccess;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientChatEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.LinkedList;
import java.util.List;

public class PrintingToMultiplayer extends PhaseBase {

	static List<BlockPos> remaining;
	static int cooldown;
	static boolean approved;

	@Override
	public void whenEntered() {
		remaining = new LinkedList<>(((TemplateBlockAccess) getModel().getMaterializedSketch()).getAllPositions());
		remaining.sort((o1, o2) -> Integer.compare(o1.getY(), o2.getY()));
		Minecraft.getInstance().player.chatSigned("boe joe", Component.literal("/setblock checking permission for 'The Mighty Architect'."));
		cooldown = 500;
		approved = false;
	}

	@Override
	public void update() {
		if (cooldown > 0 && !approved) {
			cooldown--;
			return;
		}
		if (cooldown == 0) {
			ArchitectManager.enterPhase(ArchitectPhases.Previewing);
			return;
		}

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
				
				Minecraft.getInstance().player.chatSigned("boe bing", Component.literal("/setblock " + pos.getX() + " " + pos.getY() + " "
						+ pos.getZ() + " " + blockstring));
			} else {
				ArchitectManager.unload();
				break;
			}
		}
	}

	public static void RegisterCommandFeedbackEvent()
	{
		ClientChatEvent.RECEIVED.register(PrintingToMultiplayer::onCommandFeedback);
	}

	public static CompoundEventResult<Component> onCommandFeedback(ChatType.Bound bound, Component component) {
		if (component == null)
			return CompoundEventResult.pass();

		if (cooldown > 0) {
			List<Component> checking = new LinkedList<>();
			checking.add(component);

			while (!checking.isEmpty()) {
				Component iTextComponent = checking.get(0);
				if (iTextComponent instanceof TranslatableContents) {
					String test = ((TranslatableContents) iTextComponent).getKey();
					
				TheMightyArchitect.logger.info(test);
					
					if (test.equals("command.unknown.command")) {
						cooldown = 0;
						return CompoundEventResult.interruptTrue(Component.literal(
								ChatFormatting.RED + "You do not have permission to print on this server."));
					}
					if (test.equals("parsing.int.expected")) {
						approved = true;
						Minecraft.getInstance().player
								.chatSigned("joe", Component.literal("/me is printing a structure created by the Mighty Architect."));
						Minecraft.getInstance().player.chatSigned("joe", Component.literal("/gamerule sendCommandFeedback false"));
						Minecraft.getInstance().player.chatSigned("joe", Component.literal("/gamerule logAdminCommands false"));
						return CompoundEventResult.interruptTrue(component);
					}
				} else {
					checking.addAll(iTextComponent.getSiblings());
				}
				checking.remove(iTextComponent);
			}
		}
		return CompoundEventResult.pass();
	}

	@Override
	public void render(PoseStack ms, MultiBufferSource buffer) {
	}

	@Override
	public void whenExited() {
		if (approved) {
			Minecraft.getInstance().player.displayClientMessage(Component.literal(ChatFormatting.GREEN + "Finished Printing, enjoy!"),
					false);
			Minecraft.getInstance().player.chatSigned("joe", Component.literal("/gamerule logAdminCommands true"));
			Minecraft.getInstance().player.chatSigned("joe", Component.literal("/gamerule sendCommandFeedback true"));
		}
		cooldown = 0;
	}

	@Override
	public List<String> getToolTip() {
		return ImmutableList.of("Please be patient while your building is being transferred.");
	}

}
