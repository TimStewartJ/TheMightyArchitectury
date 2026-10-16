package com.timmie.mightyarchitect.test.unit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.timmie.mightyarchitect.foundation.SuperByteBuffer;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Bootstrapped
@DisplayName("Schematic vertex normals")
class SuperByteBufferNormalsTest {

	private static final float EPSILON = 0.00001f;
	private static final float[][] UP = { { 0, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 }, { 1, 0, 0 } };
	private static final float[][] EAST = { { 0, 0, 0 }, { 0, 1, 0 }, { 0, 1, 1 }, { 0, 0, 1 } };

	@Test
	@DisplayName("every emitted vertex has a normal, even when the cached format omits it")
	void everyVertexHasANormal() {
		List<Vector3f> normals = render(new PoseStack(), UP);

		assertEquals(4, normals.size(), "a shader-extended buffer requires one normal per vertex");
		normals.forEach(normal -> assertNormal(normal, 0, 1, 0));
	}

	@Test
	@DisplayName("adjacent quads retain their own face normal")
	void normalsAreRecomputedForEachQuad() {
		List<Vector3f> normals = render(new PoseStack(), UP, EAST);

		assertEquals(8, normals.size());
		normals.subList(0, 4).forEach(normal -> assertNormal(normal, 0, 1, 0));
		normals.subList(4, 8).forEach(normal -> assertNormal(normal, 1, 0, 0));
	}

	@Test
	@DisplayName("26.x derives and normalizes sloped face normals after nonuniform scaling")
	void derivedNormalsFollowTheNormalTransform() {
		assumeTrue(derivesMissingNormals());
		PoseStack pose = new PoseStack();
		pose.scale(2, 3, 4);
		float[][] slope = { { 0, 0, 0 }, { 0, 1, 1 }, { 1, 1, 1 }, { 1, 0, 0 } };

		List<Vector3f> normals = render(pose, slope);

		assertEquals(4, normals.size());
		normals.forEach(normal -> assertNormal(normal, 0, 0.8f, -0.6f));
	}

	@Test
	@DisplayName("26.x emits finite zero normals for zero-area quads")
	void degenerateQuadsDoNotProduceNaNs() {
		assumeTrue(derivesMissingNormals());
		float[][] line = { { 0, 0, 0 }, { 1, 0, 0 }, { 2, 0, 0 }, { 3, 0, 0 } };

		List<Vector3f> normals = render(new PoseStack(), line);

		assertEquals(4, normals.size());
		normals.forEach(normal -> assertNormal(normal, 0, 0, 0));
	}

	private static boolean derivesMissingNormals() {
		return Arrays.stream(SuperByteBuffer.class.getDeclaredFields())
			.anyMatch(field -> field.getName().equals("hasNormal"));
	}

	private static List<Vector3f> render(PoseStack pose, float[][]... quads) {
		int[] data = new int[quads.length * 4 * 8];
		int offset = 0;
		for (float[][] quad : quads) {
			Vector3f normal = new Vector3f(
				(quad[1][1] - quad[0][1]) * (quad[2][2] - quad[0][2])
					- (quad[1][2] - quad[0][2]) * (quad[2][1] - quad[0][1]),
				(quad[1][2] - quad[0][2]) * (quad[2][0] - quad[0][0])
					- (quad[1][0] - quad[0][0]) * (quad[2][2] - quad[0][2]),
				(quad[1][0] - quad[0][0]) * (quad[2][1] - quad[0][1])
					- (quad[1][1] - quad[0][1]) * (quad[2][0] - quad[0][0]));
			if (normal.lengthSquared() > 0)
				normal.normalize();
			int packedNormal = ((int) (normal.x * 127) & 0xff)
				| (((int) (normal.y * 127) & 0xff) << 8)
				| (((int) (normal.z * 127) & 0xff) << 16);
			for (float[] vertex : quad) {
				data[offset] = Float.floatToRawIntBits(vertex[0]);
				data[offset + 1] = Float.floatToRawIntBits(vertex[1]);
				data[offset + 2] = Float.floatToRawIntBits(vertex[2]);
				data[offset + 3] = -1;
				data[offset + 6] = 0x00f000f0;
				data[offset + 7] = packedNormal;
				offset += 8;
			}
		}

		List<Vector3f> normals = new ArrayList<>();
		VertexConsumer consumer = (VertexConsumer) Proxy.newProxyInstance(
			VertexConsumer.class.getClassLoader(), new Class<?>[] { VertexConsumer.class },
			(proxy, method, args) -> {
				if (method.getName().equals("setNormal") || method.getName().equals("normal"))
					normals.add(new Vector3f(((Number) args[0]).floatValue(),
						((Number) args[1]).floatValue(), ((Number) args[2]).floatValue()));
				return method.getReturnType() == void.class ? null : proxy;
			});
		new SuperByteBuffer(data, quads.length * 4).renderInto(pose, consumer);
		return normals;
	}

	private static void assertNormal(Vector3f normal, float x, float y, float z) {
		assertEquals(x, normal.x, EPSILON);
		assertEquals(y, normal.y, EPSILON);
		assertEquals(z, normal.z, EPSILON);
	}
}
