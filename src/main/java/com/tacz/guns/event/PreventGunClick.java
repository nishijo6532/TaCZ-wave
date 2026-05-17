package com.tacz.guns.event;

import com.tacz.guns.api.item.IGun;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PreventGunClick {
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // 蜿ｪ隕∽ｸｻ謇区怏譫ｪ・碁ぅ荵育ｦ∵ｭ｢莠､莠・
        ItemStack itemInHand = event.getEntity().getItemInHand(InteractionHand.MAIN_HAND);
        if (itemInHand.getItem() instanceof IGun) {
            event.setUseBlock(Result.DENY);
            event.setUseItem(Result.DENY);
        }
    }
}

