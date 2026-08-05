package com.timmie.mightyarchitect.control.storage;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.IntFunction;

/**
 * The themes and palettes that ship inside the mod.
 * <p>
 * These used to sit at the <i>root</i> of the jar under {@code themes/} and {@code palettes/} and
 * were read with {@code getClassLoader().getResourceAsStream}. Two things were wrong with that.
 * They are generic top-level classpath names, so any other jar declaring
 * {@code themes/medieval/theme.json} could shadow the mod's own copy depending on classpath order;
 * and going round the resource system meant no resource pack could override or add a theme and
 * nothing reloaded on F3+T. They now live under {@code assets/mightyarchitect/} and are read
 * through {@link ResourceManager} like every other asset the mod ships.
 * <p>
 * The classpath is still the fallback, because the unit suite runs with no client at all. It cannot
 * enumerate a directory inside a jar, so {@link #list} degrades there to the numbered probe the mod
 * used before - the old behaviour exactly, and only off-game.
 * <p>
 * Nothing here names {@code ResourceLocation}: the class was renamed to {@code Identifier} in
 * 1.21.11, and {@link TheMightyArchitect#id(String)} already owns that guard.
 */
public final class ArchitectResources {

	/** Where the built-in content lives inside the jar. */
	public static final String CLASSPATH_ROOT = "assets/" + TheMightyArchitect.ID + "/";

	/** Bounds the off-game probe; the largest built-in set is two orders of magnitude below it. */
	private static final int PROBE_LIMIT = 2048;

	/**
	 * A stand-in for the resource stack, deliberately free of Minecraft types.
	 * <p>
	 * The companion test modules are not Stonecutter-processed, so anything they name has to
	 * compile unchanged on every version in the matrix - and {@code ResourceManager} is not that:
	 * its signatures moved from {@code ResourceLocation} to {@code Identifier} at 1.21.11. A seam
	 * with no Minecraft in it lets the discovery rules be asserted off-game; the wiring to the real
	 * manager is what the client matrix covers, on a real client.
	 */
	public interface ResourceIndex {

		/** @return the file's contents, or empty when this index does not provide that path */
		Optional<byte[]> read(String path);

		/**
		 * @return every path below the folder, or empty when this index cannot enumerate - which
		 *         is the honest answer for a plain classloader
		 */
		Optional<List<String>> listAll(String folder);
	}

	private static ResourceIndex indexOverride;

	private ArchitectResources() {
	}

	/** Stands an in-memory resource stack in for the real one, or restores it with null. */
	public static synchronized void setIndexForTesting(ResourceIndex index) {
		indexOverride = index;
	}

	private static synchronized ResourceIndex indexOrNull() {
		return indexOverride;
	}

	/**
	 * Whether a path can even name a built-in resource.
	 * <p>
	 * A resource location only accepts {@code [a-z0-9/._-]}, and an imported theme's folder is
	 * named whatever the user called it on disk - so {@code themes/Nordic Village/...} cannot be
	 * one. That is not an error: it means no built-in file can exist there, which is exactly what
	 * the classloader lookup this replaced reported by returning null. Letting the exception
	 * escape instead would crash the client on the first design export into a hand-installed
	 * theme.
	 * <p>
	 * Checked up front rather than around the manager call so the answer is the same with and
	 * without a client.
	 */
	public static boolean isAddressable(String path) {
		try {
			TheMightyArchitect.id(path);
			return true;
		} catch (RuntimeException notAResourcePath) {
			return false;
		}
	}

	/**
	 * Opens a built-in file.
	 *
	 * @param path relative to {@code assets/mightyarchitect/}, e.g.
	 *             {@code themes/medieval/theme.json}
	 * @return the stream, which the caller closes, or empty when nothing provides that path
	 */
	public static Optional<InputStream> open(String path) {
		if (!isAddressable(path))
			return Optional.empty();

		ResourceIndex index = indexOrNull();
		if (index != null)
			return index.read(path)
				.map(ByteArrayInputStream::new);

		ResourceManager manager = manager();
		if (manager != null) {
			Optional<Resource> resource = manager.getResource(TheMightyArchitect.id(path));
			if (!resource.isPresent())
				// Deliberately no classpath fallback once a manager exists: falling through would
				// re-read a file that a resource pack had deliberately replaced.
				return Optional.empty();
			try {
				return Optional.of(resource.get()
					.open());
			} catch (IOException e) {
				TheMightyArchitect.logger.error("Could not open the built-in " + path, e);
				return Optional.empty();
			}
		}

		return Optional.ofNullable(TheMightyArchitect.class.getClassLoader()
			.getResourceAsStream(CLASSPATH_ROOT + path));
	}

	/** @return whether anything provides that built-in path */
	public static boolean exists(String path) {
		if (!isAddressable(path))
			return false;

		ResourceIndex index = indexOrNull();
		if (index != null)
			return index.read(path)
				.isPresent();

		ResourceManager manager = manager();
		if (manager != null)
			return manager.getResource(TheMightyArchitect.id(path))
				.isPresent();
		return TheMightyArchitect.class.getClassLoader()
			.getResource(CLASSPATH_ROOT + path) != null;
	}

