package com.timmie.mightyarchitect.foundation.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/**
 * The multiplayer entry points the automated client test drives, spelled once per era.
 * <p>
 * The test companion is not Stonecutter-processed, so it cannot name these signatures itself, and
 * it used to bind them reflectively by name instead. That only ever worked in a development
 * runtime: a production Fabric client runs under intermediary, where {@code startConnecting} is
 * {@code method_36877} and a lookup by name finds nothing at all. Guarding the call sites here is
 * what lets the same harness drive the packaged jars.
 */
public final class ServerConnect {

	private ServerConnect() {
	}

	/**
	 * Describes a direct-connect target. The third argument became a {@code ServerData.Type} in
	 * 1.20.2, having been a plain "is this a LAN server" flag before that.
	 */
	//? if >=1.20.2 {
	public static ServerData serverData(String name, String address) {
		return new ServerData(name, address, ServerData.Type.OTHER);
	}
	//?} else {
	/*public static ServerData serverData(String name, String address) {
		return new ServerData(name, address, false);
	}
	*///?}

	/**
	 * Opens the vanilla connect screen against {@code address}. {@code startConnecting} gained a
	 * quick-play flag in 1.20.1 and a nullable transfer state in 1.20.6; vanilla's own multiplayer
	 * screen passes null for the latter, which is what a plain direct connect is.
	 */
	//? if >=1.20.6 {
	public static void connect(Minecraft minecraft, Screen parent, ServerAddress address, ServerData data) {
		ConnectScreen.startConnecting(parent, minecraft, address, data, false, null);
	}
	//?} else if >=1.20.1 {
	/*public static void connect(Minecraft minecraft, Screen parent, ServerAddress address, ServerData data) {
		ConnectScreen.startConnecting(parent, minecraft, address, data, false);
	}
	*///?} else {
	/*public static void connect(Minecraft minecraft, Screen parent, ServerAddress address, ServerData data) {
		ConnectScreen.startConnecting(parent, minecraft, address, data);
	}
	*///?}
}
