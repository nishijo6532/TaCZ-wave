package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.ItemRenderCompat;
import com.tacz.guns.util.RenderTypeCompat;
import net.minecraft.util.Util;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class StatueRenderer implements BlockEntityRenderer<StatueBlockEntity, StatueRenderer.RenderState> {
    public StatueRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
            StatueBlockEntity blockEntity,
            RenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.blockEntity = blockEntity;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.blockEntity == null) {
            return;
        }
        renderLegacy(state.blockEntity, state.partialTick, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY);
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.STATUE_MODEL_LOCATION);
    }

    private void renderLegacy(
            StatueBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            int combinedLightIn,
            int combinedOverlayIn
    ) {
        getModel().ifPresent(model -> {
            Level level = blockEntity.getLevel();
            if (level == null) {
                return;
            }

            poseStack.pushPose();
            {
                BlockState blockState = blockEntity.getBlockState();
                Direction facing = blockState.getValue(TargetBlock.FACING);

                poseStack.translate(0.5, 1.5, 0.5);

                poseStack.mulPose(Axis.YN.rotationDegrees((facing.get2DDataValue() + 2) % 4 * 90));
                poseStack.mulPose(Axis.ZN.rotationDegrees(180));

                RenderType renderType = RenderConfig.BLOCK_ENTITY_TRANSLUCENT.get()
                        ? RenderTypeCompat.entityTranslucent(getTextureLocation())
                        : RenderTypeCompat.entityCutout(getTextureLocation());
                model.render(poseStack, ItemDisplayContext.NONE, renderType, combinedLightIn, combinedOverlayIn);

                poseStack.scale(0.5f, 0.5f, 0.5f);
                poseStack.translate(0, -0.875, -1.2);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));

                double offset = Math.sin(Util.getMillis() / 500.0) * 0.1;
                poseStack.translate(0, offset, 0);

                ItemStack stack = blockEntity.getGunItem();
                MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
                ItemRenderCompat.renderStatic(
                        stack,
                        ItemDisplayContext.FIXED,
                        LightCoordsUtil.pack(15, 15),
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        bufferSource,
                        level,
                        0
                );
                bufferSource.endBatch();
            }
            poseStack.popPose();
        });
    }

    public static Identifier getTextureLocation() {
        return InternalAssetLoader.STATUE_TEXTURE_LOCATION;
    }

    @Override
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(StatueBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos().above()).closerThan(cameraPos, this.getViewDistance());
    }

    public static class RenderState extends BlockEntityRenderState {
        public StatueBlockEntity blockEntity;
        public float partialTick;
    }
}

