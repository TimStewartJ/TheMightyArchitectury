package com.timmie.mightyarchitect.foundation;

//? if >=1.21 {
import com.mojang.blaze3d.vertex.MeshData;
//?} else {
/*import com.mojang.blaze3d.vertex.BufferBuilder;
*///?}
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
//? if >=26 {
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
//?} else {
/*
*///?}
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
	//? if >=26 {
	private boolean hasNormal;
	private int normalIntOffset = -1;
	//?} else {
	/*
	*///?}

	public SuperByteBuffer(int[] vertexData, int vertexCount) {
		this.vertexData = vertexData;
		this.vertexCount = vertexCount;
		this.formatSize = DEFAULT_VERTEX_SIZE;
		this.intsPerVertex = DEFAULT_VERTEX_SIZE / 4;
		this.transforms = new PoseStack();
	}

	//*
	 //* Constructs a SuperByteBuffer from the renderer's finished mesh. 1.21+ hands out MeshData;
	 //* the older versions hand out BufferBuilder.RenderedBuffer. Both expose the same draw state
	 //* (vertex count + format) and a raw little-endian vertex ByteBuffer, so only the parameter
	 //* type and the draw-state accessor differ.
	 //
	//? if >=1.21 {
	public SuperByteBuffer(MeshData meshData) {
		if (meshData == null) {
	//?} else {
	/*public SuperByteBuffer(BufferBuilder.RenderedBuffer meshData) {
		if (meshData == null) {
	*///?}
			this.vertexData = new int[0];
			this.vertexCount = 0;
			this.formatSize = DEFAULT_VERTEX_SIZE;
			this.intsPerVertex = DEFAULT_VERTEX_SIZE / 4;
			this.transforms = new PoseStack();
			return;
		}

		//? if >=1.21 {
		MeshData.DrawState drawState = meshData.drawState();
		//?} else {
		/*BufferBuilder.DrawState drawState = meshData.drawState();
		*///?}
		this.vertexCount = drawState.vertexCount();
		//? if >=26 {
		VertexFormat format = drawState.format();
		this.formatSize = format.getVertexSize();
		//?} else {
		/*this.formatSize = drawState.format().getVertexSize();
		*///?}
		this.intsPerVertex = formatSize / 4;
		//? if >=26.2 {
		/*// 26.2 addresses format elements by semantic name; VertexFormatElement is a plain record now.
		this.hasNormal = format.contains(com.mojang.blaze3d.vertex.DefaultVertexFormat.NORMAL_SEMANTIC_NAME);
		this.normalIntOffset = hasNormal
			? format.getElement(com.mojang.blaze3d.vertex.DefaultVertexFormat.NORMAL_SEMANTIC_NAME).offset() / 4
			: -1;
		*///?} else if >=26 {
		this.hasNormal = format.contains(VertexFormatElement.NORMAL);
		this.normalIntOffset = hasNormal ? format.getOffset(VertexFormatElement.NORMAL) / 4 : -1;
		//?} else {
		/*
		*///?}

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

			int color = getColor(i);
			byte r = (byte) ((color >> 0) & 0xFF);
			byte g = (byte) ((color >> 8) & 0xFF);
			byte b = (byte) ((color >> 16) & 0xFF);
			byte a = (byte) ((color >> 24) & 0xFF);

			int outR, outG, outB, outA;
			if (shouldColor) {
				float lum = (r < 0 ? 255 + r : r) / 256f;
				outR = (int) (this.r * lum);
				outG = (int) (this.g * lum);
				outB = (int) (this.b * lum);
				outA = this.a;
			} else {
				outR = r & 0xFF;
				outG = g & 0xFF;
				outB = b & 0xFF;
				outA = a & 0xFF;
			}

			float u = getU(i);
			float v = getV(i);

			if (shouldShiftUV) {
				u = spriteShift.getTarget()
					.getU((getUnInterpolatedU(spriteShift.getOriginal(), getU(i)) / sheetSize) + uTarget * 16);
				v = spriteShift.getTarget()
					.getV((getUnInterpolatedV(spriteShift.getOriginal(), getV(i)) / sheetSize) + vTarget * 16);
			}

			int light;
			if (shouldLight) {
				light = packedLightCoords;
				if (lightTransform != null) {
					lightPos.mul(lightTransform);
					light = getLight(Minecraft.getInstance().level, lightPos);
				}
			} else
				light = getLightData(i);

			// 1.21 replaced the chained-and-terminated VertexConsumer builder (vertex(...)...endVertex())
			// with stateful setters that implicitly finish the previous vertex.
			//? if >=1.21 {
			builder.addVertex(pos.x(), pos.y(), pos.z());
			builder.setColor(outR, outG, outB, outA);
			builder.setUv(u, v);
			builder.setOverlay(OverlayTexture.NO_OVERLAY);
			builder.setLight(light);
			//?} else {
			/*builder.vertex(pos.x(), pos.y(), pos.z())
				.color(outR, outG, outB, outA)
				.uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(light)
				.normal(getNX(i), getNY(i), getNZ(i))
				.endVertex();
			*///?}

			//? if >=26 {
			if (hasNormal)
				builder.setNormal(getNX(i), getNY(i), getNZ(i));
			//?} else if >=1.21 {
			/*builder.setNormal(getNX(i), getNY(i), getNZ(i));
			*///?} else {
			/*
			*///?}
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
		//? if >=26 {
		int packed = vertexData[getIntOffset(index) + normalIntOffset];
		//?} else {
		/*int packed = vertexData[getIntOffset(index) + 7];
		*///?}
		return ((byte) (packed & 0xFF)) / 127f;
	}

	protected float getNY(int index) {
		//? if >=26 {
		int packed = vertexData[getIntOffset(index) + normalIntOffset];
		//?} else {
		/*int packed = vertexData[getIntOffset(index) + 7];
		*///?}
		return ((byte) ((packed >> 8) & 0xFF)) / 127f;
	}

	protected float getNZ(int index) {
		//? if >=26 {
		int packed = vertexData[getIntOffset(index) + normalIntOffset];
		//?} else {
		/*int packed = vertexData[getIntOffset(index) + 7];
		*///?}
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
