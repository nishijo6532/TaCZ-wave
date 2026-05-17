package com.tacz.guns.client.event;

import com.tacz.guns.config.client.RenderConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RenderHeadShotAABB {
    @SubscribeEvent
    public static void onRenderEntity(RenderLivingEvent.Post<?, ?, ?> event) {
        if (!RenderConfig.HEAD_SHOT_DEBUG_HITBOX.get()) {
            return;
        }
    }
}
