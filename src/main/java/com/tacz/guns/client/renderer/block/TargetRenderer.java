package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.block.entity.TargetBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.tacz.guns.util.RenderTypeCompat;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class TargetRenderer implements BlockEntityRenderer<TargetBlockEntity, TargetRenderer.RenderState> {
    private static final String UPPER_NAME = "target_upper";
    private static final String HEAD_NAME = "head";

    public TargetRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
            TargetBlockEntity blockEntity,
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
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.TARGET_MODEL_LOCATION);
    }

    private void renderLegacy(
            TargetBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            int combinedLightIn,
            int combinedOverlayIn
    ) {
        getModel().ifPresent(model -> {
            BlockState blockState = blockEntity.getBlockState();
            Direction facing = blockState.getValue(TargetBlock.FACING);
            BedrockPart headModel = model.getNode(HEAD_NAME);
            BedrockPart upperModel = model.getNode(UPPER_NAME);
            float deg = -Mth.lerp(partialTick, blockEntity.oRot, blockEntity.rot);
            upperModel.xRot = (float) Math.toRadians(deg);
            headModel.visible = false;

            poseStack.pushPose();
            poseStack.translate(0.5, 0.225, 0.5);
            poseStack.mulPose(Axis.YN.rotationDegrees(facing.get2DDataValue() * 90));
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            poseStack.translate(0, -1.275, 0.0125);
            RenderType renderType = RenderTypeCompat.entityTranslucent(InternalAssetLoader.TARGET_TEXTURE_LOCATION);
            model.render(poseStack, ItemDisplayContext.NONE, renderType, combinedLightIn, combinedOverlayIn);
            if (blockEntity.getOwner() != null) {
                poseStack.translate(0, 1.25, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(deg));
                Identifier skin = DefaultPlayerSkin.get(blockEntity.getOwner()).body().texturePath();
                headModel.visible = true;
                RenderType skullRenderType = RenderTypeCompat.entityCutout(skin);
                MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
                headModel.render(poseStack, ItemDisplayContext.NONE, bufferSource.getBuffer(skullRenderType), combinedLightIn, OverlayTexture.NO_OVERLAY);
                bufferSource.endBatch();
            }
            poseStack.popPose();
        });
    }

    @Override
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static class RenderState extends BlockEntityRenderState {
        public TargetBlockEntity blockEntity;
        public float partialTick;
    }
}
