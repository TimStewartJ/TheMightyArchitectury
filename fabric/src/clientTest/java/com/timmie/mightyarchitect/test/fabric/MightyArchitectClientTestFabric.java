package com.timmie.mightyarchitect.test.fabric;

import com.timmie.mightyarchitect.test.ClientTestController;
import net.fabricmc.api.ClientModInitializer;

public class MightyArchitectClientTestFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTestController.start();
    }
}
