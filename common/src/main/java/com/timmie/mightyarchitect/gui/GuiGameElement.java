package com.timmie.mightyarchitect.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.timmie.mightyarchitect.foundation.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
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

		public abstract void render(GuiGraphics matrixStack);

		@Deprecated
		protected void prepare() {}

		protected void prepareMatrix(PoseStack matrixStack) {
			matrixStack.pushPose();
			// In 1.21.6, lighting is handled by the render pipeline
		}

		/*@Deprecated
		protected void transform() {
			RenderSystem.translated(xBeforeScale, yBeforeScale, 0);
			RenderSystem.scaled(scale, scale, scale);
			RenderSystem.translated(x, y, z);
			RenderSystem.scaled(1, -1, 1);
			RenderSystem.translated(rotationOffset.x, rotationOffset.y, rotationOffset.z);
			RenderSystem.rotatef((float) zRot, 0, 0, 1);
			RenderSystem.rotatef((float) xRot, 1, 0, 0);
			RenderSystem.rotatef((float) yRot, 0, 1, 0);
			RenderSystem.translated(-rotationOffset.x, -rotationOffset.y, -rotationOffset.z);
		}*/

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

		@Deprecated
		protected void cleanUp() {}

		protected void cleanUpMatrix(PoseStack matrixStack) {
			matrixStack.popPose();
		}
	}

	// In 1.21.6, BakedModel rendering is simplified - we use renderSingleBlock directly
	public static class GuiBlockStateRenderBuilder extends GuiRenderBuilder {

		protected BlockState blockState;

		public GuiBlockStateRenderBuilder(BlockState blockstate) {
			this.blockState = blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
		}

		@Override
		public void render(GuiGraphics guiGraphics) {
			// In 1.21.6, guiGraphics.pose() returns Matrix3x2fStack for 2D GUI.
			// For 3D block rendering, create a new PoseStack
			PoseStack ms = new PoseStack();
			prepareMatrix(ms);

			Minecraft mc = Minecraft.getInstance();
			BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
			MultiBufferSource.BufferSource buffer = mc.renderBuffers()
				.bufferSource();

			transformMatrix(ms);

			renderModel(blockRenderer, buffer, ms);

			cleanUpMatrix(ms);
		}

		protected void renderModel(BlockRenderDispatcher blockRenderer, MultiBufferSource.BufferSource buffer, PoseStack ms) {
			// In 1.21.6, lighting is handled internally by the renderer
			blockRenderer.renderSingleBlock(blockState, ms, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			buffer.endBatch();
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
		public void render(GuiGraphics guiGraphics) {
			// In 1.21, guiGraphics.pose() returns Matrix3x2fStack for 2D transformations
			// For simple item rendering with position offset, use renderItem directly
			int renderX = (int) xBeforeScale;
			int renderY = (int) yBeforeScale;
			
			// Apply 2D scale transform using matrix stack
			guiGraphics.pose().pushMatrix();
			guiGraphics.pose().translate((float) xBeforeScale, (float) yBeforeScale);
			guiGraphics.pose().scale((float) scale, (float) scale);
			
			// Render the item at origin (transforms applied via matrix)
			guiGraphics.renderItem(stack, 0, 0);
			
			guiGraphics.pose().popMatrix();
		}

	}
}
