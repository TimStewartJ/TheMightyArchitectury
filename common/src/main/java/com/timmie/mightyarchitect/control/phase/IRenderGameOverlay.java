package com.timmie.mightyarchitect.control.phase;

import com.mojang.blaze3d.vertex.PoseStack;

public interface IRenderGameOverlay {

	public void renderGameOverlay(PoseStack ms, float partialTicks);
	
}
