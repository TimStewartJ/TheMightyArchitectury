package com.timmie.mightyarchitect.test.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(value = "mightyarchitect_test", dist = Dist.CLIENT)
public class MightyArchitectClientTestNeoForge {

    public MightyArchitectClientTestNeoForge() {
        if (currentDist() == Dist.CLIENT)
            startClientTest();
    }

    private static Dist currentDist() {
        try {
            try {
                return (Dist) FMLEnvironment.class.getMethod("getDist").invoke(null);
            } catch (NoSuchMethodException ignored) {
                return (Dist) FMLEnvironment.class.getField("dist").get(null);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to determine the NeoForge distribution", exception);
        }
    }

    private static void startClientTest() {
        try {
            Class.forName("com.timmie.mightyarchitect.test.ClientTestController")
                .getMethod("start")
                .invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to start the client-test controller", exception);
        }
    }
}
