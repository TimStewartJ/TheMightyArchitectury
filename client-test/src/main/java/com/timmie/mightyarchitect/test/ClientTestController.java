package com.timmie.mightyarchitect.test;

import com.timmie.mightyarchitect.foundation.compat.McCompat;
import com.timmie.mightyarchitect.foundation.compat.ServerConnect;
import com.timmie.mightyarchitect.foundation.compat.ScreenInput;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.timmie.mightyarchitect.AllBlocks;
import com.timmie.mightyarchitect.AllItems;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.Schematic;
import com.timmie.mightyarchitect.control.design.DesignTheme;
import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.design.Sketch;
import com.timmie.mightyarchitect.control.design.ThemeStorage;
import com.timmie.mightyarchitect.control.palette.BlockOrientation;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteBlockInfo;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.palette.PaletteStorage;
import com.timmie.mightyarchitect.control.phase.ArchitectPhases;
import com.timmie.mightyarchitect.foundation.WrappedWorld;
import com.timmie.mightyarchitect.foundation.utility.ShaderManager;
import com.timmie.mightyarchitect.foundation.utility.Shaders;
import com.timmie.mightyarchitect.gui.ArchitectMenuScreen;
import com.timmie.mightyarchitect.gui.DesignExporterScreen;
import com.timmie.mightyarchitect.gui.PalettePickerScreen;
import com.timmie.mightyarchitect.gui.TextInputPromptScreen;
import com.timmie.mightyarchitect.gui.widgets.Indicator;
import com.timmie.mightyarchitect.gui.widgets.Label;
import com.timmie.mightyarchitect.gui.widgets.ScrollInput;
import com.timmie.mightyarchitect.test.mixin.ArchitectManagerAccessor;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public final class ClientTestController {

    private static final boolean KEEP_OPEN = Boolean.getBoolean("mightyarchitect.clientTest.keepOpen");

    private enum Stage {
        CONNECT,
        WAIT_FOR_WORLD,
        CAPTURE_BASELINE,
        START_COMPOSER,
        OPEN_PALETTE,
        CAPTURE_PALETTE_PREVIEW,
        VERIFY_SCREEN_INPUT,
        VERIFY_TEXT_INPUT,
        VERIFY_SCROLL_ROUTING,
        CAPTURE_BLUEPRINT,
        CAPTURE_HUD_VISIBLE,
        CAPTURE_HUD_HIDDEN,
        CAPTURE_ALIGN_BASELINE,
        CAPTURE_ALIGN_OUTLINE,
        CAPTURE_LABEL_BASELINE,
        CAPTURE_LABEL_TEXT,
        BUILD_LARGE_SKETCH,
        VERIFY_REDRAW_BUDGET,
        VERIFY_RENDER,
        FINISHED
    }

    private static final int MAX_TICKS = 20 * 60 * 6;

    /** Summed |dR|+|dG|+|dB| above which a sampled pixel counts as changed. */
    private static final int PIXEL_DELTA_THRESHOLD = 40;
    /** Minimum fraction of the sampled region the passive HUD must paint. */
    private static final double MIN_HUD_CHANGED_FRACTION = 0.0015;
    /**
     * The passive menu is anchored to the bottom of the screen, so the HUD diff ignores the top
     * of the frame where chat and toast notifications fade in and out on their own schedule.
     */
    private static final double HUD_REGION_MIN_Y = 0.45;
    private static final double HUD_REGION_MAX_Y = 1.0;
    /** Minimum fraction of the sampled probe region the outline must paint. */
    private static final double MIN_OUTLINE_CHANGED_FRACTION = 0.005;
    /** How far the probe outline's centroid may sit from screen centre, in screen fractions. */
    private static final double MAX_CENTROID_DRIFT = 0.15;
    /**
     * The alignment diff is restricted to the middle of the frame so transient vanilla UI
     * (chat fading at the top, toasts on the right, the hotbar at the bottom) cannot be
     * mistaken for rendered world geometry.
     */
    private static final double PROBE_REGION_MIN = 0.28;
    private static final double PROBE_REGION_MAX = 0.72;
    /** Bound on how long the blueprint post-chain may take to report active. */
    private static final int POST_CHAIN_WAIT_TICKS = 20 * 30;
    /** Distance in front of the camera at which the probe outline is placed. */
    private static final double PROBE_DISTANCE = 8.0;
    private static final double PROBE_RADIUS = 1.5;
    private static final String PROBE_SLOT = "mightyarchitect-client-test-alignment-probe";
    private static final String LABEL_SLOT = "mightyarchitect-client-test-label-probe";
    private static final String PROBE_LABEL_TEXT = "8888m";
    /** The colour {@code RoomTool} gives its dimension labels. */
    private static final int PROBE_LABEL_COLOR = 0;
    /** Minimum fraction of the probe region a world-space label must paint. */
    private static final double MIN_LABEL_CHANGED_FRACTION = 0.0004;
    /**
     * Minimum luminance range across the pixels a label paints. Glyphs and backdrop sit at
     * opposite ends of it whichever way round they are coloured, so a legible label clears this
     * comfortably and one drawn in its own backdrop colour cannot.
     */
    private static final double MIN_LABEL_LUMINANCE_SPREAD = 0.5;

    /** Palette picker window size and the included-palettes grid inside it, in GUI units. */
    private static final int PALETTE_SCREEN_WIDTH = 256;
    private static final int PALETTE_SCREEN_HEIGHT = 236;
    private static final int PALETTE_GRID_X = 10;
    private static final int PALETTE_GRID_Y = 68;
    private static final int PALETTE_GRID_SPACING = 23;
    private static final int PALETTE_GRID_COLUMNS = 5;
    private static final int PALETTE_GRID_ROWS = 4;
    /** Distinct colours expected in the grid once the block previews actually draw. */
    private static final int MIN_PALETTE_GRID_COLOURS = 40;
    /** Characters typed at the text prompt; read back from its abort callback on close. */
    private static final String TYPED_PROBE = "clienttest";

    /**
     * Side of the synthetic cube the budget stage builds.
     * <p>
     * Sized so both budgets are crossed several times over rather than just barely: 36 gives
     * {@value #LARGE_SKETCH_ENTRIES} palette entries against a budget of 8192, and a bounds volume
     * of 37³ = 50,653 tesselation steps against 4096 - around six and twelve slices respectively.
     * A cube that merely cleared each threshold once would make the "spread over at least two
     * ticks" assertions turn on where the harness's tick happens to fall relative to the mod's,
     * which is exactly the kind of gate that goes intermittently red on one node in twenty-five.
     * Only the cube's surface emits quads, because the renderer culls against its neighbours.
     */
    private static final int LARGE_SKETCH_SIDE = 36;
    private static final int LARGE_SKETCH_ENTRIES = LARGE_SKETCH_SIDE * LARGE_SKETCH_SIDE * LARGE_SKETCH_SIDE;

    private static final List<String> checks = new ArrayList<>();
    private static Stage stage = Stage.CONNECT;
    private static int totalTicks;
    private static int stageTicks;
    private static boolean started;
    private static boolean screenshotPending;
    private static Path completedScreenshot;
    private static Set<Path> screenshotFilesBefore = Set.of();
    private static Path baselineScreenshot;
    private static Path blueprintScreenshot;
    private static Path hudVisibleScreenshot;
    private static Path hudHiddenScreenshot;
    private static Path alignBaselineScreenshot;
    private static Path alignOutlineScreenshot;
    private static Path labelBaselineScreenshot;
    private static Path labelTextScreenshot;
    private static Path palettePickerScreenshot;
    private static String typedText;
    private static int worldRenderFrames;
    private static int hudRenderFrames;
    private static int composerOverlayFrames;
    private static int materializeTicks;
    private static int redrawTicks;
    private static int secondRedrawTicks;
    private static int geometryGapTicks;
    private static int tokenDriftTicks;
    private static boolean paletteSwapped;
    private static Object modelGenerationToken;

    private ClientTestController() {
    }

    public static void start() {
        if (!Boolean.getBoolean("mightyarchitect.clientTest.enabled") || started)
            return;

        started = true;
        deleteResult();
        TheMightyArchitect.logger.info("[CLIENT-TEST] Starting automated client test");
    }

    public static void recordWorldRender() {
        worldRenderFrames++;
    }

    public static void recordHudRender() {
        hudRenderFrames++;
    }

    public static void recordComposerOverlay() {
        composerOverlayFrames++;
    }

    // Driven by MightyClientProbeMixin: the harness rides the mod's own client tick rather than
    // registering with a loader event, so it stays loader-agnostic.
    public static void tick(Minecraft minecraft) {
        if (!started)
            return;
        if (stage == Stage.FINISHED)
            return;

        try {
            totalTicks++;
            stageTicks++;
            if (totalTicks > MAX_TICKS)
                throw new AssertionError("Timed out in stage " + stage);

            releaseMouse(minecraft);
            if (isFrameCovered(minecraft)) {
                stageTicks--;
                return;
            }
            switch (stage) {
                case CONNECT -> connect(minecraft);
                case WAIT_FOR_WORLD -> waitForWorld(minecraft);
                case CAPTURE_BASELINE -> captureBaseline(minecraft);
                case START_COMPOSER -> startComposer(minecraft);
                case OPEN_PALETTE -> openPalette(minecraft);
                case CAPTURE_PALETTE_PREVIEW -> capturePalettePreview(minecraft);
                case VERIFY_SCREEN_INPUT -> verifyScreenInput(minecraft);
                case VERIFY_TEXT_INPUT -> verifyTextInput(minecraft);
                case VERIFY_SCROLL_ROUTING -> verifyScrollRouting(minecraft);
                case CAPTURE_BLUEPRINT -> captureBlueprint(minecraft);
                case CAPTURE_HUD_VISIBLE -> captureHudVisible(minecraft);
                case CAPTURE_HUD_HIDDEN -> captureHudHidden(minecraft);
                case CAPTURE_ALIGN_BASELINE -> captureAlignBaseline(minecraft);
                case CAPTURE_ALIGN_OUTLINE -> captureAlignOutline(minecraft);
                case CAPTURE_LABEL_BASELINE -> captureLabelBaseline(minecraft);
                case CAPTURE_LABEL_TEXT -> captureLabelText(minecraft);
                case BUILD_LARGE_SKETCH -> buildLargeSketch(minecraft);
                case VERIFY_REDRAW_BUDGET -> verifyRedrawBudget(minecraft);
                case VERIFY_RENDER -> verifyRender(minecraft);
                case FINISHED -> {
                }
            }
        } catch (Throwable throwable) {
            fail(minecraft, throwable);
        }
    }

    /**
     * Keeps the OS cursor free while the automated test drives the game. Minecraft grabs the
     * cursor as soon as it is in-world without a screen open, which hijacks the mouse on the
     * developer's desktop for the whole run. Nothing under test needs a grabbed cursor.
     * In keep-open mode this stops once the test finishes, so manual testing behaves normally.
     */
    private static void releaseMouse(Minecraft minecraft) {
        try {
            if (minecraft.mouseHandler != null && minecraft.mouseHandler.isMouseGrabbed())
                minecraft.mouseHandler.releaseMouse();
        } catch (Throwable ignored) {
            // Cursor handling is a convenience only; never fail a test because of it.
        }
    }

    /**
     * True while a full-screen overlay hides the game. The Mojang loading overlay covers every
     * pixel for as long as resources are reloading, but the client keeps ticking underneath it
     * and will happily connect to a server. Nothing may run until it clears:
     * <ul>
     * <li>Connecting early joins a world whose block models are still baking. A block-break
     * level event then hands {@code BlockModelShaper.getBlockModel} a state it has no model for,
     * and vanilla throws a {@link NullPointerException} that disconnects the client with
     * "Network Protocol Error".</li>
     * <li>Capturing early compares splash frames instead of game frames, which fails checks that
     * should pass and silently passes checks that should fail - the red splash on its own carries
     * enough distinct colours to satisfy the palette grid probe.</li>
     * </ul>
     * Waiting here makes the harness behave like a player, who cannot leave the main menu until
     * the same overlay clears.
     */
    private static boolean isFrameCovered(Minecraft minecraft) {
        return McCompat.hasOverlay(minecraft);
    }

    private static void connect(Minecraft minecraft) {
        if (minecraft.level != null && minecraft.player != null) {
            advance(Stage.WAIT_FOR_WORLD);
            return;
        }
        if (stageTicks < 20)
            return;

        String server = System.getProperty("mightyarchitect.clientTest.server", "127.0.0.1:25565");
        ServerAddress address = ServerAddress.parseString(server);
        ServerData data = ServerConnect.serverData("Mighty Architect Client Test", server);
        TheMightyArchitect.logger.info("[CLIENT-TEST] Connecting to {}", server);
        ServerConnect.connect(minecraft, McCompat.currentScreen(minecraft), address, data);
        advance(Stage.WAIT_FOR_WORLD);
    }

    // This source set is shared verbatim by every Stonecutter node (it is not preprocessed), so the
    // two multiplayer entry points whose signatures moved between 1.19.4 and 1.20.6 live in
    // ServerConnect, on the mod side of the fence where guards work. They used to be bound
    // reflectively by name here, which resolved only under Mojang mappings and so could never run
    // against the packaged jars.

    private static void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null)
            return;
        if (stageTicks < 40)
            return;

        check(AllItems.ARCHITECT_WAND != null && AllItems.ARCHITECT_WAND.get() != null,
            "architect_wand registered");
        check(AllBlocks.DESIGN_ANCHOR != null && AllBlocks.DESIGN_ANCHOR.get() != null,
            "design_anchor registered");
        check(AllBlocks.SLICE_MARKER != null && AllBlocks.SLICE_MARKER.get() != null,
            "slice_marker registered");
        check(AllItems.ARCHITECT_WAND.typeOf(new ItemStack(AllItems.ARCHITECT_WAND.get())),
            "architect_wand identity recognized");
        check(!PaletteStorage.getResourcePaletteNames().isEmpty(), "resource palettes loaded");
        check(!ThemeStorage.getIncluded().isEmpty(), "included themes loaded");

        checkPaletteRoundTrip();

        // WrappedWorld overrides methods NeoForge adds to Level that vanilla does not have. Those
        // resolve when the class is verified, so a wrong override is an AbstractMethodError the
        // moment the class first loads - which the build matrix cannot catch. It is client-only
        // (on 26.1 it implements the client BlockAndTintGetter), so this is the right place.
        BlockPos probe = minecraft.player.blockPosition();
        WrappedWorld wrapped = new WrappedWorld(minecraft.level);
        check(wrapped.getBlockState(probe).equals(minecraft.level.getBlockState(probe)),
            "WrappedWorld loads and delegates to the wrapped level");

        advance(Stage.CAPTURE_BASELINE);
    }

    /**
     * The one data-integrity assertion that is not pure logic: serializing a palette goes through
     * the block registry, so it is worth asserting against a real, fully loaded game.
     * <p>
     * The nine assertions that used to sit here were pure version-agnostic Java - filename
     * derivation and idempotence, {@code slug} traversal safety, palette independence, lenient
     * enum parsing, the {@code hashCode}/{@code equals} contract. They now live in the JUnit
     * suite under {@code unit-test/}, where they run in milliseconds instead of costing a full
     * game boot on 25 targets to check arithmetic.
     */
    private static void checkPaletteRoundTrip() {
        PaletteDefinition palette = PaletteDefinition.defaultPalette().clone();
        palette.setName("Client Test Palette");
        PaletteDefinition roundTrip = PaletteDefinition.fromNBT(palette.writeToNBT(new CompoundTag()));
        check("Client Test Palette".equals(roundTrip.getName()), "palette NBT name round-tripped");
        check(palette.get(Palette.ROOF_PRIMARY).equals(roundTrip.get(Palette.ROOF_PRIMARY)),
            "palette NBT block state round-tripped");
    }

    private static void captureBaseline(Minecraft minecraft) {
        Path captured = awaitScreenshot(minecraft);
        if (captured == null)
            return;

        baselineScreenshot = captured;
        check(Files.isRegularFile(baselineScreenshot), "baseline frame captured");
        advance(Stage.START_COMPOSER);
    }

    private static void startComposer(Minecraft minecraft) {
        DesignTheme theme = ThemeStorage.getIncluded().get(0);
        worldRenderFrames = 0;
        hudRenderFrames = 0;
        composerOverlayFrames = 0;
        ArchitectManager.compose(theme);
        check(ArchitectManager.inPhase(ArchitectPhases.Composing), "composer phase entered");
        check(ShaderManager.getActiveShader() == Shaders.Blueprint, "blueprint shader selected");
        ArchitectManager.getModel().setSketch(new Sketch());
        check(ArchitectManager.getModel().getPrimary() != null
            && ArchitectManager.getModel().getSecondary() != null, "model palettes initialized");
        advance(Stage.OPEN_PALETTE);
    }

    private static void openPalette(Minecraft minecraft) {
        if (stageTicks == 1) {
            McCompat.setScreen(minecraft, new PalettePickerScreen());
            return;
        }
        if (stageTicks < 3)
            return;

        check(McCompat.currentScreen(minecraft) instanceof PalettePickerScreen, "palette picker opened");
        advance(Stage.CAPTURE_PALETTE_PREVIEW);
    }

    /**
     * The palette picker shows each palette as a small arrangement of rendered blocks. Those
     * previews go through the mod's own GUI block renderer, which is easy to break silently:
     * the buttons still draw their frame, so the screen looks structurally correct while every
     * preview is blank. Counting distinct colours inside the palette grid separates a grid of
     * shaded 3D blocks from a grid of flat empty squares.
     */
    private static void capturePalettePreview(Minecraft minecraft) {
        if (stageTicks < 45)
            return;
        Path captured = awaitScreenshot(minecraft);
        if (captured == null)
            return;

        palettePickerScreenshot = captured;

        Window window = minecraft.getWindow();
        double scale = window.getGuiScale();
        int topLeftX = (window.getGuiScaledWidth() - PALETTE_SCREEN_WIDTH) / 2;
        int topLeftY = (window.getGuiScaledHeight() - PALETTE_SCREEN_HEIGHT) / 2;
        int colours = distinctColours(captured,
            (int) ((topLeftX + PALETTE_GRID_X) * scale),
            (int) ((topLeftY + PALETTE_GRID_Y) * scale),
            (int) (PALETTE_GRID_COLUMNS * PALETTE_GRID_SPACING * scale),
            (int) (PALETTE_GRID_ROWS * PALETTE_GRID_SPACING * scale));

        check(colours >= MIN_PALETTE_GRID_COLOURS,
            "palette block previews rendered (" + colours + " distinct colours in grid)");
        advance(Stage.VERIFY_SCREEN_INPUT);
    }

    /**
     * Drives real input at the mod's screens.
     * <p>
     * The screens register their widgets with vanilla and let {@code Screen} /
     * {@code ContainerEventHandler} dispatch clicks, characters and scroll. That is far less code
     * than re-implementing the dispatch, but it is also invisible to a screenshot: a screen whose
     * widgets are never registered still draws correctly and simply ignores the mouse. Nothing else
     * in any matrix clicks, types or scrolls, so these checks are the only thing standing between a
     * refactor of that layer and shipping dead controls.
     */
    private static void verifyScreenInput(Minecraft minecraft) {
        if (stageTicks < 3)
            return;

        verifyPaletteClicks(minecraft);
        advance(Stage.VERIFY_TEXT_INPUT);
    }

    /**
     * Clicks two different palette buttons by position and requires the selection to follow. This
     * covers the whole chain at once: the widgets are in {@code children()}, vanilla dispatches to
     * them, and hit-testing picks the widget actually under the cursor rather than merely the first
     * one registered.
     */
    private static void verifyPaletteClicks(Minecraft minecraft) {
        Screen screen = McCompat.currentScreen(minecraft);
        check(screen instanceof PalettePickerScreen, "palette picker still open for input");

        List<AbstractWidget> widgets = registeredWidgets(screen);
        check(!widgets.isEmpty(),
            "screen widgets are registered with vanilla (" + widgets.size() + " in children())");

        Window window = minecraft.getWindow();
        int gridX = (window.getGuiScaledWidth() - PALETTE_SCREEN_WIDTH) / 2 + PALETTE_GRID_X;
        int gridY = (window.getGuiScaledHeight() - PALETTE_SCREEN_HEIGHT) / 2 + PALETTE_GRID_Y;
        List<AbstractWidget> paletteButtons = new ArrayList<>();
        for (AbstractWidget widget : widgets) {
            if (!widget.active || !widget.visible)
                continue;
            if (widget.getX() < gridX || widget.getX() >= gridX + PALETTE_GRID_COLUMNS * PALETTE_GRID_SPACING)
                continue;
            if (widget.getY() < gridY || widget.getY() >= gridY + PALETTE_GRID_ROWS * PALETTE_GRID_SPACING)
                continue;
            paletteButtons.add(widget);
        }
        check(paletteButtons.size() >= 2,
            "at least two included-palette buttons to click (" + paletteButtons.size() + ")");

        PaletteDefinition afterFirst = clickCentre(screen, paletteButtons.get(0));
        PaletteDefinition afterSecond = clickCentre(screen, paletteButtons.get(1));
        check(afterFirst != afterSecond,
            "clicking two palette buttons selected two different palettes");
    }

    private static PaletteDefinition clickCentre(Screen screen, AbstractWidget widget) {
        double x = widget.getX() + widget.getWidth() / 2.0;
        double y = widget.getY() + widget.getHeight() / 2.0;
        check(ScreenInput.click(screen, x, y, 0), "click consumed at " + (int) x + "," + (int) y);
        return ArchitectManager.getModel().getPrimary();
    }

    /**
     * Typing has to reach the text field the screen focused on open. This is the check that would
     * have caught a wrong answer to "does vanilla's own {@code setInitialFocus} pass steal focus
     * from the field?", which is otherwise only answerable by disassembly.
     */
    private static void verifyTextInput(Minecraft minecraft) {
        if (stageTicks == 1) {
            typedText = null;
            TextInputPromptScreen prompt = new TextInputPromptScreen(value -> {
            }, value -> typedText = value);
            prompt.setTitle("Client Test");
            McCompat.setScreen(minecraft, prompt);
            return;
        }
        if (stageTicks < 4)
            return;

        if (typedText == null) {
            Screen prompt = McCompat.currentScreen(minecraft);
            check(prompt instanceof TextInputPromptScreen, "text prompt opened");
            check(!registeredWidgets(prompt).isEmpty(), "text prompt registered its widgets");
            for (char character : TYPED_PROBE.toCharArray())
                ScreenInput.type(prompt, character);
            // Closing the prompt without confirming reports the field contents to the abort callback.
            McCompat.setScreen(minecraft, null);
            return;
        }

        check(TYPED_PROBE.equals(typedText),
            "typed characters reached the focused text field (got \"" + typedText + "\")");
        advance(Stage.VERIFY_SCROLL_ROUTING);
    }

    /**
     * A scroll has to be routed to the scroll input that a label is drawn on top of. The exporter
     * screen registers its labels <em>before</em> those inputs, and vanilla's {@code getChildAt}
     * returns the <em>first</em> child under the cursor, so the input is only reachable because the
     * labels are inactive. Nothing about that is visible in a screenshot.
     * <p>
     * This asserts the routing decision rather than delivering a scroll, because {@code ScrollInput}
     * gates on the hover flag that {@code AbstractWidget.render} computes from the real cursor -
     * a synthesised scroll at coordinates the cursor is not actually at would be refused by the
     * widget itself, for reasons that have nothing to do with dispatch.
     */
    private static void verifyScrollRouting(Minecraft minecraft) {
        if (stageTicks == 1) {
            DesignExporter.setTheme(ThemeStorage.getIncluded().get(0));
            McCompat.setScreen(minecraft, new DesignExporterScreen());
            return;
        }
        if (stageTicks < 4)
            return;

        Screen exporter = McCompat.currentScreen(minecraft);
        check(exporter instanceof DesignExporterScreen, "design exporter opened");

        List<AbstractWidget> decorations = new ArrayList<>();
        ScrollInput scrollInput = null;
        for (AbstractWidget widget : registeredWidgets(exporter)) {
            if (widget instanceof Label || widget instanceof Indicator)
                decorations.add(widget);
            else if (scrollInput == null && widget instanceof ScrollInput candidate && candidate.active)
                scrollInput = candidate;
        }

        boolean allInert = !decorations.isEmpty();
        for (AbstractWidget decoration : decorations)
            allInert &= !decoration.active;
        check(allInert, "decorative widgets are present and inactive, so they cannot intercept input ("
            + decorations.size() + " checked)");

        check(scrollInput != null, "exporter screen has a scroll input");
        double x = scrollInput.getX() + scrollInput.getWidth() / 2.0;
        double y = scrollInput.getY() + scrollInput.getHeight() / 2.0;
        check(exporter.getChildAt(x, y).orElse(null) == scrollInput,
            "hit-testing over the scroll input resolves to it and not to the label drawn on it");

        McCompat.setScreen(minecraft, null);
        advance(Stage.CAPTURE_BLUEPRINT);
    }

    /** The screen's own widget list, which is vanilla's since the screens stopped keeping one. */
    private static List<AbstractWidget> registeredWidgets(Screen screen) {
        List<AbstractWidget> widgets = new ArrayList<>();
        if (screen == null)
            return widgets;
        for (GuiEventListener child : screen.children())
            if (child instanceof AbstractWidget widget)
                widgets.add(widget);
        return widgets;
    }

    /**
     * Waits for the blueprint post-chain to actually report active before grabbing the frame.
     * A fixed tick delay races the shader becoming ready, which showed up as an intermittent
     * "no blue shift" failure on otherwise healthy nodes. A resource reload during startup can
     * also discard an already-applied post chain, so re-assert it while waiting.
     */
    private static void captureBlueprint(Minecraft minecraft) {
        if (stageTicks < 60)
            return;
        if (!Shaders.Blueprint.isActive()) {
            if (stageTicks % 20 == 0)
                Shaders.Blueprint.setActive(true);
            if (stageTicks > POST_CHAIN_WAIT_TICKS)
                throw new AssertionError("Blueprint post-chain never became active after "
                    + stageTicks + " ticks");
            return;
        }
        Path captured = awaitScreenshot(minecraft);
        if (captured == null)
            return;

        blueprintScreenshot = captured;
        ImageStats baseline = imageStats(baselineScreenshot);
        ImageStats blueprint = imageStats(blueprintScreenshot);
        double baselineBlueBias = baseline.blue - (baseline.red + baseline.green) / 2.0;
        double blueprintBlueBias = blueprint.blue - (blueprint.red + blueprint.green) / 2.0;
        check(blueprintBlueBias > baselineBlueBias + 5.0,
            "blueprint frame has stronger blue bias (" + round(baselineBlueBias) + " -> "
                + round(blueprintBlueBias) + ")");
        check(Shaders.Blueprint.isActive(), "blueprint post-chain active");
        advance(Stage.CAPTURE_HUD_VISIBLE);
    }

    /**
     * Captures the composer HUD with the passive menu shown. Paired with
     * {@link #captureHudHidden(Minecraft)} the menu is the only thing that differs
     * between the two frames, so a no-op drawPassive produces an empty diff.
     */
    private static void captureHudVisible(Minecraft minecraft) {
        if (stageTicks == 1) {
            McCompat.clearChat(minecraft);
            ArchitectMenuScreen menu = ArchitectManagerAccessor.getMenu();
            menu.setFocused(false);
            menu.setVisible(true);
            menu.updateContents();
            settleMenuAnimation(menu);
            return;
        }
        if (stageTicks < 45)
            return;
        Path captured = awaitScreenshot(minecraft);
        if (captured == null)
            return;

        hudVisibleScreenshot = captured;
        advance(Stage.CAPTURE_HUD_HIDDEN);
    }

    private static void captureHudHidden(Minecraft minecraft) {
        if (stageTicks == 1) {
            ArchitectMenuScreen menu = ArchitectManagerAccessor.getMenu();
            menu.setVisible(false);
            settleMenuAnimation(menu);
            return;
        }
        if (stageTicks < 45)
            return;
        Path captured = awaitScreenshot(minecraft);
        if (captured == null)
            return;

        ImageDiff diff = diffImages(hudVisibleScreenshot, captured, 0.0, 1.0,
            HUD_REGION_MIN_Y, HUD_REGION_MAX_Y);
        hudHiddenScreenshot = captured;
        check(diff.changedFraction() >= MIN_HUD_CHANGED_FRACTION,
            "passive HUD overlay painted pixels (" + diff.changedPixels() + " px, "
                + round(diff.changedFraction() * 100.0) + "% of frame)");

        ArchitectMenuScreen menu = ArchitectManagerAccessor.getMenu();
        menu.setVisible(true);
        menu.updateContents();
        advance(Stage.CAPTURE_ALIGN_BASELINE);
    }

    /**
     * The menu slides in and out on an eased chaser, so a frame captured mid-slide compares
     * against an arbitrary intermediate position. Run the chaser to completion up front so both
     * HUD frames are taken from a settled layout.
     */
    private static void settleMenuAnimation(ArchitectMenuScreen menu) {
        for (int i = 0; i < 400; i++)
            menu.onClientTick();
    }

    /**
     * Captures the world without the probe outline, after pinning the camera so the
     * subsequent outline frame is directly comparable.
     */
    private static void captureAlignBaseline(Minecraft minecraft) {
        if (stageTicks == 1) {
            minecraft.player.setYRot(0.0f);
            minecraft.player.setXRot(0.0f);
            return;
        }
        if (stageTicks < 25)
            return;
        Path captured = awaitScreenshot(minecraft);
        if (captured == null)
            return;

        alignBaselineScreenshot = captured;
        advance(Stage.CAPTURE_ALIGN_OUTLINE);
    }

    /**
     * Renders an outline centred on the camera's look vector through the same world-render
     * hook the mod uses. If that hook's transform drops the camera rotation the box no
     * longer projects to the middle of the screen, which the centroid assertion catches.
     */
    private static void captureAlignOutline(Minecraft minecraft) {
        showProbeOutline(minecraft);
        if (stageTicks < 25)
            return;
        Path captured = awaitScreenshot(minecraft);
        if (captured == null)
            return;

        ImageDiff diff = diffImages(alignBaselineScreenshot, captured, PROBE_REGION_MIN, PROBE_REGION_MAX);
        MightyClient.outliner.remove(PROBE_SLOT);
        alignOutlineScreenshot = captured;
        check(diff.changedFraction() >= MIN_OUTLINE_CHANGED_FRACTION,
            "world-render probe outline painted pixels (" + diff.changedPixels() + " px, "
                + round(diff.changedFraction() * 100.0) + "% of probe region)");

        double driftX = Math.abs(diff.centroidX() - 0.5);
        double driftY = Math.abs(diff.centroidY() - 0.5);
        check(driftX <= MAX_CENTROID_DRIFT && driftY <= MAX_CENTROID_DRIFT,
            "world render aligned with camera; probe centroid (" + round(diff.centroidX()) + ", "
                + round(diff.centroidY()) + ") within " + round(MAX_CENTROID_DRIFT) + " of centre");
        advance(Stage.CAPTURE_LABEL_BASELINE);
    }

    private static void captureLabelBaseline(Minecraft minecraft) {
        if (stageTicks < 25)
            return;
        Path captured = awaitScreenshot(minecraft);
        if (captured == null)
            return;

        labelBaselineScreenshot = captured;
        advance(Stage.CAPTURE_LABEL_TEXT);
    }

    /**
     * The composer labels room dimensions with world-space text drawn through the outliner. That
     * text is easy to lose without any error - an unset alpha byte or a render pass that no longer
     * displays immediate-mode glyphs both leave the label fully invisible while everything else
     * still draws. Placing a label in front of the camera and diffing against the same frame
     * without it proves the glyphs actually reach the screen.
     * <p>
     * Painting pixels is not enough on its own: a label whose glyphs match its own backdrop still
     * changes the frame while being unreadable, which is how the room dimensions ended up as black
     * text on a dark plate. The luminance spread across the painted pixels covers that.
     */
    private static void captureLabelText(Minecraft minecraft) {
        showProbeLabel(minecraft);
        if (stageTicks < 25)
            return;
        Path captured = awaitScreenshot(minecraft);
        if (captured == null)
            return;

        ImageDiff diff = diffImages(labelBaselineScreenshot, captured, PROBE_REGION_MIN, PROBE_REGION_MAX);
        MightyClient.outliner.remove(LABEL_SLOT);
        labelTextScreenshot = captured;
        check(diff.changedFraction() >= MIN_LABEL_CHANGED_FRACTION,
            "world-space measurement label painted pixels (" + diff.changedPixels() + " px, "
                + round(diff.changedFraction() * 100.0) + "% of probe region)");
        check(diff.luminanceSpread() >= MIN_LABEL_LUMINANCE_SPREAD,
            "world-space measurement label legible against its backdrop (luminance spread "
                + round(diff.luminanceSpread()) + ")");
        advance(Stage.BUILD_LARGE_SKETCH);
    }

    /** Uses the room dimension colour, the one pairing that has actually gone unreadable. */
    private static void showProbeLabel(Minecraft minecraft) {
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 target = eye.add(minecraft.player.getLookAngle().scale(PROBE_DISTANCE));
        MightyClient.outliner.chaseText(LABEL_SLOT, target, PROBE_LABEL_TEXT)
            .colored(PROBE_LABEL_COLOR);
    }

    private static void showProbeOutline(Minecraft minecraft) {
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 target = eye.add(minecraft.player.getLookAngle().scale(PROBE_DISTANCE));
        AABB box = new AABB(target.x - PROBE_RADIUS, target.y - PROBE_RADIUS, target.z - PROBE_RADIUS,
            target.x + PROBE_RADIUS, target.y + PROBE_RADIUS, target.z + PROBE_RADIUS);
        MightyClient.outliner.showAABB(PROBE_SLOT, box)
            .colored(0xFFFF0000)
            .lineWidth(0.125f);
    }

    /**
     * Builds a schematic large enough that both the palette walk and the tesselation have to be
     * spread across ticks, then hands it to the renderer the way {@code PhasePreviewing} does.
     * <p>
     * <b>Why this stage exists.</b> Every other stage drives {@code new Sketch()} - an empty one.
     * An empty sketch assembles to empty maps, so the palette walk has nothing to do and the
     * renderer's bounds come back null; the budgeted paths never execute at all. The matrix was
     * therefore green on 25 targets while the feature under test had never run once. A gate that
     * only ever renders an empty sketch cannot see a redraw defect, in exactly the way a diff that
     * only asks "did pixels change" cannot see a black-on-black label.
     * <p>
     * The sketch is synthesised rather than composed from a theme's designs on purpose: the design
     * picker's output depends on which designs a theme happens to ship for the requested spans, so
     * composing a real building would make the volume - and therefore whether the budget engages at
     * all - a property of the content rather than of the test.
     */
    private static void buildLargeSketch(Minecraft minecraft) {
        Schematic model = ArchitectManager.getModel();

        // The planning tool sets this on the first placed room; nothing has placed one here.
        model.setAnchor(minecraft.player.blockPosition());
        model.setSketch(new FixedSketch(solidCube(LARGE_SKETCH_SIDE, Palette.HEAVY_PRIMARY)));

        // The assertion that would have caught the regression this whole stage exists to cover:
        // with no budget, setSketch applies the entire palette walk before it returns.
        check(model.isMaterializing(),
            "a " + LARGE_SKETCH_ENTRIES + "-entry sketch deferred its palette walk instead of "
                + "applying it inline");

        MightyClient.renderer.display(model);
        check(!MightyClient.renderer.hasGeometry(), "renderer starts with no geometry for a new sketch");

        modelGenerationToken = McCompat.modelGeneration(minecraft);
        check(modelGenerationToken != null, "model generation token resolved");

        materializeTicks = 0;
        redrawTicks = 0;
        secondRedrawTicks = 0;
        geometryGapTicks = 0;
        tokenDriftTicks = 0;
        paletteSwapped = false;
        advance(Stage.VERIFY_REDRAW_BUDGET);
    }

    /**
     * Watches one full build and one palette swap go through, asserting the three properties the
     * budgeting rests on: the work is spread over several ticks, it finishes, and the geometry
     * already on screen is never taken away while a replacement is being built.
     */
    private static void verifyRedrawBudget(Minecraft minecraft) {
        Schematic model = ArchitectManager.getModel();

        // A token that changed every tick would restart the redraw forever, so the build below
        // would never finish - this reports that as a token fault rather than as a timeout. It is
        // recorded once, at the end, rather than as one check per tick.
        if (McCompat.modelGeneration(minecraft) != modelGenerationToken)
            tokenDriftTicks++;

        if (model.isMaterializing())
            materializeTicks++;
        else if (MightyClient.renderer.isRedrawing())
            if (paletteSwapped)
                secondRedrawTicks++;
            else
                redrawTicks++;

        // Once the first build has landed, the preview must never go blank again: a palette swap
        // rebuilds behind the geometry already on screen rather than clearing it first.
        if (paletteSwapped && !MightyClient.renderer.hasGeometry())
            geometryGapTicks++;

        if (model.isMaterializing() || MightyClient.renderer.isRedrawing())
            return;

        if (!paletteSwapped) {
            check(materializeTicks >= 2,
                "palette walk was spread over " + materializeTicks + " ticks");
            check(redrawTicks >= 2, "tesselation was spread over " + redrawTicks + " ticks");
            check(MightyClient.renderer.hasGeometry(), "geometry present once the first build completed");

            PaletteDefinition swapped = PaletteDefinition.defaultPalette().clone();
            swapped.setName("Client Test Swap");
            swapped.put(Palette.HEAVY_PRIMARY, Blocks.GOLD_BLOCK);
            model.swapPrimaryPalette(swapped);
            MightyClient.renderer.update();

            check(model.isMaterializing(), "a palette swap deferred its walk too");
            paletteSwapped = true;
            return;
        }

        check(secondRedrawTicks >= 2,
            "the swap's tesselation was spread over " + secondRedrawTicks + " ticks");
        check(geometryGapTicks == 0,
            "the preview never went blank during the swap (" + geometryGapTicks + " blank ticks)");
        check(MightyClient.renderer.hasGeometry(), "geometry present once the swap completed");
        check(tokenDriftTicks == 0,
            "model generation token stable while resources were untouched (" + tokenDriftTicks
                + " drifting ticks)");
        advance(Stage.VERIFY_RENDER);
    }

    /** A solid cube of one palette entry, anchored at the origin. */
    private static Map<BlockPos, PaletteBlockInfo> solidCube(int side, Palette palette) {
        Map<BlockPos, PaletteBlockInfo> blocks = new HashMap<>();
        for (int x = 0; x < side; x++)
            for (int y = 0; y < side; y++)
                for (int z = 0; z < side; z++)
                    blocks.put(new BlockPos(x, y, z), info(palette));
        return blocks;
    }

    /**
     * {@code afterPosition} is filled in by the real {@code Sketch.assemble}, and
     * {@code PaletteDefinition.get} dereferences it.
     */
    private static PaletteBlockInfo info(Palette palette) {
        PaletteBlockInfo blockInfo = new PaletteBlockInfo(palette, BlockOrientation.NONE);
        blockInfo.afterPosition = BlockOrientation.NONE;
        return blockInfo;
    }

    /** A sketch that assembles to a fixed set of blocks instead of to placed designs. */
    private static final class FixedSketch extends Sketch {

        private final Vector<Map<BlockPos, PaletteBlockInfo>> assembled;

        private FixedSketch(Map<BlockPos, PaletteBlockInfo> primaryLayer) {
            assembled = new Vector<>(2);
            assembled.add(primaryLayer);
            assembled.add(new HashMap<>());
        }

        @Override
        public Vector<Map<BlockPos, PaletteBlockInfo>> assemble() {
            return assembled;
        }
    }

    private static void verifyRender(Minecraft minecraft) {
        if (stageTicks < 80)
            return;

        check(minecraft.level != null && minecraft.player != null, "client stayed connected");
        check(ArchitectManager.inPhase(ArchitectPhases.Composing), "composer survived rendered frames");
        check(worldRenderFrames > 0, "world-render hook executed (" + worldRenderFrames + " frames)");
        check(hudRenderFrames > 0, "HUD dispatch executed (" + hudRenderFrames + " frames)");
        check(composerOverlayFrames > 0,
            "composer overlay rendered (" + composerOverlayFrames + " frames)");
        if (!KEEP_OPEN)
            ArchitectManager.unload();
        pass(minecraft);
    }

    /**
     * Returns the captured screenshot exactly once, requesting one on first call and
     * yielding {@code null} until the asynchronous grab completes.
     */
    private static Path awaitScreenshot(Minecraft minecraft) {
        if (!screenshotPending && completedScreenshot == null) {
            requestScreenshot(minecraft);
            return null;
        }
        if (completedScreenshot == null)
            return null;

        Path captured = completedScreenshot;
        completedScreenshot = null;
        return captured;
    }

    private static void requestScreenshot(Minecraft minecraft) {
        screenshotFilesBefore = screenshotFiles(minecraft.gameDirectory);
        screenshotPending = true;
        Screenshot.grab(minecraft.gameDirectory, McCompat.mainRenderTarget(minecraft), message -> {
            try {
                Set<Path> after = screenshotFiles(minecraft.gameDirectory);
                after.removeAll(screenshotFilesBefore);
                completedScreenshot = after.stream()
                    .max(Comparator.comparingLong(ClientTestController::lastModified))
                    .orElseThrow(() -> new AssertionError("Screenshot callback ran but no PNG was created"));
            } catch (Throwable throwable) {
                fail(minecraft, throwable);
            } finally {
                screenshotPending = false;
            }
        });
    }

    private static Set<Path> screenshotFiles(File gameDirectory) {
        Path directory = gameDirectory.toPath().resolve("screenshots");
        if (!Files.isDirectory(directory))
            return new HashSet<>();

        try (var stream = Files.list(directory)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".png"))
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to list screenshots", exception);
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception exception) {
            return 0;
        }
    }

    private static ImageStats imageStats(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null)
                throw new IllegalStateException("ImageIO could not decode " + path);

            long red = 0;
            long green = 0;
            long blue = 0;
            long samples = 0;
            int minY = image.getHeight() / 10;
            int maxY = image.getHeight() * 9 / 10;
            for (int y = minY; y < maxY; y += 4) {
                for (int x = 0; x < image.getWidth(); x += 4) {
                    int rgb = image.getRGB(x, y);
                    red += rgb >>> 16 & 0xff;
                    green += rgb >>> 8 & 0xff;
                    blue += rgb & 0xff;
                    samples++;
                }
            }
            return new ImageStats(red / (double) samples, green / (double) samples, blue / (double) samples);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to analyze screenshot " + path, exception);
        }
    }

    private static ImageDiff diffImages(Path first, Path second) {
        return diffImages(first, second, 0.0, 1.0, 0.0, 1.0);
    }

    private static ImageDiff diffImages(Path first, Path second, double min, double max) {
        return diffImages(first, second, min, max, min, max);
    }

    /**
     * Compares two frames within the given fractional window and reports how much changed,
     * where the change is centred, and how far the changed pixels spread in luminance. Both the
     * window bounds and the returned centroid are fractions of the full frame, so results are
     * independent of window size.
     */
    private static ImageDiff diffImages(Path first, Path second, double minX, double maxX,
                                        double minY, double maxY) {
        try {
            BufferedImage before = ImageIO.read(first.toFile());
            BufferedImage after = ImageIO.read(second.toFile());
            if (before == null || after == null)
                throw new IllegalStateException("ImageIO could not decode " + first + " or " + second);

            int width = Math.min(before.getWidth(), after.getWidth());
            int height = Math.min(before.getHeight(), after.getHeight());
            int startX = (int) (width * minX);
            int endX = (int) (width * maxX);
            int startY = (int) (height * minY);
            int endY = (int) (height * maxY);
            long changed = 0;
            long samples = 0;
            double sumX = 0;
            double sumY = 0;
            double minLuminance = Double.MAX_VALUE;
            double maxLuminance = -Double.MAX_VALUE;
            for (int y = startY; y < endY; y += 2) {
                for (int x = startX; x < endX; x += 2) {
                    samples++;
                    int a = before.getRGB(x, y);
                    int b = after.getRGB(x, y);
                    int delta = Math.abs((a >>> 16 & 0xff) - (b >>> 16 & 0xff))
                        + Math.abs((a >>> 8 & 0xff) - (b >>> 8 & 0xff))
                        + Math.abs((a & 0xff) - (b & 0xff));
                    if (delta < PIXEL_DELTA_THRESHOLD)
                        continue;

                    changed++;
                    sumX += x / (double) width;
                    sumY += y / (double) height;
                    double luminance = luminance(b);
                    minLuminance = Math.min(minLuminance, luminance);
                    maxLuminance = Math.max(maxLuminance, luminance);
                }
            }
            if (samples == 0)
                throw new IllegalStateException("Screenshots contained no comparable pixels");
            if (changed == 0)
                return new ImageDiff(0, 0.0, Double.NaN, Double.NaN, 0.0);
            return new ImageDiff((int) changed, changed / (double) samples, sumX / changed, sumY / changed,
                maxLuminance - minLuminance);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compare screenshots " + first + " and " + second,
                exception);
        }
    }

    private static double luminance(int rgb) {
        return (0.299 * (rgb >>> 16 & 0xff) + 0.587 * (rgb >>> 8 & 0xff) + 0.114 * (rgb & 0xff)) / 255.0;
    }

    /**
     * Counts distinct colours in a region, quantised so that shading gradients within one block
     * face do not inflate the count. Flat, unrendered UI yields a handful of colours; a grid of
     * textured 3D blocks yields many.
     */
    private static int distinctColours(Path path, int x, int y, int width, int height) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null)
                throw new IllegalStateException("ImageIO could not decode " + path);

            int minX = Math.max(0, x);
            int minY = Math.max(0, y);
            int maxX = Math.min(image.getWidth(), x + width);
            int maxY = Math.min(image.getHeight(), y + height);
            if (minX >= maxX || minY >= maxY)
                throw new IllegalStateException("Palette grid region fell outside the frame");

            Set<Integer> seen = new HashSet<>();
            for (int py = minY; py < maxY; py++) {
                for (int px = minX; px < maxX; px++) {
                    int rgb = image.getRGB(px, py);
                    seen.add(((rgb >>> 19 & 0x1f) << 10) | ((rgb >>> 11 & 0x1f) << 5) | (rgb >>> 3 & 0x1f));
                }
            }
            return seen.size();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to analyze palette grid in " + path, exception);
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition)
            throw new AssertionError(description);
        checks.add(description);
        TheMightyArchitect.logger.info("[CLIENT-TEST] PASS {}", description);
    }

    private static void advance(Stage next) {
        stage = next;
        stageTicks = 0;
        TheMightyArchitect.logger.info("[CLIENT-TEST] Stage {}", next);
    }

    private static void pass(Minecraft minecraft) {
        writeResult("passed", null);
        TheMightyArchitect.logger.info("[CLIENT-TEST] PASS ({} checks)", checks.size());
        stage = Stage.FINISHED;
        if (KEEP_OPEN)
            TheMightyArchitect.logger.info("[CLIENT-TEST] KEEP OPEN - manual testing can continue");
        else
            minecraft.stop();
    }

    private static void fail(Minecraft minecraft, Throwable throwable) {
        if (stage == Stage.FINISHED)
            return;
        writeResult("failed", throwable);
        TheMightyArchitect.logger.error("[CLIENT-TEST] FAIL in stage {}", stage, throwable);
        stage = Stage.FINISHED;
        minecraft.stop();
    }

    private static void deleteResult() {
        try {
            Files.deleteIfExists(resultPath());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to remove stale client-test result", exception);
        }
    }

    private static void writeResult(String status, Throwable throwable) {
        try {
            JsonObject result = new JsonObject();
            result.addProperty("status", status);
            result.addProperty("stage", stage.name());
            result.addProperty("ticks", totalTicks);
            result.addProperty("keepOpen", KEEP_OPEN);
            JsonArray passedChecks = new JsonArray();
            checks.forEach(passedChecks::add);
            result.add("checks", passedChecks);
            if (baselineScreenshot != null)
                result.addProperty("baselineScreenshot", baselineScreenshot.toAbsolutePath().toString());
            if (blueprintScreenshot != null)
                result.addProperty("blueprintScreenshot", blueprintScreenshot.toAbsolutePath().toString());
            if (hudVisibleScreenshot != null)
                result.addProperty("hudVisibleScreenshot", hudVisibleScreenshot.toAbsolutePath().toString());
            if (hudHiddenScreenshot != null)
                result.addProperty("hudHiddenScreenshot", hudHiddenScreenshot.toAbsolutePath().toString());
            if (alignBaselineScreenshot != null)
                result.addProperty("alignBaselineScreenshot",
                    alignBaselineScreenshot.toAbsolutePath().toString());
            if (alignOutlineScreenshot != null)
                result.addProperty("alignOutlineScreenshot",
                    alignOutlineScreenshot.toAbsolutePath().toString());
            if (labelBaselineScreenshot != null)
                result.addProperty("labelBaselineScreenshot",
                    labelBaselineScreenshot.toAbsolutePath().toString());
            if (labelTextScreenshot != null)
                result.addProperty("labelTextScreenshot", labelTextScreenshot.toAbsolutePath().toString());
            if (palettePickerScreenshot != null)
                result.addProperty("palettePickerScreenshot",
                    palettePickerScreenshot.toAbsolutePath().toString());
            if (throwable != null) {
                result.addProperty("error", throwable.toString());
                StringWriter stack = new StringWriter();
                throwable.printStackTrace(new PrintWriter(stack));
                result.addProperty("stackTrace", stack.toString());
            }

            Path resultPath = resultPath();
            if (resultPath.getParent() != null)
                Files.createDirectories(resultPath.getParent());
            Path temporary = resultPath.resolveSibling(resultPath.getFileName() + ".tmp");
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(result),
                StandardCharsets.UTF_8);
            Files.move(temporary, resultPath, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception exception) {
            TheMightyArchitect.logger.error("[CLIENT-TEST] Unable to write result", exception);
        }
    }

    private static Path resultPath() {
        String configured = System.getProperty("mightyarchitect.clientTest.result");
        if (configured == null || configured.isBlank())
            throw new IllegalStateException("mightyarchitect.clientTest.result is not configured");
        return Path.of(configured);
    }

    private static String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private record ImageStats(double red, double green, double blue) {
    }

    private record ImageDiff(int changedPixels, double changedFraction, double centroidX,
                             double centroidY, double luminanceSpread) {
    }
}
