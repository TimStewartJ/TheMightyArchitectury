package com.timmie.mightyarchitect.control;

import com.timmie.mightyarchitect.control.compose.Cuboid;
import com.timmie.mightyarchitect.control.compose.GroundPlan;
import com.timmie.mightyarchitect.control.compose.Room;
import com.timmie.mightyarchitect.control.design.DesignTheme;
import com.timmie.mightyarchitect.control.design.Sketch;
import com.timmie.mightyarchitect.control.palette.PaletteBlockInfo;
import com.timmie.mightyarchitect.control.palette.PaletteDefinition;
import com.timmie.mightyarchitect.networking.InstantPrintPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.*;

public class Schematic {

	/**
	 * Palette entries applied per client tick while a materialization is in flight.
	 * <p>
	 * Every palette swap, reroll and preview step re-applies the palette to the whole build volume.
	 * That used to happen inside whichever input handler asked for it.
	 */
	private static final int ENTRY_BUDGET_PER_TICK = 8192;

	/** Below this there is nothing worth spreading, so small edits stay instant. */
	private static final int INLINE_ENTRY_LIMIT = ENTRY_BUDGET_PER_TICK;

	private BlockPos anchor;
	private GroundPlan groundPlan;
	private PaletteDefinition primaryPalette;
	private PaletteDefinition secondaryPalette;

	private Sketch sketch;
	private Vector<Map<BlockPos, PaletteBlockInfo>> assembledSketch;
	private TemplateBlockAccess materializedSketch;
	private Cuboid bounds;

	private Materialization pendingMaterialization;

	private PaletteDefinition editedPalette;
	private boolean editingPrimary;
	public int seed;

	public Schematic() {
		seed = new Random().nextInt(100000);
	}

	public void setGroundPlan(GroundPlan groundPlan) {
		this.groundPlan = groundPlan;
	}

	public void setAnchor(BlockPos anchor) {
		this.anchor = anchor;
	}

	public void swapPrimaryPalette(PaletteDefinition newPalette) {
		this.primaryPalette = newPalette;
		materializeSketch();
	}

	public void swapSecondaryPalette(PaletteDefinition newPalette) {
		this.secondaryPalette = newPalette;
		materializeSketch();
	}

	public void setSketch(Sketch newSketch) {
		this.sketch = newSketch;
		assembleSketch();
		materializeSketch();
	}

	public Sketch getSketch() {
		return sketch;
	}

	public GroundPlan getGroundPlan() {
		return groundPlan;
	}

	public BlockPos getAnchor() {
		return anchor;
	}

	public PaletteDefinition getPrimary() {
		return primaryPalette;
	}

	public PaletteDefinition getSecondary() {
		return secondaryPalette;
	}

	/**
	 * @return the materialized sketch, finishing a pending materialization first. Callers that need
	 *         the blocks - printing, exporting - need all of them, so this is where a budget stops
	 *         applying.
	 */
	public TemplateBlockAccess getMaterializedSketch() {
		finishMaterializing();
		return materializedSketch;
	}

	public void assembleSketch() {
		assembledSketch = sketch.assemble();
	}

	public Cuboid getLocalBounds() {
		finishMaterializing();
		return bounds;
	}

	public Cuboid getGlobalBounds() {
		Cuboid clone = getLocalBounds().clone();
		clone.move(anchor.getX(), anchor.getY(), anchor.getZ());
		return clone;
	}

	public void startCreatingNewPalette(boolean primary) {
		editedPalette = (primary ? primaryPalette : secondaryPalette).clone();
		editedPalette.setName("");
		editingPrimary = primary;
	}

	public PaletteDefinition getCreatedPalette() {
		return editedPalette;
	}

	public void updatePalettePreview() {
		if (isEditingPrimary())
			materializeSketch(editedPalette, secondaryPalette);
		else
			materializeSketch(primaryPalette, editedPalette);
	}

	public void stopPalettePreview() {
		materializeSketch();
	}

