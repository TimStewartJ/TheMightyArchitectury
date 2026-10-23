package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import com.timmie.mightyarchitect.platform.MightyPacket;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Forge entrypoint for 1.19.4 and 1.20.1.
 * <p>
 * This era predates vanilla's custom payloads, so packets travel as raw buffers on a
 * {@link SimpleChannel}. The shared code still describes a packet the same way - an id plus a
 * writer - so only the wiring here differs from the NeoForge node.
 */
@Mod(TheMightyArchitectForge.ID)
public class TheMightyArchitectForge {

	public static final String ID = "mightyarchitect";

	// Forge requires a channel to declare a protocol version and accept/reject predicates. These
	// packets are a client-side convenience, so both ends accept anything: a vanilla or mod-less
	// server simply never receives them, which is the same effect as NeoForge's optional().
	private static final String PROTOCOL = "1";
	private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		TheMightyArchitect.id("main"), () -> PROTOCOL, ignored -> true, ignored -> true);

	// Packets only: the mod registers no blocks or items, so a server running it never turns away a
	// player who does not have it. See TheMightyArchitect.
	public TheMightyArchitectForge() {
		registerPackets();
	}

	private static void registerPackets() {
		int index = 0;
		register(index++, InstantPrintPacket.class, AllPackets.INSTANT_PRINT_DECODER,
			InstantPrintPacket::handle);
		register(index++, PlaceSignPacket.class, AllPackets.PLACE_SIGN_DECODER,
			PlaceSignPacket::handle);
	}

	public static boolean canSendToServer(Connection connection) {
		return CHANNEL.isRemotePresent(connection);
	}

	public static void sendToServer(MightyPacket packet) {
		CHANNEL.sendToServer(packet);
	}

	private static <T extends MightyPacket> void register(int index, Class<T> type,
		Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler) {
		CHANNEL.registerMessage(index, type, (packet, buffer) -> packet.write(buffer), decoder,
			(packet, context) -> {
				NetworkEvent.Context ctx = context.get();
				handler.accept(packet, wrap(ctx));
				ctx.setPacketHandled(true);
			});
	}

	private static PacketContext wrap(NetworkEvent.Context context) {
		return new PacketContext() {
			@Override
			public ServerPlayer player() {
				return context.getSender();
			}

			@Override
			public void enqueue(Runnable work) {
				context.enqueueWork(work);
			}
		};
	}
}
