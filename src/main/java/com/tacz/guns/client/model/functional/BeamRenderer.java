package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.compat.ar.ARCompat;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.LaserColorUtil;
import com.tacz.guns.util.RenderTypeCompat;
import com.tacz.guns.util.VertexConsumerCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

public class BeamRenderer {
    public static final Identifier LASER_BEAM_TEXTURE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/entity/beam.png");
    private static final LaserConfig DEFAULT_LASER_CONFIG = new LaserConfig();

    private BeamRenderer() {
    }

    public static void renderLaserBeam(ItemStack stack, PoseStack poseStack, ItemDisplayContext transformType, @Nonnull List<BedrockPart> path) {
        if (stack == null || (!transformType.firstPerson() && transformType != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            return;
        }

        if (ARCompat.shouldAccelerate() && renderLaserBeamAccelerated(stack, poseStack, transformType, path)) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer builder = bufferSource.getBuffer(laserRenderType());
        poseStack.pushPose();
        for (BedrockPart bedrockPart : path) {
            bedrockPart.translateAndRotateAndScale(poseStack);
        }

        LaserConfig laserConfig = getLaserConfig(stack);
        int color = LaserColorUtil.getLaserColor(stack, laserConfig);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        stringVertex(transformType.firstPerson() ? -laserConfig.getLength() : -laserConfig.getLengthThird(),
                transformType.firstPerson() ? laserConfig.getWidth() : laserConfig.getWidthThird(),
                builder, poseStack.last(), r, g, b, RenderConfig.ENABLE_LASER_FADE_OUT.get());

        poseStack.popPose();
    }

    public static boolean renderLaserBeamAccelerated(ItemStack stack, PoseStack poseStack, ItemDisplayContext transformType, @Nonnull List<BedrockPart> path) {
        var builder = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(laserRenderType());
        if (!ARCompat.isAccelerated(builder)) {
            return false;
        }

        poseStack.pushPose();
        for (BedrockPart bedrockPart : path) {
            bedrockPart.translateAndRotateAndScale(poseStack);
        }

        var laserConfig = getLaserConfig(stack);
        ARCompat.renderLaser(
                builder,
                transformType.firstPerson() ? -laserConfig.getLength() : -laserConfig.getLengthThird(),
                transformType.firstPerson() ? laserConfig.getWidth() : laserConfig.getWidthThird(),
                RenderConfig.ENABLE_LASER_FADE_OUT.get(),
                poseStack,
                (255 << 24) | (LaserColorUtil.getLaserColor(stack, laserConfig) & 0xFF_FF_FF)
        );

        poseStack.popPose();
        return true;
    }

    private static LaserConfig getLaserConfig(ItemStack stack) {
        if (stack == null) {
            return DEFAULT_LASER_CONFIG;
        }

        if (stack.getItem() instanceof IAttachment iAttachment) {
            return TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(stack))
                    .map(ClientAttachmentIndex::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }

        if (stack.getItem() instanceof IGun) {
            return TimelessAPI.getGunDisplay(stack)
                    .map(GunDisplayInstance::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }

        return DEFAULT_LASER_CONFIG;
    }

    private static RenderType laserRenderType() {
        return RenderTypeCompat.entityTranslucent(LASER_BEAM_TEXTURE);
    }

    private static void stringVertex(float z, float width, VertexConsumer pConsumer, PoseStack.Pose pPose, int r, int g, int b, boolean fadeOut) {
        float halfWidth = width / 2;
        int endAlpha = fadeOut ? 0 : 255;
        int light = LightCoordsUtil.pack(15, 15);

        VertexConsumerCompat.addVertex(pConsumer, pPose, -halfWidth, -halfWidth, 0, r, g, b, 255, 0, 0, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, -halfWidth, halfWidth, 0, r, g, b, 255, 0, 1, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, -halfWidth, halfWidth, z, r, g, b, endAlpha, 1, 1, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, -halfWidth, -halfWidth, z, r, g, b, endAlpha, 1, 0, light);

        VertexConsumerCompat.addVertex(pConsumer, pPose, -halfWidth, halfWidth, 0, r, g, b, 255, 0, 0, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, halfWidth, halfWidth, 0, r, g, b, 255, 0, 1, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, halfWidth, halfWidth, z, r, g, b, endAlpha, 1, 1, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, -halfWidth, halfWidth, z, r, g, b, endAlpha, 1, 0, light);

        VertexConsumerCompat.addVertex(pConsumer, pPose, halfWidth, halfWidth, 0, r, g, b, 255, 0, 0, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, halfWidth, -halfWidth, 0, r, g, b, 255, 0, 1, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, halfWidth, -halfWidth, z, r, g, b, endAlpha, 1, 1, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, halfWidth, halfWidth, z, r, g, b, endAlpha, 1, 0, light);

        VertexConsumerCompat.addVertex(pConsumer, pPose, halfWidth, -halfWidth, 0, r, g, b, 255, 0, 1, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, -halfWidth, -halfWidth, 0, r, g, b, 255, 0, 1, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, -halfWidth, -halfWidth, z, r, g, b, endAlpha, 1, 1, light);
        VertexConsumerCompat.addVertex(pConsumer, pPose, halfWidth, -halfWidth, z, r, g, b, endAlpha, 1, 0, light);
    }
}
