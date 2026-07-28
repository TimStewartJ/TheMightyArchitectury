package com.timmie.mightyarchitect.foundation;

import com.mojang.blaze3d.vertex.VertexConsumer;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
*///?}

/**
 * The buffer surface the mod's world rendering draws into.
 *
 * <p>Up to 26.1 this is vanilla's {@code MultiBufferSource}: geometry is emitted into a buffer and
 * flushed. 26.2 removed that model — geometry is submitted to a {@code SubmitNodeCollector} and
 * vanilla draws it later — so the mod's own type is what render code talks to, and
 * {@link SuperRenderTypeBuffer} decides per version how the vertices reach the screen.
 */
public interface MightyBuffers {

	VertexConsumer getBuffer(RenderType type);
}
