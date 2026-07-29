package com.timmie.mightyarchitect.forge;

import com.timmie.mightyarchitect.AllPackets;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import com.timmie.mightyarchitect.networking.PlaceSignPacket;
import com.timmie.mightyarchitect.networking.SetHotbarItemPacket;
import com.timmie.mightyarchitect.platform.PacketContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TheMightyArchitectForge.ID)
public class TheMightyArchitectForge {

	public static final String ID = "mightyarchitect";

	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);

	public TheMightyArchitectForge(IEventBus modEventBus)
	{
		// The suppliers run when NeoForge fires RegisterEvent, and vanilla processes the block
		// registry before the item registry, so the block items still see their blocks.
		TheMightyArchitect.Init(BLOCKS::register, ITEMS::register);
		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);

		modEventBus.addListener(TheMightyArchitectForge::registerPackets);
	}

	private static void registerPackets(RegisterPayloadHandlersEvent event) {
		// Optional: these are client-to-server conveniences, and marking them required would stop
		// NeoForge clients from joining vanilla (or otherwise mod-less) servers at all.
		PayloadRegistrar registrar = event.registrar("1")
			.optional();
		registrar.playToServer(AllPackets.INSTANT_PRINT_TYPE, AllPackets.INSTANT_PRINT_CODEC,
			(payload, context) -> InstantPrintPacket.handle(payload, wrap(context)));
		registrar.playToServer(AllPackets.PLACE_SIGN_TYPE, AllPackets.PLACE_SIGN_CODEC,
			(payload, context) -> PlaceSignPacket.handle(payload, wrap(context)));
		registrar.playToServer(AllPackets.SET_HOTBAR_ITEM_TYPE, AllPackets.SET_HOTBAR_ITEM_CODEC,
			(payload, context) -> SetHotbarItemPacket.handle(payload, wrap(context)));
	}

	private static PacketContext wrap(IPayloadContext context) {
		return new PacketContext() {
			@Override
			public ServerPlayer player() {
				return (ServerPlayer) context.player();
			}

			@Override
			public void enqueue(Runnable work) {
				context.enqueueWork(work);
			}
		};
	}
}
