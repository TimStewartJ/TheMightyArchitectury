package com.timmie.mightyarchitect.test.server;

import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.networking.DetachedServerPlayer;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PacketWire;
import com.timmie.mightyarchitect.networking.ServerBuildGuard;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Automated coverage for the print-to-world path, driven from a real dedicated server.
 * <p>
 * The singleplayer print flow is {@code ArchitectManager.print()} -> {@code sendSchematic} ->
 * {@link InstantPrintPacket} over the wire -> server handler -> blocks in the world. Everything
 * from {@code sendSchematic} onwards is loader-agnostic and is exercised here against a real
 * {@link ServerLevel} with real registry access: payload chunking, the stream codec in both
 * directions, blockstate NBT round-tripping, and the placement itself.
 * <p>
 * Deliberately out of scope, because a dedicated server has no client to send from: the loader's
 * own payload registration and its client-to-server send. The automated client tests cover the
 * registration side indirectly - a client whose payloads failed to register does not reach a world.
 * They also cover {@code WrappedWorld}, which is client-only from 26.1 onwards and so cannot be
 * loaded here at all.
 * <p>
 * The second half of the run covers {@link ServerBuildGuard}: first the parts that need no
 * connected player - the decode-time bounds on both packets, the per-player placement budget
 * and the reach test - and then the gate end to end, with a real player, a real packet and real
 * blocks.
 * <p>
 * Results are written to the server log as {@code [SERVER-TEST]} lines;
 * {@code scripts/run-server-test-matrix.ps1} waits for the {@code RESULT} line.
 */
public final class ServerPrintTest {

    private static final String TAG = "[SERVER-TEST]";

    /** Well clear of a superflat surface, so the target volume is guaranteed to start as air. */
    private static final BlockPos ORIGIN = new BlockPos(0, 100, 0);

    private static final int WIDTH = 8;
    private static final int DEPTH = 5;

    /** Relative position of the palette entry carrying a non-default blockstate property. */
    private static final BlockPos AXIS_PROBE = new BlockPos(0, 0, 2);

    private static final long ONE_SECOND_NANOS = 1_000_000_000L;

    private static boolean ran;
    private static int passed;

    private ServerPrintTest() {
    }

    public static void run(MinecraftServer server) {
        if (ran || !Boolean.getBoolean("mightyarchitect.serverTest.enabled"))
            return;
        ran = true;

        TheMightyArchitect.logger.info("{} Starting automated print-to-world test", TAG);
        try {
            ServerLevel level = server.overworld();
            // Writing to an unloaded chunk silently does nothing, which would make every
            // assertion below vacuously true.
            level.getChunkAt(ORIGIN);

            Map<BlockPos, BlockState> schematic = buildSchematic();
            clearTargetArea(level, schematic);

            List<InstantPrintPacket> packets = InstantPrintPacket.sendSchematic(schematic, ORIGIN);
            require(packets.size() > 1,
                "expected " + schematic.size() + " blocks to need more than one packet, got " + packets.size());
            pass("sendSchematic split " + schematic.size() + " blocks into " + packets.size() + " packets");

            for (InstantPrintPacket packet : packets)
                InstantPrintPacket.apply(level, roundTrip(server, packet));
            pass("applied all " + packets.size() + " packets to the level");

            verifyPlaced(level, schematic);
            verifyGuards(server);
            verifyLiveGate(server, level);

            TheMightyArchitect.logger.info("{} RESULT PASS ({} checks)", TAG, passed);
        } catch (Throwable failure) {
            TheMightyArchitect.logger.error("{} RESULT FAIL {}", TAG, failure);
            TheMightyArchitect.logger.error("{} failure detail", TAG, failure);
        }
    }

    /**
     * Encodes and decodes the way the loader does on the wire. The encoding itself is version
     * -variable (stream codecs from 1.20.5, a plain buffer write before that) so it lives behind
     * {@link PacketWire} in the shared module; the assertions stay here.
     */
    private static InstantPrintPacket roundTrip(MinecraftServer server, InstantPrintPacket packet) {
        PacketWire.RoundTrip result = PacketWire.roundTrip(server.registryAccess(), packet);

        require(result.encodedBytes() > 0, "packet encoded to an empty buffer");
        require(result.unreadBytes() == 0,
            "decode left " + result.unreadBytes() + " of " + result.encodedBytes() + " bytes unread");
        require(result.identityMatches(), "decoded payload reports the wrong packet identity");

        passed++;
        return result.packet();
    }

