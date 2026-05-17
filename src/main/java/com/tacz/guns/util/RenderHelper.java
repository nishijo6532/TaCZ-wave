package com.tacz.guns.util;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.compat.optifine.OptifineCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class RenderHelper {
    private static final ThreadLocal<SubmitNodeCollector> FIRST_PERSON_ARM_NODE_COLLECTOR = new ThreadLocal<>();

    public static void blit(PoseStack poseStack, float x, float y, float uOffset, float vOffset, float pWidth, float height, float textureWidth, float textureHeight) {
        blit(poseStack, x, y, pWidth, height, uOffset, vOffset, pWidth, height, textureWidth, textureHeight);
    }

    private static void blit(PoseStack poseStack, float x, float y, float pWidth, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
        innerBlit(poseStack, x, x + pWidth, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    private static void innerBlit(PoseStack poseStack, float x1, float x2, float y1, float y2, float blitOffset, float uWidth, float vHeight, float uOffset, float vOffset, float textureWidth, float textureHeight) {
        innerBlit(poseStack.last().pose(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth, (uOffset + uWidth) / textureWidth, (vOffset + 0.0F) / textureHeight, (vOffset + vHeight) / textureHeight);
    }

    private static void innerBlit(Matrix4f matrix, float x1, float x2, float y1, float y2, float blitOffset, float minU, float maxU, float minV, float maxV) {
        // Forge64 uses the newer GPU render pipeline and legacy immediate blit calls are unavailable.
        // This helper currently has no in-tree call sites, so keep a no-op shim for compile compatibility.
    }

    public static void enableItemEntityStencilTest() {
        RenderSystem.assertOnRenderThread();
        if (OptifineCompat.isOptifineInstalled()) {
            // 以下代码用于应对 使用 optifine 的场景
            int depthTextureId = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
            int stencilTextureId = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
            if (depthTextureId != GL30.GL_NONE && stencilTextureId == GL30.GL_NONE) {
                GL30.glBindTexture(GL30.GL_TEXTURE_2D, depthTextureId);
                int dataType = GL30.glGetTexLevelParameteri(GL30.GL_TEXTURE_2D, 0, GL30.GL_TEXTURE_DEPTH_TYPE);
                if (dataType == GL30.GL_UNSIGNED_NORMALIZED) {
                    int width = GL30.glGetTexLevelParameteri(GL30.GL_TEXTURE_2D, 0, GL30.GL_TEXTURE_WIDTH);
                    int height = GL30.glGetTexLevelParameteri(GL30.GL_TEXTURE_2D, 0, GL30.GL_TEXTURE_HEIGHT);
                    GlStateManager._texImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_DEPTH24_STENCIL8, width, height, 0, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, null);
                    GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, 3553, depthTextureId, 0);
                }
            }
        } else {
            Minecraft.getInstance().getMainRenderTarget().enableStencil();
        }
        GL11.glEnable(GL11.GL_STENCIL_TEST);
    }

    public static void disableItemEntityStencilTest() {
        RenderSystem.assertOnRenderThread();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void setFirstPersonArmNodeCollector(SubmitNodeCollector nodeCollector) {
        FIRST_PERSON_ARM_NODE_COLLECTOR.set(nodeCollector);
    }

    public static void clearFirstPersonArmNodeCollector() {
        FIRST_PERSON_ARM_NODE_COLLECTOR.remove();
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm hand, PoseStack matrixStack, int combinedLight) {
        SubmitNodeCollector nodeCollector = FIRST_PERSON_ARM_NODE_COLLECTOR.get();
        if (player == null || nodeCollector == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher renderManager = mc.getEntityRenderDispatcher();
        var renderer = renderManager.getPlayerRenderer(player);
        var skinTexture = player.getSkin().body().texturePath();
        if (hand == HumanoidArm.RIGHT) {
            renderer.renderRightHand(matrixStack, nodeCollector, combinedLight, skinTexture, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
        } else {
            renderer.renderLeftHand(matrixStack, nodeCollector, combinedLight, skinTexture, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
        }
    }
}
