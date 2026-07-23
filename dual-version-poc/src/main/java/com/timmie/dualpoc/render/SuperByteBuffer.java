package com.timmie.dualpoc.render;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class SuperByteBuffer {

	public interface IVertexLighter {
		public int getPackedLight(float x, float y, float z);
	}

	protected int[] vertexData;
	protected int vertexCount;
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

	// Vertex format info. In 26.1, DefaultVertexFormat.BLOCK is 28 bytes (7 ints):
	// pos(3 floats) + color(1 int) + uv0(2 floats) + uv2/light(1 int). It no longer carries a normal,
	// so the normal offset must be resolved from the actual format instead of assumed at int 7.
	private static final int DEFAULT_VERTEX_SIZE = 32;
	private int intsPerVertex;
	private boolean hasNormal;
	private int normalIntOffset = -1;

	public SuperByteBuffer(int[] vertexData, int vertexCount) {
		this.vertexData = vertexData;
		this.vertexCount = vertexCount;
		this.formatSize = DEFAULT_VERTEX_SIZE;
		this.intsPerVertex = DEFAULT_VERTEX_SIZE / 4;
		this.transforms = new PoseStack();
	}

	/**
	 * Constructs a SuperByteBuffer from MeshData (1.21.1+ API)
	 */
	public SuperByteBuffer(MeshData meshData) {
		if (meshData == null) {
			this.vertexData = new int[0];
			this.vertexCount = 0;
			this.formatSize = DEFAULT_VERTEX_SIZE;
			this.intsPerVertex = DEFAULT_VERTEX_SIZE / 4;
			this.transforms = new PoseStack();
			return;
		}
		
		MeshData.DrawState drawState = meshData.drawState();
		this.vertexCount = drawState.vertexCount();
		VertexFormat format = drawState.format();
		this.formatSize = format.getVertexSize();
		this.intsPerVertex = formatSize / 4;
		this.hasNormal = format.contains(VertexFormatElement.NORMAL);
		this.normalIntOffset = hasNormal ? format.getOffset(VertexFormatElement.NORMAL) / 4 : -1;
		
		ByteBuffer vertexBuffer = meshData.vertexBuffer();
		this.vertexData = new int[vertexCount * intsPerVertex];
		
		if (vertexBuffer != null) {
			IntBuffer intBuffer = vertexBuffer.asIntBuffer();
			intBuffer.get(this.vertexData, 0, Math.min(this.vertexData.length, intBuffer.remaining()));
		}
		
		this.transforms = new PoseStack();
	}

	public static SuperByteBuffer empty() {
		return new SuperByteBuffer(new int[0], 0);
	}

	public boolean isEmpty() {
		return vertexCount == 0;
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
		if (vertexCount == 0)
			return;

		Matrix4f t = new Matrix4f(input.last().pose());
		Matrix4f localTransforms = transforms.last().pose();
		t.mul(localTransforms);

		for (int i = 0; i < vertexCount; i++) {
			float x = getX(i);
			float y = getY(i);
			float z = getZ(i);

			Vector4f pos = new Vector4f(x, y, z, 1F);
			Vector4f lightPos = new Vector4f(x, y, z, 1F);
			pos.mul(t);
			lightPos.mul(localTransforms);

			builder.addVertex(pos.x(), pos.y(), pos.z());

			int color = getColor(i);
			byte r = (byte) ((color >> 0) & 0xFF);
			byte g = (byte) ((color >> 8) & 0xFF);
			byte b = (byte) ((color >> 16) & 0xFF);
			byte a = (byte) ((color >> 24) & 0xFF);

			if (shouldColor) {
				float lum = (r < 0 ? 255 + r : r) / 256f;
				builder.setColor((int) (this.r * lum), (int) (this.g * lum), (int) (this.b * lum), this.a);
			} else
				builder.setColor(r & 0xFF, g & 0xFF, b & 0xFF, a & 0xFF);

			float u = getU(i);
			float v = getV(i);

			if (shouldShiftUV) {
				float targetU = spriteShift.getTarget()
					.getU((getUnInterpolatedU(spriteShift.getOriginal(), u) / sheetSize) + uTarget * 16);
				float targetV = spriteShift.getTarget()
					.getV((getUnInterpolatedV(spriteShift.getOriginal(), v) / sheetSize) + vTarget * 16);
				builder.setUv(targetU, targetV);
			} else
				builder.setUv(u, v);

			builder.setOverlay(OverlayTexture.NO_OVERLAY);

			if (shouldLight) {
				int light = packedLightCoords;
				if (lightTransform != null) {
					lightPos.mul(lightTransform);
					light = getLight(Minecraft.getInstance().level, lightPos);
				}
				builder.setLight(light);
			} else
				builder.setLight(getLightData(i));

			if (hasNormal)
				builder.setNormal(getNX(i), getNY(i), getNZ(i));
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

	protected int getIntOffset(int vertexIndex) {
		return vertexIndex * intsPerVertex;
	}

	protected float getX(int index) {
		return Float.intBitsToFloat(vertexData[getIntOffset(index)]);
	}

	protected float getY(int index) {
		return Float.intBitsToFloat(vertexData[getIntOffset(index) + 1]);
	}

	protected float getZ(int index) {
		return Float.intBitsToFloat(vertexData[getIntOffset(index) + 2]);
	}

	protected int getColor(int index) {
		return vertexData[getIntOffset(index) + 3];
	}

	protected float getU(int index) {
		return Float.intBitsToFloat(vertexData[getIntOffset(index) + 4]);
	}

	protected float getV(int index) {
		return Float.intBitsToFloat(vertexData[getIntOffset(index) + 5]);
	}

	protected int getLightData(int index) {
		return vertexData[getIntOffset(index) + 6];
	}

	protected float getNX(int index) {
		int packed = vertexData[getIntOffset(index) + normalIntOffset];
		return ((byte) (packed & 0xFF)) / 127f;
	}

	protected float getNY(int index) {
		int packed = vertexData[getIntOffset(index) + normalIntOffset];
		return ((byte) ((packed >> 8) & 0xFF)) / 127f;
	}

	protected float getNZ(int index) {
		int packed = vertexData[getIntOffset(index) + normalIntOffset];
		return ((byte) ((packed >> 16) & 0xFF)) / 127f;
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
