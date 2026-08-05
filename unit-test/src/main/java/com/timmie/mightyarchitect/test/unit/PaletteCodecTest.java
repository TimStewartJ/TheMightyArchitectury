package com.timmie.mightyarchitect.test.unit;

import com.mojang.serialization.JsonOps;
import com.timmie.mightyarchitect.control.palette.Palette;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import com.timmie.mightyarchitect.test.unit.MinecraftBootstrap.Bootstrapped;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Palette files used to be written by pushing SNBT text through a <i>lenient</i> JSON parser and
 * read back by pushing the JSON text through the SNBT parser. It worked by coincidence - SNBT is
 * not JSON - and it silently coerced types on the way, with nothing anywhere declaring what a field
 * was meant to be.
 * <p>
 * These assertions pin the two things that replacing it with a codec had to preserve: the bytes on
 * disk are unchanged, so every palette anyone already has still loads; and one bad entry still
 * costs that entry rather than the whole file, which is what a strict codec would have done and
 * would have been a regression rather than a fix.
 */
@Bootstrapped
@DisplayName("Palette serialization")
class PaletteCodecTest {

	private static Optional<PaletteDefinition> readJson(String json) {
		return JsonStorage.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
			PaletteDefinition.CODEC, "a test palette");
	}

	private static String writeJson(PaletteDefinition palette) {
		return JsonStorage.encode(PaletteDefinition.CODEC, palette)
			.orElseThrow()
			.toString();
	}

	@Test
	@DisplayName("a JSON round trip keeps the name and every entry")
	void jsonRoundTrip() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.setName("Unit Test Palette");
		palette.put(Palette.ROOF_PRIMARY, Blocks.GOLD_BLOCK);

		PaletteDefinition roundTrip = readJson(writeJson(palette)).orElseThrow();

		assertEquals("Unit Test Palette", roundTrip.getName());
		for (Palette key : Palette.values())
			assertEquals(palette.get(key), roundTrip.get(key), "the round trip lost the entry for " + key);
	}

	/**
	 * The exact shape of every palette file the mod has ever written, including the 21 that ship
	 * inside it. If this stops decoding, everyone's palettes are gone.
	 */
	@Test
	@DisplayName("reads the shape written by every previous release")
	void readsTheLegacyShape() {
		PaletteDefinition palette = readJson("""
			{
			  "Palette": {
			    "ROOF_PRIMARY": { "Name": "minecraft:gold_block" },
			    "INNER_SECONDARY": {
			      "Properties": { "axis": "z" },
			      "Name": "minecraft:stripped_spruce_log"
			    },
			    "Name": "Standard Medieval"
			  }
			}
			""").orElseThrow();

		assertEquals("Standard Medieval", palette.getName());
		assertEquals(Blocks.GOLD_BLOCK.defaultBlockState(), palette.get(Palette.ROOF_PRIMARY));
		assertEquals(Direction.Axis.Z, palette.get(Palette.INNER_SECONDARY)
			.getValue(BlockStateProperties.AXIS),
			"a block state property did not survive, so the palette lost its variant");
	}

	@Test
	@DisplayName("entries the file does not mention keep the standard block")
	void unmentionedEntriesFallBackToTheDefault() {
		PaletteDefinition palette = readJson("{\"Palette\":{\"Name\":\"Sparse\"}}").orElseThrow();

		assertEquals(PaletteDefinition.defaultPalette()
			.get(Palette.INNER_PRIMARY), palette.get(Palette.INNER_PRIMARY));
	}

	/**
	 * The reason the entry codec has an "anything else" arm. A palette written while another mod
	 * was installed still names that mod's blocks after it is removed, and the previous reader
	 * resolved those to air and carried on.
	 */
	@Test
	@DisplayName("one unreadable entry costs that entry, not the whole palette")
	void oneBadEntryDoesNotLoseThePalette() {
		PaletteDefinition palette = readJson("""
			{
			  "Palette": {
			    "Name": "Half Broken",
			    "ROOF_PRIMARY": { "Name": "somemod:blastproof_thatch" },
			    "FLOOR": { "Name": "minecraft:gold_block" }
			  }
			}
			""").orElseThrow();

		assertEquals("Half Broken", palette.getName());
		assertEquals(Blocks.GOLD_BLOCK.defaultBlockState(), palette.get(Palette.FLOOR),
			"a later entry was dropped because an earlier one named a missing block");
	}

	@Test
	@DisplayName("a slot name this build does not have costs that slot only")
	void unknownSlotNamesAreSkipped() {
		PaletteDefinition palette = readJson("""
			{
			  "Palette": {
			    "Name": "From The Future",
			    "GLASS_ROOF_TRIM": { "Name": "minecraft:gold_block" },
			    "FLOOR": { "Name": "minecraft:gold_block" }
			  }
			}
			""").orElseThrow();

		assertEquals("From The Future", palette.getName());
		assertEquals(Blocks.GOLD_BLOCK.defaultBlockState(), palette.get(Palette.FLOOR));
	}

	/**
	 * The one assertion that has to hold on all thirteen Minecraft versions rather than just this
	 * one: {@code BlockState.CODEC} now writes what {@code NbtUtils.writeBlockState} used to, and
	 * if any version ever disagrees the palette format silently forks.
	 */
	@Test
	@DisplayName("a block state encodes to exactly what the previous writer produced")
	void blockStateEncodingIsUnchanged() {
		BlockState withProperties = Blocks.OAK_LOG.defaultBlockState()
			.setValue(BlockStateProperties.AXIS, Direction.Axis.Z);

		for (BlockState state : new BlockState[] { Blocks.GOLD_BLOCK.defaultBlockState(), withProperties }) {
			CompoundTag expected = NbtUtils.writeBlockState(state);
			CompoundTag actual = JsonStorage.toNbt(BlockState.CODEC, state)
				.orElseThrow();
			assertEquals(expected, actual, "BlockState.CODEC and NbtUtils.writeBlockState disagree on " + state);
		}
	}

	@Test
	@DisplayName("written files carry a schema version, and reading one back ignores it")
	void carriesASchemaVersion() {
		assertEquals(1, JsonStorage.DATA_VERSION);

		// The writer stamps it after encoding, so it has to survive being handed back to a codec
		// that knows nothing about it.
		String stamped = "{\"" + JsonStorage.DATA_VERSION_KEY + "\":1,\"Palette\":{\"Name\":\"Versioned\"}}";
		PaletteDefinition palette = readJson(stamped).orElseThrow();

		assertEquals("Versioned", palette.getName());
	}

	@Test
	@DisplayName("nothing decodes to null, whatever the file said")
	void nothingDecodesToNull() {
		assertNotNull(PaletteDefinition.fromNBT(null));
		assertNotNull(PaletteDefinition.fromNBT(new CompoundTag()));
		assertTrue(readJson("{\"Palette\":{}}").isPresent());
	}

	@Test
	@DisplayName("the NBT and JSON forms of a palette agree")
	void nbtAndJsonAgree() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.setName("Both Ways");
		palette.put(Palette.WINDOW, Blocks.GOLD_BLOCK);

		PaletteDefinition viaJson = readJson(writeJson(palette)).orElseThrow();
		PaletteDefinition viaNbt = PaletteDefinition.fromNBT(palette.writeToNBT(new CompoundTag()));

		assertEquals(viaJson.getName(), viaNbt.getName());
		for (Palette key : Palette.values())
			assertEquals(viaJson.get(key), viaNbt.get(key), "the two formats disagree on " + key);
	}

	@Test
	@DisplayName("the encoded tree is a JSON object, not stringified NBT")
	void encodesRealJson() {
		PaletteDefinition palette = PaletteDefinition.defaultPalette()
			.clone();
		palette.setName("Real JSON");

		assertTrue(JsonStorage.encode(PaletteDefinition.CODEC, palette)
			.orElseThrow()
			.getAsJsonObject()
			.getAsJsonObject("Palette")
			.get("Name")
			.getAsJsonPrimitive()
			.isString(), "the palette name did not come out as a JSON string");

		// Round-tripped through the ops the files actually use rather than through toString().
		assertEquals("Real JSON", PaletteDefinition.CODEC
			.parse(JsonOps.INSTANCE, JsonStorage.encode(PaletteDefinition.CODEC, palette)
				.orElseThrow())
			.result()
			.orElseThrow()
			.getName());
	}
}
