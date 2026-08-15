package com.timmie.mightyarchitect.test.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

// The @Mod `dist` attribute only exists from NeoForge 20.6 onward, and this source set is shared
// verbatim by every node, so the client-only guard is the runtime check in the constructor.
@Mod("mightyarchitect_test")
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
            Class.forName("com.timmie.mightyarchitect.test.ClientTestEntrypoint")
                .getMethod("start")
                .invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to start the client-test controller", exception);
        }
    }
}