    /**
     * More entries than {@link InstantPrintPacket#MAX_BLOCKS_PER_PACKET} so the payload has to
     * split, and one non-default blockstate property so the NBT round-trip is genuinely exercised
     * rather than every block decoding to its default state.
     */
    private static Map<BlockPos, BlockState> buildSchematic() {
        BlockState[] palette = {
            Blocks.STONE.defaultBlockState(),
            Blocks.OAK_PLANKS.defaultBlockState(),
            Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X),
            Blocks.GLASS.defaultBlockState()
        };

        Map<BlockPos, BlockState> schematic = new LinkedHashMap<>();
        int index = 0;
        for (int x = 0; x < WIDTH; x++)
            for (int z = 0; z < DEPTH; z++)
                schematic.put(new BlockPos(x, 0, z), palette[index++ % palette.length]);
        return schematic;
    }

    private static void clearTargetArea(ServerLevel level, Map<BlockPos, BlockState> schematic) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos relative : schematic.keySet())
            level.setBlock(relative.offset(ORIGIN), air, 3);

        for (BlockPos relative : schematic.keySet()) {
            BlockPos absolute = relative.offset(ORIGIN);
            require(level.getBlockState(absolute).isAir(),
                "target area is not empty at " + absolute + ", assertions would be meaningless");
        }
        pass("target area starts empty");
    }

    private static void verifyPlaced(ServerLevel level, Map<BlockPos, BlockState> schematic) {
        List<String> mismatches = new ArrayList<>();
        schematic.forEach((relative, expected) -> {
            BlockPos absolute = relative.offset(ORIGIN);
            BlockState actual = level.getBlockState(absolute);
            if (!actual.equals(expected))
                mismatches.add(absolute + ": expected " + expected + " but found " + actual);
        });
        require(mismatches.isEmpty(), mismatches.size() + " block(s) wrong, first few: "
            + String.join(" | ", mismatches.subList(0, Math.min(3, mismatches.size()))));
        pass("all " + schematic.size() + " blocks placed with matching blockstates");

        BlockState log = level.getBlockState(AXIS_PROBE.offset(ORIGIN));
        require(log.getBlock() == Blocks.OAK_LOG
                && log.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
            "oak log lost its non-default axis through the NBT round-trip: " + log);
        pass("non-default blockstate property survived the packet round-trip");
    }

    /**
     * The authorization gate the handlers now go through. These are the parts of it that are pure -
     * decode-time bounds, the placement budget and the reach test - so they can be asserted here
     * without a connected player, and so a regression in any of them turns the matrix red rather
     * than going unnoticed until someone reads the code.
     */
    private static void verifyGuards(MinecraftServer server) {
        RegistryAccess registries = server.registryAccess();

        require(PacketWire.instantPrintRejectsSize(registries, Integer.MAX_VALUE),
            "InstantPrintPacket accepted a payload declaring Integer.MAX_VALUE blocks");
        require(PacketWire.instantPrintRejectsSize(registries, -1),
            "InstantPrintPacket accepted a payload declaring a negative block count");
        pass("InstantPrintPacket rejects an out-of-range block count at decode time");

        require(PacketWire.setHotbarItemRejectsSlot(registries, 9),
            "SetHotbarItemPacket accepted a slot past the end of the hotbar");
        require(PacketWire.setHotbarItemRejectsSlot(registries, -1),
            "SetHotbarItemPacket accepted a negative slot");
        pass("SetHotbarItemPacket rejects an out-of-range hotbar slot at decode time");

        ServerBuildGuard.Budget budget = new ServerBuildGuard.Budget(0L);
        require(budget.claim(ServerBuildGuard.BLOCK_BUDGET_BURST, 0L),
            "a fresh block budget did not cover one full burst");
        require(!budget.claim(1, 0L), "the block budget allowed an overdraw");
        require(budget.claim(ServerBuildGuard.BLOCK_BUDGET_PER_SECOND, ONE_SECOND_NANOS),
            "one second did not refill the sustained block rate");
        pass("the per-player block budget refuses an overdraw and refills over time");

        BlockPos withinReach = ORIGIN.offset(0, 0, ServerBuildGuard.MAX_BUILD_DISTANCE - 1);
        BlockPos outOfReach = ORIGIN.offset(0, 0, ServerBuildGuard.MAX_BUILD_DISTANCE + 8);
        require(ServerBuildGuard.withinReach(ORIGIN.getX(), ORIGIN.getY(), ORIGIN.getZ(), withinReach),
            "the reach test rejected a position " + (ServerBuildGuard.MAX_BUILD_DISTANCE - 1) + " blocks away");
        require(!ServerBuildGuard.withinReach(ORIGIN.getX(), ORIGIN.getY(), ORIGIN.getZ(), outOfReach),
            "the reach test accepted a position past " + ServerBuildGuard.MAX_BUILD_DISTANCE + " blocks");
        require(!ServerBuildGuard.mayBuild(null), "the build gate authorised an absent player");
        pass("the build gate refuses out-of-reach positions and an absent player");
    }

    /**
     * The gate end to end: a real packet, off the wire, handed to the real handler with a real
     * {@link ServerPlayer}, ending in real blocks or the absence of them.
     * <p>
     * {@link #verifyGuards} covers the pure halves. This covers the composition - that an
     * authorised sender still gets their build, which is the regression that would otherwise be
     * silent, and that an unauthorised or out-of-reach one does not.
     * <p>
     * The probe player is deliberately detached: both loaders deliver play payloads on the server
     * thread and {@code ServerStartedEvent} already runs there, so the context can run its work
     * inline and still be faithful to how the handler is really invoked.
     */
    private static void verifyLiveGate(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = DetachedServerPlayer.create(server, level, "GateProbe");
        // Above the printed slab, in the chunk the test already forced loaded.
        BlockPos target = ORIGIN.offset(0, 5, 0);
        clear(level, target);

        setGameMode(player, GameType.CREATIVE);
        standAt(player, target);
        printOneBlock(server, player, target);
        require(level.getBlockState(target).getBlock() == Blocks.STONE,
            "the gate refused an authorised creative player's own build at " + target);
        pass("an authorised player's packet places blocks through the live gate");

        clear(level, target);
        standAt(player, target.offset(ServerBuildGuard.MAX_BUILD_DISTANCE * 2, 0, 0));
        printOneBlock(server, player, target);
        require(level.getBlockState(target).isAir(),
            "the gate placed a block " + (ServerBuildGuard.MAX_BUILD_DISTANCE * 2) + " blocks out of the sender's reach");
        pass("the live gate refuses a position out of the sender's reach");

        clear(level, target);
        setGameMode(player, GameType.SURVIVAL);
        standAt(player, target);
        printOneBlock(server, player, target);
        require(level.getBlockState(target).isAir(),
            "the gate let a player who is neither creative nor an operator place a block");
        pass("the live gate refuses a player who is neither creative nor an operator");

        clear(level, target);
    }

    /**
     * Puts the probe player into a game mode without a client to tell about it.
     * <p>
     * Vanilla sets the mode first and only then notifies: {@code onUpdateAbilities} already returns
     * early when there is no connection, but the tab-list broadcast that follows reads
     * {@code player.connection.latency()} and throws. By that point the mode has been set, which is
     * the only part this test needs - so the notification failure is swallowed and the result is
     * then asserted rather than assumed. A version that reordered those two steps would fail here
     * instead of quietly testing the wrong game mode.
     */
    private static void setGameMode(ServerPlayer player, GameType mode) {
        try {
            player.gameMode.changeGameModeForPlayer(mode);
        } catch (NullPointerException noClientToNotify) {
            // The mode is already set; only the broadcast to a connection we do not have failed.
        }
        require(player.gameMode.getGameModeForPlayer() == mode && player.isCreative() == mode.isCreative(),
            "could not put the probe player into " + mode + " mode");
    }

    private static void standAt(ServerPlayer player, BlockPos pos) {
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        require(level.getBlockState(pos).isAir(), "could not clear " + pos + " before the next gate check");
    }

    /** Sends one block the way a client does: encoded, decoded, then through {@code handle}. */
    private static void printOneBlock(MinecraftServer server, ServerPlayer player, BlockPos target) {
        Map<BlockPos, BlockState> single = Map.of(BlockPos.ZERO, Blocks.STONE.defaultBlockState());
        for (InstantPrintPacket packet : InstantPrintPacket.sendSchematic(single, target))
            InstantPrintPacket.handle(roundTrip(server, packet), new PacketContext() {
                @Override
                public ServerPlayer player() {
                    return player;
                }

                @Override
                public void enqueue(Runnable work) {
                    work.run();
                }
            });
    }

    /**
     * {@code WrappedWorld} is deliberately not touched here: on 26.1 it implements the client-only
     * {@code BlockAndTintGetter}, so loading it on a dedicated server fails. The client test
     * covers it instead.
     */
    private static void require(boolean condition, String failure) {
        if (!condition)
            throw new AssertionError(failure);
    }

    private static void pass(String description) {
        passed++;
        TheMightyArchitect.logger.info("{} PASS {}", TAG, description);
    }
}
