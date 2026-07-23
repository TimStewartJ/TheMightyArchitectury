package com.timmie.mightyarchitect.foundation.utility;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * World-space text (measurement labels) is submitted here during the world render pass and drawn as
 * HUD text during the overlay pass.
 * <p>
 * In 26.1 immediate-mode {@code Font.drawInBatch} into a custom {@code MultiBufferSource} no longer
 * displays in the world render (glyph rendering moved to the deferred GUI/submit-node system), so
 * the labels are projected from their world position to screen coordinates and rendered with the GUI
 * text system, which does work.
 */
public class HudTextBuffer {

	private record Entry(Vec3 pos, String text, int color) {}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	/** Clear the buffer at the start of a world render frame. */
	public static void beginFrame() {
		ENTRIES.clear();
	}

	/** Submit a world-space label to be drawn as HUD text this frame. */
	public static void submit(Vec3 worldPos, String text, int color) {
		if (text == null || text.isEmpty())
			return;
		ENTRIES.add(new Entry(worldPos, text, color));
	}

	/** Draw all submitted labels, projecting each world position to the screen. */
	public static void render(GuiGraphicsExtractor gfx) {
		if (ENTRIES.isEmpty())
			return;

		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 cam = camera.position();

		int guiWidth = gfx.guiWidth();
		int guiHeight = gfx.guiHeight();
		int pxWidth = mc.getWindow().getWidth();
		int pxHeight = mc.getWindow().getHeight();
		if (pxWidth <= 0 || pxHeight <= 0)
			return;

		float aspect = (float) pxWidth / (float) pxHeight;
		float fovRad = (float) Math.toRadians(mc.options.fov().get());
		Matrix4f projectionView = new Matrix4f()
			.perspective(fovRad, aspect, 0.05f, 1000.0f)
			.mul(camera.getViewRotationMatrix(new Matrix4f()));

		for (Entry entry : ENTRIES) {
			Vector4f clip = projectionView.transform(new Vector4f(
				(float) (entry.pos.x - cam.x),
				(float) (entry.pos.y - cam.y),
				(float) (entry.pos.z - cam.z),
				1.0f));

			if (clip.w() <= 0.0001f)
				continue; // behind the camera

			float ndcX = clip.x() / clip.w();
			float ndcY = clip.y() / clip.w();
			if (ndcX < -1.3f || ndcX > 1.3f || ndcY < -1.3f || ndcY > 1.3f)
				continue; // well off-screen

			float screenX = (ndcX * 0.5f + 0.5f) * guiWidth;
			float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * guiHeight;

			int width = font.width(entry.text);
			int x = Math.round(screenX - width / 2.0f);
			int y = Math.round(screenY - font.lineHeight / 2.0f);

			gfx.fill(x - 2, y - 2, x + width + 2, y + font.lineHeight, 0xC0101018);
			int color = (entry.color & 0xFF000000) == 0 ? entry.color | 0xFFFFFFFF : entry.color;
			gfx.text(font, entry.text, x, y, color, false);
		}
	}
}
