package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.phase.MultiplayerPrintCommands;
import com.timmie.mightyarchitect.control.phase.MultiplayerPrintCommands.Command;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Bootstrapped
@DisplayName("Multiplayer command fallback")
class MultiplayerPrintCommandsTest {

    @Test
    @DisplayName("a singleton uses canonical block-state serialization")
    void singletonUsesSetblock() {
        Command command = onlyCommand(Map.of(new BlockPos(3, 7, -2), Blocks.STONE.defaultBlockState()));

        assertEquals("setblock 3 7 -2 minecraft:stone", command.text());
        assertEquals(1, command.blockCount());
    }

    @Test
    @DisplayName("contiguous equal states become one fill command")
    void contiguousRunUsesFill() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (int z = 5; z <= 9; z++)
            blocks.put(new BlockPos(2, 4, z), Blocks.OAK_PLANKS.defaultBlockState());

        Command command = onlyCommand(blocks);

        assertEquals("fill 2 4 5 2 4 9 minecraft:oak_planks", command.text());
        assertEquals(5, command.blockCount());
    }

    @Test
    @DisplayName("runs never merge across gaps or block states")
    void gapsAndStatesStaySeparate() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(new BlockPos(0, 0, 0), Blocks.STONE.defaultBlockState());
        blocks.put(new BlockPos(1, 0, 0), Blocks.STONE.defaultBlockState());
        blocks.put(new BlockPos(3, 0, 0), Blocks.STONE.defaultBlockState());
        blocks.put(new BlockPos(4, 0, 0), Blocks.GLASS.defaultBlockState());

        List<Command> commands = MultiplayerPrintCommands.plan(blocks);

        assertEquals(3, commands.size());
        assertEquals(4, commands.stream().mapToInt(Command::blockCount).sum());
    }

    @Test
    @DisplayName("revalidation splits a queued fill around a newly blocked position")
    void revalidationSplitsAQueuedFill() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (int x = 0; x < 5; x++)
            blocks.put(new BlockPos(x, 0, 0), Blocks.STONE.defaultBlockState());
        Command queued = onlyCommand(blocks);

        List<Command> current = MultiplayerPrintCommands.replan(queued, pos -> pos.getX() != 2);

        assertEquals(2, current.size());
        assertEquals("fill 0 0 0 1 0 0 minecraft:stone", current.get(0).text());
        assertEquals("fill 3 0 0 4 0 0 minecraft:stone", current.get(1).text());
    }

    @Test
    @DisplayName("long runs are split into conservative fill batches")
    void longRunsAreBounded() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (int x = 0; x < 70; x++)
            blocks.put(new BlockPos(x, 0, 0), Blocks.STONE.defaultBlockState());

        List<Command> commands = MultiplayerPrintCommands.plan(blocks);

        assertEquals(List.of(32, 32, 6), commands.stream().map(Command::blockCount).toList());
    }

    @Test
    @DisplayName("a solid volume is covered exactly once with far fewer commands")
    void solidVolumeIsCompactedWithoutLosingBlocks() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (int x = 0; x < 12; x++)
            for (int y = 0; y < 4; y++)
                for (int z = 0; z < 6; z++)
                    blocks.put(new BlockPos(x, y, z), Blocks.BRICKS.defaultBlockState());

        List<Command> commands = MultiplayerPrintCommands.plan(blocks);

        assertEquals(blocks.size(), commands.stream().mapToInt(Command::blockCount).sum());
        Set<BlockPos> covered = new HashSet<>();
        commands.forEach(command -> command.positions().forEach(
            pos -> assertTrue(covered.add(pos), () -> pos + " was covered twice")));
        assertEquals(blocks.keySet(), covered);
        assertTrue(commands.size() <= 24,
            () -> "expected at most one command per x-run, got " + commands.size());
    }

    private static Command onlyCommand(Map<BlockPos, BlockState> blocks) {
        List<Command> commands = MultiplayerPrintCommands.plan(blocks);
        assertEquals(1, commands.size());
        return commands.get(0);
    }
}
