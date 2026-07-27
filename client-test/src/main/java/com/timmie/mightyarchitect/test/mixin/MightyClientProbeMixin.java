package com.timmie.mightyarchitect.test.mixin;

import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.test.ClientTestController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MightyClient.class, remap = false)
public abstract class MightyClientProbeMixin {

    @Inject(method = "onRenderWorld", at = @At("HEAD"), remap = false)
    private static void mightyarchitectTest$recordWorldRender(CallbackInfo callback) {
        ClientTestController.recordWorldRender();
    }
}
