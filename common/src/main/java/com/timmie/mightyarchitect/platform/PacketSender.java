package com.timmie.mightyarchitect.platform;

/**
 * Client-to-server delivery, supplied by the loader (Fabric's ClientPlayNetworking, NeoForge's
 * PacketDistributor, Forge's SimpleChannel).
 */
@FunctionalInterface
public interface PacketSender {

	void sendToServer(MightyPacket packet);
}
