package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.design.DesignSlice.DesignSliceTrait;
import com.timmie.mightyarchitect.control.design.DesignTheme;
import com.timmie.mightyarchitect.control.design.ExportTools;
import com.timmie.mightyarchitect.control.design.ThemeStorage;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.storage.ArchitectResources;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The theme exporter's tools are vanilla content the mod recognises, so recognition is the whole
 * feature: two traits sharing a block, or a marker that does not read back as its own trait, would
 * export wrong designs with no error at all.
 * <p>
 * Only block states here. An {@code ItemStack} cannot be made in this bootstrap on 26.x - item
 * components are bound when the game loads its data, which a unit test never does - so the wand and
 * the toolkit items are checked in the client test instead.
 */
@Bootstrapped
@DisplayName("ExportTools")
class ExportToolsTest {

	@Test
	@DisplayName("every trait has its own marker block, and reads back as that trait")
	void everyTraitRoundTrips() {
		Set<Block> blocks = new HashSet<>();
		for (DesignSliceTrait trait : DesignSliceTrait.values()) {
			BlockState marker = ExportTools.markerState(trait);

			assertSame(trait, ExportTools.traitOf(marker), trait + " did not read back as itself");
			assertTrue(ExportTools.isMarker(marker));
			assertFalse(ExportTools.isAnchor(marker), trait + "'s marker is also the anchor");
			assertTrue(blocks.add(marker.getBlock()), trait + " shares its block with another trait");
			assertFalse(ExportTools.label(trait)
				.isEmpty());
		}
		assertEquals(DesignSliceTrait.values().length, blocks.size());
	}

	@Test
	@DisplayName("the anchor is not a marker, and ordinary blocks are neither")
	void anchorAndOrdinaryBlocksAreNotMarkers() {
		BlockState anchor = ExportTools.anchorBlock()
			.defaultBlockState();

		assertTrue(ExportTools.isAnchor(anchor));
		assertNull(ExportTools.traitOf(anchor));
		assertNull(ExportTools.traitOf(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
		assertFalse(ExportTools.isAnchor(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
	}

	@Test
	@DisplayName("cycling visits every trait in order and comes back round, both ways")
	void cyclingVisitsEveryTrait() {
		DesignSliceTrait[] traits = DesignSliceTrait.values();
		BlockState marker = ExportTools.markerState(traits[0]);
		for (int i = 1; i <= traits.length; i++) {
			marker = ExportTools.cycledMarker(marker, false);
			assertSame(traits[i % traits.length], ExportTools.traitOf(marker));
		}
		for (int i = traits.length - 1; i >= 0; i--) {
			marker = ExportTools.cycledMarker(marker, true);
			assertSame(traits[i], ExportTools.traitOf(marker));
		}
	}

	@Test
	@DisplayName("cycling keeps the way a marker faces")
	void cyclingKeepsTheFacing() {
		BlockState marker = ExportTools.markerState(DesignSliceTrait.Optional)
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);

		BlockState next = ExportTools.cycledMarker(marker, false);

		assertSame(DesignSliceTrait.MaskAbove, ExportTools.traitOf(next));
		assertEquals(Direction.EAST, next.getValue(BlockStateProperties.HORIZONTAL_FACING));
	}

	@Test
	@DisplayName("a scanner palette using a marker block is refused, and the default is not")
	void reservedBlocksAreRefused() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		assertNull(DesignExporter.reservedBlockInPalette(palette));

		palette.put(Palette.FLOOR, ExportTools.markerBlock(DesignSliceTrait.CloneThrice));

		assertNotNull(DesignExporter.reservedBlockInPalette(palette),
			"a palette with a layer-marker block in it was accepted for scanning");
	}

	/**
	 * The exporter finds a design's marker column by searching outward from the anchor, starting
	 * inside the design itself. That only works because no palette a design can be built from uses
	 * a marker block - so pin it for everything the mod ships.
	 */
	@Test
	@DisplayName("no shipped palette or theme palette uses a marker block")
	void shippedPalettesNeverUseMarkerBlocks() {
		List<String> paths = ArchitectResources.list("palettes", index -> "p" + index + ".json");
		assertFalse(paths.isEmpty());
		for (String path : paths) {
			PaletteDefinition palette = JsonStorage.readBuiltIn(path, PaletteDefinition.CODEC)
				.orElseThrow();
			assertNull(DesignExporter.reservedBlockInPalette(palette), path + " uses a layer-marker block");
		}
		for (ThemeStorage.IncludedThemes which : ThemeStorage.IncludedThemes.values()) {
			DesignTheme theme = ThemeStorage.getIncludedTheme(which);
			assertNull(DesignExporter.reservedBlockInPalette(theme.getDefaultPalette()),
				which + "'s palette uses a layer-marker block");
			assertNull(DesignExporter.reservedBlockInPalette(theme.getDefaultSecondaryPalette()),
				which + "'s secondary palette uses a layer-marker block");
		}
	}
}
