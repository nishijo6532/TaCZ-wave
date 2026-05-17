package com.tacz.guns.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

public final class ItemRenderCompat {
    private ItemRenderCompat() {
    }

    public static void renderStatic(
            ItemStack stack,
            ItemDisplayContext displayContext,
            int packedLight,
            int packedOverlay,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Level level,
            int seed
    ) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            Method getItemRenderer = minecraft.getClass().getMethod("getItemRenderer");
            Object renderer = getItemRenderer.invoke(minecraft);
            for (Method method : renderer.getClass().getMethods()) {
                if (!method.getName().equals("renderStatic")) {
                    continue;
                }
                Class<?>[] p = method.getParameterTypes();
                if (p.length == 8
                        && p[0].isAssignableFrom(ItemStack.class)
                        && p[1].isAssignableFrom(ItemDisplayContext.class)
                        && p[2] == int.class
                        && p[3] == int.class
                        && p[4].isAssignableFrom(PoseStack.class)
                        && p[5].isAssignableFrom(MultiBufferSource.class)
                        && p[7] == int.class) {
                    method.invoke(renderer, stack, displayContext, packedLight, packedOverlay, poseStack, bufferSource, level, seed);
                    return;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
