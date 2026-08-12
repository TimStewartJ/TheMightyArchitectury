package com.timmie.mightyarchitect.control.phase;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Plans the command fallback used when the connected server does not advertise the mod's payloads.
 */
public final class MultiplayerPrintCommands {

	/**
	 * The vanilla limit defaults to 32,768 but is server-configurable and not synchronized to the
	 * client. Small fills still cut command volume sharply without making one rejection lose a
	 * whole wall.
	 */
	private static final int MAX_FILL_BLOCKS = 32;
	private static final List<Axis> AXES = List.of(Axis.X, Axis.Z, Axis.Y);
	private static final Comparator<BlockPos> POSITION_ORDER = Comparator.comparingInt(
		(BlockPos pos) -> pos.getY())
		.thenComparingInt(pos -> pos.getZ())
		.thenComparingInt(pos -> pos.getX());

	private MultiplayerPrintCommands() {
	}

	/**
	 * Greedily collapses contiguous equal-state blocks into one-dimensional fill commands.
	 * Singletons remain setblock commands.
	 */
	public static List<Command> plan(Map<BlockPos, BlockState> blocks) {
		Map<BlockPos, BlockState> remaining = new HashMap<>(blocks);
		List<BlockPos> ordered = new ArrayList<>(blocks.keySet());
		ordered.sort(POSITION_ORDER);

		List<Command> commands = new ArrayList<>();
		for (BlockPos start : ordered) {
			if (!remaining.containsKey(start))
				continue;
			BlockState state = Objects.requireNonNull(remaining.get(start), "state at " + start);

			Run longest = new Run(Axis.X, 1);
			for (Axis axis : AXES) {
				int length = runLength(remaining, start, state, axis);
				if (length > longest.length())
					longest = new Run(axis, length);
			}

			BlockPos end = start;
			for (int i = 0; i < longest.length(); i++) {
				remaining.remove(end);
				if (i + 1 < longest.length())
					end = advance(end, longest.axis());
			}
			commands.add(new Command(start, end, state));
		}
		return List.copyOf(commands);
	}

	/** Replans one queued run after dropping positions that are no longer safe to place. */
	public static List<Command> replan(Command command, Predicate<BlockPos> include) {
		Map<BlockPos, BlockState> blocks = new HashMap<>();
		for (BlockPos pos : command.positions())
			if (include.test(pos))
				blocks.put(pos, command.state());
		return plan(blocks);
	}

	private static int runLength(Map<BlockPos, BlockState> blocks, BlockPos start, BlockState state,
		Axis axis) {
		int length = 1;
		BlockPos next = advance(start, axis);
		while (length < MAX_FILL_BLOCKS && state.equals(blocks.get(next))) {
			length++;
			next = advance(next, axis);
		}
		return length;
	}

	private static BlockPos advance(BlockPos pos, Axis axis) {
		return switch (axis) {
			case X -> pos.offset(1, 0, 0);
			case Y -> pos.offset(0, 1, 0);
			case Z -> pos.offset(0, 0, 1);
		};
	}

	private record Run(Axis axis, int length) {
	}

	public record Command(BlockPos from, BlockPos to, BlockState state) {

		public Command {
			Objects.requireNonNull(from, "from");
			Objects.requireNonNull(to, "to");
			Objects.requireNonNull(state, "state");
			int varyingAxes = (from.getX() == to.getX() ? 0 : 1)
				+ (from.getY() == to.getY() ? 0 : 1)
				+ (from.getZ() == to.getZ() ? 0 : 1);
			if (varyingAxes > 1)
				throw new IllegalArgumentException("fill run must be one-dimensional");
		}

		public String text() {
			String serialized = BlockStateParser.serialize(state);
			if (from.equals(to))
				return "setblock " + coordinates(from) + " " + serialized;
			return "fill " + coordinates(from) + " " + coordinates(to) + " " + serialized;
		}

		public int blockCount() {
			return Math.abs(to.getX() - from.getX())
				+ Math.abs(to.getY() - from.getY())
				+ Math.abs(to.getZ() - from.getZ()) + 1;
		}

		public List<BlockPos> positions() {
			int dx = Integer.compare(to.getX(), from.getX());
			int dy = Integer.compare(to.getY(), from.getY());
			int dz = Integer.compare(to.getZ(), from.getZ());
			List<BlockPos> positions = new ArrayList<>(blockCount());
			BlockPos pos = from;
			for (int i = 0; i < blockCount(); i++) {
				positions.add(pos);
				pos = pos.offset(dx, dy, dz);
			}
			return List.copyOf(positions);
		}

		private static String coordinates(BlockPos pos) {
			return pos.getX() + " " + pos.getY() + " " + pos.getZ();
		}
	}
}
