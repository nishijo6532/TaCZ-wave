package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class VertexConsumerCompat {
    private VertexConsumerCompat() {
    }

    public static void addVertex(
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v,
            int overlay,
            int light,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.addVertex(x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(normalX, normalY, normalZ);
    }

    public static void addVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int red,
            int green,
            int blue,
            int alpha,
            float u,
            float v,
            int light
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light);
        consumer.setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    public static void addVertex(
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            float red,
            float green,
            float blue,
            float alpha,
            int light
    ) {
        consumer.addVertex(x, y, z)
                .setUv(u, v)
                .setColor(red, green, blue, alpha)
                .setLight(light);
    }
}
