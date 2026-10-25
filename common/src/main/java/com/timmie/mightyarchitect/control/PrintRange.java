package com.timmie.mightyarchitect.control;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.ServerBuildGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * Whether a build is close enough to the player to print.
 * <p>
 * A server running the mod places printed blocks only within {@link ServerBuildGuard#MAX_BUILD_DISTANCE}
 * of the player's own position and silently drops the rest. Since the composer tools aim from the
 * camera, a build can be drafted much farther away than that, so printing checks first and refuses
 * the whole build rather than leaving half of it. Printing through commands on a vanilla server
 * has no such limit, and nothing here applies to it.
 */
public final class PrintRange {

	private static InstantPrintPacket routeProbe;

	private PrintRange() {
	}

	/** How far the model's farthest block is from the player, measured as the server measures it. */
	public static double farthest(Player player, Schematic model) {
		BlockPos anchor = model.getAnchor();
		return ServerBuildGuard.farthestDistance(player.getX(), player.getY(), player.getZ(),
			model.getMaterializedSketch()
				.getBlockMap()
				.keySet(),
			anchor == null ? BlockPos.ZERO : anchor);
	}

	public static boolean isOutOfReach(double distance) {
		return distance > ServerBuildGuard.MAX_BUILD_DISTANCE;
	}

	public static String outOfRangeMessage(double distance) {
		return "This build reaches " + (int) Math.ceil(distance) + " blocks from you. Printing only works within "
			+ ServerBuildGuard.MAX_BUILD_DISTANCE + " - move closer and print again.";
	}

	/** Whether printing would go through the mod's own packet, the route the range limit applies to. */
	public static boolean usesPacketRoute() {
		if (routeProbe == null)
			routeProbe = InstantPrintPacket.sendSchematic(Map.of(), BlockPos.ZERO)
				.get(0);
		return AllPackets.canSendToServer(routeProbe);
	}

	/** Whether this player could print at all: creative mode or operator, as {@code print()} requires. */
	public static boolean mayPrint() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.gameMode != null && minecraft.gameMode.getPlayerMode()
			.isCreative() || ArchitectManager.hasGameMasterPermission();
	}
}
