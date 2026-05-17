package com.tacz.guns.event;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.pojo.data.gun.FeedType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlayerRespawnEvent {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // 驥咲函閾ｪ蜉ｨ謐｢蠑ｹ
        if (!GunConfig.AUTO_RELOAD_WHEN_RESPAWN.get()) return;

        var player = event.getEntity();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var itemStack = player.getInventory().getItem(i);
            if (!(itemStack.getItem() instanceof IGun)) continue;

            var api = new ModernKineticGunScriptAPI();
            api.setItemStack(itemStack);
            api.setShooter(player);


            // 髓亥ｯｹ閭悟桁逶ｴ隸ｻ迚ｹ谿雁､・炊
            var useInventoryAmmo = api.getGunIndex().getGunData().getReloadData().getType() == FeedType.INVENTORY;
            // 螯よ棡荳ｺ閭悟桁逶ｴ隸ｻ蛻吩ｸ崎ｿ幄｡梧困蠑ｹ
            if (useInventoryAmmo) {
                continue;
            }

            // 髓亥ｯｹ辯・侭邀ｻ蝙狗音谿雁､・炊
            var isFuel = api.getGunIndex().getGunData().getReloadData().getType() == FeedType.FUEL;
            int needAmmoCount = api.getNeededAmmoAmount();

            if (player.isCreative()) {
                api.putAmmoInMagazine(needAmmoCount);
            } else {
                int consumedAmount = api.consumeAmmoFromPlayer(isFuel ? 1 : needAmmoCount);
                api.putAmmoInMagazine(isFuel ? (needAmmoCount * consumedAmount) : consumedAmount);
            }
        }
    }
}

