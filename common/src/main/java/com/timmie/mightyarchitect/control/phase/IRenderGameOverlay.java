//? if >=26 {
package com.timmie.mightyarchitect.control.phase;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface IRenderGameOverlay {

	void renderGameOverlay(GuiGraphicsExtractor ms, float partialTicks);
	
}
//?} else if >=1.20 {
/*package com.timmie.mightyarchitect.control.phase;

import net.minecraft.client.gui.GuiGraphics;

public interface IRenderGameOverlay {

	void renderGameOverlay(GuiGraphics ms, float partialTicks);
	
}*///?} else {
/*package com.timmie.mightyarchitect.control.phase;

import com.timmie.mightyarchitect.foundation.gui.GuiGraphics;

public interface IRenderGameOverlay {

	void renderGameOverlay(GuiGraphics ms, float partialTicks);
	
}*///?}
