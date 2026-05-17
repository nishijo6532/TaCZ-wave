package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class LivingEntityBolt {
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;
    private final LivingEntityShoot shoot;
    private final LivingEntity shooter;

    public LivingEntityBolt(ShooterDataHolder data, LivingEntity shooter, LivingEntityDrawGun draw, LivingEntityShoot shoot) {
        this.data = data;
        this.draw = draw;
        this.shoot = shoot;
        this.shooter = shooter;
    }

    public void bolt() {
        if (data.currentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof AbstractGunItem iGun)) {
            return;
        }
        Identifier gunId = iGun.getGunId(currentGunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> {
            // 蛻､譁ｭ譏ｯ蜷ｦ豁｣蝨ｨ蟆・・蜀ｷ蜊ｴ
            if (shoot.getShootCoolDown() != 0) {
                return;
            }
            // 譽譟･譏ｯ蜷ｦ豁｣蝨ｨ謐｢蠑ｹ
            if (data.reloadStateType.isReloading()) {
                return;
            }
            // 譽譟･譏ｯ蜷ｦ蝨ｨ蛻・棯
            if (draw.getDrawCoolDown() != 0) {
                return;
            }
            // 譽譟･譏ｯ蜷ｦ蝨ｨ諡画・
            if (data.isBolting) {
                return;
            }
            IGunOperator gunOperator = IGunOperator.fromLivingEntity(shooter);
            boolean shootingNeedConsumeAmmo = gunOperator.consumesAmmoOrNot();
            // 譽譟･ bolt 邀ｻ蝙区弍蜷ｦ譏ｯ manual action
            Bolt boltType = gunIndex.getGunData().getBolt();
            // 譏ｯ蜷ｦ荳ｺ閭悟桁逶ｴ隸ｻ
            boolean useInventoryAmmo = iGun.useInventoryAmmo(currentGunItem);
            // 閹帛・譏ｯ蜷ｦ譛牙ｭ仙ｼｹ
            boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(currentGunItem) && boltType != Bolt.OPEN_BOLT;
            // 閭悟桁蜀・弍蜷ｦ霑俶怏蟄仙ｼｹ (蛻幃讓｡蠑乗弍蜷ｦ豸郁苓レ蛹・､・ｼｹ)
            boolean hasInventoryAmmo = iGun.hasInventoryAmmo(shooter, currentGunItem, shootingNeedConsumeAmmo);
            // 蛻､譁ｭ豐｡譛牙ｭ仙ｼｹ逧・擅莉ｶ (閭悟桁逶ｴ隸ｻ荳泌桁蜀・ｲ｡蟄仙ｼｹ / 髱櫁レ蛹・峩隸ｻ荳泌ｼｹ蛹｣蟄仙ｼｹ謨ｰ < 1)
            boolean noAmmo = useInventoryAmmo && !hasInventoryAmmo ||
                    !useInventoryAmmo && iGun.getCurrentAmmoCount(currentGunItem) < 1;
            if (boltType != Bolt.MANUAL_ACTION) {
                return;
            }
            // 譽譟･譏ｯ蜷ｦ譛牙ｼｹ闕ｯ蝨ｨ譫ｪ閹帛・
            if (hasAmmoInBarrel) {
                return;
            }
            // 譽譟･蠑ｹ蛹｣蜀・弍蜷ｦ譛牙ｭ仙ｼｹ
            if (noAmmo) {
                return;
            }
            data.boltTimestamp = System.currentTimeMillis();
            data.isBolting = iGun.startBolt(data, currentGunItem, shooter);
        });
    }

    public void tickBolt() {
        // bolt cool down 荳ｺ -1 譌ｶ・御ｻ｣陦ｨ諡画馴ｻ霎題ｿ帷ｨ区ｲ｡譛牙ｼ蟋具ｼ御ｸ埼怙隕》ick
        if (!data.isBolting) {
            return;
        }
        if (data.currentGunItem == null) {
            data.isBolting = false;
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof AbstractGunItem iGun)) {
            data.isBolting = false;
            return;
        }
        Identifier gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        data.isBolting = gunIndex.map(index -> iGun.tickBolt(data, currentGunItem, shooter)).orElse(false);
    }
}

