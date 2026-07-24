package com.timmie.mightyarchitect.test.mixin;

import com.timmie.mightyarchitect.control.phase.PhaseComposing;
import com.timmie.mightyarchitect.test.ClientTestController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PhaseComposing.class, remap = false)
public abstract class PhaseComposingProbeMixin {

    @Inject(method = "renderGameOverlay", at = @At("HEAD"), remap = false)
    private void mightyarchitectTest$recordComposerOverlay(CallbackInfo callback) {
        ClientTestController.recordComposerOverlay();
    }
}
