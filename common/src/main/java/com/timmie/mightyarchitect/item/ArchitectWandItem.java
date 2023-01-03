package com.timmie.mightyarchitect.item;

import com.timmie.mightyarchitect.AllBlocks;
import com.timmie.mightyarchitect.control.ArchitectManager;
import com.timmie.mightyarchitect.control.design.DesignExporter;
import com.timmie.mightyarchitect.control.phase.ArchitectPhases;
import com.timmie.mightyarchitect.control.phase.export.PhaseEditTheme;
import com.timmie.mightyarchitect.gui.DesignExporterScreen;
import com.timmie.mightyarchitect.gui.ScreenHelper;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
		PhaseEditTheme.resetVisualization();
	}

	@Environment(EnvType.CLIENT)
	protected void handleUseOnDesignAnchor(Player player, Level world, BlockPos anchor, BlockState blockState) {
		if (AllBlocks.DESIGN_ANCHOR.typeOf(blockState)) {
			if (!ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
				return;

			String name = DesignExporter.exportDesign(world, anchor);
			if (!name.isEmpty()) {
				player.displayClientMessage(Component.literal(name), true);
			}

		} else {
			if (!ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
				return;
			EnvExecutor.runInEnv(EnvType.CLIENT, () -> this::resetVisualization);
		}
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
		if (!ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
			return;

		if (playerIn.isShiftKeyDown()) {
			openGui();

		} else {
			resetVisualization();
		}
	}

	@Environment(EnvType.CLIENT)
	private void openGui() {
		if (!ArchitectManager.inPhase(ArchitectPhases.EditingThemes))
			return;
		ScreenHelper.open(new DesignExporterScreen());
	}
}
