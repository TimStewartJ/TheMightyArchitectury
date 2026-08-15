package com.timmie.mightyarchitect.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.Schematic;
import com.timmie.mightyarchitect.control.TemplateBlockAccess;
import com.timmie.mightyarchitect.control.compose.GroundPlan;
import com.timmie.mightyarchitect.control.palette.PaletteStorage;
import com.timmie.mightyarchitect.foundation.compat.McCompat;
import com.timmie.mightyarchitect.foundation.compat.ServerConnect;
import com.timmie.mightyarchitect.gui.PalettePickerScreen;
import com.timmie.mightyarchitect.gui.ArchitectMenuScreen;
import com.timmie.mightyarchitect.gui.TextInputPromptScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Passive state observer for the externally driven player journey.
 * <p>
 * This class deliberately performs no architect action. The PowerShell driver sends XTEST input
 * through Minecraft's real GLFW callbacks; this companion only reports what the game did with it.
 * Connecting to the local fixture server is setup, before the recorded player journey begins.
 */
public final class PlayerJourneyController {

    private static final int STATE_WRITE_INTERVAL_TICKS = 2;
    private static final int CONNECT_AFTER_TICKS = 20;

    private static boolean started;
    private static boolean connecting;
    private static int ticks;
    private static int keyEvents;
    private static int mouseButtonEvents;
    private static int scrollEvents;
    private static int lastKey = -1;
    private static int lastKeyAction = -1;
    private static int lastMouseButton = -1;
    private static int lastMouseAction = -1;
    private static double lastHorizontalScroll;
    private static double lastVerticalScroll;
    private static String previousPhase = "";
    private static String previousScreen = "";
    private static final Map<BlockPos, BlockState> expectedPrintedBlocks = new LinkedHashMap<>();
    private static Schematic capturedModel;
    private static Object capturedPrimaryPalette;
    private static Object capturedSecondaryPalette;

    private PlayerJourneyController() {
    }

    public static void start() {
        if (!Boolean.getBoolean("mightyarchitect.playerJourney.enabled") || started)
            return;

        started = true;
        deleteState();
        TheMightyArchitect.logger.info("[PLAYER-JOURNEY] Passive observer started");
    }

    public static void tick(Minecraft minecraft) {
        if (!started)
            return;

        ticks++;
        if (minecraft.level == null || minecraft.player == null) {
            connect(minecraft);
        } else {
            captureExpectedPrint(minecraft);
        }

        if (ticks % STATE_WRITE_INTERVAL_TICKS == 0)
            writeState(minecraft);
    }

    public static void recordKey(int key, int action) {
        if (!started)
            return;
        keyEvents++;
        lastKey = key;
        lastKeyAction = action;
    }

    public static void recordMouseButton(int button, int action) {
        if (!started)
            return;
        mouseButtonEvents++;
        lastMouseButton = button;
        lastMouseAction = action;
    }

    public static void recordScroll(double horizontal, double vertical) {
        if (!started)
            return;
        scrollEvents++;
        lastHorizontalScroll = horizontal;
        lastVerticalScroll = vertical;
    }

    private static void connect(Minecraft minecraft) {
        if (connecting || ticks < CONNECT_AFTER_TICKS || McCompat.hasOverlay(minecraft))
            return;

        String server = System.getProperty("mightyarchitect.playerJourney.server", "127.0.0.1:25565");
        ServerAddress address = ServerAddress.parseString(server);
        ServerData data = ServerConnect.serverData("Mighty Architect Player Journey", server);
        connecting = true;
        TheMightyArchitect.logger.info("[PLAYER-JOURNEY] Connecting to {}", server);
        ServerConnect.connect(minecraft, McCompat.currentScreen(minecraft), address, data);
    }

