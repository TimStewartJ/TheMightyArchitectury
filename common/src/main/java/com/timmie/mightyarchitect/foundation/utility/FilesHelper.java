package com.timmie.mightyarchitect.foundation.utility;

import com.timmie.mightyarchitect.TheMightyArchitect;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;

/**
 * Filesystem plumbing shared by the storage layer.
 * <p>
 * The NBT-to-JSON helpers that used to live here - {@code saveTagCompoundAsJson} and
 * {@code loadJsonAsNBT} - are gone; they wrote SNBT through a lenient JSON parser and read it back
 * the same way. That job now belongs to
 * {@link com.timmie.mightyarchitect.control.storage.JsonStorage}, which has a schema to check
 * against. What is left here is the part that was always correct: the atomic write, and turning
 * user-supplied names into safe path elements.
 */
public class FilesHelper {

	/** Names Windows refuses as a file, with or without an extension. */
	private static final Set<String> RESERVED_NAMES = Set.of("con", "prn", "aux", "nul", "com1", "com2", "com3",
		"com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7",
		"lpt8", "lpt9");

	public static void createFolderIfMissing(Path folder) {
		if (Files.isDirectory(folder))
			return;
		try {
			// createDirectories, not createDirectory: callers pass nested paths like
			// <root>/themes/export, and createDirectory fails outright when the parent is
			// missing - after which the write it was preparing for silently does nothing.
			Files.createDirectories(folder);
		} catch (IOException e) {
			TheMightyArchitect.logger.warn("Could not create Folder: " + folder, e);
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

	/** @return a filename below that folder which is not taken yet */
	public static String findFirstValidFilename(String name, Path folder, String extension) {
		String slug = slugOr(name, "unnamed");
		int index = 0;
		String filename;
		do {
			filename = slug + ((index == 0) ? "" : "_" + index) + "." + extension;
			index++;
		} while (Files.exists(folder.resolve(filename)));
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

}
