package com.timmie.mightyarchitect.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.timmie.mightyarchitect.AllBlocks;
import com.timmie.mightyarchitect.AllItems;
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
import com.timmie.mightyarchitect.gui.PalettePickerScreen;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

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
        VERIFY_RENDER,
        FINISHED
    }

    private static final int MAX_TICKS = 20 * 60 * 6;
    private static final List<String> checks = new ArrayList<>();
    private static Stage stage = Stage.CONNECT;
    private static int totalTicks;
    private static int stageTicks;
    private static boolean started;
    private static boolean screenshotPending;
    private static Path completedScreenshot;
    private static Set<Path> screenshotFilesBefore = Set.of();
    private static Path baselineScreenshot;
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

            switch (stage) {
                case CONNECT -> connect(minecraft);
                case WAIT_FOR_WORLD -> waitForWorld(minecraft);
                case CAPTURE_BASELINE -> captureBaseline(minecraft);
                case START_COMPOSER -> startComposer(minecraft);
                case OPEN_PALETTE -> openPalette(minecraft);
                case CAPTURE_BLUEPRINT -> captureBlueprint(minecraft);
                case VERIFY_RENDER -> verifyRender(minecraft);
                case FINISHED -> {
                }
            }
        } catch (Throwable throwable) {
            fail(minecraft, throwable);
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
        if (!screenshotPending && completedScreenshot == null) {
            requestScreenshot(minecraft);
            return;
        }
        if (completedScreenshot == null)
            return;

        baselineScreenshot = completedScreenshot;
        completedScreenshot = null;
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

    private static void captureBlueprint(Minecraft minecraft) {
        if (stageTicks < 60)
            return;
        if (!screenshotPending && completedScreenshot == null) {
            requestScreenshot(minecraft);
            return;
        }
        if (completedScreenshot == null)
            return;

        ImageStats baseline = imageStats(baselineScreenshot);
        ImageStats blueprint = imageStats(completedScreenshot);
        double baselineBlueBias = baseline.blue - (baseline.red + baseline.green) / 2.0;
        double blueprintBlueBias = blueprint.blue - (blueprint.red + blueprint.green) / 2.0;
        check(blueprintBlueBias > baselineBlueBias + 5.0,
            "blueprint frame has stronger blue bias (" + round(baselineBlueBias) + " -> "
                + round(blueprintBlueBias) + ")");
        check(Shaders.Blueprint.isActive(), "blueprint post-chain active");
        advance(Stage.VERIFY_RENDER);
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
            if (completedScreenshot != null)
                result.addProperty("blueprintScreenshot", completedScreenshot.toAbsolutePath().toString());
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
}
