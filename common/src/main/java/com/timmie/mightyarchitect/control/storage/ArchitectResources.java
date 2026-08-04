package com.timmie.mightyarchitect.control.storage;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

	private static ResourceManager managerOverride;

	private ArchitectResources() {
	}

	/** Supplies a resource manager to code with no client, or restores the client's with null. */
	public static synchronized void setManagerForTesting(ResourceManager manager) {
		managerOverride = manager;
	}

	/**
	 * Opens a built-in file.
	 *
	 * @param path relative to {@code assets/mightyarchitect/}, e.g.
	 *             {@code themes/medieval/theme.json}
	 * @return the stream, which the caller closes, or empty when nothing provides that path
	 */
	public static Optional<InputStream> open(String path) {
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
		ResourceManager manager = manager();
		if (manager != null)
			return listThroughManager(manager, folder);

		List<String> found = new ArrayList<>();
		for (int index = 0; index < PROBE_LIMIT; index++) {
			String path = folder + "/" + offlineName.apply(index);
			if (!exists(path))
				break;
			found.add(path);
		}
		return found;
	}

	private static List<String> listThroughManager(ResourceManager manager, String folder) {
		String prefix = folder + "/";
		TreeSet<String> paths = new TreeSet<>(ArchitectResources::compareNaturally);

		manager.listResources(folder, location -> TheMightyArchitect.ID.equals(location.getNamespace())
			&& location.getPath()
				.endsWith(".json"))
			.keySet()
			.forEach(location -> {
				String path = location.getPath();
				// listResources recurses; every caller here wants one folder, and a theme folder
				// sits directly above layer folders whose designs it must not absorb.
				if (path.startsWith(prefix) && path.indexOf('/', prefix.length()) < 0)
					paths.add(path);
			});

		return new ArrayList<>(paths);
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
	private static synchronized ResourceManager manager() {
		if (managerOverride != null)
			return managerOverride;

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
