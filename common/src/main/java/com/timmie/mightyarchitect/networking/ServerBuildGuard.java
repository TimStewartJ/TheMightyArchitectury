package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The single authorization gate every serverbound packet in this mod passes through before it is
 * allowed to change anything.
 * <p>
 * The packets register as <em>global</em> serverbound receivers on every loader, so any client that
 * has the mod can send them to any server that also has it. Without a gate that means arbitrary
 * block placement at arbitrary coordinates by anyone. The rules here deliberately mirror what the
 * same player could already do by hand or with vanilla commands, so an authorised build behaves
 * exactly as before and an unauthorised one is simply dropped:
 * <ul>
 * <li><b>Who</b> - creative mode, or permission level 2, which is what vanilla requires for
 * {@code /setblock}. The client already applies the same test before offering multiplayer
 * printing.</li>
 * <li><b>Where</b> - inside the world, in a loaded chunk, within reach, and somewhere the player is
 * allowed to interact with. Delegating the last one to {@link Level#mayInteract} is what makes the
 * packets respect the world border, spawn protection and claim mods.</li>
 * <li><b>How much</b> - a per-player token bucket, so a client cannot pin the server thread with an
 * unbounded stream of placements.</li>
 * </ul>
 * Everything here runs on the server thread, on the server side, for a real connected player.
 */
public final class ServerBuildGuard {

	/** What vanilla asks for before it will run {@code /setblock}. */
	private static final int COMMAND_PERMISSION_LEVEL = 2;

	/**
	 * Furthest a packet may place a block from the player who sent it. A composer build is put down
	 * around the player and has to be in view to be positioned at all, so this only has to be
	 * generous enough to cover a large building plus the distance the player may have stepped back
	 * to look at it.
	 */
	public static final int MAX_BUILD_DISTANCE = 256;

	/**
	 * Blocks a single player may place through mod packets in one burst. Printing has always sent a
	 * whole schematic in one go, so this has to clear any build the composer can realistically
	 * produce - a 64x64x64 solid cube - while still being a bound.
	 */
	public static final int BLOCK_BUDGET_BURST = 262_144;

	/** Sustained refill once the burst is spent, in blocks per second. */
	public static final int BLOCK_BUDGET_PER_SECOND = 32_768;

	/** A bucket that has been full and untouched for this long carries no state worth keeping. */
	private static final long BUDGET_IDLE_TIMEOUT_NANOS = 60L * 1_000_000_000L;

	private static final Map<UUID, Budget> budgets = new HashMap<>();

	private ServerBuildGuard() {
	}

	/**
	 * Whether this player is allowed to change the world through the mod at all. Creative mode or
	 * permission level 2, the same bar vanilla sets for {@code /setblock}.
	 */
	public static boolean mayBuild(ServerPlayer player) {
		if (player == null)
			return false;
		if (player.isCreative())
			return true;
		// Numeric permission levels were replaced by named permissions in 1.21.11.
		//? if >=1.21.11 {
		return player.permissions()
			.hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
		//?} else {
		/*return player.hasPermissions(COMMAND_PERMISSION_LEVEL);
		*///?}
	}

	/**
	 * Whether this player is allowed to change this particular block. Adds the positional checks to
	 * {@link #mayBuild}: a real position inside a loaded chunk, within reach, and not somewhere the
	 * player is barred from interacting with.
	 */
	public static boolean mayBuildAt(ServerPlayer player, BlockPos pos) {
		if (!mayBuild(player) || pos == null)
			return false;

		Level level = levelOf(player);
		if (!level.isInWorldBounds(pos) || !level.isLoaded(pos))
			return false;
		if (!withinReach(player.getX(), player.getY(), player.getZ(), pos))
			return false;

		// mayInteract widened its first parameter from Player to Entity in 1.21.6; ServerPlayer
		// satisfies both, so the call itself needs no guard.
		return level.mayInteract(player, pos);
	}

	/**
	 * Distance half of {@link #mayBuildAt}, split out so it can be exercised without a live player.
	 * Measured to the centre of the block.
	 */
	public static boolean withinReach(double x, double y, double z, BlockPos pos) {
		double dx = pos.getX() + 0.5 - x;
		double dy = pos.getY() + 0.5 - y;
		double dz = pos.getZ() + 0.5 - z;
		return dx * dx + dy * dy + dz * dz <= (double) MAX_BUILD_DISTANCE * MAX_BUILD_DISTANCE;
	}

	/**
	 * Takes {@code blocks} out of the player's placement budget, or refuses if there is not enough
	 * left. A refusal is reported to the player, because the alternative is silently dropping part
	 * of a build.
	 */
	public static synchronized boolean claimBlockBudget(ServerPlayer player, int blocks) {
		if (player == null)
			return false;

		long now = System.nanoTime();
		pruneIdleBudgets(now);

		Budget budget = budgets.computeIfAbsent(player.getUUID(), id -> new Budget(now));
		if (budget.claim(blocks, now))
			return true;

		// Only on the way into the throttle: while it holds, every further packet would otherwise
		// repeat this, which is itself a spam vector.
		if (budget.reportThrottle()) {
			TheMightyArchitect.logger.warn("Throttled {} blocks from {}: over the {} blocks/s build budget",
				blocks, player.getName()
					.getString(),
				BLOCK_BUDGET_PER_SECOND);
			player.sendSystemMessage(Component.literal(ChatFormatting.RED
				+ "The Mighty Architect is placing blocks faster than the server allows; part of this build was skipped."));
		}
		return false;
	}

	/**
	 * Whether this player may have items put into their hotbar by the mod. Deliberately stricter
	 * than {@link #mayBuild}: handing out items is not building, and an operator in survival should
	 * not get free items out of it.
	 */
	public static boolean mayReceiveHotbarKit(ServerPlayer player) {
		return player != null && player.isCreative();
	}

	/** Records that a packet was refused, without giving a client a way to spam the server log. */
	public static void reportDenied(ServerPlayer player, String action) {
		if (TheMightyArchitect.logger.isDebugEnabled())
			TheMightyArchitect.logger.debug("Refused a request to {} for {}: not authorised", action,
				player == null ? "an unknown player"
					: player.getName()
						.getString());
	}

	/** Entity.level() replaced the public level field in 1.20. */
	public static Level levelOf(ServerPlayer player) {
		//? if >=1.20 {
		return player.level();
		//?} else {
		/*return player.level;
		*///?}
	}

	/**
	 * Drops buckets that are full and have not been touched recently. Recreating one gives a full
	 * bucket, so this cannot change the outcome of a later claim - it only stops disconnected
	 * players accumulating in the map.
	 */
	private static void pruneIdleBudgets(long now) {
		if (budgets.size() < 64)
			return;
		Iterator<Budget> iterator = budgets.values()
			.iterator();
		while (iterator.hasNext())
			if (iterator.next()
				.isIdle(now))
				iterator.remove();
	}

	/**
	 * A plain token bucket, with the clock passed in so the throttle can be tested rather than
	 * assumed.
	 */
	public static final class Budget {

		private double blocks = BLOCK_BUDGET_BURST;
		private long lastRefillNanos;
		private boolean throttled;

		public Budget(long nowNanos) {
			this.lastRefillNanos = nowNanos;
		}

		public boolean claim(int requested, long nowNanos) {
			blocks = Math.min(BLOCK_BUDGET_BURST, blocks + refillSince(nowNanos));
			lastRefillNanos = nowNanos;

			if (requested < 0 || blocks < requested)
				return false;
			blocks -= requested;
			throttled = false;
			return true;
		}

		/** True the first time a throttle is hit, so the report happens once per episode. */
		boolean reportThrottle() {
			if (throttled)
				return false;
			throttled = true;
			return true;
		}

		boolean isIdle(long nowNanos) {
			return nowNanos - lastRefillNanos > BUDGET_IDLE_TIMEOUT_NANOS
				&& blocks + refillSince(nowNanos) >= BLOCK_BUDGET_BURST;
		}

		private double refillSince(long nowNanos) {
			return Math.max(0, (nowNanos - lastRefillNanos) / 1_000_000_000.0 * BLOCK_BUDGET_PER_SECOND);
		}
	}
}
