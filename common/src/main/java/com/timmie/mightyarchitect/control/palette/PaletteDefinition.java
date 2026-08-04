package com.timmie.mightyarchitect.control.palette;

import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.storage.JsonStorage;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//? if >=1.21.4 {
import net.minecraft.world.level.block.state.properties.EnumProperty;
//?} else {
/*import net.minecraft.world.level.block.state.properties.DirectionProperty;*///?}
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PaletteDefinition {

	/** The object a palette file wraps its entries in. */
	public static final String PALETTE_KEY = "Palette";
	/** The one key inside that object which is not a palette slot. */
	private static final String NAME_KEY = "Name";

	/**
	 * One value inside a palette object: a block state, or anything else.
	 * <p>
	 * The "anything else" arm is what makes this total, and it has to be. A palette file mixes its
	 * own {@code Name} string in among the slots, and a palette written while some other mod was
	 * installed still names that mod's blocks afterwards - so a strict codec would reject a whole
	 * file over one entry. The old reader resolved an unknown block to air and carried on; dropping
	 * the user's entire palette instead would be a regression, not a fix.
	 */
	private static final Codec<Either<BlockState, Dynamic<?>>> ENTRY =
		Codec.either(BlockState.CODEC, Codec.PASSTHROUGH);

	/** The inside of the {@code Palette} object: the name, alongside one entry per slot. */
	public static final Codec<PaletteDefinition> ENTRIES_CODEC = Codec.unboundedMap(Codec.STRING, ENTRY)
		.xmap(PaletteDefinition::fromEntries, PaletteDefinition::toEntries);

	/** A whole palette file, {@code {"Palette": {...}}}. */
	public static final Codec<PaletteDefinition> CODEC = ENTRIES_CODEC.fieldOf(PALETTE_KEY)
		.codec();

	private Map<Palette, BlockState> definition;
	private String name;
	private BlockState clear;
	private static PaletteDefinition defaultPalette;

	/**
	 * The palette every other one starts from.
	 * <p>
	 * This is a single shared instance and {@link PaletteDefinition} is mutable, so anything that
	 * stores it, hands it to the palette editor or exports it must {@link #clone()} first -
	 * otherwise editing one theme's palette silently rewrites the default for all of them.
	 */
	public static PaletteDefinition defaultPalette() {
		if (defaultPalette == null) {
			defaultPalette = new PaletteDefinition("Standard Palette");
			defaultPalette.put(Palette.HEAVY_PRIMARY, Blocks.POLISHED_ANDESITE)
					.put(Palette.HEAVY_SECONDARY, Blocks.COBBLESTONE)
					//? if >=26.2 {
					/*.put(Palette.HEAVY_WINDOW, Blocks.STAINED_GLASS_PANE.black())
					*///?} else {
					.put(Palette.HEAVY_WINDOW, Blocks.BLACK_STAINED_GLASS_PANE)
					//?}
					.put(Palette.HEAVY_POST, Blocks.MOSSY_COBBLESTONE_WALL)
					.put(Palette.INNER_DETAIL, Blocks.SPRUCE_WOOD).put(Palette.INNER_PRIMARY, Blocks.SPRUCE_PLANKS)
					.put(Palette.INNER_SECONDARY, Blocks.DARK_OAK_PLANKS)
					.put(Palette.OUTER_FLAT,
							Blocks.OAK_TRAPDOOR.defaultBlockState()
									.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
									.setValue(BlockStateProperties.OPEN, true))
					.put(Palette.OUTER_SLAB, Blocks.COBBLESTONE_SLAB).put(Palette.OUTER_THICK, Blocks.COBBLESTONE_WALL)
					.put(Palette.OUTER_THIN, Blocks.SPRUCE_FENCE).put(Palette.ROOF_PRIMARY, Blocks.GRANITE)
					.put(Palette.FLOOR, Blocks.OAK_PLANKS).put(Palette.ROOF_DETAIL, Blocks.BRICKS)
					.put(Palette.CLEAR, Blocks.BARRIER).put(Palette.ROOF_SLAB, Blocks.BRICK_SLAB)
					.put(Palette.WINDOW, Blocks.GLASS_PANE);
		}
		return defaultPalette;
	}

	public PaletteDefinition clone() {
		PaletteDefinition clone = new PaletteDefinition(name);
		clone.clear = defaultPalette().clear();
		clone.definition = new HashMap<>(defaultPalette().getDefinition());
		definition.forEach((key, value) -> clone.definition.put(key, value));
		clone.definition.put(Palette.CLEAR, Blocks.BARRIER.defaultBlockState());
		return clone;
	}

	public PaletteDefinition(String name) {
		definition = new HashMap<>();
		definition.put(Palette.CLEAR, Blocks.BARRIER.defaultBlockState());
		this.name = name;
	}

	public PaletteDefinition put(Palette key, Block block) {
		return put(key, block.defaultBlockState());
	}

	public PaletteDefinition put(Palette key, BlockState block) {
		if (block.getBlock() instanceof TrapDoorBlock)
			block = block.setValue(TrapDoorBlock.OPEN, true);
		definition.put(key, block);
		return this;
	}

	public Map<Palette, BlockState> getDefinition() {
		return definition;
	}

	public BlockState clear() {
		if (clear == null)
			clear = get(Palette.CLEAR);
		return clear;
	}

	public BlockState get(Palette key) {
		BlockState iBlockState = get(key, BlockOrientation.NONE);
		if (iBlockState.getBlock() instanceof LeavesBlock) {
			iBlockState = iBlockState.setValue(LeavesBlock.PERSISTENT, true);
		}
		return iBlockState;
	}

	private BlockState get(Palette key, BlockOrientation orientation) {
		BlockState iBlockState = definition.get(key);
		return iBlockState == null ? Blocks.AIR.defaultBlockState() : orientation.apply(iBlockState);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Writes the palette into the {@code Palette} object of the given compound.
	 * <p>
	 * The same codec drives this and the JSON files, so the packed {@code .theme} archive and a
	 * loose {@code palette.json} cannot describe the same palette differently.
	 */
	public CompoundTag writeToNBT(CompoundTag compound) {
		compound = (compound == null) ? new CompoundTag() : compound;
		CompoundTag target = compound;
		JsonStorage.toNbt(ENTRIES_CODEC, this)
			.ifPresent(entries -> target.put(PALETTE_KEY, entries));
		return compound;
	}

	/**
	 * @return the palette the compound describes, falling back to a fresh default rather than null
	 */
	public static PaletteDefinition fromNBT(CompoundTag compound) {
		if (compound == null)
			return defaultPalette().clone();
		return JsonStorage.fromNbt(CODEC, compound, "a palette")
			.orElseGet(() -> defaultPalette().clone());
	}

	/**
	 * Rebuilds a palette from the decoded entries of a palette object.
	 * <p>
	 * Starts from a clone of the default so that a slot the file does not mention keeps the
	 * standard block rather than becoming air, and finishes by forcing {@code CLEAR}: it is the
	 * marker the composer clears with, not something a file gets to choose.
	 */
	private static PaletteDefinition fromEntries(Map<String, Either<BlockState, Dynamic<?>>> entries) {
		PaletteDefinition palette = defaultPalette().clone();

		entries.forEach((key, value) -> {
			if (NAME_KEY.equals(key)) {
				value.right()
					.flatMap(dynamic -> dynamic.asString()
						.result())
					.ifPresent(palette::setName);
				return;
			}

			Palette slot = slotOrNull(key);
			if (slot == null)
				return;

			if (value.left()
				.isPresent())
				palette.put(slot, value.left()
					.get());
			else
				TheMightyArchitect.logger.warn("Ignoring unreadable block for palette slot {}", key);
		});

		palette.put(Palette.CLEAR, Blocks.BARRIER.defaultBlockState());
		return palette;
	}

	/** Insertion-ordered so a rewritten palette file is a readable diff of the previous one. */
	private Map<String, Either<BlockState, Dynamic<?>>> toEntries() {
		Map<String, Either<BlockState, Dynamic<?>>> entries = new LinkedHashMap<>();
		entries.put(NAME_KEY,
			Either.right(new Dynamic<>(JsonOps.INSTANCE, new JsonPrimitive(getName()))));
		for (Palette key : Palette.values())
			entries.put(key.name(), Either.left(get(key)));
		return entries;
	}

	/**
	 * @return the slot that name refers to, or null after warning - a file written by a newer build
	 *         can name a slot this one does not have, and that costs it one entry rather than all
	 *         of them
	 */
	private static Palette slotOrNull(String name) {
		try {
			return Palette.valueOf(name);
		} catch (IllegalArgumentException unknown) {
			TheMightyArchitect.logger.warn("Ignoring unknown palette slot '{}'", name);
			return null;
		}
	}

	public BlockState get(PaletteBlockInfo paletteInfo) {
		BlockState state = definition.get(paletteInfo.palette);
		state = state == null ? Blocks.AIR.defaultBlockState() : paletteInfo.apply(state);

		Collection<Property<?>> properties = state.getProperties();

		for (Property<?> property : properties) {
			//? if >=1.21.4 {
			if (property instanceof EnumProperty<?> enumProperty && enumProperty.getValueClass() == Direction.class) {
				@SuppressWarnings("unchecked")
				EnumProperty<Direction> directionProperty = (EnumProperty<Direction>) property;
				Direction facing = state.getValue(directionProperty);
			//?} else {
			/*if (property instanceof DirectionProperty) {
				Direction facing = (Direction) state.getValue(property);*///?}
				if (facing.getAxis() == Axis.Y)
					continue;

				if ((paletteInfo.mirrorZ && facing.getAxis() != Axis.Z)
						|| (paletteInfo.mirrorX && facing.getAxis() != Axis.X))
					//? if >=1.21.4 {
					state = state.setValue(directionProperty, facing.getOpposite());
					//?} else {
					/*state = state.setValue((DirectionProperty) property, facing.getOpposite());*///?}
			}
		}

		return state;
	}

	public String getDuplicates() {
		for (Palette key : definition.keySet()) {
			Palette keyIgnoreRotation = getKeyIgnoreRotation(definition.get(key));
			if (key != keyIgnoreRotation) {
				return key.getDisplayName() + " = " + keyIgnoreRotation.getDisplayName();
			}
		}
		return "";
	}

	public boolean hasDuplicates() {
		for (Palette key : definition.keySet()) {
			if (key != getKeyIgnoreRotation(definition.get(key))) {
				return true;
			}
		}
		return false;
	}

	public Palette scan(BlockState state) {
		if (state.getBlock() == Blocks.AIR)
			return null;

		if (definition.containsValue(state)) {
			for (Palette key : definition.keySet())
				if (definition.get(key).equals(state))
					return key;
		}

		// contains but rotated
		Palette keyIgnoreRotation = getKeyIgnoreRotation(state);
		return keyIgnoreRotation;
	}

	protected Palette getKeyIgnoreRotation(BlockState state) {
		Map<Block, Palette> scanMap = new HashMap<>();
		definition.forEach((palette, block) -> {
			scanMap.put(block.getBlock(), palette);
		});
		
		if (scanMap.containsKey(state.getBlock()))
			return scanMap.get(state.getBlock());
		
		return null;
	}

}
