package com.timmie.mightyarchitect.platform;

import net.minecraft.server.level.ServerPlayer;

/**
 * The part of a received-packet context the shared handlers actually need. Both loaders already run
 * play payload handlers on the server thread, but {@link #enqueue(Runnable)} keeps that guarantee
 * explicit and in the loader's hands.
 */
public interface PacketContext {

	ServerPlayer player();

	void enqueue(Runnable work);
}
