package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IBlock;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.tacz.guns.util.RenderTypeCompat;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class GunSmithTableRenderer implements BlockEntityRenderer<GunSmithTableBlockEntity, GunSmithTableRenderer.RenderState> {
    public GunSmithTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
            GunSmithTableBlockEntity blockEntity,
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

    public Optional<ClientBlockIndex> getIndex(GunSmithTableBlockEntity blockEntity) {
        Identifier id = blockEntity.getId();
        if (id == null || id.equals(DefaultAssets.EMPTY_BLOCK_ID)) {
            id = DefaultAssets.DEFAULT_BLOCK_ID;
        }
        return TimelessAPI.getClientBlockIndex(id);
    }

    public static Optional<ClientBlockIndex> getIndex(ItemStack stack) {
        if (stack.getItem() instanceof IBlock iBlock) {
            Identifier id = iBlock.getBlockId(stack);
            if (id.equals(DefaultAssets.EMPTY_BLOCK_ID)) {
                id = DefaultAssets.DEFAULT_BLOCK_ID;
            }
            return TimelessAPI.getClientBlockIndex(id);
        }
        return Optional.empty();
    }

    private void renderLegacy(
            GunSmithTableBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            int combinedLightIn,
            int combinedOverlayIn
    ) {
        getIndex(blockEntity).ifPresent(index -> {
            BedrockModel model = index.getModel();
            Identifier texture = index.getTexture();
            if (model == null) {
                return;
            }
            BlockState blockState = blockEntity.getBlockState();
            if (blockState.getBlock() instanceof AbstractGunSmithTableBlock block) {
                if (!block.isRoot(blockState)) {
                    return;
                }
                Direction facing = blockState.getValue(AbstractGunSmithTableBlock.FACING);
                poseStack.pushPose();
                poseStack.translate(0.5, 1.5, 0.5);
                poseStack.mulPose(Axis.ZN.rotationDegrees(180));
                poseStack.mulPose(Axis.YN.rotationDegrees(block.parseRotation(facing)));
                RenderType renderType = RenderConfig.BLOCK_ENTITY_TRANSLUCENT.get()
                        ? RenderTypeCompat.entityTranslucent(texture)
                        : RenderTypeCompat.entityCutout(texture);
                model.render(poseStack, ItemDisplayContext.NONE, renderType, combinedLightIn, combinedOverlayIn);
                poseStack.popPose();
            }
        });
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static class RenderState extends BlockEntityRenderState {
        public GunSmithTableBlockEntity blockEntity;
        public float partialTick;
    }
}
