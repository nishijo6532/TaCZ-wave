package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.papi.PapiManager;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class TextShowRender implements IFunctionalRenderer {
    private final BedrockModel bedrockModel;
    private final String nodeName;
    private final TextShow textShow;
    private final ItemStack gunStack;
    private final boolean requireAiming;

    public TextShowRender(BedrockModel bedrockModel, TextShow textShow, ItemStack gunStack) {
        this(bedrockModel, "", textShow, gunStack, false);
    }

    public TextShowRender(BedrockModel bedrockModel, String nodeName, TextShow textShow, ItemStack gunStack) {
        this(bedrockModel, nodeName, textShow, gunStack, false);
    }

    public TextShowRender(BedrockModel bedrockModel, String nodeName, TextShow textShow, ItemStack gunStack, boolean requireAiming) {
        this.bedrockModel = bedrockModel;
        this.nodeName = nodeName;
        this.textShow = textShow;
        this.gunStack = gunStack;
        this.requireAiming = requireAiming;
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay) {
        if (!transformType.firstPerson()) {
            return;
        }
        if (requireAiming && getAimingProgress() <= 0.01F) {
            return;
        }
        String text = PapiManager.getTextShow(textShow.getTextKey(), gunStack);
        if (StringUtils.isBlank(text)) {
            return;
        }

        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
        Matrix3f normal = new Matrix3f(poseStack.last().normal());
        Matrix4f pose = new Matrix4f(poseStack.last().pose());

        bedrockModel.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
            Font font = Minecraft.getInstance().font;
            boolean shadow = textShow.isShadow();
            int color = 0xFF000000 | textShow.getColorInt();
            float scale = textShow.getScale();
            int packLight = LightCoordsUtil.pack(textShow.getTextLight(), textShow.getTextLight());
            int width = font.width(text);
            int xOffset;
            switch (textShow.getAlign()) {
                case CENTER -> xOffset = width / 2;
                case RIGHT -> xOffset = width;
                default -> xOffset = 0;
            }

            PoseStack poseStack2 = new PoseStack();
            poseStack2.last().normal().mul(normal);
            poseStack2.last().pose().mul(pose);
            poseStack2.scale(2 / 300f * scale, -2 / 300f * scale, -2 / 300f);

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            font.drawInBatch(text, -xOffset, -font.lineHeight / 2f, color, shadow, poseStack2.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, packLight);
            bufferSource.endBatch();
        });
    }

    private float getAimingProgress() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return 0.0F;
        }
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(partialTick);
    }
}
