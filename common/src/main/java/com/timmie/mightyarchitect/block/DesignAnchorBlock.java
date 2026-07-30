package com.timmie.mightyarchitect.block;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
//? if >=1.21.4 {
//?} else if >=1.20 {
/*import net.minecraft.world.level.material.MapColor;
*///?} else {
/*import net.minecraft.world.level.material.Material;
*///?}

public class DesignAnchorBlock extends Block {

	public static final BooleanProperty compass = BooleanProperty.create("compass");

	//? if >=1.21.4 {
	public DesignAnchorBlock(Properties properties) {
		super(properties);
	//?} else if >=1.20 {
	/*public DesignAnchorBlock() {
		super(Properties.of().mapColor(MapColor.STONE));
	*///?} else {
	/*public DesignAnchorBlock() {
		super(Properties.of(Material.STONE));
	*///?}
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(compass);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(compass, true);
	}
}