	public void applyCreatedPalette() {
		if (isEditingPrimary())
			primaryPalette = editedPalette;
		else
			secondaryPalette = editedPalette;
		materializeSketch();
	}

	public void materializeSketch() {
		if (primaryPalette == null) {
			primaryPalette = groundPlan.theme.getDefaultPalette()
				.clone();
			secondaryPalette = groundPlan.theme.getDefaultSecondaryPalette()
				.clone();
		}

		materializeSketch(primaryPalette, secondaryPalette);
	}

	private void materializeSketch(PaletteDefinition primary, PaletteDefinition secondary) {
		// A new request supersedes one still in flight rather than queueing behind it: cycling
		// through palettes should cost one materialization, not one per press.
		pendingMaterialization = new Materialization(primary, secondary, assembledSketch.get(0),
			assembledSketch.get(1));

		if (pendingMaterialization.total <= INLINE_ENTRY_LIMIT)
			finishMaterializing();
	}

	/**
	 * Applies a slice of a pending palette change.
	 * <p>
	 * Driven from the renderer's tick, which is the one place that both runs every tick and knows
	 * whether the result is being looked at.
	 *
	 * @return true while work remains
	 */
	public boolean advanceMaterialization() {
		if (pendingMaterialization == null)
			return false;
		if (!pendingMaterialization.advance(ENTRY_BUDGET_PER_TICK))
			return true;

		publishMaterialization();
		return false;
	}

	/**
	 * True while a palette change is being applied across ticks rather than in one pass.
	 * <p>
	 * Exposed for the client test harness: "the work was deferred rather than done inline" is the
	 * property, and it is invisible from the outside otherwise.
	 */
	public boolean isMaterializing() {
		return pendingMaterialization != null;
	}

	private void finishMaterializing() {
		if (pendingMaterialization == null)
			return;
		pendingMaterialization.advance(Integer.MAX_VALUE);
		publishMaterialization();
	}

	/**
	 * Swaps the finished result in.
	 * <p>
	 * Building the {@link TemplateBlockAccess} is the one step that stays whole: it settles every
	 * block against its neighbours in two passes, so a half-applied one would show fences and walls
	 * connected to blocks that are about to change. It is also the cheap half now that it no longer
	 * allocates a {@code RandomSource} per neighbour per block.
	 */
	private void publishMaterialization() {
		bounds = pendingMaterialization.bounds;
		Map<BlockPos, BlockState> blockMap = pendingMaterialization.blockMap;
		pendingMaterialization = null;
		materializedSketch = new TemplateBlockAccess(blockMap, bounds, anchor);
	}

	/**
	 * One palette change, applied a slice at a time.
	 * <p>
	 * It accumulates into its own map and its own bounding box, so the sketch and bounds already on
	 * display stay coherent until the whole thing is ready to replace them.
	 * <p>
	 * Public for the same reason {@link Schematic#growToInclude} is: this is the half of
	 * materializing a sketch that needs no world, so it is the half a unit test can pin down. What
	 * it has to guarantee is that applying the palette in slices lands on exactly the same blocks
	 * as applying it in one pass, and that is not a property a screenshot can check.
	 */
	public static final class Materialization {

		private final PaletteDefinition primary;
		private final PaletteDefinition secondary;
		private final Map<BlockPos, PaletteBlockInfo> primaryLayer;
		private final Iterator<Map.Entry<BlockPos, PaletteBlockInfo>> primaryEntries;
		private final Iterator<Map.Entry<BlockPos, PaletteBlockInfo>> secondaryEntries;

		/** The blocks applied so far; complete once {@link #advance} has returned true. */
		public final Map<BlockPos, BlockState> blockMap;
		/** Entries across both layers, which is what the budget is measured against. */
		public final int total;
		/** The box grown over every position applied so far, or null before the first. */
		public Cuboid bounds;

