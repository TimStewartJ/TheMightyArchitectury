package com.timmie.mightyarchitect.control.phase;

//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;
*///?}

/**
 * Implemented by an {@link IArchitectPhase} that draws onto the vanilla HUD.
 * <p>
 * Only the graphics parameter varies: 1.20 replaced the mod's own {@code GuiGraphics} shim with
 * vanilla's, and 26 renamed vanilla's to {@code GuiGraphicsExtractor}. The declaration itself is the
 * same on every version, so only the import and the signature are guarded.
 */
public interface IRenderGameOverlay {

	//? if >=26 {
	void renderGameOverlay(GuiGraphicsExtractor ms, float partialTicks);
	//?} else {
	/*void renderGameOverlay(GuiGraphics ms, float partialTicks);
	*///?}

}
