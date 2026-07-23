package com.timmie.mightyarchitect.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.timmie.mightyarchitect.foundation.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public class GuiGameElement {

	public static GuiRenderBuilder of(ItemStack stack) {
		return new GuiItemRenderBuilder(stack);
	}

	public static GuiRenderBuilder of(ItemLike itemProvider) {
		return new GuiItemRenderBuilder(itemProvider);
	}

	public static GuiRenderBuilder of(BlockState state) {
		return new GuiBlockStateRenderBuilder(state);
	}

	public static GuiRenderBuilder of(Fluid fluid) {
		return new GuiBlockStateRenderBuilder(fluid.defaultFluidState()
			.createLegacyBlock()
			.setValue(LiquidBlock.LEVEL, 0));
	}

	public static abstract class GuiRenderBuilder {
		double xBeforeScale, yBeforeScale, zBeforeScale = 0;
		double x, y, z;
		double xRot, yRot, zRot;
		double scale = 1;
		int color = 0xFFFFFF;
		Vec3 rotationOffset = Vec3.ZERO;

		public GuiRenderBuilder atLocal(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			return this;
		}

		public GuiRenderBuilder at(double x, double y) {
			this.xBeforeScale = x;
			this.yBeforeScale = y;
			return this;
		}

		public GuiRenderBuilder at(double x, double y, double z) {
			this.xBeforeScale = x;
			this.yBeforeScale = y;
			this.zBeforeScale = z;
			return this;
		}

		public GuiRenderBuilder rotate(double xRot, double yRot, double zRot) {
			this.xRot = xRot;
			this.yRot = yRot;
			this.zRot = zRot;
			return this;
		}

		public GuiRenderBuilder rotateBlock(double xRot, double yRot, double zRot) {
			return this.rotate(xRot, yRot, zRot)
				.withRotationOffset(VecHelper.getCenterOf(BlockPos.ZERO));
		}

		public GuiRenderBuilder scale(double scale) {
			this.scale = scale;
			return this;
		}

		public GuiRenderBuilder color(int color) {
			this.color = color;
			return this;
		}

		public GuiRenderBuilder withRotationOffset(Vec3 offset) {
			this.rotationOffset = offset;
			return this;
		}

		public abstract void render(GuiGraphicsExtractor matrixStack);

		protected void prepareMatrix(PoseStack matrixStack) {
			matrixStack.pushPose();
		}

		protected void transformMatrix(PoseStack matrixStack) {
			matrixStack.translate(xBeforeScale, yBeforeScale, zBeforeScale);
			matrixStack.scale((float) scale, (float) scale, (float) scale);
			matrixStack.translate(x, y, z);
			matrixStack.scale(1, -1, 1);
			matrixStack.translate(rotationOffset.x, rotationOffset.y, rotationOffset.z);

			matrixStack.mulPose(Axis.ZP.rotationDegrees((float) zRot));
			matrixStack.mulPose(Axis.XP.rotationDegrees((float) xRot));
			matrixStack.mulPose(Axis.YP.rotationDegrees((float) yRot));
			matrixStack.translate(-rotationOffset.x, -rotationOffset.y, -rotationOffset.z);
		}

		protected void cleanUpMatrix(PoseStack matrixStack) {
			matrixStack.popPose();
		}
	}

	// 26.1 removed BlockRenderDispatcher; GUI block previews tesselate through ModelBlockRenderer.
	public static class GuiBlockStateRenderBuilder extends GuiRenderBuilder {

		protected BlockState blockState;

		public GuiBlockStateRenderBuilder(BlockState blockstate) {
			this.blockState = blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
		}

		@Override
		public void render(GuiGraphicsExtractor guiGraphics) {
			// pose() is a Matrix3x2fStack for 2D GUI; 3D block rendering needs its own PoseStack.
			PoseStack ms = new PoseStack();
			prepareMatrix(ms);

			Minecraft mc = Minecraft.getInstance();
			MultiBufferSource.BufferSource buffer = mc.renderBuffers()
				.bufferSource();

			transformMatrix(ms);

			renderModel(mc, buffer, ms);

			cleanUpMatrix(ms);
		}

		protected void renderModel(Minecraft minecraft, MultiBufferSource.BufferSource buffer, PoseStack ms) {
			MovingBlockRenderState renderState = new MovingBlockRenderState();
			renderState.blockPos = BlockPos.ZERO;
			renderState.randomSeedPos = BlockPos.ZERO;
			renderState.blockState = blockState;
			if (minecraft.level != null) {
				renderState.biome = minecraft.level.getBiome(BlockPos.ZERO);
				renderState.cardinalLighting = minecraft.level.cardinalLighting();
				renderState.lightEngine = minecraft.level.getLightEngine();
			}

			ModelBlockRenderer blockRenderer =
				new ModelBlockRenderer(minecraft.options.ambientOcclusion().get(), false, minecraft.getBlockColors());
			BlockStateModel model = minecraft.getModelManager()
				.getBlockStateModelSet()
				.get(blockState);
			BlockQuadOutput output = (x, y, z, quad, instance) -> {
				ms.pushPose();
				ms.translate(x, y, z);
				buffer.getBuffer(layerToRenderType(quad.materialInfo().layer()))
					.putBakedQuad(ms.last(), quad, instance);
				ms.popPose();
			};
			blockRenderer.tesselateBlock(output, 0, 0, 0, renderState, BlockPos.ZERO, blockState, model,
				blockState.getSeed(BlockPos.ZERO));
			buffer.endBatch();
		}

		private RenderType layerToRenderType(ChunkSectionLayer layer) {
			return switch (layer) {
				case SOLID -> RenderTypes.solidMovingBlock();
				case CUTOUT -> RenderTypes.cutoutMovingBlock();
				case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
			};
		}
	}

	public static class GuiItemRenderBuilder extends GuiRenderBuilder {

		private final ItemStack stack;

		public GuiItemRenderBuilder(ItemStack stack) {
			this.stack = stack;
		}

		public GuiItemRenderBuilder(ItemLike provider) {
			this(new ItemStack(provider));
		}

		@Override
		public void render(GuiGraphicsExtractor guiGraphics) {
			// Apply 2D scale transform using matrix stack
			guiGraphics.pose().pushMatrix();
			guiGraphics.pose().translate((float) xBeforeScale, (float) yBeforeScale);
			guiGraphics.pose().scale((float) scale, (float) scale);
			
			// Render the item at origin (transforms applied via matrix)
			guiGraphics.item(stack, 0, 0);
			
			guiGraphics.pose().popMatrix();
		}

	}
}