    private static void captureExpectedPrint(Minecraft minecraft) {
        Schematic model = ArchitectManager.getModel();
        if (model.getSketch() == null || model.isMaterializing() || model.getAnchor() == null)
            return;

        TemplateBlockAccess materialized = model.getMaterializedSketch();
        if (materialized == null || materialized.getBlockMap().isEmpty())
            return;
        if (model == capturedModel && model.getPrimary() == capturedPrimaryPalette
            && model.getSecondary() == capturedSecondaryPalette)
            return;

        expectedPrintedBlocks.clear();
        materialized.getBlockMap().forEach((local, state) ->
            expectedPrintedBlocks.put(local.offset(model.getAnchor()), state));
        capturedModel = model;
        capturedPrimaryPalette = model.getPrimary();
        capturedSecondaryPalette = model.getSecondary();
    }

    private static void writeState(Minecraft minecraft) {
        try {
            JsonObject state = new JsonObject();
            state.addProperty("tick", ticks);
            boolean worldReady = minecraft.level != null && minecraft.player != null;
            state.addProperty("worldReady", worldReady);
            state.addProperty("overlayVisible", McCompat.hasOverlay(minecraft));
            state.addProperty("keyEvents", keyEvents);
            state.addProperty("mouseButtonEvents", mouseButtonEvents);
            state.addProperty("scrollEvents", scrollEvents);
            state.addProperty("lastKey", lastKey);
            state.addProperty("lastKeyAction", lastKeyAction);
            state.addProperty("lastMouseButton", lastMouseButton);
            state.addProperty("lastMouseAction", lastMouseAction);
            state.addProperty("lastHorizontalScroll", lastHorizontalScroll);
            state.addProperty("lastVerticalScroll", lastVerticalScroll);
            state.addProperty("targetX", (Number) null);
            state.addProperty("targetY", (Number) null);
            state.addProperty("targetZ", (Number) null);
            state.addProperty("paletteTargetX", (Number) null);
            state.addProperty("paletteTargetY", (Number) null);

            Screen screen = McCompat.currentScreen(minecraft);
            String screenName = screenName(screen);
            state.addProperty("screen", screenName);
            String phase = ArchitectManager.getPhase().name();
            state.addProperty("phase", phase);

            if (!phase.equals(previousPhase) || !screenName.equals(previousScreen)) {
                TheMightyArchitect.logger.info("[PLAYER-JOURNEY] State phase={} screen={}", phase,
                    screenName.isEmpty() ? "<none>" : screenName);
                previousPhase = phase;
                previousScreen = screenName;
            }

            if (worldReady) {
                state.addProperty("selectedHotbarSlot", McCompat.selectedHotbarSlot(minecraft.player));
                state.addProperty("playerX", minecraft.player.getX());
                state.addProperty("playerY", minecraft.player.getY());
                state.addProperty("playerZ", minecraft.player.getZ());
                state.addProperty("playerYaw", minecraft.player.getYRot());
                state.addProperty("playerPitch", minecraft.player.getXRot());
                state.addProperty("gameMode", minecraft.gameMode.getPlayerMode().toString());

                if (minecraft.hitResult instanceof BlockHitResult blockHit) {
                    BlockPos target = blockHit.getBlockPos();
                    state.addProperty("targetX", target.getX());
                    state.addProperty("targetY", target.getY());
                    state.addProperty("targetZ", target.getZ());
                }

                Schematic model = ArchitectManager.getModel();
                GroundPlan groundPlan = model.getGroundPlan();
                int[] roomCount = { 0 };
                if (groundPlan != null)
                    groundPlan.forEachRoom(room -> roomCount[0]++);
                state.addProperty("roomCount", roomCount[0]);
                state.addProperty("themeName",
                    groundPlan == null ? "" : groundPlan.theme.getDisplayName());
                state.addProperty("sketchPresent", model.getSketch() != null);
                state.addProperty("materializing", model.isMaterializing());
                state.addProperty("rendererGeometry", MightyClient.renderer.hasGeometry());
                state.addProperty("paletteName",
                    model.getPrimary() == null ? "" : model.getPrimary().getName());

                int previewBlocks = 0;
                if (model.getSketch() != null && !model.isMaterializing()
                    && model.getMaterializedSketch() != null) {
                    previewBlocks = model.getMaterializedSketch().getBlockMap().size();
                }
                state.addProperty("previewBlocks", previewBlocks);

                int matches = 0;
                int blockMatches = 0;
                JsonArray mismatchSamples = new JsonArray();
                for (Map.Entry<BlockPos, BlockState> expected : expectedPrintedBlocks.entrySet()) {
                    BlockState actual = minecraft.level.getBlockState(expected.getKey());
                    if (actual.getBlock() == expected.getValue().getBlock())
                        blockMatches++;
                    if (actual.equals(expected.getValue())) {
                        matches++;
                    } else if (mismatchSamples.size() < 8) {
                        mismatchSamples.add(expected.getKey() + ": expected " + expected.getValue()
                            + ", found " + actual);
                    }
                }
                state.addProperty("expectedPrintedBlocks", expectedPrintedBlocks.size());
                state.addProperty("matchingPrintedBlockTypes", blockMatches);
                state.addProperty("matchingPrintedBlocks", matches);
                state.addProperty("printedWorldBlockTypesMatch",
                    !expectedPrintedBlocks.isEmpty() && blockMatches == expectedPrintedBlocks.size());
                state.addProperty("printedWorldMatches",
                    !expectedPrintedBlocks.isEmpty() && matches == expectedPrintedBlocks.size());
                state.add("printedStateMismatchSamples", mismatchSamples);
            }

            addPaletteTarget(state, minecraft, screen);
            writeJsonAtomically(statePath(), state);
        } catch (Throwable throwable) {
            TheMightyArchitect.logger.error("[PLAYER-JOURNEY] Unable to write observer state", throwable);
        }
    }

