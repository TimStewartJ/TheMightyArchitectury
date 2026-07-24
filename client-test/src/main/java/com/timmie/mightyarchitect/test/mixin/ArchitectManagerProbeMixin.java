package com.timmie.mightyarchitect.test.mixin;

import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.test.ClientTestController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArchitectManager.class, remap = false)
public abstract class ArchitectManagerProbeMixin {

    @Inject(method = "onDrawGameOverlay", at = @At("HEAD"), remap = false)
    private static void mightyarchitectTest$recordHudRender(CallbackInfo callback) {
        ClientTestController.recordHudRender();
    }
}
