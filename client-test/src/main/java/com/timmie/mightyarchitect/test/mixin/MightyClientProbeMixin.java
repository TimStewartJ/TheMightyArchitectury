package com.timmie.mightyarchitect.test.mixin;

import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.test.ClientTestController;
import net.minecraft.client.Minecraft;
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

    // HEAD, not TAIL: onTick returns early until a world is loaded, and the harness has to run
    // through its connect stage before that happens.
    @Inject(method = "onTick", at = @At("HEAD"), remap = false)
    private static void mightyarchitectTest$tick(Minecraft minecraft, CallbackInfo callback) {
        ClientTestController.tick(minecraft);
    }
}
