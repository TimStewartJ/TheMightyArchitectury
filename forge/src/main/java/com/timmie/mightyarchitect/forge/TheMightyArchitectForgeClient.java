package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.platform.Env;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side setup for the Forge nodes. This era has no {@code dist} attribute on {@code @Mod}, so
 * the side is expressed with an event-bus subscriber restricted to {@link Dist#CLIENT} instead.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TheMightyArchitectForgeClient {

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		// Reaching this listener at all proves the physical client, and key mappings have to exist
		// before Options is built - which is exactly when Forge fires this event.
		Env.setClient(true);
		MightyClient.init();

		event.register(MightyClient.COMPOSE);
		event.register(MightyClient.TOOL_MENU);
	}
}
