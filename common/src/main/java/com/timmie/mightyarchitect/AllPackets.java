package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import com.timmie.mightyarchitect.networking.SetHotbarItemPacket;
import com.timmie.mightyarchitect.platform.PacketSender;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

// Payload identities and codecs. Registration and delivery are the loader's job: this class only
// describes what the packets are, so the same description drives Fabric's PayloadTypeRegistry and
// NeoForge's PayloadRegistrar.
public class AllPackets {

	//? if >=1.21.11 {
	public static final CustomPacketPayload.Type<InstantPrintPacket> INSTANT_PRINT_TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "instant_print"));
	public static final CustomPacketPayload.Type<PlaceSignPacket> PLACE_SIGN_TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "place_sign"));
	public static final CustomPacketPayload.Type<SetHotbarItemPacket> SET_HOTBAR_ITEM_TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TheMightyArchitect.ID, "set_hotbar_item"));
	//?} else {
	/*public static final CustomPacketPayload.Type<InstantPrintPacket> INSTANT_PRINT_TYPE =
		new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "instant_print"));
	public static final CustomPacketPayload.Type<PlaceSignPacket> PLACE_SIGN_TYPE =
		new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "place_sign"));
	public static final CustomPacketPayload.Type<SetHotbarItemPacket> SET_HOTBAR_ITEM_TYPE =
		new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID, "set_hotbar_item"));
	*///?}

	public static final StreamCodec<RegistryFriendlyByteBuf, InstantPrintPacket> INSTANT_PRINT_CODEC =
		StreamCodec.of(InstantPrintPacket::write, InstantPrintPacket::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, PlaceSignPacket> PLACE_SIGN_CODEC =
		StreamCodec.of(PlaceSignPacket::write, PlaceSignPacket::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, SetHotbarItemPacket> SET_HOTBAR_ITEM_CODEC =
		StreamCodec.of(SetHotbarItemPacket::write, SetHotbarItemPacket::new);

	private static PacketSender sender = packet -> {
	};

	public static void setSender(PacketSender value) {
		sender = value;
	}

	public static void sendToServer(CustomPacketPayload packet) {
		sender.sendToServer(packet);
	}
}
