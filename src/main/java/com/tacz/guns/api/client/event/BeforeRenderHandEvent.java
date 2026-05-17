package com.tacz.guns.api.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.event.common.KubeJSGunEventPoster;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.SelfPosting;

/**
 * 陜ｨ・ｨ髫ｹ繝ｻ逡・ItemInHandRenderer#renderHandsWithItems 隴・ｽｹ雎募｢捺・髫暦ｽｦ陷ｿ鬘鯉ｽｯ・･闔蛟ｶ・ｻ・ｶ
 * 騾包ｽｨ闔螳亥ｶ瑚ｭ幢ｽｺ陷会ｽｨ騾包ｽｻ騾ｶ・ｸ陷茨ｽｳ髫ｹ繝ｻ逡・
 */
public class BeforeRenderHandEvent extends MutableEvent implements KubeJSGunEventPoster<BeforeRenderHandEvent>, SelfPosting<BeforeRenderHandEvent> {
    public static final EventBus<BeforeRenderHandEvent> BUS = EventBus.create(BeforeRenderHandEvent.class);

    private final PoseStack poseStack;
    private final float partialTick;

    public BeforeRenderHandEvent(PoseStack poseStack, float partialTick) {
        this.poseStack = poseStack;
        this.partialTick = partialTick;
        postClientEventToKubeJS(this);
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public float getPartialTick() {
        return partialTick;
    }

    @Override
    public EventBus<BeforeRenderHandEvent> getDefaultBus() {
        return BUS;
    }
}

