package com.timmie.mightyarchitect.test.server.forge;

import com.timmie.mightyarchitect.test.server.ServerPrintTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(MightyArchitectServerTestForge.ID)
public class MightyArchitectServerTestForge {

	public static final String ID = "mightyarchitect_server_test";

	public MightyArchitectServerTestForge() {
		// ServerStartedEvent rather than mod construction: the test needs a loaded overworld.
		MinecraftForge.EVENT_BUS.addListener(MightyArchitectServerTestForge::onServerStarted);
	}

	private static void onServerStarted(ServerStartedEvent event) {
		ServerPrintTest.run(event.getServer());
	}
}
