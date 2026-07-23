package com.timmie.dualpoc.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Minimal stand-in for the mod's SpriteShiftEntry, providing just enough surface for
 * SuperByteBuffer to compile. This type does not diverge by Minecraft version, so it is
 * deliberately trivial: the point of this POC file is to measure MC render-API divergence.
 */
public class SpriteShiftEntry {
    private final TextureAtlasSprite original;
    private final TextureAtlasSprite target;

    public SpriteShiftEntry(TextureAtlasSprite original, TextureAtlasSprite target) {
        this.original = original;
        this.target = target;
    }

    public TextureAtlasSprite getOriginal() {
        return original;
    }

    public TextureAtlasSprite getTarget() {
        return target;
    }
}