    private static void addPaletteTarget(JsonObject state, Minecraft minecraft, Screen screen) {
        if (!(screen instanceof PalettePickerScreen))
            return;

        int gridX = (minecraft.getWindow().getGuiScaledWidth() - 256) / 2 + 10;
        int gridY = (minecraft.getWindow().getGuiScaledHeight() - 236) / 2 + 68;
        List<AbstractWidget> buttons = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget widget) || !widget.active || !widget.visible)
                continue;
            if (widget.getX() < gridX || widget.getX() >= gridX + 5 * 23)
                continue;
            if (widget.getY() < gridY || widget.getY() >= gridY + 4 * 23)
                continue;
            buttons.add(widget);
        }

        buttons.sort(Comparator.comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getX));
        List<String> paletteNames = PaletteStorage.getResourcePaletteNames();
        int targetIndex = -1;
        String currentName = ArchitectManager.getModel().getPrimary() == null
            ? "" : ArchitectManager.getModel().getPrimary().getName();
        for (int i = 0; i < paletteNames.size() && i < buttons.size(); i++) {
            if (!paletteNames.get(i).equals(currentName)) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex < 0)
            return;

        AbstractWidget target = buttons.get(targetIndex);
        double scale = minecraft.getWindow().getGuiScale();
        state.addProperty("paletteTargetX", (target.getX() + target.getWidth() / 2.0) * scale);
        state.addProperty("paletteTargetY", (target.getY() + target.getHeight() / 2.0) * scale);
        state.addProperty("paletteTargetName", paletteNames.get(targetIndex));
    }

    private static String screenName(Screen screen) {
        if (screen == null)
            return "";
        if (screen instanceof ArchitectMenuScreen)
            return "ArchitectMenuScreen";
        if (screen instanceof PalettePickerScreen)
            return "PalettePickerScreen";
        if (screen instanceof TextInputPromptScreen)
            return "TextInputPromptScreen";
        if (screen instanceof ChatScreen)
            return "ChatScreen";
        return screen.getClass().getSimpleName();
    }

    private static void deleteState() {
        try {
            Files.deleteIfExists(statePath());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to remove stale player-journey state", exception);
        }
    }

    private static Path statePath() {
        String configured = System.getProperty("mightyarchitect.playerJourney.state");
        if (configured == null || configured.isBlank())
            throw new IllegalStateException("mightyarchitect.playerJourney.state is not configured");
        return Path.of(configured);
    }

    private static void writeJsonAtomically(Path path, JsonObject json) throws Exception {
        if (path.getParent() != null)
            Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(json),
            StandardCharsets.UTF_8);
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
    }
}
