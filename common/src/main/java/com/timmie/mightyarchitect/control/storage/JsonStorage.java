package com.timmie.mightyarchitect.control.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.foundation.utility.FilesHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
//? if >=1.20.3 {
import net.minecraft.nbt.NbtAccounter;
//?} else {
/*
*///?}

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Reads and writes the mod's files through {@link Codec}s.
 * <p>
 * The previous implementation wrote {@code JsonParser.parseString(compound.toString())} and read
 * {@code TagParser.parseTag(element.toString())}: SNBT text pushed through a <i>lenient</i> JSON
 * parser and back. SNBT is not JSON - {@code 1b}, {@code 3.0f} and {@code [I;1,2,3]} are all valid
 * SNBT and none of them are valid JSON - so that only ever worked because the parser was lenient,
 * and where it did work it silently changed types: a byte round-tripped into the string
 * {@code "1b"}. Nothing declared what any field was supposed to be, so nothing could notice.
 * <p>
 * A codec declares the schema, which is what makes the conversion total instead of accidental. The
 * same codec drives {@link JsonOps} for the files and {@link NbtOps} for the packed {@code .theme}
 * archive, so the two formats cannot drift apart.
 * <p>
 * Files written from here carry {@value #DATA_VERSION_KEY}. Nothing reads it yet - every schema
 * this replaces happens to be expressible unchanged, so v1 files and pre-versioning files are the
 * same bytes - but a future shape change needs a way to tell them apart that is not guesswork, and
 * that has to be written before it is needed rather than after.
 */
public final class JsonStorage {

	public static final String DATA_VERSION_KEY = "DataVersion";
	public static final int DATA_VERSION = 1;

	private JsonStorage() {
	}

	/**
	 * Encodes a value and writes it, replacing the target only once it is complete.
	 *
	 * @return whether the file was written
	 */
	public static <T> boolean write(Path target, Codec<T> codec, T value) {
		return write(target, codec, value, "  ");
	}

	/** {@link #write}, without the indentation - used for the single-file theme export. */
	public static <T> boolean writeCompact(Path target, Codec<T> codec, T value) {
		return write(target, codec, value, "");
	}

	private static <T> boolean write(Path target, Codec<T> codec, T value, String indent) {
		Optional<JsonElement> encoded = encode(codec, value);
		if (!encoded.isPresent()) {
			TheMightyArchitect.logger.error("Refusing to write {}: it could not be encoded", target);
			return false;
		}

		JsonElement json = encoded.get();
		if (json.isJsonObject())
			json.getAsJsonObject()
				.addProperty(DATA_VERSION_KEY, DATA_VERSION);

		return FilesHelper.writeAtomically(target, out -> {
			try (Writer stream = new OutputStreamWriter(out, StandardCharsets.UTF_8);
				JsonWriter writer = new JsonWriter(stream)) {
				writer.setIndent(indent);
				Streams.write(json, writer);
			}
		});
	}

	/** @return the encoded tree, or empty after logging why it could not be produced */
	public static <T> Optional<JsonElement> encode(Codec<T> codec, T value) {
		return codec.encodeStart(JsonOps.INSTANCE, value)
			.resultOrPartial(error -> TheMightyArchitect.logger.error("Could not encode: {}", error));
	}

	/**
	 * Reads and decodes a file.
	 *
	 * @return the value, or empty when the file is missing, unreadable or does not match the schema
	 */
	public static <T> Optional<T> read(Path path, Codec<T> codec) {
		if (!Files.exists(path))
			return Optional.empty();
		try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
			return read(in, codec, path.toString());
		} catch (IOException e) {
			TheMightyArchitect.logger.error("Could not open " + path, e);
			return Optional.empty();
		}
	}

	/** Reads and decodes one of the files that ship inside the mod. */
	public static <T> Optional<T> readBuiltIn(String resourcePath, Codec<T> codec) {
		Optional<InputStream> stream = ArchitectResources.open(resourcePath);
		if (!stream.isPresent())
			return Optional.empty();
		try (InputStream in = stream.get()) {
			return read(in, codec, resourcePath);
		} catch (IOException e) {
			TheMightyArchitect.logger.error("Could not read the built-in " + resourcePath, e);
			return Optional.empty();
		}
	}

	/**
	 * Decodes a stream.
	 * <p>
	 * The reader stays lenient so that the files the mod has already written - which went through
	 * the SNBT detour and can carry unquoted keys - still parse. What is no longer lenient is what
	 * happens afterwards: the codec, not the parser, decides what each field means.
	 */
	public static <T> Optional<T> read(InputStream in, Codec<T> codec, String describedAs) {
		JsonElement element;
		try (JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))) {
			reader.setLenient(true);
			element = Streams.parse(reader);
		} catch (Exception e) {
			// Exception, not IOException: Gson throws unchecked parse errors, and one malformed
			// file must not abort the load of every other one.
			TheMightyArchitect.logger.error("Could not parse " + describedAs, e);
			return Optional.empty();
		}

		if (element == null || element.isJsonNull())
			return Optional.empty();

		if (element.isJsonObject())
			element.getAsJsonObject()
				.remove(DATA_VERSION_KEY);

		return decode(element, codec, describedAs);
	}

	/** @return the decoded value, or empty after logging what the schema rejected */
	public static <T> Optional<T> decode(JsonElement element, Codec<T> codec, String describedAs) {
		DataResult<T> result = codec.parse(JsonOps.INSTANCE, element);
		return result
			.resultOrPartial(error -> TheMightyArchitect.logger.error("Could not read {}: {}", describedAs, error));
	}

	/** Encodes a value to NBT with the same codec the JSON files use. */
	public static <T> Optional<CompoundTag> toNbt(Codec<T> codec, T value) {
		return codec.encodeStart(NbtOps.INSTANCE, value)
			.resultOrPartial(error -> TheMightyArchitect.logger.error("Could not encode to NBT: {}", error))
			.filter(CompoundTag.class::isInstance)
			.map(CompoundTag.class::cast);
	}

	/** Decodes a value from NBT with the same codec the JSON files use. */
	public static <T> Optional<T> fromNbt(Codec<T> codec, Tag tag, String describedAs) {
		if (tag == null)
			return Optional.empty();
		return codec.parse(NbtOps.INSTANCE, tag)
			.resultOrPartial(error -> TheMightyArchitect.logger.error("Could not read {}: {}", describedAs, error));
	}

	/**
	 * Reads a gzipped NBT archive - the single-file {@code .theme} export.
	 * <p>
	 * The size guard {@code NbtIo.readCompressed} takes arrived in 1.20.3, which is the only reason
	 * this is guarded. Both arms are unbounded because the file is one the user chose to import.
	 *
	 * @return the archive, or empty when it is missing or unreadable
	 */
	public static Optional<CompoundTag> readCompressed(Path path) {
		if (!Files.exists(path))
			return Optional.empty();
		try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
			//? if >=1.20.3 {
			return Optional.ofNullable(NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
			//?} else {
			/*return Optional.ofNullable(NbtIo.readCompressed(in));
			*///?}
		} catch (IOException e) {
			TheMightyArchitect.logger.error("Could not read " + path, e);
			return Optional.empty();
		}
	}

	/** Writes a gzipped NBT archive, replacing the target only once it is complete. */
	public static boolean writeCompressed(Path target, CompoundTag tag) {
		return FilesHelper.writeAtomically(target, (OutputStream out) -> NbtIo.writeCompressed(tag, out));
	}
}
