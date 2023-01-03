package com.timmie.mightyarchitect.control.phase.export;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.control.phase.PhaseBase;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.List;

public class PhaseListThemesForEditing extends PhaseBase {

	@Override
	public void whenEntered() {
	}

	@Override
	public void update() {
		
	}

	@Override
	public void render(PoseStack ms, MultiBufferSource buffer) {
		
	}

	@Override
	public void whenExited() {
	}

	@Override
	public List<String> getToolTip() {
		return ImmutableList.of("Edit one of your previously created or imported design packs.");
	}

}
