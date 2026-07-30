package com.timmie.mightyarchitect.fabric;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import com.timmie.mightyarchitect.networking.SetHotbarItemPacket;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.fabricmc.api.ModInitializer;
// PayloadTypeRegistry is the 1.20.5+ payload API; before that packets are raw buffers on a channel.
//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?} else {
/*
*///?}
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

	// 26.1 renamed the Fabric API accessors to Mojang-style names along with the Yarn retirement,
	// and 1.20.5 is where the payload registry replaced raw channel handlers altogether.
	//? if >=26 {
	private static void registerPackets() {
		PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> c2s = PayloadTypeRegistry.serverboundPlay();
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
	//?} else if >=1.20.5 {
	/*private static void registerPackets() {
		PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> c2s = PayloadTypeRegistry.playC2S();
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
				context.server().execute(work);
			}
		};
	}
	*///?} else {
	/*// The raw buffer is only valid on the network thread, so each packet is decoded immediately
	// and only the handling is deferred onto the server thread.
	private static void registerPackets() {
		ServerPlayNetworking.registerGlobalReceiver(AllPackets.INSTANT_PRINT_ID,
			(server, player, handler, buf, responseSender) -> {
				InstantPrintPacket packet = AllPackets.INSTANT_PRINT_DECODER.apply(buf);
				InstantPrintPacket.handle(packet, wrap(server, player));
			});
		ServerPlayNetworking.registerGlobalReceiver(AllPackets.PLACE_SIGN_ID,
			(server, player, handler, buf, responseSender) -> {
				PlaceSignPacket packet = AllPackets.PLACE_SIGN_DECODER.apply(buf);
				PlaceSignPacket.handle(packet, wrap(server, player));
			});
		ServerPlayNetworking.registerGlobalReceiver(AllPackets.SET_HOTBAR_ITEM_ID,
			(server, player, handler, buf, responseSender) -> {
				SetHotbarItemPacket packet = AllPackets.SET_HOTBAR_ITEM_DECODER.apply(buf);
				SetHotbarItemPacket.handle(packet, wrap(server, player));
			});
	}

	private static PacketContext wrap(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
		return new PacketContext() {
			@Override
			public ServerPlayer player() {
				return player;
			}

			@Override
			public void enqueue(Runnable work) {
				server.execute(work);
			}
		};
	}
	*///?}
}
