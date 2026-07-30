package com.timmie.mightyarchitect.mixin;

import com.timmie.mightyarchitect.platform.ClientHooks;
import net.minecraft.client.DeltaTracker;
//? if >=26.2 {
/*import net.minecraft.client.gui.Hud;
*///?} else {
import net.minecraft.client.gui.Gui;
//?}
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
 * extraction, which renamed both the method and its graphics type. 26.2 then split the HUD out of
 * Gui into its own class: Gui.extractRenderState became (DeltaTracker, boolean, boolean) and the
 * graphics-taking overload this hooks now lives on Hud.
 */
//? if >=26.2 {
/*@Mixin(Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
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
