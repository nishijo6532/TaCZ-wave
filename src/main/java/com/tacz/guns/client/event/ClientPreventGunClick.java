package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.InteractKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class ClientPreventGunClick {
    @SubscribeEvent
    public static boolean onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        // 蠖謎ｺ､莠帝醗謖我ｸ区慮・悟・隶ｸ莠､莠・
        if (InteractKey.INTERACT_KEY.isDown()) {
            return false;
        }
        // 蜿ｪ隕∽ｸｻ謇区怏譫ｪ・碁ぅ荵育ｦ∵ｭ｢莠､莠・
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (itemInHand.getItem() instanceof IGun) {
            // 螻慕､ｺ譯・庄莉･莠､莠・
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof ItemFrame) {
                return false;
            }
            // 霑吩ｸｪ隶ｾ鄂ｮ荳ｺ false 蟆ｱ閭ｽ髦ｻ豁｢螳｢謌ｷ遶ｯ邊貞ｭ千噪逕滓・
            event.setSwingHand(false);
            return true;
        }
        return false;
    }
}

