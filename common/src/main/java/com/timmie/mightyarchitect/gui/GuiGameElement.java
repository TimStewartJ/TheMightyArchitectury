package com.timmie.mightyarchitect.gui;

//? if >=1.21.6 {
//?} else {
/*import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
*///?}
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.21.6 {
//?} else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
*///?}
import com.mojang.math.Axis;
//? if >=1.21.6 {
//?} else {
/*import com.timmie.mightyarchitect.foundation.utility.ColorHelper;
*///?}
import com.timmie.mightyarchitect.foundation.utility.VecHelper;
import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else if >=1.21.10 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
*///?} else if >=1.21.6 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
*///?} else if >=1.21.4 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
*///?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
*///?}
import net.minecraft.core.BlockPos;
//? if >=1.21.10 {
//?} else if >=1.21.4 {
/*import net.minecraft.world.item.ItemDisplayContext;
*///?} else {
/*import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
*///?}
import net.minecraft.world.item.ItemStack;
//? if >=1.21.6 {
import org.joml.Matrix3x2fStack;
//?} else {
/*
*///?}
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.6 {
//?} else {
/*import net.minecraft.world.level.block.FireBlock;
*///?}
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

//? if >=1.21.6 {
//?} else {
/*import javax.annotation.Nullable;

*///?}
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

		//? if >=26 {
		public abstract void render(GuiGraphicsExtractor matrixStack);
		//?} else {
		/*public abstract void render(GuiGraphics matrixStack);
		*///?}

		//? if >=26 {
		//?} else {
		/*@Deprecated
		protected void prepare() {}

		*///?}
		protected void prepareMatrix(PoseStack matrixStack) {
			matrixStack.pushPose();
			//? if >=1.21.6 {
			//?} else {
			/*RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			//prepareLighting(matrixStack);
			Lighting.setupFor3DItems();
			*///?}
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

		//? if >=26 {
		//?} else {
		/*@Deprecated
		protected void cleanUp() {}

		*///?}
		protected void cleanUpMatrix(PoseStack matrixStack) {
			matrixStack.popPose();
		}
	}

	//? if >=26 {
	// 26.1 removed BlockRenderDispatcher; GUI block previews tesselate through ModelBlockRenderer.
	public static class GuiBlockStateRenderBuilder extends GuiRenderBuilder {
	//?} else if >=1.21.6 {
	/*// In 1.21.6, BakedModel rendering is simplified - we use renderSingleBlock directly
	public static class GuiBlockStateRenderBuilder extends GuiRenderBuilder {
	*///?} else {
	/*private static class GuiBlockModelRenderBuilder extends GuiRenderBuilder {
	*///?}

		//? if >=1.21.6 {
		//?} else {
		/*protected BakedModel blockmodel;
		*///?}
		protected BlockState blockState;

		//? if >=1.21.6 {
		public GuiBlockStateRenderBuilder(BlockState blockstate) {
			this.blockState = blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
		//?} else {
		/*public GuiBlockModelRenderBuilder(BakedModel blockmodel, @Nullable BlockState blockState) {
			this.blockState = blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
			this.blockmodel = blockmodel;
		*///?}
		}

		@Override
		//? if >=26 {
		public void render(GuiGraphicsExtractor guiGraphics) {
			renderAsGuiItem(guiGraphics);
		}

		//*
		 //* From 1.21.6 the GUI is recorded into a deferred render state and replayed later, so
		 //* tesselating a block into a buffer source and flushing it mid-draw never reaches the
		 //* screen - the panel behind it is composited afterwards and covers it, leaving an empty
		 //* frame. Draw the block through the GUI's own item pipeline instead, which is recorded
		 //* alongside everything else and ends up correctly ordered.
		 //
		private void renderAsGuiItem(GuiGraphicsExtractor guiGraphics) {
			ItemStack stack = new ItemStack(blockState.getBlock());
			if (stack.isEmpty())
				return;

			float size = (float) scale;
			float guiX = (float) (xBeforeScale + x * scale);
			float guiY = (float) (yBeforeScale + y * scale) - size;
			Matrix3x2fStack pose = guiGraphics.pose();
			pose.pushMatrix();
			pose.translate(guiX, guiY);
			pose.scale(size / 16f, size / 16f);
			guiGraphics.item(stack, 0, 0);
			pose.popMatrix();
		}
		//?} else if >=1.21.6 {
		/*public void render(GuiGraphics guiGraphics) {
			renderAsGuiItem(guiGraphics);
		}

		//*
		 //* From 1.21.6 the GUI is recorded into a deferred render state and replayed later, so
		 //* tesselating a block into a buffer source and flushing it mid-draw never reaches the
		 //* screen - the panel behind it is composited afterwards and covers it, leaving an empty
		 //* frame. Draw the block through the GUI's own item pipeline instead, which is recorded
		 //* alongside everything else and ends up correctly ordered.
		 //
		private void renderAsGuiItem(GuiGraphics guiGraphics) {
			ItemStack stack = new ItemStack(blockState.getBlock());
			if (stack.isEmpty())
				return;

			float size = (float) scale;
			float guiX = (float) (xBeforeScale + x * scale);
			float guiY = (float) (yBeforeScale + y * scale) - size;
			Matrix3x2fStack pose = guiGraphics.pose();
			pose.pushMatrix();
			pose.translate(guiX, guiY);
			pose.scale(size / 16f, size / 16f);
			guiGraphics.renderItem(stack, 0, 0);
			pose.popMatrix();
		}
		*///?} else if >=1.21.4 {
		/*public void render(GuiGraphics guiGraphics) {
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
		*///?} else {
		/*public void render(GuiGraphics guiGraphics) {
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
		*///?}

		//? if >=26 {
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
		//?} else if >=1.21.6 {
		/*protected void renderModel(BlockRenderDispatcher blockRenderer, MultiBufferSource.BufferSource buffer, PoseStack ms) {
			// In 1.21.6, lighting is handled internally by the renderer
			blockRenderer.renderSingleBlock(blockState, ms, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
		*///?} else {
		/*protected void renderModel(BlockRenderDispatcher blockRenderer, MultiBufferSource.BufferSource buffer,
			RenderType renderType, VertexConsumer vb, PoseStack ms) {
			Vec3 rgb = ColorHelper.getRGB(color);
			Lighting.setupForFlatItems();
			blockRenderer.getModelRenderer()
				.renderModel(ms.last(), vb, blockState, blockmodel, (float) rgb.x, (float) rgb.y, (float) rgb.z,
					0xF000F0, OverlayTexture.NO_OVERLAY);
		*///?}
			buffer.endBatch();
			//? if >=1.21.6 {
			//?} else {
			/*Lighting.setupFor3DItems();
			*///?}
		}
		//? if >=26 {

		private RenderType layerToRenderType(ChunkSectionLayer layer) {
			return switch (layer) {
				case SOLID -> RenderTypes.solidMovingBlock();
				case CUTOUT -> RenderTypes.cutoutMovingBlock();
				case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
			};
		}
		//?} else {
		/*
		*///?}
	}

	//? if >=1.21.6 {
	//?} else {
	/*public static class GuiBlockStateRenderBuilder extends GuiBlockModelRenderBuilder {

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

	*///?}
	public static class GuiItemRenderBuilder extends GuiRenderBuilder {

		private final ItemStack stack;

		public GuiItemRenderBuilder(ItemStack stack) {
			this.stack = stack;
		}

		public GuiItemRenderBuilder(ItemLike provider) {
			this(new ItemStack(provider));
		}

		@Override
		//? if >=26 {
		public void render(GuiGraphicsExtractor guiGraphics) {
			// Apply 2D scale transform using matrix stack
			guiGraphics.pose().pushMatrix();
			guiGraphics.pose().translate((float) xBeforeScale, (float) yBeforeScale);
			guiGraphics.pose().scale((float) scale, (float) scale);
		//?} else if >=1.21.10 {
		/*public void render(GuiGraphics guiGraphics) {
			// In 1.21, guiGraphics.pose() returns Matrix3x2fStack for 2D transformations
			// For simple item rendering with position offset, use renderItem directly
			int renderX = (int) xBeforeScale;
			int renderY = (int) yBeforeScale;
		*///?} else if >=1.21.6 {
		/*public void render(GuiGraphics guiGraphics) {
			// In 1.21.6, guiGraphics.pose() returns Matrix3x2fStack for 2D GUI.
			// For 3D item rendering, create a new PoseStack
			PoseStack ms = new PoseStack();
			prepareMatrix(ms);
//			matrixStack.translate(0, 80, 0);
			transformMatrix(ms);
			renderItemIntoGUI(ms, stack, true);
			cleanUpMatrix(ms);
		}
		*///?} else {
		/*public void render(GuiGraphics guiGraphics) {
			var ms = guiGraphics.pose();
			prepareMatrix(ms);
//			matrixStack.translate(0, 80, 0);
			transformMatrix(ms);
			renderItemIntoGUI(ms, stack, true);
			cleanUpMatrix(ms);
		}
		*///?}

			//? if >=26 {
			// Render the item at origin (transforms applied via matrix)
			guiGraphics.item(stack, 0, 0);
			//?} else if >=1.21.10 {
			/*// Apply 2D scale transform using matrix stack
			guiGraphics.pose().pushMatrix();
			guiGraphics.pose().translate((float) xBeforeScale, (float) yBeforeScale);
			guiGraphics.pose().scale((float) scale, (float) scale);
			*///?} else if >=1.21.6 {
			/*public static void renderItemIntoGUI(PoseStack matrixStack, ItemStack stack, boolean useDefaultLighting) {
			ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
			*///?} else if >=1.21.4 {
			/*public static void renderItemIntoGUI(PoseStack matrixStack, ItemStack stack, boolean useDefaultLighting) {
			ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
			var modelManager = Minecraft.getInstance().getModelManager();
			var itemModels = Minecraft.getInstance().getItemModelResolver();
			*///?} else {
			/*public static void renderItemIntoGUI(PoseStack matrixStack, ItemStack stack, boolean useDefaultLighting) {
			ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
			BakedModel bakedModel = renderer.getModel(stack, null, null, 0);
			*///?}

			//? if >=26 {
			guiGraphics.pose().popMatrix();
			//?} else if >=1.21.10 {
			/*// Render the item at origin (transforms applied via matrix)
			guiGraphics.renderItem(stack, 0, 0);

			guiGraphics.pose().popMatrix();
			*///?} else if >=1.21.6 {
			/*// In 1.21.6, texture filter and blend state are handled by pipeline
			matrixStack.pushPose();
			matrixStack.translate(0, 0, 100.0F);
			matrixStack.translate(8.0F, -8.0F, 0.0F);
			matrixStack.scale(16.0F, 16.0F, 16.0F);
			MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

			renderer.renderStatic(stack, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, matrixStack, buffer, null, 0);
			buffer.endBatch();

			matrixStack.popPose();
			*///?} else if >=1.21.4 {
			/*// maybe should use renderer.textureManager
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
			*///?} else {
			/*// maybe should use renderer.textureManager
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
			*///?}
		}

	}
}
