package com.tacz.guns.client.renderer.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.entity.TargetMinecart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.tacz.guns.util.RenderTypeCompat;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class TargetMinecartRenderer extends MinecartRenderer {
    private static final String HEAD_NAME = "head";
    private static final String HEAD_2_NAME = "head2";

    public TargetMinecartRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, ModelLayers.TNT_MINECART);
        this.shadowRadius = 0.25F;
    }

    @Override
    public MinecartRenderState createRenderState() {
        return new TargetMinecartRenderState();
    }

    @Override
    public void extractRenderState(AbstractMinecart entity, MinecartRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof TargetMinecartRenderState targetState && entity instanceof TargetMinecart targetMinecart) {
            targetState.gameProfile = targetMinecart.getGameProfile();
        }
    }

    @Override
    public void submit(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        if (!(state instanceof TargetMinecartRenderState targetState) || targetState.gameProfile == null) {
            return;
        }

        poseStack.pushPose();
        applyMinecartTransform(state, poseStack);
        renderTargetContents(targetState.gameProfile, poseStack, state.lightCoords);
        poseStack.popPose();
    }

    private void applyMinecartTransform(MinecartRenderState state, PoseStack poseStack) {
        long seed = state.offsetSeed;
        float offsetX = (((float) (seed >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float offsetY = (((float) (seed >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float offsetZ = (((float) (seed >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        poseStack.translate(offsetX, offsetY, offsetZ);

        if (state.isNewRender) {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-state.xRot));
            poseStack.translate(0.0F, 0.375F, 0.0F);
        } else {
            float xRot = state.xRot;
            float rotation = state.yRot;
            if (state.posOnRail != null && state.frontPos != null && state.backPos != null) {
                Vec3 direction = state.backPos.add(-state.frontPos.x, -state.frontPos.y, -state.frontPos.z);
                if (direction.length() != 0.0) {
                    direction = direction.normalize();
                    rotation = (float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI);
                    xRot = (float) (Math.atan(direction.y) * 73.0);
                }
                poseStack.translate(state.posOnRail.x - state.x, (state.frontPos.y + state.backPos.y) / 2.0 - state.y, state.posOnRail.z - state.z);
            }
            poseStack.translate(0.0F, 0.375F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - rotation));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-xRot));
        }
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.TARGET_MINECART_MODEL_LOCATION);
    }

    private void renderTargetContents(GameProfile gameProfile, PoseStack stack, int packedLight) {
        getModel().ifPresent(model -> {
            BedrockPart headModel = model.getNode(HEAD_NAME);
            BedrockPart head2Model = model.getNode(HEAD_2_NAME);
            headModel.visible = false;
            head2Model.visible = false;

            stack.pushPose();
            stack.translate(0.5, 1.875, 0.5);
            stack.scale(1.5f, 1.5f, 1.5f);
            stack.mulPose(Axis.ZN.rotationDegrees(180));
            stack.mulPose(Axis.YN.rotationDegrees(90));
            RenderType renderType = RenderTypeCompat.entityTranslucent(InternalAssetLoader.TARGET_MINECART_TEXTURE_LOCATION);
            model.render(stack, ItemDisplayContext.NONE, renderType, packedLight, OverlayTexture.NO_OVERLAY);

            stack.translate(0, 1, -4.5 / 16d);
            Minecraft minecraft = Minecraft.getInstance();
            Identifier skin = DefaultPlayerSkin.get(gameProfile).body().texturePath();
            headModel.visible = true;
            RenderType skullRenderType = RenderTypeCompat.entityTranslucentCull(skin);
            var bufferSource = minecraft.renderBuffers().bufferSource();
            headModel.render(stack, ItemDisplayContext.NONE, bufferSource.getBuffer(skullRenderType), packedLight, OverlayTexture.NO_OVERLAY);

            head2Model.visible = true;
            stack.translate(0, 0, 0.01);
            head2Model.render(stack, ItemDisplayContext.NONE, bufferSource.getBuffer(skullRenderType), packedLight, OverlayTexture.NO_OVERLAY);
            bufferSource.endBatch();
            stack.popPose();
        });
    }

    private static class TargetMinecartRenderState extends MinecartRenderState {
        private GameProfile gameProfile;
    }
}