	/**
	 * Lists the {@code .json} files directly inside a built-in folder.
	 * <p>
	 * Ordered naturally - {@code p2} before {@code p10} - because a resource manager returns
	 * entries in whatever order the pack stack yields, the palette picker renders them in list
	 * order, and plain lexicographic sorting would reshuffle the buttons relative to every previous
	 * release.
	 *
	 * @param folder      relative to {@code assets/mightyarchitect/}, without a trailing slash
	 * @param offlineName the naming convention to walk when there is no resource manager to ask
	 * @return paths relative to {@code assets/mightyarchitect/}
	 */
	public static List<String> list(String folder, IntFunction<String> offlineName) {
		Optional<List<String>> enumerated = enumerate(folder);
		if (enumerated.isPresent()) {
			String prefix = folder + "/";
			TreeSet<String> paths = new TreeSet<>(ArchitectResources::compareNaturally);
			for (String path : enumerated.get())
				// The enumeration recurses; every caller here wants one folder, and a theme folder
				// sits directly above layer folders whose designs it must not absorb.
				if (path.startsWith(prefix) && path.endsWith(".json")
					&& path.indexOf('/', prefix.length()) < 0)
					paths.add(path);
			return new ArrayList<>(paths);
		}

		List<String> found = new ArrayList<>();
		for (int index = 0; index < PROBE_LIMIT; index++) {
			String path = folder + "/" + offlineName.apply(index);
			if (!exists(path))
				break;
			found.add(path);
		}
		return found;
	}

	/**
	 * Names the folders directly inside {@code parent} that contain a given file.
	 * <p>
	 * This is what lets a resource pack ship a whole new theme rather than only override one of
	 * the five the mod happens to have: the theme list is discovered from the resource stack
	 * instead of read off a hardcoded enum.
	 * <p>
	 * A plain classloader cannot enumerate a directory, so with no client this falls back to
	 * checking the names it was given - which still validates that each one is really there, and
	 * is only ever reached off-game, where there are no resource packs to discover anyway.
	 *
	 * @param parent   relative to {@code assets/mightyarchitect/}, without a trailing slash
	 * @param marker   the file a folder must contain to count, e.g. {@code theme.json}
	 * @param fallback the folder names to check when the stack cannot be enumerated
	 * @return the folder names, sorted naturally
	 */
	public static List<String> listFoldersContaining(String parent, String marker, Collection<String> fallback) {
		String prefix = parent + "/";
		String suffix = "/" + marker;
		TreeSet<String> folders = new TreeSet<>(ArchitectResources::compareNaturally);

		Optional<List<String>> enumerated = enumerate(parent);
		if (enumerated.isPresent()) {
			for (String path : enumerated.get()) {
				if (!path.startsWith(prefix) || !path.endsWith(suffix))
					continue;

				int begin = prefix.length();
				int end = path.length() - suffix.length();
				// The prefix and the suffix share the separator, so "themes/theme.json" satisfies
				// both and would slice backwards. That file is one directory level too high to be
				// a theme - the most likely first mistake a pack author makes - and it must cost
				// itself rather than throwing out of the whole theme list.
				if (end <= begin)
					continue;

				String rest = path.substring(begin, end);
				// Exactly one level down: themes/medieval/theme.json counts, and a stray
				// themes/medieval/regular/theme.json does not become a theme of its own.
				if (rest.indexOf('/') < 0)
					folders.add(rest);
			}
			return new ArrayList<>(folders);
		}

		for (String name : fallback)
			if (exists(parent + "/" + name + "/" + marker))
				folders.add(name);
		return new ArrayList<>(folders);
	}

	/**
	 * @return every path below the folder, or empty when the stack cannot be enumerated - which is
	 *         the case off-game, where a plain classloader cannot list a directory inside a jar
	 */
	private static Optional<List<String>> enumerate(String folder) {
		ResourceIndex index = indexOrNull();
		if (index != null)
			return index.listAll(folder);

		ResourceManager manager = manager();
		if (manager == null)
			return Optional.empty();

		List<String> paths = new ArrayList<>();
		manager.listResources(folder, location -> TheMightyArchitect.ID.equals(location.getNamespace()))
			.keySet()
			.forEach(location -> paths.add(location.getPath()));
		return Optional.of(paths);
	}

	/** Compares two paths treating each run of digits as one number. */
	public static int compareNaturally(String left, String right) {
		int l = 0;
		int r = 0;
		while (l < left.length() && r < right.length()) {
			char lc = left.charAt(l);
			char rc = right.charAt(r);

			if (Character.isDigit(lc) && Character.isDigit(rc)) {
				int lEnd = l;
				int rEnd = r;
				while (lEnd < left.length() && Character.isDigit(left.charAt(lEnd)))
					lEnd++;
				while (rEnd < right.length() && Character.isDigit(right.charAt(rEnd)))
					rEnd++;

				// Compared as text once leading zeroes are gone, so an arbitrarily long run of
				// digits cannot overflow the way parsing it would.
				String lNumber = left.substring(l, lEnd)
					.replaceFirst("^0+(?=.)", "");
				String rNumber = right.substring(r, rEnd)
					.replaceFirst("^0+(?=.)", "");
				if (lNumber.length() != rNumber.length())
					return lNumber.length() - rNumber.length();
				int byDigits = lNumber.compareTo(rNumber);
				if (byDigits != 0)
					return byDigits;

				l = lEnd;
				r = rEnd;
				continue;
			}

			if (lc != rc)
				return lc - rc;
			l++;
			r++;
		}
		return (left.length() - l) - (right.length() - r);
	}

	/**
	 * @return the resource manager to read through, or null when running without a client
	 */
	private static ResourceManager manager() {
		Minecraft client = ArchitectPaths.clientOrNull();
		if (client == null)
			return null;
		try {
			return client.getResourceManager();
		} catch (Throwable tooEarly) {
			// Reachable while the client is still constructing itself, which is when a mod
			// initializer runs on some loaders.
			return null;
		}
	}
}
