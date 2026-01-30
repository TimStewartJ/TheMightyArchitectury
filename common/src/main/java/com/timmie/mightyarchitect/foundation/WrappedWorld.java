package com.timmie.mightyarchitect.foundation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import org.jetbrains.annotations.Nullable;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WrappedWorld extends Level {

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
			world.isClientSide, false, 0L, 0);
		this.world = world;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return world.getBlockState(pos);
	}

	// In 1.21.6, Level requires these playSeededSound implementations with Entity as first parameter (source/excluded entity)

	@Override
	public void playSeededSound(@Nullable Entity source, @Nullable Entity entity, Holder<SoundEvent> holder, SoundSource soundSource, float f, float g, long l) {
		// No-op for wrapped world
	}

	@Override
	public void playSeededSound(@Nullable Entity source, double x, double y, double z, Holder<SoundEvent> holder, SoundSource soundSource, float volume, float pitch, long seed) {
		// No-op for wrapped world
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
	public void levelEvent(@Nullable Entity entity, int type, BlockPos pos, int data) {}

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
	public void playSound(@Nullable Entity source, double x, double y, double z, Holder<SoundEvent> soundIn, SoundSource category,
		float volume, float pitch) {}

	@Override
	public void playSound(@Nullable Entity source, Entity entity, SoundEvent sound,
		SoundSource category, float volume, float pitch) {}

	@Override
	public String gatherChunkSourceStats() {
		return world.gatherChunkSourceStats();
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

	// In 1.21.6, setMapData and getFreeMapId are on ServerLevel, not Level

	@Override
	public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {}

	@Override
	public Scoreboard getScoreboard() {
		return world.getScoreboard();
	}

	@Override
	public RecipeAccess recipeAccess() {
		return world.recipeAccess();
	}

	@Override
	public FuelValues fuelValues() {
		return world.fuelValues();
	}

	@Override
	public java.util.Collection<net.minecraft.world.entity.boss.EnderDragonPart> dragonParts() {
		return world.dragonParts();
	}

	@Override
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
	public void explode(
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
	public float getShade(Direction p_230487_1_, boolean p_230487_2_) {
		return 1;
	}

	@Override
	protected LevelEntityGetter<Entity> getEntities() {
		return entityGetter;
	}

	public Level getWorld() {
		return world;
	}

}
