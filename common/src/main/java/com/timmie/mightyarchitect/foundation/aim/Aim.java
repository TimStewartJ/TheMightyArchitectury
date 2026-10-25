package com.timmie.mightyarchitect.foundation.aim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * A ray the composer tools trace along: where it starts, which way it points, and the entity whose
 * collision rules apply to it.
 *
 * @param origin    where the ray starts, normally an entity's eyes
 * @param direction which way the ray points; any non-zero length, normalized on construction
 * @param source    the entity the ray is traced on behalf of; it decides collision shapes that depend
 *                  on who is asking, such as scaffolding and powder snow
 */
public record Aim(Vec3 origin, Vec3 direction, Entity source) {

	public Aim {
		Objects.requireNonNull(origin, "origin");
		Objects.requireNonNull(direction, "direction");
		Objects.requireNonNull(source, "source");
		direction = direction.normalize();
	}

	/**
	 * Aims from an entity's eyes along its view, exactly as vanilla's crosshair does when that entity
	 * is the camera. Ticks run at partial tick 1, so this is the entity's current position.
	 */
	public static Aim fromEyes(Entity entity) {
		return new Aim(entity.getEyePosition(1.0F), entity.getViewVector(1.0F), entity);
	}

	/** The point {@code distance} blocks along the ray. */
	public Vec3 at(double distance) {
		return origin.add(direction.scale(distance));
	}

	/** Whether this ray can be traced: finite numbers and a direction that survived normalizing. */
	public boolean isTraceable() {
		return isFinite(origin) && isFinite(direction) && direction.lengthSqr() > 0.5;
	}

	private static boolean isFinite(Vec3 vec) {
		return Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z);
	}
}
