package com.timmie.mightyarchitect.test.unit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.timmie.mightyarchitect.foundation.SuperByteBuffer;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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

	@Test
	@DisplayName("26.2+ draws the once-baked preview exactly as rendering it every frame did")
	void bakedPreviewMatchesDirectRendering() throws Exception {
		Class<?> baked;
		try {
			baked = Class.forName("com.timmie.mightyarchitect.foundation.SuperRenderTypeBuffer$StaticGeometry");
		} catch (ClassNotFoundException absent) {
			baked = null;
		}
		assumeTrue(baked != null, "only 26.2+ bakes the preview");
		PoseStack pose = new PoseStack();
		pose.translate(-3.5f, 70.25f, 12.75f);
		pose.last().pose().rotateY(0.7f);
		pose.last().normal().rotateY(0.7f);
		int[] data = vertexData(0x80402010, UP, EAST);

		List<double[]> direct = new ArrayList<>();
		new SuperByteBuffer(data, 8).renderInto(pose, capture(direct));

		Object geometry = baked.getDeclaredConstructor().newInstance();
		new SuperByteBuffer(data, 8).renderInto(new PoseStack(), (VertexConsumer) geometry);
		Method emit = baked.getDeclaredMethod("emit", PoseStack.Pose.class, VertexConsumer.class);
		emit.setAccessible(true);
		List<double[]> replayed = new ArrayList<>();
		emit.invoke(geometry, pose.last(), capture(replayed));

		assertEquals(8, direct.size());
		assertEquals(direct.size(), replayed.size());
		for (int v = 0; v < direct.size(); v++)
			for (int field = 0; field < VERTEX_FIELDS; field++)
				assertEquals(direct.get(v)[field], replayed.get(v)[field], 0.0001f,
					"vertex " + v + " field " + field);
	}

	// x, y, z, color (ARGB), u, v, overlay, light, normal x, y, z
	private static final int VERTEX_FIELDS = 11;

	private static VertexConsumer capture(List<double[]> vertices) {
		return (VertexConsumer) Proxy.newProxyInstance(
			VertexConsumer.class.getClassLoader(), new Class<?>[] { VertexConsumer.class },
			(proxy, method, args) -> {
				double[] current = vertices.isEmpty() ? null : vertices.get(vertices.size() - 1);
				switch (method.getName()) {
					case "addVertex" -> {
						double[] vertex = new double[VERTEX_FIELDS];
						for (int i = 0; i < args.length; i++)
							vertex[i] = args[i] instanceof Integer integer ? integer : ((Number) args[i]).floatValue();
						vertices.add(vertex);
					}
					case "setColor" -> current[3] = args.length == 1 ? ((Number) args[0]).intValue()
						: ((((Number) args[3]).intValue() & 0xFF) << 24 | (((Number) args[0]).intValue() & 0xFF) << 16
							| (((Number) args[1]).intValue() & 0xFF) << 8 | ((Number) args[2]).intValue() & 0xFF);
					case "setUv" -> {
						current[4] = ((Number) args[0]).floatValue();
						current[5] = ((Number) args[1]).floatValue();
					}
					case "setOverlay" -> current[6] = ((Number) args[0]).intValue();
					case "setLight" -> current[7] = ((Number) args[0]).intValue();
					case "setNormal" -> {
						current[8] = ((Number) args[0]).floatValue();
						current[9] = ((Number) args[1]).floatValue();
						current[10] = ((Number) args[2]).floatValue();
					}
					default -> {
					}
				}
				return method.getReturnType() == void.class ? null : proxy;
			});
	}

	private static List<Vector3f> render(PoseStack pose, float[][]... quads) {
		int[] data = vertexData(-1, quads);

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

	private static int[] vertexData(int color, float[][]... quads) {
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
				data[offset + 3] = color;
				data[offset + 6] = 0x00f000f0;
				data[offset + 7] = packedNormal;
				offset += 8;
			}
		}
		return data;
	}

	private static void assertNormal(Vector3f normal, float x, float y, float z) {
		assertEquals(x, normal.x, EPSILON);
		assertEquals(y, normal.y, EPSILON);
		assertEquals(z, normal.z, EPSILON);
	}
}
