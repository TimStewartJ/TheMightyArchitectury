package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import com.timmie.mightyarchitect.networking.SetHotbarItemPacket;
import com.timmie.mightyarchitect.platform.MightyPacket;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;

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

	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);

	public TheMightyArchitectForge() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get()
			.getModEventBus();

		// DeferredRegister.register returns a RegistryObject; the shared registrar contract is
		// void, so the handle is simply discarded - the mod reads its blocks back through
		// AllBlocks, which the factories populate when Forge runs them.
		TheMightyArchitect.Init(
			(name, factory) -> BLOCKS.register(name, factory::get),
			(name, factory) -> ITEMS.register(name, factory::get));
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);

		registerPackets();
	}

	private static void registerPackets() {
		int index = 0;
		register(index++, InstantPrintPacket.class, AllPackets.INSTANT_PRINT_DECODER,
			InstantPrintPacket::handle);
		register(index++, PlaceSignPacket.class, AllPackets.PLACE_SIGN_DECODER,
			PlaceSignPacket::handle);
		register(index++, SetHotbarItemPacket.class, AllPackets.SET_HOTBAR_ITEM_DECODER,
			SetHotbarItemPacket::handle);

		AllPackets.setSender(CHANNEL::sendToServer);
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
