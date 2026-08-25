package com.timmie.mightyarchitect.test;

public final class ClientTestEntrypoint {

    private ClientTestEntrypoint() {
    }

    public static void start() {
        ClientTestController.start();
        PlayerJourneyController.start();
    }
}
