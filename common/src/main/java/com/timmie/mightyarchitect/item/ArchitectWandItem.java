package com.timmie.mightyarchitect.item;

import com.timmie.mightyarchitect.platform.Env;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.21.4 {
//?} else {
/*import net.minecraft.world.InteractionResultHolder;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
//? if >=26 {
//?} else {
/*import net.minecraft.world.item.ItemStack;
*///?}
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ArchitectWandItem extends Item {

	public ArchitectWandItem(Properties properties) {
		super(properties.stacksTo(1)
			.rarity(Rarity.RARE));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level world = context.getLevel();

		//? if >=1.21.10 {
		if (!world.isClientSide())
		//?} else {
		/*if (!world.isClientSide)
		*///?}
			return InteractionResult.SUCCESS;

		if (player.isShiftKeyDown()) {
			Env.runOnClient(() -> () -> openGui());
			return InteractionResult.SUCCESS;
		}

		BlockPos anchor = context.getClickedPos();
		BlockState blockState = world.getBlockState(anchor);

		Env.runOnClient(() -> () -> handleUseOnDesignAnchor(player, world, anchor, blockState));

		player.getCooldowns()
			//? if >=1.21.4 {
			.addCooldown(context.getItemInHand(), 5);
			//?} else {
			/*.addCooldown(this, 5);
			*///?}
		return InteractionResult.SUCCESS;
	}

	protected void resetVisualization() {
		ArchitectWandClient.resetVisualization();
	}

	protected void handleUseOnDesignAnchor(Player player, Level world, BlockPos anchor, BlockState blockState) {
		ArchitectWandClient.handleUseOnDesignAnchor(player, world, anchor, blockState);
	}

	@Override
	//? if >=1.21.10 {
	public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (worldIn.isClientSide()) {
	//?} else if >=1.21.4 {
	/*public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (worldIn.isClientSide) {
	*///?} else {
	/*public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (worldIn.isClientSide) {
	*///?}
			Env.runOnClient(() -> () -> handleRightClick(worldIn, playerIn, handIn));
			playerIn.getCooldowns()
				//? if >=1.21.4 {
				.addCooldown(playerIn.getItemInHand(handIn), 5);
				//?} else {
				/*.addCooldown(this, 5);
				*///?}
		}
		//? if >=1.21.4 {
		return InteractionResult.SUCCESS;
		//?} else {
		/*return super.use(worldIn, playerIn, handIn);
		*///?}
	}

	protected void handleRightClick(Level worldIn, Player playerIn, InteractionHand handIn) {
		ArchitectWandClient.handleRightClick(playerIn);
	}

	private void openGui() {
		ArchitectWandClient.openGui();
	}
}
