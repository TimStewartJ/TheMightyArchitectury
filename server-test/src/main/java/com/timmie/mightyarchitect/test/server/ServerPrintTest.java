package com.timmie.mightyarchitect.test.server;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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

            TheMightyArchitect.logger.info("{} RESULT PASS ({} checks)", TAG, passed);
        } catch (Throwable failure) {
            TheMightyArchitect.logger.error("{} RESULT FAIL {}", TAG, failure);
            TheMightyArchitect.logger.error("{} failure detail", TAG, failure);
        }
    }

    /**
     * Encodes and decodes through the registered stream codec, which is what the loader does on
     * the wire. A codec that writes and reads asymmetrically corrupts every later field, so the
     * fully-drained buffer is the assertion that matters most here.
     */
    private static InstantPrintPacket roundTrip(MinecraftServer server, InstantPrintPacket packet) {
        RegistryFriendlyByteBuf buffer =
            new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());

        AllPackets.INSTANT_PRINT_CODEC.encode(buffer, packet);
        int encoded = buffer.readableBytes();
        require(encoded > 0, "packet encoded to an empty buffer");

        InstantPrintPacket decoded = AllPackets.INSTANT_PRINT_CODEC.decode(buffer);
        require(buffer.readableBytes() == 0,
            "decode left " + buffer.readableBytes() + " of " + encoded + " bytes unread");
        require(decoded.type() == AllPackets.INSTANT_PRINT_TYPE,
            "decoded payload reports type " + decoded.type());

        passed++;
        return decoded;
    }

    /**
     * More entries than {@code BunchOfBlocks.MAX_SIZE} so the payload has to split, and one
     * non-default blockstate property so the NBT round-trip is genuinely exercised rather than
     * every block decoding to its default state.
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
