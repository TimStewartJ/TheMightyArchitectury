package com.timmie.mightyarchitect.control.design.partials;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.timmie.mightyarchitect.TheMightyArchitect;
import com.timmie.mightyarchitect.control.design.DesignSlice.SliceData;
import com.timmie.mightyarchitect.control.design.partials.Wall.ExpandBehaviour;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * A design file, decoded.
 * <p>
 * One record covers every {@link Design} subclass because the file format is one flat object with a
 * handful of optional extras - a roof reads {@code Roofspan}, a tower reads {@code Radius}, a wall
 * reads {@code ExpandBehaviour}, and each ignores the rest. Modelling that as a dispatch codec keyed
 * on the design type would be tidier in the abstract, but the type is not written into the file: it
 * comes from which folder the design sits in.
 * <p>
 * This replaces four Stonecutter arms in {@code Design.applyNBT} and one in each subclass. Reading
 * {@code Size} needed them because it went through {@code NbtUtils}/{@code CompoundTag} accessors
 * whose shape changed twice in the matrix; a codec has one shape everywhere and, as a bonus,
 * accepts both on-disk spellings of {@code Size} instead of branching on the Minecraft version to
 * decide which one to expect.
 */
public record DesignData(BlockPos size, List<SliceData> layers, int roofspan, int margin, int radius,
	ExpandBehaviour expandBehaviour) {

	/**
	 * {@code Size} is an int triple on every design written since 1.20.5 and an
	 * {@code {X, Y, Z}} object in everything older - including all 674 designs that ship with the
	 * mod. Both are read; the triple is what gets written.
	 */
	private static final Codec<BlockPos> LEGACY_SIZE = RecordCodecBuilder.create(instance -> instance
		.group(Codec.INT.fieldOf("X")
			.forGetter(BlockPos::getX),
			Codec.INT.fieldOf("Y")
				.forGetter(BlockPos::getY),
			Codec.INT.fieldOf("Z")
				.forGetter(BlockPos::getZ))
		.apply(instance, BlockPos::new));

	private static final Codec<BlockPos> SIZE = Codec.either(BlockPos.CODEC, LEGACY_SIZE)
		.xmap(either -> either.map(triple -> triple, legacy -> legacy), Either::left);

	/**
	 * Tolerates a behaviour name this build does not have, and supplies one when the file has none.
	 * <p>
	 * A facade never writes {@code ExpandBehaviour}, and the previous reader ran
	 * {@code ExpandBehaviour.valueOf} on the empty string it got back for the missing key - which
	 * threw, on every version below 1.21.6.
	 */
	private static final Codec<ExpandBehaviour> EXPAND_BEHAVIOUR = Codec.STRING.xmap(name -> {
		try {
			return ExpandBehaviour.valueOf(name);
		} catch (IllegalArgumentException unknown) {
			TheMightyArchitect.logger.warn("Ignoring unknown wall expand behaviour '{}'", name);
			return ExpandBehaviour.None;
		}
	}, ExpandBehaviour::name);

	public static final Codec<DesignData> CODEC = RecordCodecBuilder.create(instance -> instance
		.group(SIZE.optionalFieldOf("Size", BlockPos.ZERO)
			.forGetter(DesignData::size),
			Codec.list(SliceData.CODEC)
				.optionalFieldOf("Layers", List.of())
				.forGetter(DesignData::layers),
			Codec.INT.optionalFieldOf("Roofspan", 0)
				.forGetter(DesignData::roofspan),
			Codec.INT.optionalFieldOf("Margin", 0)
				.forGetter(DesignData::margin),
			Codec.INT.optionalFieldOf("Radius", 0)
				.forGetter(DesignData::radius),
			EXPAND_BEHAVIOUR.optionalFieldOf("ExpandBehaviour", ExpandBehaviour.None)
				.forGetter(DesignData::expandBehaviour))
		.apply(instance, DesignData::new));
}
