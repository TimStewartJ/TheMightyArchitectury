package com.timmie.mightyarchitect.platform;

/**
 * Client-to-server capability and delivery, supplied by the loader (Fabric's
 * ClientPlayNetworking, NeoForge's NetworkRegistry/PacketDistributor, Forge's SimpleChannel).
 */
public interface PacketSender {

	boolean canSendToServer(MightyPacket packet);

	void sendToServer(MightyPacket packet);
}
