package com.timmie.mightyarchitect.foundation.utility;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;

public class FilesHelper {

	/** Names Windows refuses as a file, with or without an extension. */
	private static final Set<String> RESERVED_NAMES = Set.of("con", "prn", "aux", "nul", "com1", "com2", "com3",
		"com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7",
		"lpt8", "lpt9");

	public static void createFolderIfMissing(String name) {
		if (!Files.isDirectory(Paths.get(name))) {
			try {
				// createDirectories, not createDirectory: every caller here passes a nested path
				// like "themes/export", and createDirectory fails outright when the parent is
				// missing - after which the write it was preparing for silently does nothing.
				Files.createDirectories(Paths.get(name));
			} catch (IOException e) {
				TheMightyArchitect.logger.warn("Could not create Folder: " + name, e);
			}
		}
	}

	/** The body of a write, run against a stream that only becomes the real file once it succeeds. */
	@FunctionalInterface
	public interface StreamWriter {
		void writeTo(OutputStream out) throws IOException;
	}

	/**
	 * Writes a file without ever leaving a half-written one behind.
	 * <p>
	 * The previous pattern here was delete, open, write, close - so a crash anywhere in between
	 * destroyed the old file and produced nothing in its place. Themes and palettes are hours of
	 * user work, so instead the content goes to a temporary sibling and only replaces the target
	 * once it is complete and closed, which the filesystem does atomically.
	 *
	 * @return whether the file was written
	 */
	public static boolean writeAtomically(Path target, StreamWriter body) {
		Path directory = target.toAbsolutePath()
			.getParent();
		Path temp = target.resolveSibling(target.getFileName() + ".tmp");
		try {
			if (directory != null)
				Files.createDirectories(directory);

			try (OutputStream out = Files.newOutputStream(temp, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
				body.writeTo(out);
			}

			try {
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException unsupported) {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;

		} catch (IOException e) {
			TheMightyArchitect.logger.error("Could not write " + target, e);
			try {
				Files.deleteIfExists(temp);
			} catch (IOException cleanup) {
				TheMightyArchitect.logger.warn("Could not remove the leftover " + temp, cleanup);
			}
			return false;
		}
	}

	public static String findFirstValidFilename(String name, String folderPath, String extension) {
		String slug = slugOr(name, "unnamed");
		int index = 0;
		String filename;
		String filepath;
		do {
			filename = slug + ((index == 0) ? "" : "_" + index) + "." + extension;
			index++;
			filepath = folderPath + "/" + filename;
		} while (Files.exists(Paths.get(filepath)));
		return filename;
	}

	/**
	 * Reduces a user-supplied name to something safe to use as a single path element.
	 * <p>
	 * These names come from sign text and text prompts and are pasted straight into a path, so
	 * anything outside {@code [a-z0-9_]} is dropped rather than escaped - that removes directory
	 * separators, {@code ..}, drive colons, wildcards and control characters in one pass instead of
	 * trying to enumerate them. Windows also reserves a handful of device names, which get a
	 * suffix rather than being rejected.
	 *
	 * @return the sanitized name, which is empty when nothing usable survived
	 */
	public static String slug(String name) {
		StringBuilder builder = new StringBuilder(name.length());
		for (char c : name.toLowerCase(Locale.ROOT)
			.toCharArray()) {
			if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')
				builder.append(c);
			else if (c == ' ' || c == '-' || c == '.')
				builder.append('_');
		}

		while (builder.length() > 0 && builder.charAt(0) == '_')
			builder.deleteCharAt(0);
		while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '_')
			builder.deleteCharAt(builder.length() - 1);

		String slug = builder.toString();
		if (slug.length() > 64)
			slug = slug.substring(0, 64);
		return RESERVED_NAMES.contains(slug) ? slug + "_" : slug;
	}

	/** {@link #slug(String)}, with a fallback for names that sanitize away to nothing. */
	public static String slugOr(String name, String fallback) {
		String slug = slug(name);
		return slug.isEmpty() ? fallback : slug;
	}

	public static boolean saveTagCompoundAsJson(CompoundTag compound, String path) {
		return saveTagCompoundAsJson(compound, path, "  ");
	}

	public static boolean saveTagCompoundAsJsonCompact(CompoundTag compound, String path) {
		return saveTagCompoundAsJson(compound, path, "");
	}

	private static boolean saveTagCompoundAsJson(CompoundTag compound, String path, String indent) {
		return writeAtomically(Paths.get(path), out -> {
			try (Writer stream = new OutputStreamWriter(out, StandardCharsets.UTF_8);
				JsonWriter writer = new JsonWriter(stream)) {
				writer.setIndent(indent);
				Streams.write(JsonParser.parseString(compound.toString()), writer);
			}
		});
	}

	public static CompoundTag loadJsonNBT(InputStream inputStream) {
		if (inputStream == null)
			return null;

		try (JsonReader reader =
			new JsonReader(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))) {
			reader.setLenient(true);
			JsonElement element = Streams.parse(reader);
			//? if >=1.21.6 {
			return TagParser.create(net.minecraft.nbt.NbtOps.INSTANCE).parseCompoundFully(element.toString());
			//?} else {
			/*return TagParser.parseTag(element.toString());
			*///?}

		} catch (Exception e) {
			// Exception rather than the exact types: the parser throws a checked
			// CommandSyntaxException before 1.21.6 and an unchecked parse error after it, and
			// either way one malformed file must not abort the load of every other one.
			TheMightyArchitect.logger.error("Could not read NBT from json", e);
		}
		return null;
	}

	public static CompoundTag loadJsonResourceAsNBT(String filepath) {
		return loadJsonNBT(TheMightyArchitect.class.getClassLoader()
			.getResourceAsStream(filepath));
	}

	/** @return the file's contents, or null if it is missing or unreadable - callers must check. */
	public static CompoundTag loadJsonAsNBT(String filepath) {
		try {
			return loadJsonNBT(Files.newInputStream(Paths.get(filepath), StandardOpenOption.READ));
		} catch (IOException e) {
			TheMightyArchitect.logger.error("Could not open " + filepath, e);
		}
		return null;
	}

}
