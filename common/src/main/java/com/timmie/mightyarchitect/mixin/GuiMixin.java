package com.timmie.mightyarchitect.mixin;

import com.timmie.mightyarchitect.platform.ClientHooks;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?} else {
/*
*///?}
//? if >=26.2 {
/*import net.minecraft.client.gui.Hud;
*///?} else {
import net.minecraft.client.gui.Gui;
//?}
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD hook, drawn after the vanilla overlay.
 * <p>
 * The target moves four times across the supported range: 1.20 replaced the raw PoseStack with
 * GuiGraphics, 1.21 replaced the partial-tick float with DeltaTracker, 26.1 replaced the immediate
 * GUI pass with render-state extraction (renaming both the method and its graphics type), and 26.2
 * split the HUD out of Gui into its own class - Gui.extractRenderState became
 * (DeltaTracker, boolean, boolean) and the graphics-taking overload this hooks now lives on Hud.
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
		ClientHooks.renderHud(graphics, deltaTracker);
	}
	//?} else if >=1.21 {
	/*@Inject(method = "render", at = @At("TAIL"))
	private void mightyarchitect$renderHud(GuiGraphics graphics, DeltaTracker deltaTracker,
		CallbackInfo ci) {
		ClientHooks.renderHud(graphics, deltaTracker);
	}
	*///?} else if >=1.20 {
	/*@Inject(method = "render", at = @At("TAIL"))
	private void mightyarchitect$renderHud(GuiGraphics graphics, float partialTicks,
		CallbackInfo ci) {
		ClientHooks.renderHud(graphics, partialTicks);
	}
	*///?} else {
	/*@Inject(method = "render", at = @At("TAIL"))
	private void mightyarchitect$renderHud(PoseStack poseStack, float partialTicks,
		CallbackInfo ci) {
		ClientHooks.renderHud(new com.timmie.mightyarchitect.foundation.gui.GuiGraphics(poseStack), partialTicks);
	}
	*///?}
}
