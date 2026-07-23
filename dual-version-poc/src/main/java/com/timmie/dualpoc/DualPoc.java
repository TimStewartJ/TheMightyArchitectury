package com.timmie.dualpoc;

import net.fabricmc.api.ModInitializer;
import net.minecraft.nbt.CompoundTag;

/**
 * Minimal but real proof that Stonecutter can produce two genuinely-remapped jars from two
 * Minecraft version nodes in a single branch. The divergence below is lifted from the mod's own
 * Roof.fromNBT: Minecraft 1.21.5+/1.21.6 changed CompoundTag.getInt(String) to return
 * Optional&lt;Integer&gt;, so this code does NOT compile unless each node builds against its own API.
 */
public class DualPoc implements ModInitializer {
    @Override
    public void onInitialize() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Roofspan", 3);
        System.out.println("[dualpoc] Roofspan=" + readSpan(tag));
    }

    static int readSpan(CompoundTag compound) {
        //? if >=1.21.6 {
        return compound.getInt("Roofspan").orElse(0);
        //?} else {
        /*return compound.getInt("Roofspan");
        *///?}
    }
}
