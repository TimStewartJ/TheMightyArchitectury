package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import com.timmie.mightyarchitect.networking.SetHotbarItemPacket;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class AllPackets {

	public static final CustomPacketPayload.Type<InstantPrintPacket> INSTANT_PRINT_TYPE = 
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "instant_print"));
	public static final CustomPacketPayload.Type<PlaceSignPacket> PLACE_SIGN_TYPE = 
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "place_sign"));
	public static final CustomPacketPayload.Type<SetHotbarItemPacket> SET_HOTBAR_ITEM_TYPE = 
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "set_hotbar_item"));

	public static final StreamCodec<RegistryFriendlyByteBuf, InstantPrintPacket> INSTANT_PRINT_CODEC = 
		StreamCodec.of(InstantPrintPacket::write, InstantPrintPacket::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, PlaceSignPacket> PLACE_SIGN_CODEC = 
		StreamCodec.of(PlaceSignPacket::write, PlaceSignPacket::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, SetHotbarItemPacket> SET_HOTBAR_ITEM_CODEC = 
		StreamCodec.of(SetHotbarItemPacket::write, SetHotbarItemPacket::new);

	public static void init() {
		NetworkManager.registerReceiver(NetworkManager.Side.C2S, INSTANT_PRINT_TYPE, INSTANT_PRINT_CODEC, InstantPrintPacket::handle);
		NetworkManager.registerReceiver(NetworkManager.Side.C2S, PLACE_SIGN_TYPE, PLACE_SIGN_CODEC, PlaceSignPacket::handle);
		NetworkManager.registerReceiver(NetworkManager.Side.C2S, SET_HOTBAR_ITEM_TYPE, SET_HOTBAR_ITEM_CODEC, SetHotbarItemPacket::handle);
	}

	public static <T extends CustomPacketPayload> void sendToServer(T packet) {
		NetworkManager.sendToServer(packet);
	}

}
