package com.tacz.guns.client.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.util.RenderTypeCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

final class Forge64ScopeStencilCircle {
    private static final int SEGMENTS = 90;
    private static final float Z = -90.0F;
    private static final int FULL_LIGHT = LightCoordsUtil.pack(15, 15);

    private Forge64ScopeStencilCircle() {
    }

    static void render(float centerX, float centerY, float radius) {
        RenderType renderType = RenderTypeCompat.entityTranslucent(BeamRenderer.LASER_BEAM_TEXTURE);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        for (int j = 0; j < SEGMENTS; j++) {
            float angle1 = (float) j * ((float) Math.PI * 2.0F) / SEGMENTS;
            float angle2 = (float) (j + 1) * ((float) Math.PI * 2.0F) / SEGMENTS;
            float sin1 = Mth.sin(angle1);
            float cos1 = Mth.cos(angle1);
            float sin2 = Mth.sin(angle2);
            float cos2 = Mth.cos(angle2);
            addVertex(consumer, centerX, centerY, Z);
            addVertex(consumer, centerX + cos1 * radius, centerY + sin1 * radius, Z);
            addVertex(consumer, centerX + cos2 * radius, centerY + sin2 * radius, Z);
            addVertex(consumer, centerX, centerY, Z);
        }
        if (!OculusCompat.endBatch(bufferSource)) {
            bufferSource.endBatch(renderType);
        }
    }

    private static void addVertex(VertexConsumer consumer, float x, float y, float z) {
        consumer.addVertex(x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_LIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }
}
