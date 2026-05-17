package com.tacz.guns.client.event;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayerHurtByGunEvent {
    @SubscribeEvent
    public static void onPlayerHurtByGun(EntityHurtByGunEvent.Post event) {
        LogicalSide logicalSide = event.getLogicalSide();
        if (logicalSide != LogicalSide.CLIENT) {
            return;
        }
        Entity hurtEntity = event.getHurtEntity();
        LocalPlayer player = Minecraft.getInstance().player;
        // 蠖灘女莨､逧・弍閾ｪ蟾ｱ逧・慮蛟呻ｼ瑚ｧｦ蜿大女莨､譎・勘逧・ｰ・紛蜿よ焚
        if (player != null && player.equals(hurtEntity)) {
            Identifier gunId = event.getGunId();
            TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
                float tweakMultiplier = index.getGunData().getHurtBobTweakMultiplier();
                GunHurtBobTweak.markTimestamp(tweakMultiplier);
            });
        }
    }
}

