package com.timmie.mightyarchitect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.SchematicRenderer;
import com.timmie.mightyarchitect.foundation.SuperRenderTypeBuffer;
import com.timmie.mightyarchitect.foundation.utility.AnimationTickHolder;
import com.timmie.mightyarchitect.foundation.utility.Keyboard;
import com.timmie.mightyarchitect.foundation.utility.ShaderManager;
import com.timmie.mightyarchitect.foundation.utility.outliner.Outliner;
import com.timmie.mightyarchitect.gui.ScreenHelper;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public class MightyClient {

	public static KeyMapping COMPOSE;
	public static KeyMapping TOOL_MENU;

	public static SchematicRenderer renderer = new SchematicRenderer();
	public static Outliner outliner = new Outliner();

	public static void init() {
		AllItems.initColorHandlers();
		String modName = TheMightyArchitect.NAME;
		COMPOSE = new KeyMapping("key.mightyclient.compose", Keyboard.G, modName);
		TOOL_MENU = new KeyMapping("key.mightyclient.toolmenu", Keyboard.LALT, modName);
		KeyMappingRegistry.register(COMPOSE);
		KeyMappingRegistry.register(TOOL_MENU);

		ClientTickEvent.CLIENT_POST.register(MightyClient::onTick);

		ClientTickEvent.CLIENT_POST.register(ScreenHelper::onClientTick);
		ClientTickEvent.CLIENT_PRE.register(ShaderManager::onClientTick);

		ArchitectManager.registerAllEvents();
	}

	public static void onTick(Minecraft event) {
		AnimationTickHolder.tick();

		if (!isGameActive())
			return;

		ArchitectManager.tickBlockHighlightOutlines();
		MightyClient.outliner.tickOutlines();
		MightyClient.renderer.tick();
	}

	public static void onRenderWorld(PoseStack poseStack) {
		PoseStack ms = poseStack;
		Camera info = Minecraft.getInstance().gameRenderer.getMainCamera();
		Vec3 view = info.getPosition();

		ms.pushPose();
		ms.translate(-view.x(), -view.y(), -view.z());
		MultiBufferSource.BufferSource buffer = Minecraft.getInstance()
			.renderBuffers()
			.bufferSource();

		SuperRenderTypeBuffer b = SuperRenderTypeBuffer.getInstance();

		MightyClient.renderer.render(ms, b);
		ArchitectManager.render(ms, b);
		MightyClient.outliner.renderOutlines(ms, b);

		b.draw();
		buffer.endBatch();
		ms.popPose();
	}

	protected static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}

}
