package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.platform.Env;
import net.fabricmc.api.ClientModInitializer;
//? if >=26 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?}
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class TheMightyArchitectFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		Env.setClient(true);
		MightyClient.iris_presence = FabricLoader.getInstance().isModLoaded("iris");

		MightyClient.init();
		// 26.1 renamed the Fabric API helper to Mojang-style names along with the Yarn retirement.
		//? if >=26 {
		KeyMappingHelper.registerKeyMapping(MightyClient.COMPOSE);
		KeyMappingHelper.registerKeyMapping(MightyClient.TOOL_MENU);
		//?} else {
		/*KeyBindingHelper.registerKeyBinding(MightyClient.COMPOSE);
		KeyBindingHelper.registerKeyBinding(MightyClient.TOOL_MENU);
		*///?}

		// Before 1.20.5 there is no payload type to send: packets go out as a raw buffer on a
		// named channel, so the mod serialises them itself.
		//? if >=1.20.5 {
		AllPackets.setSender(ClientPlayNetworking::send);
		//?} else {
		/*AllPackets.setSender(packet -> {
			net.minecraft.network.FriendlyByteBuf buffer =
				net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
			packet.write(buffer);
			ClientPlayNetworking.send(packet.id(), buffer);
		});
		*///?}

		OnRenderWorld.RegisterRenderEvent();
	}
}
