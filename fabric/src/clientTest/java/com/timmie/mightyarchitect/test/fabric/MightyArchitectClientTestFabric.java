package com.timmie.mightyarchitect.test.fabric;

import com.timmie.mightyarchitect.test.ClientTestEntrypoint;
import net.fabricmc.api.ClientModInitializer;

public class MightyArchitectClientTestFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTestEntrypoint.start();
    }
}
