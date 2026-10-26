package com.timmie.mightyarchitect.control.compose.planner;

import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.control.compose.Cuboid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

/**
 * Vertical lines from the corners of a shape being drawn down to the ground under them - or up out
 * of it, where the ground rises through the shape's floor.
 * <p>
 * A flat outline seen from above gives no clue to its height: it reads the same sitting on the
 * ground, floating ten blocks up, or half buried in a hill. Posts down to the terrain make that
 * obvious from any camera, which matters once the tools can be aimed from far away.
 */
public final class GroundPosts {

	/** How far to look for ground before giving up and drawing a post this long. */
	public static final int SEARCH_LIMIT = 48;

	private static final int HANGING_COLOR = 0x222222;
	private static final int BURIED_COLOR = 0xcc3333;
	private static final Object[] KEYS = { new Object(), new Object(), new Object(), new Object() };

	private GroundPosts() {
	}

	/** Draws a post at each corner of a shape whose floor is {@code selection.y}. */
	static void draw(BlockGetter level, Cuboid selection) {
		int[][] corners = { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } };
		for (int i = 0; i < corners.length; i++) {
			int edgeX = selection.x + corners[i][0] * selection.width;
			int edgeZ = selection.z + corners[i][1] * selection.length;
			// The column of the corner block itself, just inside the edge the post stands on.
			int columnX = corners[i][0] == 0 ? edgeX : edgeX - 1;
			int columnZ = corners[i][1] == 0 ? edgeZ : edgeZ - 1;
			int ground = groundLevel(level, columnX, selection.y, columnZ);
			if (ground == selection.y)
				continue;
			MightyClient.outliner.chaseLine(KEYS[i], new Vec3(edgeX, selection.y, edgeZ), new Vec3(edgeX, ground, edgeZ))
				.lineWidth(1 / 16f)
				.colored(ground > selection.y ? BURIED_COLOR : HANGING_COLOR);
		}
	}

	/**
	 * The height of the ground surface in one column, relative to a floor at {@code floorY}: the top
	 * of the first solid block below an open floor, or the first open space above a buried one.
	 * Equal to {@code floorY} when the floor sits exactly on the ground; at most
	 * {@link #SEARCH_LIMIT} away when no surface is found.
	 */
	public static int groundLevel(BlockGetter level, int x, int floorY, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, floorY, z);
		if (isSolid(level, pos)) {
			for (int y = floorY + 1; y <= floorY + SEARCH_LIMIT; y++)
				if (!isSolid(level, pos.setY(y)))
					return y;
			return floorY + SEARCH_LIMIT;
		}
		for (int y = floorY - 1; y >= floorY - SEARCH_LIMIT; y--)
			if (isSolid(level, pos.setY(y)))
				return y + 1;
		return floorY - SEARCH_LIMIT;
	}

	private static boolean isSolid(BlockGetter level, BlockPos pos) {
		return !level.getBlockState(pos)
			.getCollisionShape(level, pos)
			.isEmpty();
	}
}
