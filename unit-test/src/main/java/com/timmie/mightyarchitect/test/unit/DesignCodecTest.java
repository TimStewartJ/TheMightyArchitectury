package com.timmie.mightyarchitect.test.unit;

import com.timmie.mightyarchitect.control.design.DesignSlice.DesignSliceTrait;
import com.timmie.mightyarchitect.control.design.DesignType;
import com.timmie.mightyarchitect.control.design.partials.Design;
import com.timmie.mightyarchitect.control.design.partials.DesignData;
import com.timmie.mightyarchitect.control.design.partials.Wall;
import com.timmie.mightyarchitect.control.design.partials.Wall.ExpandBehaviour;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading a design used to need four Stonecutter arms for the {@code Size} field alone, because it
 * went through {@code CompoundTag} accessors whose shape changed twice across the matrix, and each
 * arm had to guess which of the two on-disk spellings to expect from the Minecraft version rather
 * than from the file.
 * <p>
 * A codec has one shape everywhere and reads both spellings, so these assertions are the ones that
 * make deleting those arms safe - and they run on all thirteen nodes, which is exactly the coverage
 * guarded arms never had.
 */
@Bootstrapped
@DisplayName("Design serialization")
class DesignCodecTest {

	private static Optional<DesignData> read(String json) {
		return JsonStorage.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), DesignData.CODEC,
			"a test design");
	}

	private static final String TWO_BY_TWO_LAYERS = """
		"Layers": [
		  { "Trait": "Standard", "Blocks": "GD,DA", "Facing": "DA,AA" },
		  { "Trait": "CloneOnce", "Blocks": "G , B", "Facing": "DA,AA" }
		]
		""";

	@Test
	@DisplayName("reads the object spelling of Size that every shipped design uses")
	void readsTheObjectSize() {
		DesignData design = read("{ \"Size\": { \"X\": 2, \"Y\": 5, \"Z\": 3 }, " + TWO_BY_TWO_LAYERS + "}")
			.orElseThrow();

		assertEquals(new BlockPos(2, 5, 3), design.size());
	}

	@Test
	@DisplayName("reads the triple spelling of Size that newer exports use")
	void readsTheTripleSize() {
		DesignData design = read("{ \"Size\": [2, 5, 3], " + TWO_BY_TWO_LAYERS + "}").orElseThrow();

		assertEquals(new BlockPos(2, 5, 3), design.size());
	}

	@Test
	@DisplayName("writes the triple spelling, the same one on every version")
	void writesTheTripleSize() {
		DesignData design = new DesignData(new BlockPos(2, 5, 3), List.of(), 0, 0, 0, ExpandBehaviour.None);

		assertTrue(JsonStorage.encode(DesignData.CODEC, design)
			.orElseThrow()
			.getAsJsonObject()
			.get("Size")
			.isJsonArray(), "Size was not written as an int triple");
	}

	@Test
	@DisplayName("a round trip keeps the layers")
	void roundTripKeepsLayers() {
		DesignData original = read("{ \"Size\": [2, 2, 2], " + TWO_BY_TWO_LAYERS + "}").orElseThrow();
		DesignData roundTrip = read(JsonStorage.encode(DesignData.CODEC, original)
			.orElseThrow()
			.toString()).orElseThrow();

		assertEquals(original.size(), roundTrip.size());
		assertEquals(original.layers(), roundTrip.layers());
		assertEquals(DesignSliceTrait.CloneOnce, roundTrip.layers()
			.get(1)
			.trait());
		assertEquals(Optional.of("DA,AA"), roundTrip.layers()
			.get(0)
			.facing());
	}

	/**
	 * A facade never writes {@code ExpandBehaviour}, and the previous reader ran
	 * {@code ExpandBehaviour.valueOf} on the empty string it got back for the missing key - which
	 * threw, on every version below 1.21.6.
	 */
	@Test
	@DisplayName("a design with no ExpandBehaviour reads as None instead of throwing")
	void missingExpandBehaviourIsNone() {
		DesignData design = read("{ \"Size\": [1, 1, 1], \"Layers\": [] }").orElseThrow();

		assertEquals(ExpandBehaviour.None, design.expandBehaviour());

		Design wall = new Wall().fromData(design);
		assertNotNull(wall);
	}

	@Test
	@DisplayName("an unknown slice trait costs that slice its trait, not the theme its designs")
	void unknownTraitDegrades() {
		DesignData design =
			read("{ \"Size\": [1, 1, 1], \"Layers\": [ { \"Trait\": \"HoverAbove\", \"Blocks\": \"A\" } ] }")
				.orElseThrow();

		assertEquals(DesignSliceTrait.Standard, design.layers()
			.get(0)
			.trait());
	}

	@Test
	@DisplayName("a slice with no Facing reads, and one with a short Facing does not throw")
	void facingIsOptionalAndTolerant() {
		DesignData noFacing = read("{ \"Size\": [2, 1, 1], \"Layers\": [ { \"Blocks\": \"GD\" } ] }").orElseThrow();
		assertFalse(noFacing.layers()
			.get(0)
			.facing()
			.isPresent());
		assertNotNull(DesignType.WALL.getDesign()
			.fromData(noFacing));

		DesignData shortFacing =
			read("{ \"Size\": [2, 1, 1], \"Layers\": [ { \"Blocks\": \"GD\", \"Facing\": \"D\" } ] }").orElseThrow();
		assertNotNull(DesignType.WALL.getDesign()
			.fromData(shortFacing), "a hand-edited design with a short Facing row took the whole theme down");
	}

	@Test
	@DisplayName("each design type reads only its own extra field")
	void extraFieldsAreReadPerType() {
		DesignData roof = read("{ \"Size\": [5, 1, 1], \"Layers\": [], \"Roofspan\": 7 }").orElseThrow();
		assertEquals(7, roof.roofspan());
		assertEquals(0, roof.radius());
		assertEquals(0, roof.margin());

		DesignData tower = read("{ \"Size\": [5, 1, 1], \"Layers\": [], \"Radius\": 3 }").orElseThrow();
		assertEquals(3, tower.radius());

		DesignData flatRoof = read("{ \"Size\": [5, 1, 1], \"Layers\": [], \"Margin\": 2 }").orElseThrow();
		assertEquals(2, flatRoof.margin());

		DesignData wall =
			read("{ \"Size\": [5, 1, 1], \"Layers\": [], \"ExpandBehaviour\": \"MergedRepeat\" }").orElseThrow();
		assertEquals(ExpandBehaviour.MergedRepeat, wall.expandBehaviour());
	}

	@Test
	@DisplayName("a design that declares more layers than it carries does not throw")
	void truncatedLayersDoNotThrow() {
		DesignData design = read("{ \"Size\": [1, 4, 1], \"Layers\": [ { \"Blocks\": \"A\" } ] }").orElseThrow();

		assertNotNull(DesignType.WALL.getDesign()
			.fromData(design), "a truncated design file escaped as an index out of bounds");
	}

	@Test
	@DisplayName("the extras a design does not use are left out of the file")
	void unusedExtrasAreNotWritten() {
		DesignData roof = new DesignData(new BlockPos(5, 1, 1), List.of(), 7, 0, 0, ExpandBehaviour.None);
		String written = JsonStorage.encode(DesignData.CODEC, roof)
			.orElseThrow()
			.toString();

		assertTrue(written.contains("\"Roofspan\":7"));
		assertFalse(written.contains("Radius"), "a roof was written with a tower's field");
		assertFalse(written.contains("ExpandBehaviour"), "a roof was written with a wall's field");
	}
}
