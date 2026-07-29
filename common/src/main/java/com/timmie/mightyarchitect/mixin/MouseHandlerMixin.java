package com.timmie.mightyarchitect.mixin;

import com.timmie.mightyarchitect.platform.ClientHooks;
import net.minecraft.client.MouseHandler;
//? if >=1.21.10 {
import net.minecraft.client.input.MouseButtonInfo;
//?} else {
/*
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Raw mouse input. Neither loader exposes this, and 1.21.10 replaced the packed button/modifier
 * arguments with a MouseButtonInfo record, so the button hook is version-split.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

	//? if >=1.21.10 {
	@Inject(method = "onButton", at = @At("HEAD"))
	private void mightyarchitect$onMouseButton(long window, MouseButtonInfo buttonInfo, int action,
		CallbackInfo ci) {
		ClientHooks.mouseClicked(buttonInfo.button(), action);
	}

	//?} else {
	/*@Inject(method = "onPress", at = @At("HEAD"))
	private void mightyarchitect$onMouseButton(long window, int button, int action, int modifiers,
		CallbackInfo ci) {
		ClientHooks.mouseClicked(button, action);
	}

	*///?}
	// Cancelling here swallows the scroll before vanilla can also cycle the hotbar, which is what
	// the composer's scroll-to-adjust needs.
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void mightyarchitect$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		if (ClientHooks.mouseScrolled(xOffset, yOffset))
			ci.cancel();
	}
}
