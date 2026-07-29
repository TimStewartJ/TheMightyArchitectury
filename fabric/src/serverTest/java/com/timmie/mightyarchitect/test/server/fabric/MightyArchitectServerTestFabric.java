package com.timmie.mightyarchitect.test.server.fabric;

import com.timmie.mightyarchitect.test.server.ServerPrintTest;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class MightyArchitectServerTestFabric implements DedicatedServerModInitializer {

	@Override
	public void onInitializeServer() {
		// SERVER_STARTED rather than the initializer itself: the test needs a loaded overworld.
		ServerLifecycleEvents.SERVER_STARTED.register(ServerPrintTest::run);
	}
}
