package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerCrawl;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

public class LocalPlayerCrawl {
    /**
     * 蜀ｷ蜊ｴ譌ｶ髣ｴ荳ｺ 10 tick
     */
    private static final int COOLDOWN_TICKS = 10;
    private final LocalPlayer player;
    private boolean isCrawling = false;
    private int crawCooldownTicks = 0;

    public LocalPlayerCrawl(LocalPlayer player) {
        this.player = player;
    }

    public void crawl(boolean isCrawl) {
        // 謖∵棯謇崎・謖蛾醗雜ｴ荳・
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        // 荳榊・隶ｸ雜ｴ荳狗噪豁ｦ蝎ｨ
        if (!iGun.isCanCrawl(mainHandItem)) {
            return;
        }
        // 蜀ｷ蜊ｴ譌ｶ髣ｴ豐｡蛻ｰ・御ｸ肴鴬陦・
        if (crawCooldownTicks > 0) {
            return;
        }
        if (player.isSpectator() || player.isPassenger() || !player.onGround()) {
            return;
        }
        Identifier gunId = iGun.getGunId(mainHandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            this.isCrawling = isCrawl;
            this.crawCooldownTicks = COOLDOWN_TICKS;
            NetworkHandler.sendToServer(new ClientMessagePlayerCrawl(isCrawl));
        });
    }

    public void tickCrawl() {
        if (crawCooldownTicks > 0) {
            crawCooldownTicks--;
        }
        // 謖∵棯謇崎・謖蛾醗雜ｴ荳・
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 荳榊・隶ｸ雜ｴ荳狗噪豁ｦ蝎ｨ・悟・蜿匁ｶ郁ｶｴ荳狗憾諤・
        if (!iGun.isCanCrawl(mainHandItem)) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 螯よ棡闔ｷ蜿紋ｸ榊芦 gunIndex・悟・蜿匁ｶ郁ｶｴ荳狗憾諤・
        Identifier gunId = iGun.getGunId(mainHandItem);
        if (TimelessAPI.getCommonGunIndex(gunId).isEmpty()) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 螯よ棡邇ｩ螳ｶ譏ｯ隗ょｯ溯・ｨ｡蝙九・ｪ台ｹ倥∬ｷｳ霍・∝惠貂ｸ豕ｳ縲∽ｸ榊惠蝨ｰ荳奇ｼ悟叙豸・
        if (player.isSpectator() || player.isPassenger() || player.input.keyPresses.jump() || player.isSwimming() || !player.onGround()) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        this.setCrawlPose();
    }

    public boolean isCrawling() {
        return isCrawling;
    }

    private void setCrawlPose() {
        if (isCrawling) {
            player.setForcedPose(Pose.SWIMMING);
        } else {
            player.setForcedPose(null);
        }
    }
}

