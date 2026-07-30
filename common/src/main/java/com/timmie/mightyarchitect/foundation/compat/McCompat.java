package com.timmie.mightyarchitect.foundation.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;

/**
 * Shims for client API that moved between versions, chiefly in 26.2. Kept in one place so call
 * sites stay version-agnostic: many of them sit inside guarded arms already, and Stonecutter
 * cannot nest a guard inside a commented arm.
 */
public final class McCompat {

	private McCompat() {
	}

	public static Camera mainCamera(Minecraft mc) {
		//? if >=26.2 {
		/*return mc.gameRenderer.mainCamera();
		*///?} else {
		return mc.gameRenderer.getMainCamera();
		//?}
	}

	public static Vec3 cameraPos(Minecraft mc) {
		//? if >=26.2 {
		/*return mc.gameRenderer.mainCamera().position();
		*///?} else if >=1.21.11 {
		return mc.gameRenderer.getMainCamera().position();
		//?} else {
		/*return mc.gameRenderer.getMainCamera().getPosition();
		*///?}
	}

	/** The screen currently open, or null. 26.2 moved screen ownership from Minecraft to Gui. */
	public static Screen currentScreen(Minecraft mc) {
		//? if >=26.2 {
		/*return mc.gui.screen();
		*///?} else {
		return mc.screen;
		//?}
	}

	public static void setScreen(Minecraft mc, Screen screen) {
		//? if >=26.2 {
		/*// Minecraft.setScreenAndShow additionally forces a frame; Gui.setScreen is the plain setter.
		mc.gui.setScreen(screen);
		*///?} else {
		mc.setScreen(screen);
		//?}
	}

	public static boolean hasOverlay(Minecraft mc) {
		//? if >=26.2 {
		/*return mc.gui.overlay() != null;
		*///?} else {
		return mc.getOverlay() != null;
		//?}
	}

	public static void clearChat(Minecraft mc) {
		//? if >=26.2 {
		/*mc.gui.hud.getChat().clearMessages(true);
		*///?} else {
		mc.gui.getChat().clearMessages(true);
		//?}
	}

	/** 26.2 moved the main render target from Minecraft to GameRenderer. */	public static RenderTarget mainRenderTarget(Minecraft mc) {
		//? if >=26.2 {
		/*return mc.gameRenderer.mainRenderTarget();
		*///?} else {
		return mc.getMainRenderTarget();
		//?}
	}

	/** 26.2 replaced VertexFormat.Mode with com.mojang.blaze3d.PrimitiveTopology. */
	public static BufferBuilder quadBuffer(ByteBufferBuilder byteBuffer, VertexFormat format) {
		//? if >=26.2 {
		/*return new BufferBuilder(byteBuffer, com.mojang.blaze3d.PrimitiveTopology.QUADS, format);
		*///?} else {
		return new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, format);
		//?}
	}
}
