package com.tacz.guns.util;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class RenderTypeCompat {
    private RenderTypeCompat() {
    }

    public static RenderType entityCutout(Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    public static RenderType entityCutoutCull(Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }

    public static RenderType entityTranslucent(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    public static RenderType entityTranslucentCull(Identifier texture) {
        return RenderTypes.entityTranslucentCullItemTarget(texture);
    }

    public static RenderType energySwirl(Identifier texture, float u, float v) {
        return RenderTypes.energySwirl(texture, u, v);
    }
}
