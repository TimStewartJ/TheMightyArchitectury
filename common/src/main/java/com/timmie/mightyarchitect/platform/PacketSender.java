package com.timmie.mightyarchitect.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client-to-server delivery, supplied by the loader (Fabric's ClientPlayNetworking, NeoForge's
 * PacketDistributor).
 */
@FunctionalInterface
public interface PacketSender {

	void sendToServer(CustomPacketPayload packet);
}
