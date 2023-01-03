package com.timmie.mightyarchitect.control.design;

import com.timmie.mightyarchitect.control.compose.Room;
import com.timmie.mightyarchitect.control.compose.Stack;
import com.timmie.mightyarchitect.control.design.partials.Design;

import java.util.HashMap;
import java.util.Map;

public class RoomDesignCache {
	
	private Map<Room, Design> cachedDesigns;
	
	public RoomDesignCache() {
		cachedDesigns = new HashMap<>();
	}

	public void rerollAll() {
		cachedDesigns.clear();
	}
	
	public void rerollRoom(Room room) {
		if (cachedDesigns.containsKey(room))
			cachedDesigns.remove(room);
	}
	
	public void rerollStack(Stack stack) {
		stack.forEach(this::rerollRoom);
	}

}
