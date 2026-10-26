package com.timmie.mightyarchitect.foundation.aim;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Where the composer tools aim from.
 * <p>
 * The answer is the entity the view is rendered from, {@code Minecraft.getCameraEntity()} - the
 * same entity vanilla's crosshair aims from. Normally that is the player, so nothing changes in
 * first or third person, and free-look mods that turn only the view keep aiming from the player
 * just as vanilla does. Spectating a mob, or a free-camera mod that swaps in its own camera entity
 * (Freecam, Tweakeroo), moves the aim with the view, so a build can be drafted from wherever the
 * camera floats.
 * <p>
 * This only moves where the tools <em>look</em>. Everything that changes the world is still
 * authorized on the server against the real player's position and permissions.
 * <p>
 * Camera mods that need a different answer register an {@link AimSource}. Sources are asked in
 * descending priority; the first non-null answer wins, and the camera entity is the fallback when
 * every source declines. Built-in compatibility sources sit at {@link #BUILT_IN_PRIORITY}, below
 * the default {@code 0}, so another mod's own source always takes precedence over the guess made
 * here about it.
 */
public final class ArchitectAim {

	/** Priority of the compatibility sources this mod registers for other mods. */
	public static final int BUILT_IN_PRIORITY = -1000;

	private static final List<Registration> sources = new CopyOnWriteArrayList<>();
	private static boolean builtInsRegistered;

	private ArchitectAim() {
	}

	/**
	 * Adds a source, replacing any earlier one registered under the same id.
	 *
	 * @param id       a stable, namespaced name such as {@code "examplemod:shoulder_camera"}, used to
	 *                 replace the source and in log messages
	 * @param priority higher is asked first; use 0 unless ordering against another source matters
	 * @return a handle that removes this source again
	 */
	public static synchronized Registration register(String id, int priority, AimSource source) {
		Registration registration = new Registration(Objects.requireNonNull(id, "id"), priority,
			Objects.requireNonNull(source, "source"));
		List<Registration> updated = new ArrayList<>(sources);
		updated.removeIf(existing -> existing.id.equals(id));
		updated.add(registration);
		// List.sort is stable, so equal priorities keep registration order.
		updated.sort(Comparator.comparingInt((Registration r) -> r.priority)
			.reversed());
		sources.clear();
		sources.addAll(updated);
		return registration;
	}

	/** The ray the tools should trace this tick, or null outside a world. */
	@Nullable
	public static Aim current() {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null)
			return null;
		Entity camera = minecraft.getCameraEntity();
		return resolve(camera != null ? camera : player, player);
	}

	/**
	 * The decision {@link #current()} makes, for a given camera entity and player: each enabled
	 * source in priority order, then the camera entity's eyes.
	 */
	public static Aim resolve(Entity camera, LocalPlayer player) {
		registerBuiltIns();
		for (Registration registration : sources) {
			Aim aim = registration.ask(camera, player);
			if (aim != null)
				return aim;
		}
		return Aim.fromEyes(camera);
	}

	private static synchronized void registerBuiltIns() {
		if (builtInsRegistered)
			return;
		builtInsRegistered = true;
		FreecamAimSource.registerIfPresent();
	}

	/** A registered source; {@link #unregister()} removes it. */
	public static final class Registration {

		private final String id;
		private final int priority;
		private final AimSource source;
		private volatile boolean disabled;

		private Registration(String id, int priority, AimSource source) {
			this.id = id;
			this.priority = priority;
			this.source = source;
		}

		public String id() {
			return id;
		}

		/** Whether the source was switched off after throwing or returning an untraceable ray. */
		public boolean isDisabled() {
			return disabled;
		}

		public void unregister() {
			synchronized (ArchitectAim.class) {
				List<Registration> updated = new ArrayList<>(sources);
				if (updated.remove(this)) {
					sources.clear();
					sources.addAll(updated);
				}
			}
		}

		@Nullable
		private Aim ask(Entity camera, LocalPlayer player) {
			if (disabled)
				return null;
			Aim aim;
			try {
				aim = source.aim(camera, player);
			} catch (Throwable throwable) {
				disable("threw", throwable);
				return null;
			}
			if (aim != null && !aim.isTraceable()) {
				disable("returned an untraceable ray " + aim, null);
				return null;
			}
			return aim;
		}

		private void disable(String reason, @Nullable Throwable throwable) {
			disabled = true;
			TheMightyArchitect.logger.warn("Aim source '{}' {}; the composer tools ignore it from now on", id,
				reason, throwable);
		}
	}
}
