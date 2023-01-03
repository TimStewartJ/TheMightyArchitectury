package com.timmie.mightyarchitect.control.phase;

import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.Schematic;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public abstract class PhaseBase implements IArchitectPhase {

	protected Minecraft minecraft;
	
	public PhaseBase() {
		minecraft = Minecraft.getInstance();
	}
	
	@Override
	public void onClick(int button) {
	}

	@Override
	public void onKey(int key, boolean released) {
	}
	
	@Override
	public boolean onScroll(int amount) {
		return false;
	}
	
	protected Schematic getModel() {
		return ArchitectManager.getModel();
	}
	
	protected void sendStatusMessage(String message) {
		if (message == null) 
			return;
		
		minecraft.player.displayClientMessage(Component.literal(message), true);
	}

	
}
