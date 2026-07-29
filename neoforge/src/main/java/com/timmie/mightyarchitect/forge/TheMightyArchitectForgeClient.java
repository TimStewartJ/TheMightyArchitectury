package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.platform.Env;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
//? if >=1.21.8 {
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else {
/*import net.neoforged.neoforge.network.PacketDistributor;
*///?}
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

// Constructed by FML only on the physical client, which is also how the mod learns its side
// without reaching into loader internals.
@Mod(value = TheMightyArchitectForge.ID, dist = Dist.CLIENT)
public class TheMightyArchitectForgeClient {

	public TheMightyArchitectForgeClient(IEventBus modEventBus) {
		Env.setClient(true);

		// Key mappings have to exist before RegisterKeyMappingsEvent, which NeoForge fires while
		// Options is being built - so build them here rather than in a setup listener.
		MightyClient.init();

		//? if >=1.21.8 {
		AllPackets.setSender(ClientPacketDistributor::sendToServer);
		//?} else {
		/*AllPackets.setSender(PacketDistributor::sendToServer);
		*///?}

		modEventBus.addListener(TheMightyArchitectForgeClient::registerKeyMappings);
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		//? if >=1.21.10 {
		event.registerCategory(MightyClient.CATEGORY);
		//?} else {
		/*
		*///?}
		event.register(MightyClient.COMPOSE);
		event.register(MightyClient.TOOL_MENU);
	}
}
