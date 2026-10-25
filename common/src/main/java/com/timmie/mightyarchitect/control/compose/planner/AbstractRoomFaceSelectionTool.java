package com.timmie.mightyarchitect.control.compose.planner;

import com.timmie.mightyarchitect.control.compose.Room;
import com.timmie.mightyarchitect.control.compose.Stack;
import com.timmie.mightyarchitect.foundation.aim.Aim;
import net.minecraft.core.BlockPos;

public abstract class AbstractRoomFaceSelectionTool extends GroundPlanningToolBase {

	protected boolean highlightRoom;

	@Override
	public void init() {
		super.init();
		highlightRoom = true;
	}

	@Override
	protected void makeStacksTransparent(Aim aim, BlockPos hit) {}

	@Override
	public void tickToolOutlines() {}

	@Override
	protected boolean isStackHighlighted(Stack stack) {
		return stack == selectedStack;
	}

	@Override
	protected boolean isRoomHighlighted(Room room) {
		return room == selectedRoom && highlightRoom;
	}

}
