package com.timmie.mightyarchitect.test.forge;

import com.timmie.mightyarchitect.test.ClientTestEntrypoint;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("mightyarchitect_test")
public class MightyArchitectClientTestForge {

    public MightyArchitectClientTestForge() {
        if (FMLEnvironment.dist == Dist.CLIENT)
            ClientTestEntrypoint.start();
    }
}
