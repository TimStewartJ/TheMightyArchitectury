package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.platform.Env;
import com.timmie.mightyarchitect.platform.MightyPacket;
import com.timmie.mightyarchitect.platform.PacketSender;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
//? if >=1.21.8 {
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else {
/*import net.neoforged.neoforge.network.PacketDistributor;
*///?}
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

// From 20.5 FML constructs this only on the physical client, which is also how the mod learns its
// side without reaching into loader internals. 20.4's @Mod has no dist attribute, so there the main
// entrypoint calls init() behind its own side check instead.
//? if >=1.20.5 {
@Mod(value = TheMightyArchitectForge.ID, dist = Dist.CLIENT)
public class TheMightyArchitectForgeClient {

	public TheMightyArchitectForgeClient(IEventBus modEventBus) {
//?} else {
/*public class TheMightyArchitectForgeClient {

	public static void init(IEventBus modEventBus) {
*///?}
		Env.setClient(true);

		// Key mappings have to exist before RegisterKeyMappingsEvent, which NeoForge fires while
		// Options is being built - so build them here rather than in a setup listener.
		MightyClient.init();

		AllPackets.setSender(new PacketSender() {
			@Override
			public boolean canSendToServer(MightyPacket packet) {
				if (Minecraft.getInstance().getConnection() == null)
					return false;

				// 20.4 predates the static channel query. Its packet check asks the same question
				// after wrapping the payload in the vanilla serverbound packet.
				//? if >=1.20.5 {
				return NetworkRegistry.hasChannel(Minecraft.getInstance().getConnection(), packet.type().id());
				//?} else {
				/*return NetworkRegistry.getInstance().canSendPacket(
					new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(packet),
					Minecraft.getInstance().getConnection());
				*///?}
			}

			@Override
			public void sendToServer(MightyPacket packet) {
				// The client-side distributor moved to its own class in 21.8, and before 1.20.5
				// payloads have to be wrapped in the vanilla packet first.
				//? if >=1.21.8 {
				ClientPacketDistributor.sendToServer(packet);
				//?} else if >=1.20.5 {
				/*PacketDistributor.sendToServer(packet);
				*///?} else {
				/*PacketDistributor.SERVER.noArg()
					.send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(packet));
				*///?}
			}
		});

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
