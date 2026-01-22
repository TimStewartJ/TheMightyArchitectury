package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SuperByteBuffer {

	public interface IVertexLighter {
		public int getPackedLight(float x, float y, float z);
	}

	protected ByteBuffer template;
	protected int formatSize;

	// Vertex Position
	private PoseStack transforms;

	// Vertex Texture Coords
	private boolean shouldShiftUV;
	private SpriteShiftEntry spriteShift;
	private float uTarget, vTarget;

	// Vertex Lighting
	private boolean shouldLight;
	private int packedLightCoords;
	private Matrix4f lightTransform;

	// Vertex Coloring
	private boolean shouldColor;
	private int r, g, b, a;
	private float sheetSize;

	public SuperByteBuffer(BufferBuilder.RenderedBuffer renderedBuffer) {
		formatSize = renderedBuffer.drawState().format().getVertexSize();

		// Defensive copy: allocate our own buffer and copy data to avoid stale references
		// Use direct buffer for better performance with large schematics
		ByteBuffer original = renderedBuffer.vertexBuffer();
		int size = original.remaining();
		template = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
		template.put(original);
		template.flip();

		transforms = new PoseStack();
	}

	/**
	 * Returns true if this buffer contains no vertex data.
	 */
	public boolean isEmpty() {
		return template == null || template.limit() == 0;
	}

	public static float getUnInterpolatedU(TextureAtlasSprite sprite, float u) {
		float f = sprite.getU1() - sprite.getU0();
		return (u - sprite.getU0()) / f * 16.0F;
	}

	public static float getUnInterpolatedV(TextureAtlasSprite sprite, float v) {
		float f = sprite.getV1() - sprite.getV0();
		return (v - sprite.getV0()) / f * 16.0F;
	}

	public void renderInto(PoseStack input, VertexConsumer builder) {
		renderInto(input, builder, OverlayTexture.NO_OVERLAY);
	}

	public void renderInto(PoseStack input, VertexConsumer builder, int overlay) {
		if (isEmpty())
			return;
		
		ByteBuffer buffer = template;
		buffer.rewind();

		Matrix4f modelMat = input.last().pose();
		Matrix3f normalMat = input.last().normal();
		
		Matrix4f localTransforms = transforms.last().pose();
		Matrix3f localNormalMat = transforms.last().normal();
		
		// Combined transforms
		Matrix4f combinedPose = new Matrix4f(modelMat).mul(localTransforms);
		Matrix3f combinedNormal = new Matrix3f(normalMat).mul(localNormalMat);

		int vertexCount = vertexCount(buffer);
		for (int i = 0; i < vertexCount; i++) {
			float x = getX(buffer, i);
			float y = getY(buffer, i);
			float z = getZ(buffer, i);

			Vector4f pos = new Vector4f(x, y, z, 1F);
			pos.mul(combinedPose);

			// Color handling
			int r, g, b, a;
			if (shouldColor) {
				byte origR = getR(buffer, i);
				float lum = (origR < 0 ? 255 + origR : origR) / 256f;
				r = (int) (this.r * lum);
				g = (int) (this.g * lum);
				b = (int) (this.b * lum);
				a = this.a;
			} else {
				r = Byte.toUnsignedInt(getR(buffer, i));
				g = Byte.toUnsignedInt(getG(buffer, i));
				b = Byte.toUnsignedInt(getB(buffer, i));
				a = Byte.toUnsignedInt(getA(buffer, i));
			}

			// UV handling
			float u, v;
			if (shouldShiftUV) {
				float origU = getU(buffer, i);
				float origV = getV(buffer, i);
				u = spriteShift.getTarget()
					.getU((getUnInterpolatedU(spriteShift.getOriginal(), origU) / sheetSize) + uTarget * 16);
				v = spriteShift.getTarget()
					.getV((getUnInterpolatedV(spriteShift.getOriginal(), origV) / sheetSize) + vTarget * 16);
			} else {
				u = getU(buffer, i);
				v = getV(buffer, i);
			}

			// Light handling
			int light;
			if (shouldLight) {
				if (lightTransform != null) {
					Vector4f lightPos = new Vector4f(x, y, z, 1F);
					lightPos.mul(localTransforms);
					lightPos.mul(lightTransform);
					light = getLight(Minecraft.getInstance().level, lightPos);
				} else {
					light = packedLightCoords;
				}
			} else {
				light = getLight(buffer, i);
			}

			// Normal handling - convert signed bytes to floats
			float nx = getNX(buffer, i) / 127f;
			float ny = getNY(buffer, i) / 127f;
			float nz = getNZ(buffer, i) / 127f;
			Vector3f normal = new Vector3f(nx, ny, nz);
			normal.mul(combinedNormal);

			// Atomic vertex submission - compatible with Sodium
			builder.vertex(pos.x(), pos.y(), pos.z())
				.color(r, g, b, a)
				.uv(u, v)
				.overlayCoords(overlay)
				.uv2(light)
				.normal(normal.x(), normal.y(), normal.z())
				.endVertex();
		}

		transforms = new PoseStack();
		shouldShiftUV = false;
		shouldColor = false;
		shouldLight = false;
	}

	public SuperByteBuffer translate(double x, double y, double z) {
		return translate((float) x, (float) y, (float) z);
	}

	public SuperByteBuffer translate(float x, float y, float z) {
		transforms.translate(x, y, z);
		return this;
	}

	public SuperByteBuffer rotate(Direction axis, float radians) {
		if (radians == 0)
			return this;
		Quaternionf quaternionOfAxisRotation = new Quaternionf();
		quaternionOfAxisRotation.fromAxisAngleRad(axis.step(), radians);
		transforms.mulPose(quaternionOfAxisRotation);
		return this;
	}

	public SuperByteBuffer rotateCentered(Direction axis, float radians) {
		return translate(.5f, .5f, .5f).rotate(axis, radians)
			.translate(-.5f, -.5f, -.5f);
	}

	public SuperByteBuffer shiftUV(SpriteShiftEntry entry) {
		shouldShiftUV = true;
		spriteShift = entry;
		uTarget = 0;
		vTarget = 0;
		sheetSize = 1;
		return this;
	}

	public SuperByteBuffer shiftUVtoSheet(SpriteShiftEntry entry, float uTarget, float vTarget, int sheetSize) {
		shouldShiftUV = true;
		spriteShift = entry;
		this.uTarget = uTarget;
		this.vTarget = vTarget;
		this.sheetSize = sheetSize;
		return this;
	}

	public SuperByteBuffer light(int packedLightCoords) {
		shouldLight = true;
		lightTransform = null;
		this.packedLightCoords = packedLightCoords;
		return this;
	}

	public SuperByteBuffer light(Matrix4f lightTransform) {
		shouldLight = true;
		this.lightTransform = lightTransform;
		return this;
	}

	public SuperByteBuffer color(int color) {
		shouldColor = true;
		r = ((color >> 16) & 0xFF);
		g = ((color >> 8) & 0xFF);
		b = (color & 0xFF);
		a = 255;
		return this;
	}

	protected int vertexCount(ByteBuffer buffer) {
		return buffer.limit() / formatSize;
	}

	protected int getBufferPosition(int vertexIndex) {
		return vertexIndex * formatSize;
	}

	protected float getX(ByteBuffer buffer, int index) {
		return buffer.getFloat(getBufferPosition(index));
	}

	protected float getY(ByteBuffer buffer, int index) {
		return buffer.getFloat(getBufferPosition(index) + 4);
	}

	protected float getZ(ByteBuffer buffer, int index) {
		return buffer.getFloat(getBufferPosition(index) + 8);
	}

	protected byte getR(ByteBuffer buffer, int index) {
		return buffer.get(getBufferPosition(index) + 12);
	}

	protected byte getG(ByteBuffer buffer, int index) {
		return buffer.get(getBufferPosition(index) + 13);
	}

	protected byte getB(ByteBuffer buffer, int index) {
		return buffer.get(getBufferPosition(index) + 14);
	}

	protected byte getA(ByteBuffer buffer, int index) {
		return buffer.get(getBufferPosition(index) + 15);
	}

	protected float getU(ByteBuffer buffer, int index) {
		return buffer.getFloat(getBufferPosition(index) + 16);
	}

	protected float getV(ByteBuffer buffer, int index) {
		return buffer.getFloat(getBufferPosition(index) + 20);
	}

	protected int getLight(ByteBuffer buffer, int index) {
		return buffer.getInt(getBufferPosition(index) + 24);
	}

	protected byte getNX(ByteBuffer buffer, int index) {
		return buffer.get(getBufferPosition(index) + 28);
	}

	protected byte getNY(ByteBuffer buffer, int index) {
		return buffer.get(getBufferPosition(index) + 29);
	}

	protected byte getNZ(ByteBuffer buffer, int index) {
		return buffer.get(getBufferPosition(index) + 30);
	}

	private static int getLight(Level world, Vector4f lightPos) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		float sky = 0, block = 0;
		float offset = 1 / 8f;
		for (float zOffset = offset; zOffset >= -offset; zOffset -= 2 * offset)
			for (float yOffset = offset; yOffset >= -offset; yOffset -= 2 * offset)
				for (float xOffset = offset; xOffset >= -offset; xOffset -= 2 * offset) {
					pos.set(lightPos.x() + xOffset, lightPos.y() + yOffset, lightPos.z() + zOffset);
					sky += world.getBrightness(LightLayer.SKY, pos) / 8f;
					block += world.getBrightness(LightLayer.BLOCK, pos) / 8f;
				}

		return ((int) sky) << 20 | ((int) block) << 4;
	}

}
