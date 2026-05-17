package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerAim;
import com.tacz.guns.resource.modifier.custom.AdsModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class LocalPlayerAim {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerAim(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void aim(boolean isAim) {
        // 證ょｮ壻ｸｺ荳ｻ謇・
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        Identifier gunId = iGun.getGunId(mainHandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            data.clientIsAiming = isAim;
            // 蜿鷹∝・謐｢蠑轣ｫ讓｡蠑冗噪謨ｰ謐ｮ蛹・ｼ碁夂衍譛榊苅蝎ｨ
            NetworkHandler.sendToServer(new ClientMessagePlayerAim(isAim));
        });
    }

    public float getClientAimingProgress(float partialTicks) {
        return Mth.lerp(partialTicks, LocalPlayerDataHolder.oldAimingProgress, data.clientAimingProgress);
    }

    public boolean isAim() {
        return data.clientIsAiming;
    }

    public void tickAimingProgress() {
        ItemStack mainHandItem = player.getMainHandItem();
        // 螯よ棡荳ｻ謇狗黄蜩∽ｸ肴弍譫ｪ譴ｰ・悟・蜿匁ｶ育桷蜃・憾諤∝ｹｶ蟆・aimingProgress 蠖帝峺・瑚ｿ泌屓縲・
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            data.clientAimingProgress = 0;
            LocalPlayerDataHolder.oldAimingProgress = 0;
            return;
        }
        // 螯よ棡豁｣蝨ｨ謾ｶ譫ｪ・悟・荳崎・迸・㊥
        if (System.currentTimeMillis() - data.clientDrawTimestamp < 0) {
            data.clientIsAiming = false;
        }
        Identifier gunId = iGun.getGunId(mainHandItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresentOrElse(index -> {
            float alphaProgress = this.getAlphaProgress(index.getGunData());
            this.aimProgressCalculate(alphaProgress);
        }, () -> {
            data.clientAimingProgress = 0;
            LocalPlayerDataHolder.oldAimingProgress = 0;
        });
    }

    private void aimProgressCalculate(float alphaProgress) {
        LocalPlayerDataHolder.oldAimingProgress = data.clientAimingProgress;
        if (data.clientIsAiming) {
            // 螟・ｺ取鴬陦檎桷蜃・憾諤・ｼ悟｢槫刈 aimingProgress
            data.clientAimingProgress += alphaProgress;
            if (data.clientAimingProgress > 1) {
                data.clientAimingProgress = 1;
            }
        } else {
            // 螟・ｺ主叙豸育桷蜃・憾諤・ｼ悟㍼蟆・aimingProgress
            data.clientAimingProgress -= alphaProgress;
            if (data.clientAimingProgress < 0) {
                data.clientAimingProgress = 0;
            }
        }
        data.clientAimingTimestamp = System.currentTimeMillis();
    }

    private float getAlphaProgress(GunData gunData) {
        float aimTime = gunData.getAimTime();
        IGunOperator operator = IGunOperator.fromLivingEntity(this.player);
        if (operator.getCacheProperty() != null) {
            aimTime = operator.getCacheProperty().<Float>getCache(AdsModifier.ID);
        }
        aimTime = Math.max(0, aimTime);
        return (System.currentTimeMillis() - data.clientAimingTimestamp + 1) / (aimTime * 1000);
    }
}

