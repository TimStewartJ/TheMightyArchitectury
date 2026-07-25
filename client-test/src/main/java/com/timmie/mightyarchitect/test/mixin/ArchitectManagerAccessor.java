package com.timmie.mightyarchitect.test.mixin;

import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.gui.ArchitectMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the private static menu instance so the render assertions can toggle
 * its visibility without adding a test-only accessor to production code.
 */
@Mixin(value = ArchitectManager.class, remap = false)
public interface ArchitectManagerAccessor {

    @Accessor(value = "menu", remap = false)
    static ArchitectMenuScreen getMenu() {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
