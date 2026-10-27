package com.timmie.mightyarchitect.mixin;

import com.timmie.mightyarchitect.platform.ClientHooks;
import net.minecraft.client.KeyboardHandler;
//? if >=1.21.10 {
import net.minecraft.client.input.KeyEvent;
//?} else {
/*
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Raw key input, taken at RETURN rather than HEAD: vanilla registers the key mapping click inside
 * this method, and the mod's handler asks KeyMapping#consumeClick, which would otherwise always see
 * the previous frame's state.
 * <p>
 * Esc is the exception and is also looked at on HEAD, because vanilla opens the pause menu from
 * inside this same method: by RETURN it is too late to use the key for cancelling instead.
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

	//? if >=1.21.10 {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void mightyarchitect$keyPressFirst(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
		if (ClientHooks.keyPressedBeforeVanilla(keyEvent.key(), action))
			ci.cancel();
	}
	//?} else {
	/*@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void mightyarchitect$keyPressFirst(long window, int key, int scanCode, int action, int modifiers,
		CallbackInfo ci) {
		if (ClientHooks.keyPressedBeforeVanilla(key, action))
			ci.cancel();
	}
	*///?}

	//? if >=1.21.10 {
	@Inject(method = "keyPress", at = @At("RETURN"))
	private void mightyarchitect$keyPress(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
		ClientHooks.keyPressed(keyEvent.key(), action);
	}
	//?} else {
	/*@Inject(method = "keyPress", at = @At("RETURN"))
	private void mightyarchitect$keyPress(long window, int key, int scanCode, int action, int modifiers,
		CallbackInfo ci) {
		ClientHooks.keyPressed(key, action);
	}
	*///?}
}
