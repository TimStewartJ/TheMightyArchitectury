package com.timmie.mightyarchitect.control.phase.export;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.timmie.mightyarchitect.AllSpecialTextures;
import com.timmie.mightyarchitect.MightyClient;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.compose.Cuboid;
import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.design.DesignSlice.DesignSliceTrait;
import com.timmie.mightyarchitect.control.design.DesignType;
import com.timmie.mightyarchitect.control.design.ExportTools;
import com.timmie.mightyarchitect.control.phase.PhaseBase;
import com.timmie.mightyarchitect.foundation.utility.BuildingHelper;
import com.timmie.mightyarchitect.foundation.utility.outliner.AABBOutline;
import com.timmie.mightyarchitect.foundation.utility.outliner.BlockClusterOutline;
import com.timmie.mightyarchitect.foundation.utility.outliner.Outline;
import com.timmie.mightyarchitect.foundation.MightyBuffers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PhaseEditTheme extends PhaseBase {

	public static Cuboid selectedDesign;
	public static Outline effectiveSelectedDesign;
	public static int effectiveHeight;

	public String frontText;
	public String rightText;
	public String backText;
	public String leftText;

	private DesignType lastType;

	@Override
	public void whenEntered() {
		selectedDesign = null;
		effectiveSelectedDesign = null;
		frontText = null;
		rightText = null;
		leftText = null;
		backText = null;
		lastType = null;
		effectiveHeight = 0;
	}

	@Override
	public void update() {
		tickOutlines();
		tickToolLabels();
		if (lastType == DesignExporter.getType())
			return;

		frontText = null;
		rightText = null;
		leftText = null;
		backText = null;

		lastType = DesignExporter.getType();

		switch (lastType) {
		case CORNER:
			frontText = "Facade";
			rightText = "Side Facade";
			break;
		case FACADE:
		case WALL:
			frontText = "Facade";
			backText = "Back";
			break;
		case FLAT_ROOF:
			frontText = "Front";
			rightText = "Back";
			backText = "Back";
			leftText = "Front";
			break;
		case ROOF:
			frontText = "Facade";
			leftText = "Side";
			rightText = "Side";
			backText = "Back";
			break;
		case TOWER:
		case TOWER_FLAT_ROOF:
		case TOWER_ROOF:
			break;
		default:
			break;
		}

	}

	static final Object effectiveDesignKey = new Object();
	static final Object textKey1 = new Object();
	static final Object textKey2 = new Object();
	static final Object textKey3 = new Object();
	static final Object textKey4 = new Object();

	private void tickOutlines() {
		if (selectedDesign == null)
			return;

		MightyClient.outliner.chaseAABB("editThemeSelection", selectedDesign.toAABB());

		if (effectiveSelectedDesign != null)
			MightyClient.outliner.show(effectiveDesignKey, effectiveSelectedDesign)
				.withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED)
				.lineWidth(1 / 16f);

		Cuboid selection = selectedDesign;
		float hw = selection.width / 2f;
		float hl = selection.length / 2f;

		chaseText(textKey1, selection.x + hw, selection.y + .5f, selection.z - 1, backText);
		chaseText(textKey2, selection.x + hw, selection.y + .5f, selection.z + selection.length + 1, frontText);
		chaseText(textKey3, selection.x + selection.width + 1, selection.y + .5f, selection.z + hl, rightText);
		chaseText(textKey4, selection.x - 1, selection.y + .5f, selection.z + hl, leftText);
	}

	private void chaseText(Object key, float x, float y, float z, String text) {
		MightyClient.outliner.chaseText(key, new Vec3(x, y, z), text)
		.disableNormals()
			.colored(0xffffff);
	}

	/** How far around the player marker and anchor blocks are labelled and guided. */
	static final int LABEL_RADIUS = 12;
	static final int LABEL_REFRESH_TICKS = 10;
	static final int MAX_LABELS = 64;
	static final int MAX_GUIDES = 4;
	/** Size of the hint drawn beside a Design Marker that has no marker column yet. */
	static final int GUIDE_HINT_SIZE = 4;

	private record ToolLabel(BlockPos pos) {
	}

	private record GuideBox(BlockPos anchor) {
	}

	private record GuideText(BlockPos anchor) {
	}

	/** A Design Marker near the player, and the design it would scan, if it has a column yet. */
	private record Guide(BlockPos anchor, Cuboid design) {
	}

	private final Map<BlockPos, String> toolLabels = new HashMap<>();
	private final List<Guide> guides = new ArrayList<>();
	private int labelRefreshTicks;

	/**
	 * Names the trait of every layer marker near the player, and the Design Marker itself. The
	 * markers are plain glazed terracotta, so this is the only place their meaning shows up. The
	 * scan is a few thousand block reads, so it is refreshed twice a second while the labels are
	 * kept alive every tick.
	 * <p>
	 * Each Design Marker also gets a guide, because the layout has a direction nothing in the world
	 * shows: without a marker column, a box on its south-east side where the design goes; with one,
	 * the outline of exactly what the Design Exporter will scan.
	 */
	private void tickToolLabels() {
		if (--labelRefreshTicks <= 0) {
			labelRefreshTicks = LABEL_REFRESH_TICKS;
			scanToolLabels();
		}
		toolLabels.forEach((pos, text) -> MightyClient.outliner
			.chaseText(new ToolLabel(pos), Vec3.atCenterOf(pos), text)
			.disableNormals()
			.colored(0xffdd55));
		for (Guide guide : guides) {
			if (guide.design() != null) {
				// The confirm-click outline already shows this one.
				if (guide.design()
					.equals(selectedDesign))
					continue;
				MightyClient.outliner.chaseAABB(new GuideBox(guide.anchor()), guide.design()
					.toAABB())
					.colored(0x66dd66)
					.lineWidth(1 / 16f);
				continue;
			}
			BlockPos corner = guide.anchor()
				.offset(1, 0, 1);
			MightyClient.outliner.chaseAABB(new GuideBox(guide.anchor()), new AABB(Vec3.atLowerCornerOf(corner),
				Vec3.atLowerCornerOf(corner.offset(GUIDE_HINT_SIZE, 1, GUIDE_HINT_SIZE))))
				.colored(0xffaa33)
				.lineWidth(1 / 16f);
			MightyClient.outliner.chaseText(new GuideText(guide.anchor()), Vec3.atLowerCornerOf(corner)
				.add(GUIDE_HINT_SIZE / 2.0, 1.5, GUIDE_HINT_SIZE / 2.0), HINT_TEXT)
				.disableNormals()
				.colored(0xffaa33);
		}
	}

	static final String HINT_TEXT = "Design goes here; layer markers past its far corner";

	private void scanToolLabels() {
		toolLabels.clear();
		guides.clear();
		Level level = minecraft.level;
		if (level == null || minecraft.player == null)
			return;
		BlockPos centre = minecraft.player.blockPosition();
		List<BlockPos> anchors = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-LABEL_RADIUS, -LABEL_RADIUS, -LABEL_RADIUS),
			centre.offset(LABEL_RADIUS, LABEL_RADIUS, LABEL_RADIUS))) {
			BlockState state = level.getBlockState(pos);
			String text = null;
			DesignSliceTrait trait = ExportTools.traitOf(state);
			if (trait != null)
				text = ExportTools.label(trait);
			else if (ExportTools.isAnchor(state)) {
				text = ExportTools.ANCHOR_NAME;
				if (anchors.size() < MAX_GUIDES)
					anchors.add(pos.immutable());
			}
			if (text == null)
				continue;
			if (toolLabels.size() < MAX_LABELS)
				toolLabels.put(pos.immutable(), text);
		}
		for (BlockPos anchor : anchors)
			guides.add(new Guide(anchor, guideDesign(level, anchor)));
	}

	/** What the exporter would scan from this Design Marker, or {@code null} with no column yet. */
	public static Cuboid guideDesign(Level level, BlockPos anchor) {
		BlockPos column = DesignExporter.findMarkerColumn(level, anchor);
		if (column == null)
			return null;
		return DesignExporter.designBounds(anchor, column, DesignExporter.markerColumnHeight(level, column));
	}

	@Override
	public void whenExited() {
		toolLabels.clear();
		guides.clear();
	}

	@Override
	public List<String> getToolTip() {
		return ImmutableList.of("Right click a Design Marker (lodestone) with the Exporter to scan.",
			"Layer markers go level with it, past the design's south-east corner.",
			"Right click the side of a layer marker to change its trait.",
			"Don't forget to pick the correct traits (type, layer, size) for the next design!");
	}

	public static void setVisualization(Cuboid bounds) {
		MightyClient.outliner.remove(effectiveDesignKey);

		selectedDesign = bounds;
		effectiveSelectedDesign = null;
		if (selectedDesign == null)
			return;

		Function<Cuboid, Outline> outlineFunc = c -> new AABBOutline(c.toAABB());
		switch (DesignExporter.getType()) {
		case CORNER:
			effectiveSelectedDesign = outlineFunc.apply(new Cuboid(selectedDesign.getOrigin(), 1, effectiveHeight, 1));
			break;
		case WALL:
		case FACADE:
			effectiveSelectedDesign =
				outlineFunc.apply(new Cuboid(selectedDesign.getOrigin(), selectedDesign.width, effectiveHeight, 1));
			break;
		case FLAT_ROOF:
			int margin = DesignExporter.getDesignParameter();
			effectiveSelectedDesign = outlineFunc.apply(new Cuboid(selectedDesign.getOrigin()
				.east(margin),
				selectedDesign.getSize()
					.offset(-margin, 0, -margin)));
			break;
		case ROOF:
			int span = DesignExporter.getDesignParameter();
			margin = (selectedDesign.width - span) / 2;
			effectiveSelectedDesign = outlineFunc.apply(new Cuboid(selectedDesign.getOrigin()
				.offset(margin, 0, 0), selectedDesign.width - 2 * margin, selectedDesign.height, 3));
			break;
		case TOWER:
		case TOWER_FLAT_ROOF:
		case TOWER_ROOF:
			int radius = DesignExporter.getDesignParameter();
			margin = (selectedDesign.width - (radius * 2 + 1)) / 2;
			BlockPos center = selectedDesign.getCenter()
				.below(selectedDesign.height / 2);
			List<BlockPos> cylinderSet = new ArrayList<>();
			for (int i = 0; i < effectiveHeight; i++) {
				final int offset = i;
				BuildingHelper.getCircle(center, radius)
					.forEach(pos -> cylinderSet.add(pos.above(offset)));
			}
			effectiveSelectedDesign = new BlockClusterOutline(cylinderSet);
			break;
		default:
			break;
		}
	}

	public static boolean isVisualizing() {
		return selectedDesign != null;
	}

	public static void resetVisualization() {
		selectedDesign = null;
		effectiveSelectedDesign = null;
		effectiveHeight = 0;
	}

	// The exporter's first click on a Design Marker only outlines what would be exported; the
	// second writes it. Esc backs out between the two.
	@Override
	public String cancellableAction() {
		return isVisualizing() ? "Cancel export" : null;
	}

	@Override
	public boolean cancelAction() {
		if (!isVisualizing())
			return false;
		resetVisualization();
		ArchitectManager.status("Export cancelled");
		return true;
	}

	@Override
	public void render(PoseStack ms, MightyBuffers buffer) {}

}
