package com.tacz.guns.util;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public final class ClientRenderCompat {
    private static final AnimateGeoItemRenderer<?, ?> GUN_RENDERER_FALLBACK = new GunItemRendererWrapper();

    private ClientRenderCompat() {
    }

    @Nullable
    public static AnimateGeoItemRenderer<?, ?> getCustomRenderer(ItemStack stack) {
        IClientItemExtensions extensions = IClientItemExtensions.of(stack.getItem());
        try {
            Method method = extensions.getClass().getMethod("getCustomRenderer");
            Object renderer = method.invoke(extensions);
            if (renderer instanceof AnimateGeoItemRenderer<?, ?> animateGeoItemRenderer) {
                return animateGeoItemRenderer;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        if (stack.getItem() instanceof IGun) {
            return GUN_RENDERER_FALLBACK;
        }
        return null;
    }

    public static float getPartialTicks() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
}
