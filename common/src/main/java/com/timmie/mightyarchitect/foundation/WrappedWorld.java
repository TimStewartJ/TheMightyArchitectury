package com.timmie.mightyarchitect.foundation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
//? if >=26 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.clock.ClockManager;
//?} else {
/*
*///?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
//? if >=26 {
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
//?} else if >=1.21.4 {
/*import net.minecraft.world.item.crafting.RecipeAccess;
*///?} else {
/*import net.minecraft.world.item.crafting.RecipeManager;
*///?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.4 {
import net.minecraft.world.level.block.entity.FuelValues;
//?} else {
/*
*///?}
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
//? if >=1.21.6 {
import org.jetbrains.annotations.Nullable;
//?} else {
/*
*///?}
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.TickRateManager;

//? if >=1.21.6 {
//?} else {
/*import javax.annotation.Nullable;
*///?}
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

//? if >=26 {
public class WrappedWorld extends Level implements BlockAndTintGetter {
//?} else {
/*public class WrappedWorld extends Level {
*///?}

	protected Level world;

	protected LevelEntityGetter<Entity> entityGetter = new LevelEntityGetter<Entity>() {
		@Nullable
		@Override
		public Entity get(int p_156931_) {
			return null;
		}

		@Nullable
		@Override
		public Entity get(UUID p_156939_) {
			return null;
		}

		@Override
		public Iterable<Entity> getAll() {
			return Collections.emptyList();
		}

		@Override
		public <U extends Entity> void get(EntityTypeTest<Entity, U> entityTypeTest, AbortableIterationConsumer<U> abortableIterationConsumer) {

		}

		@Override
		public void get(AABB p_156937_, Consumer<Entity> p_156938_) {

		}

		@Override
		public <U extends Entity> void get(EntityTypeTest<Entity, U> entityTypeTest, AABB aABB, AbortableIterationConsumer<U> abortableIterationConsumer) {

		}
	};

	public WrappedWorld(Level world) {
		super((WritableLevelData) world.getLevelData(), world.dimension(), world.registryAccess(), world.dimensionTypeRegistration(),
			//? if >=1.21.10 {
			world.isClientSide(), false, 0L, 0);
			//?} else if >=1.21.4 {
			/*world.isClientSide, false, 0L, 0);
			*///?} else {
			/*() -> world.getProfiler(), world.isClientSide, false, 0, 0);
			*///?}
		this.world = world;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return world.getBlockState(pos);
	}

	// Level requires these playSeededSound overloads (Entity source as the first parameter).
	//? if >=1.21.6 {
	//?} else {
	/*@Override
	public void playSeededSound(@org.jetbrains.annotations.Nullable Player player, double d, double e, double f, Holder<SoundEvent> holder, SoundSource soundSource, float g, float h, long l) {
	*///?}

	//? if >=1.21.6 {
	//?} else {
	/*}

	*///?}
	@Override
	//? if >=1.21.6 {
	public void playSeededSound(@Nullable Entity source, @Nullable Entity entity, Holder<SoundEvent> holder, SoundSource soundSource, float f, float g, long l) {
		// No-op for wrapped world
	//?} else {
	/*public void playSeededSound(@org.jetbrains.annotations.Nullable Player player, double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h, long l) {

	*///?}
	}

	@Override
	//? if >=1.21.6 {
	public void playSeededSound(@Nullable Entity source, double x, double y, double z, Holder<SoundEvent> holder, SoundSource soundSource, float volume, float pitch, long seed) {
		// No-op for wrapped world
	//?} else {
	/*public void playSeededSound(@org.jetbrains.annotations.Nullable Player player, Entity entity, Holder<SoundEvent> holder, SoundSource soundSource, float f, float g, long l) {

	*///?}
	}

