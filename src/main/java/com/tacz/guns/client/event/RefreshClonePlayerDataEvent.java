package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.util.DelayedTask;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.BooleanSupplier;

/**
 * 蠖鍋自螳ｶ霍ｨ雜顔ｻｴ蠎ｦ譌ｶ・悟ｮ｢謌ｷ遶ｯ髴隕∝姐譁ｰ荳谺｡邇ｩ螳ｶ逧・・莉ｶ螻樊ｧ郛灘ｭ・
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class RefreshClonePlayerDataEvent {
    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        LocalPlayer newPlayer = event.getNewPlayer();
        // 菴・弍霑吩ｸｪ莠倶ｻｶ隗ｦ蜿第慮・檎自螳ｶ逧・レ蛹・ｹｶ譛ｪ蜷梧ｭ･・悟ｯｼ閾ｴ譌豕戊ｯｻ蜿匁棯譴ｰ謨ｰ謐ｮ霑幄｡碁・莉ｶ螻樊ｧ郛灘ｭ倡噪蛻ｷ譁ｰ
        // 蟒ｶ霑・10 tick 謇ｧ陦檎ｼ灘ｭ伜姐譁ｰ蟆ｱ螂ｽ莠・
        DelayedTask.add(() -> IGunOperator.fromLivingEntity(newPlayer).initialData(), 10);
    }

    /**
     * 蟒ｶ霑滓鴬陦梧弍騾夊ｿ・ｿ吩ｸｪ譁ｹ豕墓鴬陦檎噪
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Pre event) {
        try {
            DelayedTask.SUPPLIERS.removeIf(BooleanSupplier::getAsBoolean);
        } catch (Exception e) {
            DelayedTask.SUPPLIERS.clear();
            GunMod.LOGGER.catching(e);
        }
    }
}

