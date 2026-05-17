package com.tacz.guns.entity.shooter;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunReload;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.util.ForgeEventCompat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

public class LivingEntityReload {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;
    private final LivingEntityShoot shoot;

    public LivingEntityReload(LivingEntity shooter, ShooterDataHolder data, LivingEntityDrawGun draw, LivingEntityShoot shoot) {
        this.shooter = shooter;
        this.data = data;
        this.draw = draw;
        this.shoot = shoot;
    }

    public void reload() {
        ItemStack mainHandItem = shooter.getMainHandItem();
        if (mainHandItem.getItem() instanceof AbstractGunItem) {
            // Align reload target with the actual main-hand gun stack on Forge64.
            data.currentGunItem = () -> shooter.getMainHandItem();
        }
        if (data.currentGunItem == null) {
            IGunOperator.fromLivingEntity(shooter).initialData();
            if (data.currentGunItem == null) {
                return;
            }
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof AbstractGunItem gunItem)) {
            return;
        }
        Identifier gunId = gunItem.getGunId(currentGunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> {
            // 譽譟･譏ｯ蜷ｦ荳ｺ閭悟桁逶ｴ隸ｻ
            if (gunItem.useInventoryAmmo(currentGunItem)) {
                return;
            }
            // 譽譟･謐｢蠑ｹ譏ｯ蜷ｦ霑俶悴螳梧・
            if (data.reloadStateType.isReloading()) {
                return;
            }
            // 譽譟･譏ｯ蜷ｦ豁｣蝨ｨ蠑轣ｫ蜀ｷ蜊ｴ
            if (shoot.getShootCoolDown() != 0) {
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
            // 譽譟･蠑ｹ闕ｯ
            boolean needCheckAmmo = IGunOperator.fromLivingEntity(shooter).needCheckAmmo();
            boolean canReload = gunItem.canReload(shooter, currentGunItem);
            if (gunItem.getCurrentAmmoCount(currentGunItem) >= AttachmentDataUtils.getAmmoCountWithAttachment(currentGunItem, gunIndex.getGunData())) {
                return;
            }
            if (needCheckAmmo && !canReload) {
                GunMod.LOGGER.debug("Reload denied: gunId={}, useInventoryAmmo={}, useDummyAmmo={}, dummyAmmo={}, currentAmmo={}, hasBulletInBarrel={}, hasConsumableAmmo={}",
                        gunId, gunItem.useInventoryAmmo(currentGunItem), gunItem.useDummyAmmo(currentGunItem), gunItem.getDummyAmmoAmount(currentGunItem),
                        gunItem.getCurrentAmmoCount(currentGunItem), gunItem.hasBulletInBarrel(currentGunItem), gunItem.hasConsumableAmmo(shooter, currentGunItem));
                return;
            }
            // 隗ｦ蜿題｣・ｼｹ莠倶ｻｶ
            if (ForgeEventCompat.post(new GunReloadEvent(shooter, currentGunItem, LogicalSide.SERVER))) {
                return;
            }
            GunMod.LOGGER.debug("Reload accepted: gunId={}, currentAmmo={}, hasBulletInBarrel={}",
                    gunId, gunItem.getCurrentAmmoCount(currentGunItem), gunItem.hasBulletInBarrel(currentGunItem));
            NetworkHandler.sendToTrackingEntity(new ServerMessageGunReload(shooter.getId(), currentGunItem), shooter);
            Bolt boltType = gunIndex.getGunData().getBolt();
            int ammoCount = gunItem.getCurrentAmmoCount(currentGunItem) + (gunItem.hasBulletInBarrel(currentGunItem) && boltType != Bolt.OPEN_BOLT ? 1 : 0);
            if (ammoCount <= 0) {
                // 蛻晏ｧ句喧遨ｺ莉捺困蠑ｹ逧・tick 逧・憾諤・
                data.reloadStateType = ReloadState.StateType.EMPTY_RELOAD_FEEDING;
            } else {
                // 蛻晏ｧ句喧謌俶惘謐｢蠑ｹ逧・tick 逧・憾諤・
                data.reloadStateType = ReloadState.StateType.TACTICAL_RELOAD_FEEDING;
            }
            data.reloadTimestamp = System.currentTimeMillis();
            // 隹・畑譫ｪ譴ｰ騾ｻ霎・
            if (!gunItem.startReload(data, currentGunItem, shooter)) {
                GunMod.LOGGER.debug("Reload startReload returned false: gunId={}", gunId);
                data.reloadStateType = ReloadState.StateType.NOT_RELOADING;
                data.reloadTimestamp = -1;
            }
        });
    }

    public void cancelReload() {
        if (data.currentGunItem == null) {
            IGunOperator.fromLivingEntity(shooter).initialData();
            if (data.currentGunItem == null) {
                return;
            }
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof AbstractGunItem gunItem)) {
            return;
        }
        // 譽譟･譏ｯ蜷ｦ蝨ｨ謐｢蠑ｹ
        if (!data.reloadStateType.isReloading()) {
            return;
        }
        gunItem.interruptReload(data, currentGunItem, shooter);
    }

    public ReloadState tickReloadState() {
        ReloadState result = new ReloadState();
        // 螯よ棡豐｡譛牙惠謐｢蠑ｹ・檎峩謗･霑泌屓
        if (data.reloadTimestamp == -1) {
            return result;
        }
        // 隹・畑譫ｪ譴ｰ騾ｻ霎・
        if (data.currentGunItem != null) {
            ItemStack currentGunItem = data.currentGunItem.get();
            if (currentGunItem != null && currentGunItem.getItem() instanceof AbstractGunItem abstractGunItem) {
                 result = abstractGunItem.tickReload(data, currentGunItem, shooter);
            }
        }
        // 蟆・tick 逧・ｻ捺棡菫晏ｭ伜芦 data holder
        data.reloadStateType = result.getStateType();
        if (!result.getStateType().isReloading()) {
            data.reloadTimestamp = -1;
        }
        return result;
    }
}

