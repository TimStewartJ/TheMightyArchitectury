package com.timmie.mightyarchitect.control.phase;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.foundation.MightyBuffers;

import java.util.List;

public interface IArchitectPhase {

	public void whenEntered();
	public void update();
	public void render(PoseStack ms, MightyBuffers buffer);
	public void whenExited();
	
	public List<String> getToolTip();
	
	public void onClick(int button);
	public void onKey(int key, boolean released);
	public boolean onScroll(int amount);

	/**
	 * What pressing Esc would cancel right now, worded for the on-screen hint (for example
	 * "Cancel room"), or {@code null} when nothing is in progress and Esc should open the pause menu.
	 */
	default String cancellableAction() {
		return null;
	}

	/**
	 * Cancels the step in progress, most recent first.
	 *
	 * @return whether anything was cancelled; when it was, Esc is kept from vanilla
	 */
	default boolean cancelAction() {
		return false;
	}
	
}
