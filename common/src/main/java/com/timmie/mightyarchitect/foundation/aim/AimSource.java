package com.timmie.mightyarchitect.foundation.aim;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * Decides where the composer tools aim from, for camera mods whose aiming the mod cannot infer on
 * its own. Register one with {@link ArchitectAim#register}.
 * <p>
 * Most camera mods need nothing: the tools already aim from {@code Minecraft.getCameraEntity()},
 * which is how vanilla spectating and the usual free-camera mods detach the view. A source is for
 * the cases that rule gets wrong - a mod that keeps the crosshair on the player while the view
 * floats, or one that aims from a point that is no entity's eyes.
 * <p>
 * Called once per client tick on the client thread while a tool is active, so keep it cheap. A
 * source that throws, or answers with a ray that cannot be traced, is switched off for the rest
 * of the session and logged once; it cannot take the composer down with it.
 */
@FunctionalInterface
public interface AimSource {

	/**
	 * @param camera the entity the view is currently rendered from; the player when nothing has
	 *               detached the camera
	 * @param player the local player
	 * @return the ray to aim along, or null to leave the decision to lower-priority sources and,
	 *         after them, to the camera entity
	 */
	@Nullable
	Aim aim(Entity camera, LocalPlayer player);
}
