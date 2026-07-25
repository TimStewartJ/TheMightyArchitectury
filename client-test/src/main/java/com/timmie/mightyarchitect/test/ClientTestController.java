package com.timmie.mightyarchitect.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.timmie.mightyarchitect.AllBlocks;
import com.timmie.mightyarchitect.AllItems;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.design.DesignTheme;
import com.timmie.mightyarchitect.control.design.Sketch;
import com.timmie.mightyarchitect.control.design.ThemeStorage;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.palette.PaletteStorage;
import com.timmie.mightyarchitect.control.phase.ArchitectPhases;
import com.timmie.mightyarchitect.foundation.utility.ShaderManager;
import com.timmie.mightyarchitect.foundation.utility.Shaders;
import com.timmie.mightyarchitect.gui.ArchitectMenuScreen;
import com.timmie.mightyarchitect.gui.PalettePickerScreen;
import com.timmie.mightyarchitect.test.mixin.ArchitectManagerAccessor;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientTestController {

    private static final boolean KEEP_OPEN = Boolean.getBoolean("mightyarchitect.clientTest.keepOpen");

    private enum Stage {
        CONNECT,
        WAIT_FOR_WORLD,
        CAPTURE_BASELINE,
        START_COMPOSER,
        OPEN_PALETTE,
        CAPTURE_BLUEPRINT,
        CAPTURE_HUD_VISIBLE,
        CAPTURE_HUD_HIDDEN,
        CAPTURE_ALIGN_BASELINE,
        CAPTURE_ALIGN_OUTLINE,
        VERIFY_RENDER,
        FINISHED
    }

    private static final int MAX_TICKS = 20 * 60 * 6;

    /** Summed |dR|+|dG|+|dB| above which a sampled pixel counts as changed. */
    private static final int PIXEL_DELTA_THRESHOLD = 40;
    /** Minimum fraction of sampled pixels the passive HUD must paint. */
    private static final double MIN_HUD_CHANGED_FRACTION = 0.0015;
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
    private static int worldRenderFrames;
    private static int hudRenderFrames;
    private static int composerOverlayFrames;

    private ClientTestController() {
    }

    public static void start() {
        if (!Boolean.getBoolean("mightyarchitect.clientTest.enabled") || started)
            return;

        started = true;
        deleteResult();
        TheMightyArchitect.logger.info("[CLIENT-TEST] Starting automated client test");
        ClientTickEvent.CLIENT_POST.register(ClientTestController::tick);
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

    private static void tick(Minecraft minecraft) {
        if (stage == Stage.FINISHED)
            return;

        try {
            totalTicks++;
            stageTicks++;
            if (totalTicks > MAX_TICKS)
                throw new AssertionError("Timed out in stage " + stage);

            releaseMouse(minecraft);
            switch (stage) {
                case CONNECT -> connect(minecraft);
                case WAIT_FOR_WORLD -> waitForWorld(minecraft);
                case CAPTURE_BASELINE -> captureBaseline(minecraft);
                case START_COMPOSER -> startComposer(minecraft);
                case OPEN_PALETTE -> openPalette(minecraft);
                case CAPTURE_BLUEPRINT -> captureBlueprint(minecraft);
                case CAPTURE_HUD_VISIBLE -> captureHudVisible(minecraft);
                case CAPTURE_HUD_HIDDEN -> captureHudHidden(minecraft);
                case CAPTURE_ALIGN_BASELINE -> captureAlignBaseline(minecraft);
                case CAPTURE_ALIGN_OUTLINE -> captureAlignOutline(minecraft);
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

    private static void connect(Minecraft minecraft) {
        if (minecraft.level != null && minecraft.player != null) {
            advance(Stage.WAIT_FOR_WORLD);
            return;
        }
        if (stageTicks < 20)
            return;

        String server = System.getProperty("mightyarchitect.clientTest.server", "127.0.0.1:25565");
        ServerAddress address = ServerAddress.parseString(server);
        ServerData data = new ServerData("Mighty Architect Client Test", server, ServerData.Type.OTHER);
        TheMightyArchitect.logger.info("[CLIENT-TEST] Connecting to {}", server);
        ConnectScreen.startConnecting(minecraft.screen, minecraft, address, data, false, null);
        advance(Stage.WAIT_FOR_WORLD);
    }

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

        PaletteDefinition palette = PaletteDefinition.defaultPalette().clone();
        palette.setName("Client Test Palette");
        PaletteDefinition roundTrip = PaletteDefinition.fromNBT(palette.writeToNBT(new CompoundTag()));
        check("Client Test Palette".equals(roundTrip.getName()), "palette NBT name round-tripped");
        check(palette.get(Palette.ROOF_PRIMARY).equals(roundTrip.get(Palette.ROOF_PRIMARY)),
            "palette NBT block state round-tripped");
        advance(Stage.CAPTURE_BASELINE);
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
            minecraft.setScreen(new PalettePickerScreen());
            return;
        }
        if (stageTicks < 3)
            return;

        check(minecraft.screen instanceof PalettePickerScreen, "palette picker opened");
        minecraft.setScreen(null);
        advance(Stage.CAPTURE_BLUEPRINT);
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
            minecraft.gui.getChat().clearMessages(true);
            ArchitectMenuScreen menu = ArchitectManagerAccessor.getMenu();
            menu.setFocused(false);
            menu.setVisible(true);
            menu.updateContents();
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
            ArchitectManagerAccessor.getMenu().setVisible(false);
            return;
        }
        if (stageTicks < 45)
            return;
        Path captured = awaitScreenshot(minecraft);
        if (captured == null)
            return;

        ImageDiff diff = diffImages(hudVisibleScreenshot, captured);
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
        advance(Stage.VERIFY_RENDER);
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
        Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(), message -> {
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
        return diffImages(first, second, 0.0, 1.0);
    }

    /**
     * Compares two frames within the given fractional region and reports how much changed
     * and where the change is centred. Both the region bounds and the returned centroid are
     * fractions of the full frame, so results are independent of window size.
     */
    private static ImageDiff diffImages(Path first, Path second, double regionMin, double regionMax) {
        try {
            BufferedImage before = ImageIO.read(first.toFile());
            BufferedImage after = ImageIO.read(second.toFile());
            if (before == null || after == null)
                throw new IllegalStateException("ImageIO could not decode " + first + " or " + second);

            int width = Math.min(before.getWidth(), after.getWidth());
            int height = Math.min(before.getHeight(), after.getHeight());
            int minX = (int) (width * regionMin);
            int maxX = (int) (width * regionMax);
            int minY = (int) (height * regionMin);
            int maxY = (int) (height * regionMax);
            long changed = 0;
            long samples = 0;
            double sumX = 0;
            double sumY = 0;
            for (int y = minY; y < maxY; y += 2) {
                for (int x = minX; x < maxX; x += 2) {
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
                }
            }
            if (samples == 0)
                throw new IllegalStateException("Screenshots contained no comparable pixels");
            if (changed == 0)
                return new ImageDiff(0, 0.0, Double.NaN, Double.NaN);
            return new ImageDiff((int) changed, changed / (double) samples, sumX / changed, sumY / changed);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compare screenshots " + first + " and " + second,
                exception);
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
                             double centroidY) {
    }
}
