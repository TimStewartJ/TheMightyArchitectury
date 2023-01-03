package com.timmie.mightyarchitect;

import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import com.timmie.mightyarchitect.networking.SetHotbarItemPacket;
import dev.architectury.networking.NetworkChannel;
import net.minecraft.resources.ResourceLocation;

public class AllPackets {

	public static NetworkChannel channel;

	public static void registerPackets() {
		channel = NetworkChannel.create(new ResourceLocation(TheMightyArchitect.ID, "simple_channel"));

		int i = 0;

		channel.register(InstantPrintPacket.class, InstantPrintPacket::toBytes, InstantPrintPacket::new,
				InstantPrintPacket::handle);
		channel.register(PlaceSignPacket.class, PlaceSignPacket::toBytes, PlaceSignPacket::new,
				PlaceSignPacket::handle);
		channel.register(SetHotbarItemPacket.class, SetHotbarItemPacket::toBytes, SetHotbarItemPacket::new,
				SetHotbarItemPacket::handle);
	}

}
