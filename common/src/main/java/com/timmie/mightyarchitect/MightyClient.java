package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.foundation.compat.McCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.SchematicRenderer;
import com.timmie.mightyarchitect.foundation.SuperRenderTypeBuffer;
import com.timmie.mightyarchitect.foundation.utility.AnimationTickHolder;
import com.timmie.mightyarchitect.foundation.utility.Keyboard;
//? if >=1.21.4 {
import com.timmie.mightyarchitect.foundation.utility.PostChainManager;
//?} else {
/*
*///?}
import com.timmie.mightyarchitect.foundation.utility.outliner.Outliner;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//? if >=26.2 {

//?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else if >=1.21.10 {
/*import net.minecraft.resources.ResourceLocation;
*///?} else {
/*
*///?}
import net.minecraft.world.phys.Vec3;

public class MightyClient {

	public static KeyMapping COMPOSE;
	public static KeyMapping TOOL_MENU;

	//? if >=1.21.11 {
	public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "keys"));

	//?} else if >=1.21.10 {
	/*public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "keys"));

	*///?} else {
	/*
	*///?}
	public static SchematicRenderer renderer = new SchematicRenderer();
	public static Outliner outliner = new Outliner();

	public static boolean iris_presence;

	// Called by the loader on client setup, before it registers the key mappings created here.
	public static void init() {
		AllItems.initColorHandlers();
		//? if >=1.21.10 {
		COMPOSE = new KeyMapping("key.mightyclient.compose", Keyboard.G, CATEGORY);
		TOOL_MENU = new KeyMapping("key.mightyclient.toolmenu", Keyboard.LALT, CATEGORY);
		//?} else {
		/*String modName = TheMightyArchitect.NAME;
		COMPOSE = new KeyMapping("key.mightyclient.compose", Keyboard.G, modName);
		TOOL_MENU = new KeyMapping("key.mightyclient.toolmenu", Keyboard.LALT, modName);
		*///?}
	}

	public static void onTick(Minecraft event) {
		AnimationTickHolder.tick();

		if (!isGameActive())
			return;

		ArchitectManager.tickBlockHighlightOutlines();
		MightyClient.outliner.tickOutlines();
		MightyClient.renderer.tick();
	}

	//? if >=26.2 {
	/*// 26.2 hands mods a SubmitNodeCollector for the frame instead of a buffer source to flush.
	public static void onRenderWorld(net.minecraft.client.renderer.SubmitNodeCollector collector) {
		Minecraft mc = Minecraft.getInstance();
		SuperRenderTypeBuffer.beginFrame(collector);
		Camera info = McCompat.mainCamera(mc);
		Vec3 view = info.position();
	*///?} else if >=26 {
	public static void onRenderWorld() {
		Minecraft mc = Minecraft.getInstance();
		Camera info = McCompat.mainCamera(mc);
		Vec3 view = info.position();
	//?} else if >=1.21.11 {
	/*public static void onRenderWorld() {
		// In 1.21.6, world rendering uses PoseStack directly, not GuiGraphics
		PoseStack ms = new PoseStack();
		Camera info = McCompat.mainCamera(Minecraft.getInstance());
		Vec3 view = info.position();
	*///?} else if >=1.21.6 {
	/*public static void onRenderWorld() {
		// In 1.21.6, world rendering uses PoseStack directly, not GuiGraphics
		PoseStack ms = new PoseStack();
		Camera info = McCompat.mainCamera(Minecraft.getInstance());
		Vec3 view = info.getPosition();
	*///?} else {
	/*public static void onRenderWorld(PoseStack poseStack) {
		PoseStack ms = poseStack;
		Camera info = McCompat.mainCamera(Minecraft.getInstance());
		Vec3 view = info.getPosition();
	*///?}

		// Reset the per-frame HUD label buffer; outlines re-submit their labels as they render below.
		com.timmie.mightyarchitect.foundation.utility.HudTextBuffer.beginFrame(view);

		//? if >=26 {
		// Rendered from a mid-level stage (after translucent particles / features) where the active
		// RenderSystem modelview still holds the camera view rotation, so only the world-space offset
		// is applied here. Do not re-apply the camera rotation or it double-transforms.
		PoseStack ms = new PoseStack();
		//?} else {
		/*
		*///?}
		ms.pushPose();
		ms.translate(-view.x(), -view.y(), -view.z());
		//? if >=26.2 {
		
		//?} else if >=26 {
		MultiBufferSource.BufferSource buffer = mc.renderBuffers()
			.bufferSource();
		//?} else {
		/*MultiBufferSource.BufferSource buffer = Minecraft.getInstance()
			.renderBuffers()
			.bufferSource();
		*///?}

		SuperRenderTypeBuffer b = SuperRenderTypeBuffer.getInstance();

		MightyClient.renderer.render(ms, b);
		ArchitectManager.render(ms, b);
		MightyClient.outliner.renderOutlines(ms, b);

		b.draw();
		//? if >=26.2 {
		
		//?} else {
		buffer.endBatch();
		//?}
		ms.popPose();
	}

	protected static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}

	//? if >=1.21.4 {
	//*
	 //* Processes post-processing shader effects.
	 //* This should be called after the world has been rendered to apply effects like the blueprint shader.
	 //*
	 //* @param partialTicks The partial tick time for smooth animation
	 //
	public static void onPostRender(float partialTicks) {
		PostChainManager.processShader(partialTicks);
	}

	//?} else {
	/*
	*///?}
}
