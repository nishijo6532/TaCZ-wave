package com.tacz.guns.event;

import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import com.tacz.guns.util.EntityUtil;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class LoadingConfigEvent {
    private static final String CONFIG_NAME = "tacz-server.toml";

    /**
     * 螳｢謌ｷ遶ｯ蜥梧恪蜉｡遶ｯ蜷ｯ蜉ｨ譌ｶ・御ｼ夊ｧｦ蜿第ｭ､莠倶ｻｶ
     */
    @SubscribeEvent
    public static void onLoadingConfig(ModConfigEvent.Loading event) {
        String fileName = event.getConfig().getFileName();
        if (CONFIG_NAME.equals(fileName)) {
            HeadShotAABBConfigRead.init();
            EntityUtil.clearHeadshotAabbCache();
            InteractKeyConfigRead.init();
        }
    }

    /**
     * 邇ｩ螳ｶ霑帛・譛榊苅遶ｯ・梧・閠・恪蜉｡遶ｯ閾ｪ蜉ｨ驥咲ｽｮ驟咲ｽｮ譌ｶ・御ｼ夊ｧｦ蜿第ｭ､譁ｹ豕・
     */
    @SubscribeEvent
    public static void onReloadingConfig(ModConfigEvent.Reloading event) {
        String fileName = event.getConfig().getFileName();
        if (CONFIG_NAME.equals(fileName)) {
            HeadShotAABBConfigRead.init();
            EntityUtil.clearHeadshotAabbCache();
            InteractKeyConfigRead.init();
//            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientGunPackDownloadManager::downloadClientGunPack);
        }
    }
}

