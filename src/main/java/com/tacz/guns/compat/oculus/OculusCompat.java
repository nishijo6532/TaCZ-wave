package com.tacz.guns.compat.oculus;

import net.minecraft.client.renderer.MultiBufferSource;

public final class OculusCompat {
    private OculusCompat() {
    }

    public static boolean endBatch(MultiBufferSource.BufferSource source) {
        return false;
    }

    public static boolean isRenderShadow() {
        return false;
    }
}
