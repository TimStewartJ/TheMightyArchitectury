package com.timmie.mightyarchitect.platform;

import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.foundation.utility.ShaderManager;
import com.timmie.mightyarchitect.gui.ScreenHelper;
// DeltaTracker replaced the raw partial-tick float in 1.21, and GuiGraphics itself only exists
// from 1.20 - before that the HUD is drawn straight onto a PoseStack, which the mod's own
// GuiGraphics stand-in wraps.
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?} else {
/*
*///?}
import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;
*///?}

/**
 * Single entry point from the client hooks into the mod.
 * <p>
 * The hooks themselves are Mixins on vanilla classes (see {@code com.timmie.mightyarchitect.mixin}),
 * which both loaders support, so there is exactly one implementation of the tick, HUD and raw input
 * plumbing instead of one per loader per Minecraft version.
 */
public final class ClientHooks {

	private ClientHooks() {
	}

	public static void clientTickPre(Minecraft minecraft) {
		ShaderManager.onClientTick(minecraft);
	}

	public static void clientTickPost(Minecraft minecraft) {
		MightyClient.onTick(minecraft);
		ScreenHelper.onClientTick(minecraft);
		ArchitectManager.onClientTick(minecraft);
	}

	//? if >=26 {
	public static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		ArchitectManager.onDrawGameOverlay(graphics, deltaTracker);
	}
	//?} else if >=1.21 {
	/*public static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
		ArchitectManager.onDrawGameOverlay(graphics, deltaTracker);
	}
	*///?} else {
	/*public static void renderHud(GuiGraphics graphics, float partialTicks) {
		ArchitectManager.onDrawGameOverlay(graphics, partialTicks);
	}
	*///?}

	/**
	 * @return true when the mod consumed the scroll, so vanilla must not also act on it.
	 */
	public static boolean mouseScrolled(double horizontal, double vertical) {
		return ArchitectManager.onMouseScrolled(horizontal, vertical);
	}

	public static void mouseClicked(int button, int action) {
		ArchitectManager.onClick(button, action);
	}

	public static void keyPressed(int keyCode, int action) {
		ArchitectManager.onKeyTyped(keyCode, action);
	}
}
