package com.timmie.mightyarchitect.networking;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Builds a {@link ServerPlayer} with no network connection behind it, so the automated
 * dedicated-server test can drive {@link ServerBuildGuard} with a real player against a real level
 * rather than only exercising the pure halves of it.
 * <p>
 * This lives in {@code common/} rather than in the server-test companion for the same reason
 * {@link PacketWire} does: the constructor gained a {@code ClientInformation} argument in 1.20.2 and
 * the test modules are not Stonecutter-processed. Nothing in the mod's own code paths calls it.
 * <p>
 * A player built this way is safe to ask about permissions and game mode - {@code permissions()}
 * resolves through the server rather than the connection, and {@code onUpdateAbilities} returns
 * early when there is no connection - but it is not registered with the level or the player list,
 * so it must not be used for anything that expects a connected player.
 */
public final class DetachedServerPlayer {

	private DetachedServerPlayer() {
	}

	public static ServerPlayer create(MinecraftServer server, ServerLevel level, String name) {
		GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name);
		//? if >=1.20.2 {
		return new ServerPlayer(server, level, profile,
			net.minecraft.server.level.ClientInformation.createDefault());
		//?} else {
		/*return new ServerPlayer(server, level, profile);
		*///?}
	}
}
