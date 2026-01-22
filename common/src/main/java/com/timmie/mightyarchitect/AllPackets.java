package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import com.timmie.mightyarchitect.networking.SetHotbarItemPacket;
import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;

public class AllPackets {

	public static final SimpleNetworkManager NET = SimpleNetworkManager.create(TheMightyArchitect.ID);

	public static final MessageType INSTANT_PRINT = NET.registerC2S("instant_print", InstantPrintPacket::new);
	public static final MessageType PLACE_SIGN = NET.registerC2S("place_sign", PlaceSignPacket::new);
	public static final MessageType SET_HOTBAR_ITEM = NET.registerC2S("set_hotbar_item", SetHotbarItemPacket::new);

	public static void init() {
		// Force class loading to trigger static initialization and packet registration
	}

}
