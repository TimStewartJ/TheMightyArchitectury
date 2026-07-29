package com.timmie.mightyarchitect.mixin;

import com.timmie.mightyarchitect.platform.ClientHooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client tick hook. Minecraft#tick has a single exit, so HEAD/TAIL bracket the whole tick.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "tick", at = @At("HEAD"))
	private void mightyarchitect$tickPre(CallbackInfo ci) {
		ClientHooks.clientTickPre((Minecraft) (Object) this);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void mightyarchitect$tickPost(CallbackInfo ci) {
		ClientHooks.clientTickPost((Minecraft) (Object) this);
	}
}
