package com.timmie.mightyarchitect.block;

import com.timmie.mightyarchitect.AllItems;
import com.timmie.mightyarchitect.control.design.DesignSlice.DesignSliceTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
//? if >=1.21.4 {
//?} else if >=1.20 {
/*import net.minecraft.world.level.material.MapColor;
*///?} else {
/*import net.minecraft.world.level.material.Material;
*///?}
import net.minecraft.world.phys.BlockHitResult;

public class SliceMarkerBlock extends Block {

	public static final BooleanProperty compass = BooleanProperty.create("compass");
	public static final EnumProperty<DesignSliceTrait> VARIANT = EnumProperty.<DesignSliceTrait>create("variant",
			DesignSliceTrait.class);

	//? if >=1.21.4 {
	public SliceMarkerBlock(Properties properties) {
		super(properties);
	//?} else if >=1.20 {
	/*public SliceMarkerBlock() {
		super(Properties.of().mapColor(MapColor.STONE));
	*///?} else {
	/*public SliceMarkerBlock() {
		super(Properties.of(Material.STONE));
	*///?}
		this.registerDefaultState(defaultBlockState().setValue(VARIANT, DesignSliceTrait.Standard));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(compass, VARIANT);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		if (context.getLevel().getBlockState(context.getClickedPos().below()).getBlock() == this)
			return defaultBlockState().setValue(compass, false);
		return defaultBlockState().setValue(compass, true);
	}

	@Override
	// 1.20.5 split the old use(...) hook into useItemOn/useWithoutItem and dropped the hand argument.
	//? if >=1.20.5 {
	protected InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player player,
			BlockHitResult hit) {
	//?} else {
	/*public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player,
			net.minecraft.world.InteractionHand handIn, BlockHitResult hit) {
	*///?}
		if (hit.getDirection().getAxis() == Axis.Y)
			return InteractionResult.PASS;
		if (AllItems.ARCHITECT_WAND.typeOf(player.getMainHandItem()) || AllItems.ARCHITECT_WAND.typeOf(player.getOffhandItem()))
			return InteractionResult.PASS;
		//? if >=1.21.10 {
		if (worldIn.isClientSide())
		//?} else {
		/*if (worldIn.isClientSide)
		*///?}
			return InteractionResult.SUCCESS;

		DesignSliceTrait currentTrait = state.getValue(VARIANT);
		DesignSliceTrait newTrait = currentTrait.cycle(player.isShiftKeyDown() ? -1 : 1);
		worldIn.setBlockAndUpdate(pos, state.setValue(VARIANT, newTrait));
		//? if >=26 {
		player.sendOverlayMessage(Component.literal(newTrait.getDescription()));
		//?} else {
		/*player.displayClientMessage(Component.literal(newTrait.getDescription()), true);
		*///?}

		return InteractionResult.SUCCESS;
	}

}