		public Materialization(PaletteDefinition primary, PaletteDefinition secondary,
			Map<BlockPos, PaletteBlockInfo> primaryLayer, Map<BlockPos, PaletteBlockInfo> secondaryLayer) {
			this.primary = primary;
			this.secondary = secondary;
			this.primaryLayer = primaryLayer;
			this.primaryEntries = primaryLayer.entrySet()
				.iterator();
			this.secondaryEntries = secondaryLayer.entrySet()
				.iterator();
			this.total = primaryLayer.size() + secondaryLayer.size();
			this.blockMap = new HashMap<>(Math.max(16, total));
		}

		/** @return true once every entry of both layers has been applied */
		public boolean advance(int budget) {
			int remaining = budget;

			while (remaining > 0 && primaryEntries.hasNext()) {
				remaining--;
				Map.Entry<BlockPos, PaletteBlockInfo> entry = primaryEntries.next();
				blockMap.put(entry.getKey(), primary.get(entry.getValue()));
				bounds = growToInclude(bounds, entry.getKey());
			}
			if (primaryEntries.hasNext())
				return false;

			while (remaining > 0 && secondaryEntries.hasNext()) {
				remaining--;
				Map.Entry<BlockPos, PaletteBlockInfo> entry = secondaryEntries.next();
				BlockPos pos = entry.getKey();
				PaletteBlockInfo paletteInfo = entry.getValue();

				// The primary layer wins wherever it says it should
				PaletteBlockInfo covering = primaryLayer.get(pos);
				if (covering != null && covering.palette.isPrefferedOver(paletteInfo.palette))
					continue;

				blockMap.put(pos, secondary.get(paletteInfo));
				bounds = growToInclude(bounds, pos);
			}
			return !secondaryEntries.hasNext();
		}
	}

	/**
	 * Grows a bounding box to include one position, creating it when there is none yet.
	 * <p>
	 * Pure integer arithmetic, and the only part of {@link #materializeSketch()} that is: the rest
	 * of it ends in a {@code TemplateBlockAccess}, which reads the client's level and so cannot be
	 * built outside a running game. Kept static and public for that reason - it is what the unit
	 * suite asserts, one block at a time, instead of paying a game boot to check addition.
	 *
	 * @param bounds the box so far, or null before the first position
	 * @param pos    a position that must end up inside the box
	 * @return the grown box, which is {@code bounds} itself when one was supplied
	 */
	public static Cuboid growToInclude(Cuboid bounds, BlockPos pos) {
		if (bounds == null)
			bounds = new Room(pos, BlockPos.ZERO);

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		if (x < bounds.x) {
			bounds.width += bounds.x - x;
			bounds.x = x;
		}
		if (y < bounds.y) {
			bounds.height += bounds.y - y;
			bounds.y = y;
		}
		if (z < bounds.z) {
			bounds.length += bounds.z - z;
			bounds.z = z;
		}

		BlockPos maxPos = bounds.getOrigin()
			.offset(bounds.getSize());
		if (x >= maxPos.getX())
			bounds.width = x - bounds.x + 1;
		if (y >= maxPos.getY())
			bounds.height = y - bounds.y + 1;
		if (z >= maxPos.getZ())
			bounds.length = z - bounds.z + 1;

		return bounds;
	}

	public StructureTemplate writeToTemplate() {
		final StructureTemplate template = new StructureTemplate();
		template.setAuthor(Minecraft.getInstance().player.getName()
			.getString());

		TemplateBlockAccess sketch = getMaterializedSketch();
		sketch.localMode(true);
		template.fillFromWorld(sketch, sketch.getBounds()
			.getOrigin(),
			sketch.getBounds()
				.getSize(),
			false, null);
		sketch.localMode(false);

		return template;
	}

	public List<InstantPrintPacket> getPackets() {
		return InstantPrintPacket.sendSchematic(getMaterializedSketch().getBlockMap(), anchor);
	}

	public boolean isEditingPrimary() {
		return editingPrimary;
	}

	public DesignTheme getTheme() {
		return groundPlan.theme;
	}

	public boolean isEmpty() {
		return groundPlan == null;
	}

}
