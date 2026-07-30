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
// NeoForge reworked payload registration in 20.5: the event, the registrar and the registration
// method were all renamed, and codecs replaced plain buffer readers.
//? if >=1.20.5 {
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
//?} else {
/*import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
*///?}
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

		// 20.4's @Mod cannot declare a dist, so the client half is started from here. The class is
		// only touched inside this branch, so a dedicated server never loads it.
		//? if >=1.20.5 {
		//?} else {
		/*if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT)
			TheMightyArchitectForgeClient.init(modEventBus);
		*///?}
	}

	// Optional in both eras: these are client-to-server conveniences, and marking them required
	// would stop NeoForge clients from joining vanilla (or otherwise mod-less) servers at all.
	//? if >=1.20.5 {
	private static void registerPackets(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1")
			.optional();
		registrar.playToServer(AllPackets.INSTANT_PRINT_TYPE, AllPackets.INSTANT_PRINT_CODEC,
			(payload, context) -> InstantPrintPacket.handle(payload, wrap(context)));
		registrar.playToServer(AllPackets.PLACE_SIGN_TYPE, AllPackets.PLACE_SIGN_CODEC,
			(payload, context) -> PlaceSignPacket.handle(payload, wrap(context)));
		registrar.playToServer(AllPackets.SET_HOTBAR_ITEM_TYPE, AllPackets.SET_HOTBAR_ITEM_CODEC,
			(payload, context) -> SetHotbarItemPacket.handle(payload, wrap(context)));
	}
	//?} else {
	/*private static void registerPackets(RegisterPayloadHandlerEvent event) {
		// 20.4 takes the *namespace* here and carries the protocol version separately; 20.5
		// swapped the argument for the version and derives the namespace from the mod container.
		IPayloadRegistrar registrar = event.registrar(ID)
			.versioned("1")
			.optional();
		registrar.play(AllPackets.INSTANT_PRINT_ID, AllPackets.INSTANT_PRINT_DECODER::apply,
			(payload, context) -> InstantPrintPacket.handle(payload, wrap(context)));
		registrar.play(AllPackets.PLACE_SIGN_ID, AllPackets.PLACE_SIGN_DECODER::apply,
			(payload, context) -> PlaceSignPacket.handle(payload, wrap(context)));
		registrar.play(AllPackets.SET_HOTBAR_ITEM_ID, AllPackets.SET_HOTBAR_ITEM_DECODER::apply,
			(payload, context) -> SetHotbarItemPacket.handle(payload, wrap(context)));
	}
	*///?}

	//? if >=1.20.5 {
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
	//?} else {
	/*// Before 1.20.5 the context exposes an Optional player and a separate work handler.
	private static PacketContext wrap(IPayloadContext context) {
		return new PacketContext() {
			@Override
			public ServerPlayer player() {
				return (ServerPlayer) context.player()
					.orElseThrow(() -> new IllegalStateException("Serverbound payload without a player"));
			}

			@Override
			public void enqueue(Runnable work) {
				context.workHandler().execute(work);
			}
		};
	}
	*///?}
}
