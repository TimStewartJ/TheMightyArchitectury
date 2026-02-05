package com.timmie.mightyarchitect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.SchematicRenderer;
import com.timmie.mightyarchitect.foundation.SuperRenderTypeBuffer;
import com.timmie.mightyarchitect.foundation.utility.AnimationTickHolder;
import com.timmie.mightyarchitect.foundation.utility.Keyboard;
import com.timmie.mightyarchitect.foundation.utility.PostChainManager;
import com.timmie.mightyarchitect.foundation.utility.ShaderManager;
import com.timmie.mightyarchitect.foundation.utility.outliner.Outliner;
import com.timmie.mightyarchitect.gui.ScreenHelper;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class MightyClient {

	public static KeyMapping COMPOSE;
	public static KeyMapping TOOL_MENU;

	public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "keys"));

	public static SchematicRenderer renderer = new SchematicRenderer();
	public static Outliner outliner = new Outliner();

	public static boolean iris_presence;

	public static void init() {
		AllItems.initColorHandlers();
		COMPOSE = new KeyMapping("key.mightyclient.compose", Keyboard.G, CATEGORY);
		TOOL_MENU = new KeyMapping("key.mightyclient.toolmenu", Keyboard.LALT, CATEGORY);
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

	public static void onRenderWorld() {
		// In 1.21.6, world rendering uses PoseStack directly, not GuiGraphics
		PoseStack ms = new PoseStack();
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

	/**
	 * Processes post-processing shader effects.
	 * This should be called after the world has been rendered to apply effects like the blueprint shader.
	 *
	 * @param partialTicks The partial tick time for smooth animation
	 */
	public static void onPostRender(float partialTicks) {
		PostChainManager.processShader(partialTicks);
	}

}
