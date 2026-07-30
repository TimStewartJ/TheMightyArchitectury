package com.timmie.mightyarchitect.foundation.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
//? if >=1.21 {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
//?} else {
/*
*///?}
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

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

	/**
	 * The camera's view rotation, ready to be multiplied onto a projection matrix.
	 * <p>
	 * 26 exposes this directly. 1.21 has {@code Camera.rotation()}, which maps camera space into
	 * world space, so the view rotation is its inverse. Before 1.21 that quaternion does not carry
	 * the 180 degree flip that turns Minecraft's world-space heading into the -Z-forward eye space,
	 * so the rotation is rebuilt the way {@code GameRenderer.renderLevel} does it: pitch about X,
	 * then yaw + 180 about Y.
	 */
	public static Matrix4f viewRotation(Camera camera) {
		//? if >=26 {
		return camera.getViewRotationMatrix(new Matrix4f());
		//?} else if >=1.21 {
		/*return new Matrix4f().rotation(camera.rotation())
			.invert();
		*///?} else {
		/*return new Matrix4f().rotateX(camera.getXRot() * ((float) Math.PI / 180f))
			.rotateY((camera.getYRot() + 180.0f) * ((float) Math.PI / 180f));
		*///?}
	}

	/** Draws unshadowed GUI text. 26 renamed {@code drawString} to {@code text}. */
	//? if >=26 {
	public static void drawText(GuiGraphicsExtractor gfx, Font font, String text, int x, int y, int color) {
		gfx.text(font, text, x, y, color, false);
	}
	//?} else {
	/*public static void drawText(GuiGraphics gfx, Font font, String text, int x, int y, int color) {
		gfx.drawString(font, text, x, y, color, false);
	}
	*///?}

	// 26.2 replaced VertexFormat.Mode with com.mojang.blaze3d.PrimitiveTopology, and
	// ByteBufferBuilder does not exist at all before 1.21 - those nodes build their BufferBuilder
	// directly and never call this. The arms are flat because a guard cannot nest inside a
	// commented one.
	//? if >=26.2 {
	/*public static BufferBuilder quadBuffer(ByteBufferBuilder byteBuffer, VertexFormat format) {
		return new BufferBuilder(byteBuffer, com.mojang.blaze3d.PrimitiveTopology.QUADS, format);
	}
	*///?} else if >=1.21 {
	public static BufferBuilder quadBuffer(ByteBufferBuilder byteBuffer, VertexFormat format) {
		return new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, format);
	}
	//?} else {
	/*
	*///?}
}
