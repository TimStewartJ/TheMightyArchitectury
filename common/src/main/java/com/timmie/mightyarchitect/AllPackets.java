package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import com.timmie.mightyarchitect.networking.SetHotbarItemPacket;
import com.timmie.mightyarchitect.platform.MightyPacket;
import com.timmie.mightyarchitect.platform.PacketSender;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Function;
*///?}

// Payload identities and codecs. Registration and delivery are the loader's job: this class only
// describes what the packets are, so the same description drives Fabric's PayloadTypeRegistry and
// NeoForge's PayloadRegistrar - or, before 1.20.5, the loader's raw channel registration.
public class AllPackets {

	//? if >=1.20.5 {
	public static final CustomPacketPayload.Type<InstantPrintPacket> INSTANT_PRINT_TYPE =
		new CustomPacketPayload.Type<>(TheMightyArchitect.id("instant_print"));
	public static final CustomPacketPayload.Type<PlaceSignPacket> PLACE_SIGN_TYPE =
		new CustomPacketPayload.Type<>(TheMightyArchitect.id("place_sign"));
	public static final CustomPacketPayload.Type<SetHotbarItemPacket> SET_HOTBAR_ITEM_TYPE =
		new CustomPacketPayload.Type<>(TheMightyArchitect.id("set_hotbar_item"));

	public static final StreamCodec<RegistryFriendlyByteBuf, InstantPrintPacket> INSTANT_PRINT_CODEC =
		StreamCodec.of(InstantPrintPacket::write, InstantPrintPacket::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, PlaceSignPacket> PLACE_SIGN_CODEC =
		StreamCodec.of(PlaceSignPacket::write, PlaceSignPacket::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, SetHotbarItemPacket> SET_HOTBAR_ITEM_CODEC =
		StreamCodec.of(SetHotbarItemPacket::write, SetHotbarItemPacket::new);
	//?} else {
	/*// Before 1.20.5 there are no payload types and no stream codecs: a packet is a channel id
	// plus a decoder, and the loader pairs the two up itself.
	public static final ResourceLocation INSTANT_PRINT_ID = TheMightyArchitect.id("instant_print");
	public static final ResourceLocation PLACE_SIGN_ID = TheMightyArchitect.id("place_sign");
	public static final ResourceLocation SET_HOTBAR_ITEM_ID = TheMightyArchitect.id("set_hotbar_item");

	public static final Function<FriendlyByteBuf, InstantPrintPacket> INSTANT_PRINT_DECODER = InstantPrintPacket::new;
	public static final Function<FriendlyByteBuf, PlaceSignPacket> PLACE_SIGN_DECODER = PlaceSignPacket::new;
	public static final Function<FriendlyByteBuf, SetHotbarItemPacket> SET_HOTBAR_ITEM_DECODER = SetHotbarItemPacket::new;
	*///?}

	private static PacketSender sender = packet -> {
	};

	public static void setSender(PacketSender value) {
		sender = value;
	}

	public static void sendToServer(MightyPacket packet) {
		sender.sendToServer(packet);
	}
}
