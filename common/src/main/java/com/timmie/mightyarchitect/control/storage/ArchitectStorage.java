package com.timmie.mightyarchitect.control.storage;

import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.design.ThemeStorage;
import com.timmie.mightyarchitect.control.palette.PaletteStorage;

/**
 * The one owner of the mod's loaded content, and the only thing that can throw it away.
 * <p>
 * Themes, palettes and the design exporter used to be three independent piles of static fields.
 * Nothing owned them, so nothing could reset them: a theme picked in one world was still selected
 * after disconnecting into another, and a resource reload could not touch the built-in themes
 * because the cache lived inside {@code ThemeStorage.IncludedThemes}' own enum constants. Putting
 * them behind one holder is what makes {@link #reset()} and {@link #onResourceReload()} possible at
 * all.
 * <p>
 * Still reached through static facades on the three classes, deliberately: the screens and phases
 * that call them are a separate sweep, and doing both at once would bury the storage change in
 * mechanical churn.
 */
public final class ArchitectStorage {

	private static ArchitectStorage instance;

	private final ThemeStorage themes = new ThemeStorage();
	private final PaletteStorage palettes = new PaletteStorage();
	private final DesignExporter designExporter = new DesignExporter();

	private ArchitectStorage() {
	}

	public static synchronized ArchitectStorage get() {
		if (instance == null)
			instance = new ArchitectStorage();
		return instance;
	}

	public static ThemeStorage themes() {
		return get().themes;
	}

	public static PaletteStorage palettes() {
		return get().palettes;
	}

	public static DesignExporter designExporter() {
		return get().designExporter;
	}

	/** Throws away everything loaded, so the next request rebuilds it from disk and from the jar. */
	public static synchronized void reset() {
		instance = null;
	}

	/**
	 * Drops what a resource pack can supply.
	 * <p>
	 * Called from the client's own reload, so F3+T now picks up an edited built-in theme or a
	 * palette a pack added. The user's own themes are dropped too, which costs one rescan and
	 * means the reload key does what people already expect it to.
	 */
	public static void onResourceReload() {
		themes().invalidate();
		palettes().invalidate();
	}
}
