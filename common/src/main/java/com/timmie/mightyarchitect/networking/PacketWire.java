package com.timmie.mightyarchitect.networking;

import com.timmie.mightyarchitect.AllPackets;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
//?} else {
/*import net.minecraft.network.FriendlyByteBuf;
*///?}

/**
 * Serialises a packet and reads it straight back, the way the loader does on the wire.
 * <p>
 * This lives in {@code common/} rather than in the server-test companion because the wire format is
 * genuinely version-variable - 1.20.5 introduced stream codecs and the registry-aware buffer, and
 * everything older writes through the payload itself onto a plain buffer - while the test modules
 * are not Stonecutter-processed and so have to stay version-agnostic.
 */
public final class PacketWire {

	private PacketWire() {
	}

	/**
	 * The outcome of one encode/decode cycle. A codec that writes and reads asymmetrically corrupts
	 * every later field, so {@code unreadBytes} is the assertion that matters most.
	 */
	public record RoundTrip(InstantPrintPacket packet, int encodedBytes, int unreadBytes,
		boolean identityMatches) {
	}

	public static RoundTrip roundTrip(RegistryAccess registries, InstantPrintPacket packet) {
		//? if >=1.20.5 {
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		AllPackets.INSTANT_PRINT_CODEC.encode(buffer, packet);
		int encoded = buffer.readableBytes();
		InstantPrintPacket decoded = AllPackets.INSTANT_PRINT_CODEC.decode(buffer);
		boolean identity = decoded.type() == AllPackets.INSTANT_PRINT_TYPE;
		//?} else {
		/*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		packet.write(buffer);
		int encoded = buffer.readableBytes();
		InstantPrintPacket decoded = AllPackets.INSTANT_PRINT_DECODER.apply(buffer);
		boolean identity = decoded.id().equals(AllPackets.INSTANT_PRINT_ID);
		*///?}
		return new RoundTrip(decoded, encoded, buffer.readableBytes(), identity);
	}
}
