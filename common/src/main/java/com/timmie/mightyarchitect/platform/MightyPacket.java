package com.timmie.mightyarchitect.platform;

//? if >=1.20.2 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * The mod's own packet supertype, so the shared code has one name to talk about a packet regardless
 * of what the game underneath calls one.
 * <p>
 * Vanilla gained {@code CustomPacketPayload} in 1.20.2, so from there this is simply that. Before
 * it, custom packets were raw buffers on a named channel with no vanilla type at all, so this
 * declares the identity and writer the loaders need. Either way the members below 1.20.5 are the
 * same pair ({@code id()} + {@code write(FriendlyByteBuf)}), which is why every node older than
 * 1.20.5 shares a single packet shape.
 */
//? if >=1.20.2 {
public interface MightyPacket extends CustomPacketPayload {
}
//?} else {
/*public interface MightyPacket {

	ResourceLocation id();

	void write(FriendlyByteBuf buffer);
}
*///?}
