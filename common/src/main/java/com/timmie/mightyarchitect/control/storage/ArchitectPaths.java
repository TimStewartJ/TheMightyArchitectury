package com.timmie.mightyarchitect.control.storage;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Where the mod keeps user data.
 * <p>
 * Everything used to be written with {@code Paths.get("themes/")} and friends, which is relative to
 * the process working directory rather than to the game - so the mod's three folders landed
 * wherever the launcher happened to start Java, and on a launcher that does not chdir into the
 * instance they were not next to the world saves at all. Paths now hang off
 * {@link Minecraft#gameDirectory} (identical on every version in the matrix, checked with javap)
 * and share one {@value #FOLDER} folder instead of scattering three generic names into the
 * instance root.
 * <p>
 * <b>The old data still exists</b>, because the theme screen has been telling people to drop theme
 * files into that folder for years. Moving where data lives without carrying it across is how you
 * orphan hours of someone's work, so {@link #migrateLegacyData()} copies it and
 * {@link #readRoots(String)} keeps reading the old place afterwards. Nothing is ever deleted or
 * overwritten there: a botched migration has to stay recoverable.
 */
public final class ArchitectPaths {

	public static final String FOLDER = "mightyarchitect";

	public static final String THEMES = "themes";
	public static final String PALETTES = "palettes";
	public static final String SCHEMATICS = "schematics";
	public static final String THEME_EXPORTS = "themes/export";

	/** Written into the new root once the copy has run, so it runs once rather than every load. */
	private static final String MIGRATION_MARKER = ".migrated-v1";

	private static Path rootOverride;
	private static Path legacyRootOverride;
	private static boolean migrationAttempted;

	private ArchitectPaths() {
	}

	/**
	 * Points the whole storage layer at a scratch directory.
	 * <p>
	 * Exists so the path and migration logic can be asserted without a game: passing null restores
	 * the real, client-derived locations.
	 */
	public static synchronized void setRootsForTesting(Path root, Path legacyRoot) {
		rootOverride = root;
		legacyRootOverride = legacyRoot;
		migrationAttempted = false;
	}

	/**
	 * The instance folder, or the working directory when there is no client to ask.
	 * <p>
	 * A dedicated server never reaches this - the composer is client-side - but the unit suite does,
	 * and it has no {@code Minecraft} instance.
	 */
	public static Path gameDirectory() {
		Minecraft client = clientOrNull();
		return client == null ? Paths.get("") : client.gameDirectory.toPath();
	}

	/** The mod's own folder, {@code <game directory>/mightyarchitect}. */
	public static synchronized Path root() {
		if (rootOverride != null)
			return rootOverride;
		return gameDirectory().resolve(FOLDER);
	}

	/** Where the mod's folders used to be: loose in the instance root. */
	public static synchronized Path legacyRoot() {
		if (legacyRootOverride != null)
			return legacyRootOverride;
		return gameDirectory();
	}

	public static Path themes() {
		return resolve(THEMES);
	}

	public static Path palettes() {
		return resolve(PALETTES);
	}

	public static Path schematics() {
		return resolve(SCHEMATICS);
	}

	public static Path themeExports() {
		return resolve(THEME_EXPORTS);
	}

	/** Resolves a slash-separated path below the mod folder, migrating the old data first. */
	public static Path resolve(String relative) {
		migrateLegacyData();
		return resolveIn(root(), relative);
	}

	/**
	 * Every directory a scan should look in, best first.
	 * <p>
	 * The migration copies rather than moves, so the legacy folder normally holds nothing new - but
	 * a copy that partially failed, or a file dropped into the old folder afterwards by someone
	 * following an old tutorial, would otherwise be invisible. Callers merge these in order and
	 * keep the first entry for any given name, so the new root always wins.
	 */
	public static List<Path> readRoots(String relative) {
		migrateLegacyData();
		List<Path> roots = new ArrayList<>(2);
		roots.add(resolveIn(root(), relative));

		Path legacy = resolveIn(legacyRoot(), relative);
		if (!legacy.equals(roots.get(0)) && Files.isDirectory(legacy))
			roots.add(legacy);

		return roots;
	}

	/**
	 * Lists the entries of a folder across {@link #readRoots(String)}, first occurrence winning.
	 *
	 * @return the entries, which is empty rather than null when nothing is readable
	 */
	public static List<Path> listAcrossRoots(String relative) {
		List<Path> entries = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();

		for (Path root : readRoots(relative)) {
			if (!Files.isDirectory(root))
				continue;
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
				for (Path entry : stream)
					if (seen.add(entry.getFileName()
						.toString()))
						entries.add(entry);
			} catch (IOException e) {
				TheMightyArchitect.logger.error("Could not list " + root, e);
			}
		}

		return entries;
	}

	/**
	 * Resolves a file across {@link #readRoots(String)}.
	 *
	 * @return the first existing candidate, or the one below the mod folder when none exists
	 */
	public static Path findAcrossRoots(String relative, String filename) {
		List<Path> roots = readRoots(relative);
		for (Path root : roots) {
			Path candidate = root.resolve(filename);
			if (Files.exists(candidate))
				return candidate;
		}
		return roots.get(0)
			.resolve(filename);
	}

	/**
	 * Copies the pre-{@value #FOLDER} folders into the mod folder, once.
	 * <p>
	 * Copy, not move, and never over an existing file: if this goes wrong the user's themes are
	 * still exactly where they were. The marker means a later launch does not resurrect a theme
	 * somebody has since deleted from the new folder.
	 */
	public static synchronized void migrateLegacyData() {
		if (migrationAttempted)
			return;
		migrationAttempted = true;

		Path root = root();
		Path legacy = legacyRoot();
		if (root.equals(legacy))
			return;

		try {
			if (Files.exists(root.resolve(MIGRATION_MARKER)))
				return;

			int copied = 0;
			for (String folder : new String[] { THEMES, PALETTES, SCHEMATICS })
				copied += copyTree(legacy.resolve(folder), root.resolve(folder));

			Files.createDirectories(root);
			Files.write(root.resolve(MIGRATION_MARKER), new byte[0]);

			if (copied > 0)
				TheMightyArchitect.logger.info(
					"Copied {} file(s) from {} into {}. The originals were left untouched.", copied, legacy, root);

		} catch (IOException e) {
			// Deliberately not fatal, and deliberately not retried: the old folders are still
			// intact and readable through readRoots, so the worst case is that the mod keeps
			// loading from where it always did.
			TheMightyArchitect.logger.error("Could not copy the previous data folders into " + root, e);
		}
	}

	/** @return how many files were copied */
	private static int copyTree(Path from, Path to) throws IOException {
		if (!Files.isDirectory(from))
			return 0;

		int copied = 0;
		Files.createDirectories(to);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(from)) {
			for (Path entry : stream) {
				Path target = to.resolve(entry.getFileName()
					.toString());
				if (Files.isDirectory(entry)) {
					copied += copyTree(entry, target);
				} else if (!Files.exists(target)) {
					Files.copy(entry, target, StandardCopyOption.COPY_ATTRIBUTES);
					copied++;
				}
			}
		}
		return copied;
	}

	private static Path resolveIn(Path root, String relative) {
		Path resolved = root;
		for (String element : relative.split("/"))
			if (!element.isEmpty())
				resolved = resolved.resolve(element);
		return resolved;
	}

	/**
	 * @return the running client, or null off-game
	 */
	static Minecraft clientOrNull() {
		try {
			return Minecraft.getInstance();
		} catch (Throwable noClient) {
			// The unit suite has no client at all; loading the class is enough to fail there, so
			// this cannot be a plain null check.
			return null;
		}
	}
}
