package com.timmie.mightyarchitect.mixin;

import com.timmie.mightyarchitect.platform.ClientHooks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD hook, drawn after the vanilla overlay. 26.1 replaced the immediate GUI pass with render-state
 * extraction, which renamed both the method and its graphics type.
 */
@Mixin(Gui.class)
public class GuiMixin {

	//? if >=26 {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void mightyarchitect$renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
		CallbackInfo ci) {
	//?} else {
	/*@Inject(method = "render", at = @At("TAIL"))
	private void mightyarchitect$renderHud(GuiGraphics graphics, DeltaTracker deltaTracker,
		CallbackInfo ci) {
	*///?}
		ClientHooks.renderHud(graphics, deltaTracker);
	}
}
