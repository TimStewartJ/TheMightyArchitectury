package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import com.timmie.mightyarchitect.networking.SetHotbarItemPacket;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

public class TheMightyArchitectFabric implements ModInitializer {

	@Override
	public void onInitialize()
	{
		TheMightyArchitect.Init(
			(name, factory) -> Registry.register(BuiltInRegistries.BLOCK, TheMightyArchitect.id(name), factory.get()),
			(name, factory) -> Registry.register(BuiltInRegistries.ITEM, TheMightyArchitect.id(name), factory.get()));

		registerPackets();
	}

	private static void registerPackets() {
		// 26.1 renamed the Fabric API accessors to Mojang-style names along with the Yarn retirement.
		//? if >=26 {
		PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> c2s = PayloadTypeRegistry.serverboundPlay();
		//?} else {
		/*PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> c2s = PayloadTypeRegistry.playC2S();
		*///?}
		c2s.register(AllPackets.INSTANT_PRINT_TYPE, AllPackets.INSTANT_PRINT_CODEC);
		c2s.register(AllPackets.PLACE_SIGN_TYPE, AllPackets.PLACE_SIGN_CODEC);
		c2s.register(AllPackets.SET_HOTBAR_ITEM_TYPE, AllPackets.SET_HOTBAR_ITEM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(AllPackets.INSTANT_PRINT_TYPE,
			(payload, context) -> InstantPrintPacket.handle(payload, wrap(context)));
		ServerPlayNetworking.registerGlobalReceiver(AllPackets.PLACE_SIGN_TYPE,
			(payload, context) -> PlaceSignPacket.handle(payload, wrap(context)));
		ServerPlayNetworking.registerGlobalReceiver(AllPackets.SET_HOTBAR_ITEM_TYPE,
			(payload, context) -> SetHotbarItemPacket.handle(payload, wrap(context)));
	}

	private static PacketContext wrap(ServerPlayNetworking.Context context) {
		return new PacketContext() {
			@Override
			public ServerPlayer player() {
				return context.player();
			}

			@Override
			public void enqueue(Runnable work) {
				// Fabric already dispatches play payloads on the server thread, so execute() runs
				// inline; it still queues correctly if that ever stops being true.
				context.server().execute(work);
			}
		};
	}
}
