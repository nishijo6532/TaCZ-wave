package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LivingEntityCrawl {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;

    public LivingEntityCrawl(LivingEntity shooter, ShooterDataHolder data) {
        this.shooter = shooter;
        this.data = data;
    }

    public void crawl(boolean isCrawl) {
        data.isCrawling = isCrawl;
    }

    public void tickCrawling() {
        // currentGunItem 螯よ棡荳ｺ null・悟・蜿匁ｶ郁ｶｴ荳狗憾諤・
        if (data.currentGunItem == null || !(data.currentGunItem.get().getItem() instanceof IGun iGun)) {
            data.isCrawling = false;
            this.setCrawlPose();
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        // 荳榊・隶ｸ雜ｴ荳狗噪豁ｦ蝎ｨ・悟・蜿匁ｶ郁ｶｴ荳狗憾諤・
        if (!iGun.isCanCrawl(currentGunItem)) {
            data.isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 螯よ棡闔ｷ蜿紋ｸ榊芦 gunIndex・悟・蜿匁ｶ郁ｶｴ荳狗憾諤・
        Identifier gunId = iGun.getGunId(currentGunItem);
        if (TimelessAPI.getCommonGunIndex(gunId).isEmpty()) {
            data.isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 螯よ棡譏ｯ隗ょｯ溯・ｨ｡蝙九・ｪ台ｹ倥∬ｷｳ霍・∝惠貂ｸ豕ｳ縲∽ｸ榊惠蝨ｰ荳奇ｼ悟叙豸・
        if (shooter.isSpectator() || shooter.isPassenger() || shooter.isJumping() || shooter.isSwimming() || !shooter.onGround()) {
            data.isCrawling = false;
            this.setCrawlPose();
            return;
        }
        this.setCrawlPose();
    }

    private void setCrawlPose() {
        if (data.isCrawling) {
            if (shooter instanceof Player player) {
                player.setForcedPose(Pose.SWIMMING);
            } else {
                this.shooter.setPose(Pose.SWIMMING);
            }
        } else {
            if (shooter instanceof Player player) {
                player.setForcedPose(null);
            }
        }
    }
}

