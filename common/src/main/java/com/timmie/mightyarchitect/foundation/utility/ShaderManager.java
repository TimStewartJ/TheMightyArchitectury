package com.timmie.mightyarchitect.foundation.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class ShaderManager {

	/** Ticks to wait before retrying an effect that did not come back on the last attempt. */
	private static final int REAPPLY_BACKOFF_TICKS = 100;

	private static Shaders activeShader = Shaders.None;
	private static int reapplyDelay;

	public static void onClientTick(Minecraft minecraft) {
		if (Minecraft.getInstance().level == null && activeShader != Shaders.None)
			stopUsingShaders();

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return;
		reapplyIfDropped();
		MobEffectInstance activePotionEffect = player.getEffect(MobEffects.NIGHT_VISION);

		if (activeShader == Shaders.Blueprint) {
			if (activePotionEffect == null || activePotionEffect.getDuration() < 999)
				player.addEffect(new NVEffectInstance());
			return;
		}

		if (activePotionEffect instanceof NVEffectInstance)
			player.removeEffectNoUpdate(MobEffects.NIGHT_VISION);
	}

	public static Shaders getActiveShader() {
		return activeShader;
	}

	/**
	 * Puts the selected effect back when something else took it away.
	 * <p>
	 * Before 1.21.4 the effect sits in vanilla's single post-effect slot, and
	 * {@code Minecraft.setCameraEntity} empties that slot on every call (javap, 1.19.4-1.21.1:
	 * {@code checkEntityPostEffect} closes {@code postEffect} first). Spectating, or toggling a
	 * free-camera mod, would otherwise leave the composer without its blueprint view until the
	 * phase was re-entered. A resource reload can drop it the same way. Retried immediately, then
	 * with a back-off if it does not come back, so a shader that cannot load does not log every tick.
	 */
	private static void reapplyIfDropped() {
		if (activeShader == Shaders.None || activeShader.isActive()) {
			reapplyDelay = 0;
			return;
		}
		if (--reapplyDelay > 0)
			return;
		activeShader.setActive(true);
		reapplyDelay = activeShader.isActive() ? 0 : REAPPLY_BACKOFF_TICKS;
	}

	public static void setActiveShader(Shaders activeShader) {
		if (getActiveShader() == activeShader)
			return;
		ShaderManager.activeShader = activeShader;
		activeShader.setActive(true);
	}

	public static void stopUsingShaders() {
		activeShader = Shaders.None;
		activeShader.setActive(true);
	}

	private static class NVEffectInstance extends MobEffectInstance {

		public NVEffectInstance() {
			super(MobEffects.NIGHT_VISION, 1000, 0, false, false, false);
		}

	}

}
