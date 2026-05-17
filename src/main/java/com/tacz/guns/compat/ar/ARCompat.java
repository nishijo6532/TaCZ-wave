package com.tacz.guns.compat.ar;

import com.mojang.blaze3d.vertex.PoseStack;

public final class ARCompat {
    private static Runnable renderBefore = () -> {
    };
    private static Runnable renderAfter = () -> {
    };

    private ARCompat() {
    }

    public static void init() {
    }

    public static boolean shouldAccelerate() {
        return false;
    }

    public static void setRenderLayer(int layer) {
    }

    public static void resetRenderLayer() {
    }

    public static void setRenderBeforeFunction(Runnable runnable) {
        renderBefore = runnable == null ? () -> {
        } : runnable;
    }

    public static void resetRenderBeforeFunction() {
        renderBefore = () -> {
        };
    }

    public static void setRenderAfterFunction(Runnable runnable) {
        renderAfter = runnable == null ? () -> {
        } : runnable;
    }

    public static void resetRenderAfterFunction() {
        renderAfter = () -> {
        };
    }

    public static void disableAcceleration() {
    }

    public static void resetAcceleration() {
    }

    public static boolean isAccelerated(Object vertexConsumer) {
        return false;
    }

    public static void renderLaser(Object vertexConsumer, float z, float width, boolean fadeOut, PoseStack poseStack, int argbColor) {
    }
}
