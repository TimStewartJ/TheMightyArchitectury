package com.timmie.mightyarchitect.test.mixin;

import com.timmie.mightyarchitect.platform.ClientHooks;
import com.timmie.mightyarchitect.test.PlayerJourneyController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientHooks.class, remap = false)
public abstract class ClientHooksProbeMixin {

    @Inject(method = "mouseScrolled", at = @At("HEAD"), remap = false)
    private static void mightyarchitectTest$recordScroll(double horizontal, double vertical,
                                                         CallbackInfoReturnable<Boolean> callback) {
        PlayerJourneyController.recordScroll(horizontal, vertical);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), remap = false)
    private static void mightyarchitectTest$recordClick(int button, int action, CallbackInfo callback) {
        PlayerJourneyController.recordMouseButton(button, action);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), remap = false)
    private static void mightyarchitectTest$recordKey(int key, int action, CallbackInfo callback) {
        PlayerJourneyController.recordKey(key, action);
    }
}
