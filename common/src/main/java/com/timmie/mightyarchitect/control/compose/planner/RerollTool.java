package com.timmie.mightyarchitect.control.compose.planner;

import com.timmie.mightyarchitect.control.ArchitectManager;

public class RerollTool extends WallDecorationToolBase {

	@Override
	public boolean handleMouseWheel(int amount) {
		model.seed += amount;
		model.getTheme().getDesignPicker().rerollAll();
		ArchitectManager.reAssemble();
		
		return true;
	}
	
}