	@Override
	public boolean isStateAtPosition(BlockPos p_217375_1_, Predicate<BlockState> p_217375_2_) {
		return world.isStateAtPosition(p_217375_1_, p_217375_2_);
	}

	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return world.getBlockEntity(pos);
	}

	@Override
	public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
		return world.setBlock(pos, newState, flags);
	}

	@Override
	public int getMaxLocalRawBrightness(BlockPos pos) {
		return 15;
	}

	@Override
	public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
		world.sendBlockUpdated(pos, oldState, newState, flags);
	}

	@Override
	public LevelTickAccess<Block> getBlockTicks() {
		return world.getBlockTicks();
	}

	@Override
	public LevelTickAccess<Fluid> getFluidTicks() {
		return world.getFluidTicks();
	}

	@Override
	//? if >=1.21.6 {
	public void levelEvent(@Nullable Entity entity, int type, BlockPos pos, int data) {}
	//?} else {
	/*public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {}
	*///?}

	@Override
	public void gameEvent(Holder<GameEvent> gameEvent, Vec3 vec3, GameEvent.Context context) {

	}


	public void gameEvent(@Nullable Entity p_151549_, GameEvent p_151550_, BlockPos p_151551_) {

	}

	@Override
	public List<? extends Player> players() {
		return Collections.emptyList();
	}

	@Override
	//? if >=1.21.6 {
	public void playSound(@Nullable Entity source, double x, double y, double z, Holder<SoundEvent> soundIn, SoundSource category,
	//?} else {
	/*public void playSound(@Nullable Player player, double x, double y, double z, Holder<SoundEvent> soundIn, SoundSource category,
	*///?}
		float volume, float pitch) {}

	//? if >=1.21.6 {
	@Override
	public void playSound(@Nullable Entity source, Entity entity, SoundEvent sound,
	//?} else {
	/*public void playSound(@Nullable Player player, Entity entity, Holder<SoundEvent> sound,
	*///?}
		SoundSource category, float volume, float pitch) {}

	//? if >=1.21.6 {
	@Override
	//?} else {
	/*
	*///?}
	public String gatherChunkSourceStats() {
		//? if >=1.21.6 {
		return world.gatherChunkSourceStats();
		//?} else {
		/*return null;
		*///?}
	}

	@Override
	public Entity getEntity(int id) {
		return null;
	}

	@Override
	public MapItemSavedData getMapData(MapId mapId) {
		return null;
	}

	@Override
	public boolean addFreshEntity(Entity entityIn) {
		return world.addFreshEntity(entityIn);
	}

	// setMapData and getFreeMapId live on ServerLevel, not Level

	@Override
	//? if >=1.21.6 {
	public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {}
	//?} else {
	/*public void setMapData(MapId mapId, MapItemSavedData mapDataIn) {}
	*///?}

	@Override
	//? if >=1.21.6 {
	public Scoreboard getScoreboard() {
		return world.getScoreboard();
	//?} else {
	/*public MapId getFreeMapId() {
		return new MapId(0);
	*///?}
	}

	@Override
	//? if >=1.21.6 {
	public RecipeAccess recipeAccess() {
		return world.recipeAccess();
	}
	//?} else {
	/*public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {}
	*///?}

	@Override
	//? if >=1.21.6 {
	public FuelValues fuelValues() {
		return world.fuelValues();
	//?} else {
	/*public Scoreboard getScoreboard() {
		return world.getScoreboard();
	*///?}
	}

	@Override
	// NeoForge holds its own PartEntity type in Level's dragonParts. The shared source is compiled
	// against the patched Level in the NeoForge module and against vanilla in the Fabric one, and a
	// raw Collection is return-type-substitutable for either signature.
	@SuppressWarnings("rawtypes")
	//? if >=1.21.6 {
	public java.util.Collection dragonParts() {
		return world.dragonParts();
	//?} else if >=1.21.4 {
	/*public RecipeAccess recipeAccess() {
		return world.recipeAccess();
	*///?} else {
	/*public RecipeManager getRecipeManager() {
		return world.getRecipeManager();
	*///?}
	}

	//? if <26 {
	/*// NeoForge's day-time-per-tick extension: abstract on Level up to 1.21.11, absent from
	// vanilla. Negative means "not overridden", which is what a fake world wants.
	public void setDayTimeFraction(float fraction) {
	}

	public float getDayTimeFraction() {
		return -1.0F;
	}

	public void setDayTimePerTick(float dayTimePerTick) {
	}

	public float getDayTimePerTick() {
		return -1.0F;
	}

	*///?}

	@Override
	//? if >=1.21.11 {
	public net.minecraft.world.attribute.EnvironmentAttributeSystem environmentAttributes() {
		return world.environmentAttributes();
	}

	@Override
	//?} else if >=1.21.6 {
	/*
	*///?} else if >=1.21.4 {
	/*public FuelValues fuelValues() {
		return world.fuelValues();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public java.util.Collection dragonParts() {
		return world.dragonParts();
	}

	@Override
	*///?} else {
	/*
	*///?}
	public Holder<Biome> getUncachedNoiseBiome(int p_225604_1_, int p_225604_2_, int p_225604_3_) {
		return world.getUncachedNoiseBiome(p_225604_1_, p_225604_2_, p_225604_3_);
	}

	@Override
	public ChunkSource getChunkSource() {
		return world.getChunkSource();
	}

	@Override
	public RegistryAccess registryAccess() {
		return world.registryAccess();
	}

	@Override
	public FeatureFlagSet enabledFeatures() {
		return world.enabledFeatures();
	}

	@Override
	public PotionBrewing potionBrewing() {
		return world.potionBrewing();
	}

	@Override
	public TickRateManager tickRateManager() {
		return world.tickRateManager();
	}

	@Override
	//? if >=26 {
	public ClockManager clockManager() {
		return world.clockManager();
	}

	@Override
	public CardinalLighting cardinalLighting() {
		if (world instanceof BlockAndTintGetter tintGetter)
			return tintGetter.cardinalLighting();
		return world.dimensionType().cardinalLightType().get();
	}

	@Override
	public int getBlockTint(BlockPos pos, ColorResolver color) {
		if (world instanceof BlockAndTintGetter tintGetter)
			return tintGetter.getBlockTint(pos, color);
		return color.getColor(world.getBiome(pos).value(), pos.getX(), pos.getZ());
	}

	@Override
	public void explode(
			net.minecraft.world.entity.Entity entity,
			net.minecraft.world.damagesource.DamageSource damageSource,
			net.minecraft.world.level.ExplosionDamageCalculator calculator,
			double x, double y, double z,
			float radius, boolean fire,
			Level.ExplosionInteraction interaction,
			net.minecraft.core.particles.ParticleOptions smallParticle,
			net.minecraft.core.particles.ParticleOptions largeParticle,
			net.minecraft.util.random.WeightedList<net.minecraft.core.particles.ExplosionParticleInfo> particleInfo,
			net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound) {
		world.explode(entity, damageSource, calculator, x, y, z, radius, fire, interaction, smallParticle, largeParticle, particleInfo, sound);
	}

	@Override
	public int getSeaLevel() {
		return world.getSeaLevel();
	}

	@Override
	public net.minecraft.world.level.storage.LevelData.@org.jetbrains.annotations.Nullable RespawnData getRespawnData() {
		return null;
	}

	@Override
	public void setRespawnData(net.minecraft.world.level.storage.LevelData.RespawnData respawnData) {
		// No-op for wrapped world
	}

	//?} else if >=1.21.10 {
	/*public void explode(
			net.minecraft.world.entity.Entity entity,
			net.minecraft.world.damagesource.DamageSource damageSource,
			net.minecraft.world.level.ExplosionDamageCalculator calculator,
			double x, double y, double z,
			float radius, boolean fire,
			Level.ExplosionInteraction interaction,
			net.minecraft.core.particles.ParticleOptions smallParticle,
			net.minecraft.core.particles.ParticleOptions largeParticle,
			net.minecraft.util.random.WeightedList<net.minecraft.core.particles.ExplosionParticleInfo> particleInfo,
			net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound) {
		world.explode(entity, damageSource, calculator, x, y, z, radius, fire, interaction, smallParticle, largeParticle, particleInfo, sound);
	}

	@Override
	public int getSeaLevel() {
		return world.getSeaLevel();
	}

	@Override
	public net.minecraft.world.level.storage.LevelData.@org.jetbrains.annotations.Nullable RespawnData getRespawnData() {
		return null;
	}

	@Override
	public void setRespawnData(net.minecraft.world.level.storage.LevelData.RespawnData respawnData) {
		// No-op for wrapped world
	}

	@Override
	*///?} else if >=1.21.4 {
	/*public void explode(
			net.minecraft.world.entity.Entity entity,
			net.minecraft.world.damagesource.DamageSource damageSource,
			net.minecraft.world.level.ExplosionDamageCalculator calculator,
			double x, double y, double z,
			float radius, boolean fire,
			Level.ExplosionInteraction interaction,
			net.minecraft.core.particles.ParticleOptions smallParticle,
			net.minecraft.core.particles.ParticleOptions largeParticle,
			net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound) {
		world.explode(entity, damageSource, calculator, x, y, z, radius, fire, interaction, smallParticle, largeParticle, sound);
	}

	@Override
	public int getSeaLevel() {
		return world.getSeaLevel();
	}

	@Override
	*///?} else {
	/*
	*///?}
	public float getShade(Direction p_230487_1_, boolean p_230487_2_) {
		return 1;
	}

	@Override
	//? if >=1.21.10 {
	public net.minecraft.world.level.border.WorldBorder getWorldBorder() {
		return world.getWorldBorder();
	}

	@Override
	//?} else {
	/*
	*///?}
	protected LevelEntityGetter<Entity> getEntities() {
		return entityGetter;
	}

	public Level getWorld() {
		return world;
	}

}
