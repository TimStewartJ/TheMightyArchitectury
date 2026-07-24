//? if >=26 {
package com.timmie.mightyarchitect.item;

import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

		if (!world.isClientSide())
			return InteractionResult.SUCCESS;

		if (player.isShiftKeyDown()) {
			EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> openGui());
			return InteractionResult.SUCCESS;
		}

		BlockPos anchor = context.getClickedPos();
		BlockState blockState = world.getBlockState(anchor);

		EnvExecutor.runInEnv(EnvType.CLIENT,
				() -> () -> handleUseOnDesignAnchor(player, world, anchor, blockState));

		player.getCooldowns()
			.addCooldown(context.getItemInHand(), 5);
		return InteractionResult.SUCCESS;
	}

	protected void resetVisualization() {
		ArchitectWandClient.resetVisualization();
	}

	protected void handleUseOnDesignAnchor(Player player, Level world, BlockPos anchor, BlockState blockState) {
		ArchitectWandClient.handleUseOnDesignAnchor(player, world, anchor, blockState);
	}

	@Override
	public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (worldIn.isClientSide()) {
			EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> handleRightClick(worldIn, playerIn, handIn));
			playerIn.getCooldowns()
				.addCooldown(playerIn.getItemInHand(handIn), 5);
		}
		return InteractionResult.SUCCESS;
	}

	protected void handleRightClick(Level worldIn, Player playerIn, InteractionHand handIn) {
		ArchitectWandClient.handleRightClick(playerIn);
	}

	private void openGui() {
		ArchitectWandClient.openGui();
	}
}
//?} else if >=1.21.10 {
/*package com.timmie.mightyarchitect.item;

import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

		if (!world.isClientSide())
			return InteractionResult.SUCCESS;

		if (player.isShiftKeyDown()) {
			EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> openGui());
			return InteractionResult.SUCCESS;
		}

		BlockPos anchor = context.getClickedPos();
		BlockState blockState = world.getBlockState(anchor);

		EnvExecutor.runInEnv(EnvType.CLIENT,
				() -> () -> handleUseOnDesignAnchor(player, world, anchor, blockState));

		player.getCooldowns()
			.addCooldown(context.getItemInHand(), 5);
		return InteractionResult.SUCCESS;
	}

	protected void resetVisualization() {
		ArchitectWandClient.resetVisualization();
	}

	protected void handleUseOnDesignAnchor(Player player, Level world, BlockPos anchor, BlockState blockState) {
		ArchitectWandClient.handleUseOnDesignAnchor(player, world, anchor, blockState);
	}

	@Override
	public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (worldIn.isClientSide()) {
			EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> handleRightClick(worldIn, playerIn, handIn));
			playerIn.getCooldowns()
				.addCooldown(playerIn.getItemInHand(handIn), 5);
		}
		return InteractionResult.SUCCESS;
	}

	protected void handleRightClick(Level worldIn, Player playerIn, InteractionHand handIn) {
		ArchitectWandClient.handleRightClick(playerIn);
	}

	private void openGui() {
		ArchitectWandClient.openGui();
	}
}*/
//?} else if >=1.21.4 {
/*package com.timmie.mightyarchitect.item;

import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

		if (!world.isClientSide)
			return InteractionResult.SUCCESS;

		if (player.isShiftKeyDown()) {
			EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> openGui());
			return InteractionResult.SUCCESS;
		}

		BlockPos anchor = context.getClickedPos();
		BlockState blockState = world.getBlockState(anchor);

		EnvExecutor.runInEnv(EnvType.CLIENT,
				() -> () -> handleUseOnDesignAnchor(player, world, anchor, blockState));

		player.getCooldowns()
			.addCooldown(context.getItemInHand(), 5);
		return InteractionResult.SUCCESS;
	}

	@Environment(EnvType.CLIENT)
	protected void resetVisualization() {
		ArchitectWandClient.resetVisualization();
	}

	@Environment(EnvType.CLIENT)
	protected void handleUseOnDesignAnchor(Player player, Level world, BlockPos anchor, BlockState blockState) {
		ArchitectWandClient.handleUseOnDesignAnchor(player, world, anchor, blockState);
	}

	@Override
	public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (worldIn.isClientSide) {
			EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> handleRightClick(worldIn, playerIn, handIn));
			playerIn.getCooldowns()
				.addCooldown(playerIn.getItemInHand(handIn), 5);
		}
		return InteractionResult.SUCCESS;
	}

	@Environment(EnvType.CLIENT)
	protected void handleRightClick(Level worldIn, Player playerIn, InteractionHand handIn) {
		ArchitectWandClient.handleRightClick(playerIn);
	}

	@Environment(EnvType.CLIENT)
	private void openGui() {
		ArchitectWandClient.openGui();
	}
}*/
//?} else {
/*package com.timmie.mightyarchitect.item;

import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

		if (!world.isClientSide)
			return InteractionResult.SUCCESS;

		if (player.isShiftKeyDown()) {
			EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> openGui());
			return InteractionResult.SUCCESS;
		}

		BlockPos anchor = context.getClickedPos();
		BlockState blockState = world.getBlockState(anchor);

		EnvExecutor.runInEnv(EnvType.CLIENT,
				() -> () -> handleUseOnDesignAnchor(player, world, anchor, blockState));

		player.getCooldowns()
			.addCooldown(this, 5);
		return InteractionResult.SUCCESS;
	}

	@Environment(EnvType.CLIENT)
	protected void resetVisualization() {
		ArchitectWandClient.resetVisualization();
	}

	@Environment(EnvType.CLIENT)
	protected void handleUseOnDesignAnchor(Player player, Level world, BlockPos anchor, BlockState blockState) {
		ArchitectWandClient.handleUseOnDesignAnchor(player, world, anchor, blockState);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (worldIn.isClientSide) {
			EnvExecutor.runInEnv(EnvType.CLIENT, () -> () -> handleRightClick(worldIn, playerIn, handIn));
			playerIn.getCooldowns()
				.addCooldown(this, 5);
		}
		return super.use(worldIn, playerIn, handIn);
	}

	@Environment(EnvType.CLIENT)
	protected void handleRightClick(Level worldIn, Player playerIn, InteractionHand handIn) {
		ArchitectWandClient.handleRightClick(playerIn);
	}

	@Environment(EnvType.CLIENT)
	private void openGui() {
		ArchitectWandClient.openGui();
	}
}*///?}
