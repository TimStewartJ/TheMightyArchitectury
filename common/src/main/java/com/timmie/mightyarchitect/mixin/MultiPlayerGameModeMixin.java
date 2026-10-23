package com.timmie.mightyarchitect.mixin;

import com.timmie.mightyarchitect.platform.ClientHooks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Right-clicks with the theme exporter's tools, before vanilla turns them into a packet.
 * <p>
 * Both signatures are identical on every node from 1.19.4 to 26.3 and are not changed by the Forge
 * or NeoForge patches (javap'd). The hook sits here rather than on {@code Minecraft.startUseItem}
 * because that method sets the four-tick right-click delay first: cancelling before it would fire
 * on every tick the button is held.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void mightyarchitect$useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
		CallbackInfoReturnable<InteractionResult> cir) {
		InteractionResult result = ClientHooks.useItemOn(player, hand, hit);
		if (result != null)
			cir.setReturnValue(result);
	}

	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void mightyarchitect$useItem(Player player, InteractionHand hand,
		CallbackInfoReturnable<InteractionResult> cir) {
		InteractionResult result = ClientHooks.useItem(player, hand);
		if (result != null)
			cir.setReturnValue(result);
	}
}
