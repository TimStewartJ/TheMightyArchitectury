//? if >=26 {
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
//?} else if >=1.21.6 {
/*package com.timmie.mightyarchitect.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.timmie.mightyarchitect.foundation.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
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

		//@Deprecated
		//protected void transform() {
			//RenderSystem.translated(xBeforeScale, yBeforeScale, 0);
			//RenderSystem.scaled(scale, scale, scale);
			//RenderSystem.translated(x, y, z);
			//RenderSystem.scaled(1, -1, 1);
			//RenderSystem.translated(rotationOffset.x, rotationOffset.y, rotationOffset.z);
			//RenderSystem.rotatef((float) zRot, 0, 0, 1);
			//RenderSystem.rotatef((float) xRot, 1, 0, 0);
			//RenderSystem.rotatef((float) yRot, 0, 1, 0);
			//RenderSystem.translated(-rotationOffset.x, -rotationOffset.y, -rotationOffset.z);
		//}

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
			// In 1.21.6, guiGraphics.pose() returns Matrix3x2fStack for 2D GUI.
			// For 3D item rendering, create a new PoseStack
			PoseStack ms = new PoseStack();
			prepareMatrix(ms);
//			matrixStack.translate(0, 80, 0);
			transformMatrix(ms);
			renderItemIntoGUI(ms, stack, true);
			cleanUpMatrix(ms);
		}

		public static void renderItemIntoGUI(PoseStack matrixStack, ItemStack stack, boolean useDefaultLighting) {
			ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();

			// In 1.21.6, texture filter and blend state are handled by pipeline
			matrixStack.pushPose();
			matrixStack.translate(0, 0, 100.0F);
			matrixStack.translate(8.0F, -8.0F, 0.0F);
			matrixStack.scale(16.0F, 16.0F, 16.0F);
			MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

			renderer.renderStatic(stack, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, matrixStack, buffer, null, 0);
			buffer.endBatch();

			matrixStack.popPose();
		}

	}
}*/
//?} else if >=1.21.4 {
/*package com.timmie.mightyarchitect.gui;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.timmie.mightyarchitect.foundation.utility.ColorHelper;
import com.timmie.mightyarchitect.foundation.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

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
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			//prepareLighting(matrixStack);
			Lighting.setupFor3DItems();
		}

		//@Deprecated
		//protected void transform() {
			//RenderSystem.translated(xBeforeScale, yBeforeScale, 0);
			//RenderSystem.scaled(scale, scale, scale);
			//RenderSystem.translated(x, y, z);
			//RenderSystem.scaled(1, -1, 1);
			//RenderSystem.translated(rotationOffset.x, rotationOffset.y, rotationOffset.z);
			//RenderSystem.rotatef((float) zRot, 0, 0, 1);
			//RenderSystem.rotatef((float) xRot, 1, 0, 0);
			//RenderSystem.rotatef((float) yRot, 0, 1, 0);
			//RenderSystem.translated(-rotationOffset.x, -rotationOffset.y, -rotationOffset.z);
		//}

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

	private static class GuiBlockModelRenderBuilder extends GuiRenderBuilder {

		protected BakedModel blockmodel;
		protected BlockState blockState;

		public GuiBlockModelRenderBuilder(BakedModel blockmodel, @Nullable BlockState blockState) {
			this.blockState = blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
			this.blockmodel = blockmodel;
		}

		@Override
		public void render(GuiGraphics guiGraphics) {
			var ms = guiGraphics.pose();
			prepareMatrix(ms);

			Minecraft mc = Minecraft.getInstance();
			BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
			MultiBufferSource.BufferSource buffer = mc.renderBuffers()
				.bufferSource();
			RenderType renderType = blockState.getBlock() == Blocks.AIR ? Sheets.translucentItemSheet()
				: ItemBlockRenderTypes.getRenderType(blockState);
			VertexConsumer vb = buffer.getBuffer(renderType);

			transformMatrix(ms);

			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
			renderModel(blockRenderer, buffer, renderType, vb, ms);

			cleanUpMatrix(ms);
		}

		protected void renderModel(BlockRenderDispatcher blockRenderer, MultiBufferSource.BufferSource buffer,
			RenderType renderType, VertexConsumer vb, PoseStack ms) {
			Vec3 rgb = ColorHelper.getRGB(color);
			Lighting.setupForFlatItems();
			blockRenderer.getModelRenderer()
				.renderModel(ms.last(), vb, blockState, blockmodel, (float) rgb.x, (float) rgb.y, (float) rgb.z,
					0xF000F0, OverlayTexture.NO_OVERLAY);
			buffer.endBatch();
			Lighting.setupFor3DItems();
		}
	}

	public static class GuiBlockStateRenderBuilder extends GuiBlockModelRenderBuilder {

		public GuiBlockStateRenderBuilder(BlockState blockstate) {
			super(Minecraft.getInstance()
				.getBlockRenderer()
				.getBlockModel(blockstate), blockstate);
		}

		@Override
		protected void renderModel(BlockRenderDispatcher blockRenderer, MultiBufferSource.BufferSource buffer,
				RenderType renderType, VertexConsumer vb, PoseStack ms) {
			if (blockState.getBlock() instanceof FireBlock) {
				Lighting.setupForFlatItems();
				blockRenderer.renderSingleBlock(blockState, ms, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
				buffer.endBatch();
				Lighting.setupFor3DItems();
				return;
			}

			super.renderModel(blockRenderer, buffer, renderType, vb, ms);

			if (blockState.getFluidState()
					.isEmpty())
				return;

			//todo fluids..
			//FluidRenderer.renderFluidBox(new FluidStack(blockState.getFluidState().getType(), 1000), 0, 0, 0, 1, 1, 1, buffer, ms, LightTexture.FULL_BRIGHT, false);
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
			var ms = guiGraphics.pose();
			prepareMatrix(ms);
//			matrixStack.translate(0, 80, 0);
			transformMatrix(ms);
			renderItemIntoGUI(ms, stack, true);
			cleanUpMatrix(ms);
		}

		public static void renderItemIntoGUI(PoseStack matrixStack, ItemStack stack, boolean useDefaultLighting) {
			ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
			var modelManager = Minecraft.getInstance().getModelManager();
			var itemModels = Minecraft.getInstance().getItemModelResolver();

			// maybe should use renderer.textureManager
			Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			matrixStack.pushPose();
			matrixStack.translate(0, 0, 100.0F);
			matrixStack.translate(8.0F, -8.0F, 0.0F);
			matrixStack.scale(16.0F, 16.0F, 16.0F);
			MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
			if (useDefaultLighting) {
				Lighting.setupForFlatItems();
			}

			renderer.renderStatic(stack, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, matrixStack, buffer, null, 0);
			buffer.endBatch();
			RenderSystem.enableDepthTest();
			if (useDefaultLighting) {
				Lighting.setupFor3DItems();
			}

			matrixStack.popPose();
		}

	}
}*/
//?} else {
/*package com.timmie.mightyarchitect.gui;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.timmie.mightyarchitect.foundation.utility.ColorHelper;
import com.timmie.mightyarchitect.foundation.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

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
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			//prepareLighting(matrixStack);
			Lighting.setupFor3DItems();
		}

		//@Deprecated
		//protected void transform() {
			//RenderSystem.translated(xBeforeScale, yBeforeScale, 0);
			//RenderSystem.scaled(scale, scale, scale);
			//RenderSystem.translated(x, y, z);
			//RenderSystem.scaled(1, -1, 1);
			//RenderSystem.translated(rotationOffset.x, rotationOffset.y, rotationOffset.z);
			//RenderSystem.rotatef((float) zRot, 0, 0, 1);
			//RenderSystem.rotatef((float) xRot, 1, 0, 0);
			//RenderSystem.rotatef((float) yRot, 0, 1, 0);
			//RenderSystem.translated(-rotationOffset.x, -rotationOffset.y, -rotationOffset.z);
		//}

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

	private static class GuiBlockModelRenderBuilder extends GuiRenderBuilder {

		protected BakedModel blockmodel;
		protected BlockState blockState;

		public GuiBlockModelRenderBuilder(BakedModel blockmodel, @Nullable BlockState blockState) {
			this.blockState = blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
			this.blockmodel = blockmodel;
		}

		@Override
		public void render(GuiGraphics guiGraphics) {
			var ms = guiGraphics.pose();
			prepareMatrix(ms);

			Minecraft mc = Minecraft.getInstance();
			BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
			MultiBufferSource.BufferSource buffer = mc.renderBuffers()
				.bufferSource();
			RenderType renderType = blockState.getBlock() == Blocks.AIR ? Sheets.translucentCullBlockSheet()
				: ItemBlockRenderTypes.getRenderType(blockState, true);
			VertexConsumer vb = buffer.getBuffer(renderType);

			transformMatrix(ms);

			RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
			renderModel(blockRenderer, buffer, renderType, vb, ms);

			cleanUpMatrix(ms);
		}

		protected void renderModel(BlockRenderDispatcher blockRenderer, MultiBufferSource.BufferSource buffer,
			RenderType renderType, VertexConsumer vb, PoseStack ms) {
			Vec3 rgb = ColorHelper.getRGB(color);
			Lighting.setupForFlatItems();
			blockRenderer.getModelRenderer()
				.renderModel(ms.last(), vb, blockState, blockmodel, (float) rgb.x, (float) rgb.y, (float) rgb.z,
					0xF000F0, OverlayTexture.NO_OVERLAY);
			buffer.endBatch();
			Lighting.setupFor3DItems();
		}
	}

	public static class GuiBlockStateRenderBuilder extends GuiBlockModelRenderBuilder {

		public GuiBlockStateRenderBuilder(BlockState blockstate) {
			super(Minecraft.getInstance()
				.getBlockRenderer()
				.getBlockModel(blockstate), blockstate);
		}

		@Override
		protected void renderModel(BlockRenderDispatcher blockRenderer, MultiBufferSource.BufferSource buffer,
				RenderType renderType, VertexConsumer vb, PoseStack ms) {
			if (blockState.getBlock() instanceof FireBlock) {
				Lighting.setupForFlatItems();
				blockRenderer.renderSingleBlock(blockState, ms, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
				buffer.endBatch();
				Lighting.setupFor3DItems();
				return;
			}

			super.renderModel(blockRenderer, buffer, renderType, vb, ms);

			if (blockState.getFluidState()
					.isEmpty())
				return;

			//todo fluids..
			//FluidRenderer.renderFluidBox(new FluidStack(blockState.getFluidState().getType(), 1000), 0, 0, 0, 1, 1, 1, buffer, ms, LightTexture.FULL_BRIGHT, false);
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
			var ms = guiGraphics.pose();
			prepareMatrix(ms);
//			matrixStack.translate(0, 80, 0);
			transformMatrix(ms);
			renderItemIntoGUI(ms, stack, true);
			cleanUpMatrix(ms);
		}

		public static void renderItemIntoGUI(PoseStack matrixStack, ItemStack stack, boolean useDefaultLighting) {
			ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
			BakedModel bakedModel = renderer.getModel(stack, null, null, 0);

			// maybe should use renderer.textureManager
			Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
			RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			matrixStack.pushPose();
			matrixStack.translate(0, 0, 100.0F);
			matrixStack.translate(8.0F, -8.0F, 0.0F);
			matrixStack.scale(16.0F, 16.0F, 16.0F);
			MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
			boolean flatLighting = !bakedModel.usesBlockLight();
			if (useDefaultLighting && flatLighting) {
				Lighting.setupForFlatItems();
			}

			renderer.render(stack, ItemDisplayContext.GUI, false, matrixStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, bakedModel);
			buffer.endBatch();
			RenderSystem.enableDepthTest();
			if (useDefaultLighting && flatLighting) {
				Lighting.setupFor3DItems();
			}

			matrixStack.popPose();
		}

	}
}*///?}
