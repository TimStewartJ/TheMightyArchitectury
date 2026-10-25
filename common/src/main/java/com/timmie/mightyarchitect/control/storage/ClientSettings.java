package com.timmie.mightyarchitect.control.storage;

import com.mojang.serialization.Codec;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The player's own preferences, as opposed to their content: {@code <mod folder>/settings.json}.
 * <p>
 * Read on first use and written whenever a setting changes. A missing or unreadable file means
 * the defaults, so deleting it is always a safe reset.
 */
public final class ClientSettings {

	public static final String FILE = "settings.json";

	/** Whether composing tints the world with the blueprint effect. On unless the player turns it off. */
	public static final Codec<Boolean> CODEC = Codec.BOOL.optionalFieldOf("BlueprintView")
		.xmap(value -> value.orElse(true), Optional::of)
		.codec();

	private static Boolean blueprintView;

	private ClientSettings() {
	}

	public static Path file() {
		return ArchitectPaths.root()
			.resolve(FILE);
	}

	public static synchronized boolean blueprintView() {
		if (blueprintView == null)
			blueprintView = JsonStorage.read(file(), CODEC)
				.orElse(true);
		return blueprintView;
	}

	public static synchronized void setBlueprintView(boolean enabled) {
		blueprintView = enabled;
		JsonStorage.write(file(), CODEC, enabled);
	}

	/** Forgets what was loaded, so the next read goes back to the file. */
	public static synchronized void reload() {
		blueprintView = null;
	}
}
