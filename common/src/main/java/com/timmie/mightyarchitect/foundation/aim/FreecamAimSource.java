package com.timmie.mightyarchitect.foundation.aim;

import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Follows Freecam's own choice of where the crosshair aims from.
 * <p>
 * Freecam (MinecraftFreecam/Freecam, all loaders) swaps in a {@code FreeCamera} entity with
 * {@code Minecraft.setCameraEntity}, so by default the generic camera-entity rule already aims from
 * it and this source stays out of the way. Two of its modes move vanilla's crosshair back onto the
 * player while the view keeps floating, and the tools follow it there too:
 * <ul>
 * <li>player control, where the player walks around while the camera stays put; and</li>
 * <li>"interact from player", where the camera flies but clicks still come from the player.</li>
 * </ul>
 * Freecam's mixin on {@code pick} makes the same decision. Nothing here is compiled against
 * Freecam: it is looked up by name, so the mod neither depends on it nor breaks without it. Two
 * shapes of its API are recognised, checked with javap against the real jars:
 * <ul>
 * <li>1.4 and later: {@code ModConfig.get().allowInteractionsFromPlayer()}, which is true only
 * with interactions allowed and the mode set to PLAYER;</li>
 * <li>1.3 and earlier: {@code ModConfig.INSTANCE.utility.interactionMode == PLAYER}, which those
 * versions' {@code pick} mixin reads without the allow flag.</li>
 * </ul>
 * If a future Freecam changes either, only the interaction-mode check is lost and the tools still
 * follow its camera entity; if the core methods disappear the source is not registered at all.
 */
final class FreecamAimSource implements AimSource {

	static final String ID = TheMightyArchitect.ID + ":freecam";

	private static final String FREECAM = "net.xolt.freecam.Freecam";
	private static final String MOD_CONFIG = "net.xolt.freecam.config.ModConfig";

	private final Method isEnabled;
	private final Method isPlayerControlEnabled;
	@Nullable
	private final InteractionModeCheck interactsFromPlayer;

	private FreecamAimSource(Class<?> freecam, @Nullable InteractionModeCheck interactsFromPlayer)
		throws NoSuchMethodException {
		this.isEnabled = freecam.getMethod("isEnabled");
		this.isPlayerControlEnabled = freecam.getMethod("isPlayerControlEnabled");
		this.interactsFromPlayer = interactsFromPlayer;
	}

	static void registerIfPresent() {
		ClassLoader loader = FreecamAimSource.class.getClassLoader();
		Class<?> freecam;
		try {
			freecam = Class.forName(FREECAM, false, loader);
		} catch (ClassNotFoundException | LinkageError absent) {
			return;
		}

		try {
			InteractionModeCheck modeCheck = bindInteractionMode(loader);
			ArchitectAim.register(ID, ArchitectAim.BUILT_IN_PRIORITY, new FreecamAimSource(freecam, modeCheck));
			TheMightyArchitect.logger.info("Freecam found; the composer tools follow its camera{}",
				modeCheck == null ? " (its interaction-mode setting is not recognised and is ignored)" : "");
		} catch (ReflectiveOperationException | RuntimeException | LinkageError unrecognised) {
			TheMightyArchitect.logger.warn(
				"Freecam found but its API is not recognised; the composer tools follow its camera entity only",
				unrecognised);
		}
	}

	@Override
	@Nullable
	public Aim aim(Entity camera, LocalPlayer player) {
		try {
			if (!(boolean) isEnabled.invoke(null))
				return null;
			if ((boolean) isPlayerControlEnabled.invoke(null)
				|| (interactsFromPlayer != null && interactsFromPlayer.test()))
				return Aim.fromEyes(player);
			return null;
		} catch (ReflectiveOperationException failed) {
			throw new IllegalStateException("Freecam state could not be read", failed);
		}
	}

	@Nullable
	private static InteractionModeCheck bindInteractionMode(ClassLoader loader) {
		Class<?> modConfig;
		try {
			modConfig = Class.forName(MOD_CONFIG, false, loader);
		} catch (ClassNotFoundException | LinkageError absent) {
			return null;
		}

		try {
			Method get = modConfig.getMethod("get");
			Method fromPlayer = modConfig.getMethod("allowInteractionsFromPlayer");
			return () -> (boolean) fromPlayer.invoke(get.invoke(null));
		} catch (NoSuchMethodException notCurrentShape) {
			// Fall through to the 1.3-and-earlier shape.
		}

		try {
			Field instance = modConfig.getField("INSTANCE");
			Field utility = instance.getType()
				.getField("utility");
			Field interactionMode = utility.getType()
				.getField("interactionMode");
			return () -> {
				Object mode = interactionMode.get(utility.get(instance.get(null)));
				return mode instanceof Enum<?> value && "PLAYER".equals(value.name());
			};
		} catch (NoSuchFieldException notLegacyShape) {
			return null;
		}
	}

	@FunctionalInterface
	private interface InteractionModeCheck {
		boolean test() throws ReflectiveOperationException;
	}
}
