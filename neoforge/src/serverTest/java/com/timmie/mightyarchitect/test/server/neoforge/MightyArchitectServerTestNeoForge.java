package com.timmie.mightyarchitect.test.server.neoforge;

import com.timmie.mightyarchitect.test.server.ServerPrintTest;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod(MightyArchitectServerTestNeoForge.ID)
public class MightyArchitectServerTestNeoForge {

	public static final String ID = "mightyarchitect_server_test";

	public MightyArchitectServerTestNeoForge(IEventBus modEventBus) {
		// ServerStartedEvent rather than mod construction: the test needs a loaded overworld.
		NeoForge.EVENT_BUS.addListener(MightyArchitectServerTestNeoForge::onServerStarted);
	}

	private static void onServerStarted(ServerStartedEvent event) {
		ServerPrintTest.run(event.getServer());
	}
}
