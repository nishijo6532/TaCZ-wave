package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.GunMod;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncBaseTimestamp;
import com.tacz.guns.network.message.event.ServerMessageGunShoot;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.modifier.custom.RpmModifier;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.util.ForgeEventCompat;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.PacketDistributor;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class LivingEntityShoot {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;

    public LivingEntityShoot(LivingEntity shooter, ShooterDataHolder data, LivingEntityDrawGun draw) {
        this.shooter = shooter;
        this.data = data;
        this.draw = draw;
    }

    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw, long timestamp) {
        syncCurrentGunItemWithMainHandGun();
        if (data.currentGunItem == null) {
            return ShootResult.NOT_DRAW;
        }
        ItemStack currentGunItem = resolveCurrentGunItem();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return ShootResult.NOT_GUN;
        }
        Identifier gunId = iGun.getGunId(currentGunItem);
        FireMode currentFireMode = iGun.getFireMode(currentGunItem);
        if (!Objects.equals(data.lastShootGunId, gunId) || data.lastShootFireMode != currentFireMode) {
            GunMod.LOGGER.debug("ShootTrace[server] context reset: {} -> {}, shootTs={}, lastShootTs={}",
                    data.lastShootGunId, gunId, data.shootTimestamp, data.lastShootTimestamp);
            data.shootTimestamp = -1L;
            data.lastShootTimestamp = -1L;
            data.lastShootGunId = null;
            data.lastShootFireMode = null;
            AttachmentPropertyManager.postChangeEvent(shooter, currentGunItem);
        }
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return ShootResult.ID_NOT_EXIST;
        }
        CommonGunIndex gunIndex = gunIndexOptional.get();
        long serverShootInterval = gunIndex.getGunData().getShootInterval(this.shooter, currentFireMode, currentGunItem);
        int baseRpm = gunIndex.getGunData().getRoundsPerMinute(currentFireMode);
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(shooter).getCacheProperty();
        Integer cacheRpm = cacheProperty == null ? null : Mth.clamp(cacheProperty.<Integer>getCache(RpmModifier.ID), 1, 1200);
        if (SyncConfig.SERVER_SHOOT_COOLDOWN_V.get()) {
            // 蛻､譁ｭ蟆・・譏ｯ蜷ｦ豁｣蝨ｨ蜀ｷ蜊ｴ
            long coolDown = getShootCoolDown(timestamp);
            if (coolDown == -1) {
                // 荳闊ｬ譚･隸ｴ荳榊､ｪ蜿ｯ閭ｽ荳ｺ -1・悟次蝗譛ｪ遏･
                return ShootResult.UNKNOWN_FAIL;
            }
            if (coolDown > 0) {
                GunMod.LOGGER.debug("ShootTrace[server] cooldown deny: gunId={}, fireMode={}, cooldownMs={}, shootTs={}, reqTs={}, intervalMs={}, baseRpm={}, cacheRpm={}",
                        gunId, currentFireMode, coolDown, data.shootTimestamp, timestamp, serverShootInterval, baseRpm, cacheRpm);
                return ShootResult.COOL_DOWN;
            }
        }
        if (SyncConfig.SERVER_SHOOT_NETWORK_V.get()) {
            // 譬ｹ謐ｮ tick time 蜥・蜈∬ｮｸ逧・ｽ醍ｻ懷ｻｶ霑滓ｳ｢蜉ｨ 隶｡邂・譌ｶ髣ｴ謌ｳ逧・磁蜿礼ｪ怜哨
            double tickTime = 50;
            long alpha = System.currentTimeMillis() - data.baseTimestamp - timestamp;
            if (alpha < -300 || alpha > 300 + tickTime * 2) { // 蜈∬ｮｸ +- 300ms 逧・ｽ醍ｻ懈ｳ｢蜉ｨ縲∫ｪ怜哨荳矩剞蜀肴黄螟ｧ 2 荳ｪ tick time 譌ｶ髣ｴ(譛蝮乗ュ蜀ｵ蟆・・莨壼ｻｶ霑・荳ｪ tick)
                if (shooter instanceof ServerPlayer player) {
                    NetworkHandler.CHANNEL.send(new ServerMessageSyncBaseTimestamp(), PacketDistributor.PLAYER.with(player));
                }
                return ShootResult.NETWORK_FAIL;
            }
        }
        // 譽譟･譏ｯ蜷ｦ豁｣蝨ｨ謐｢蠑ｹ
        if (data.reloadStateType.isReloading()) {
            return ShootResult.IS_RELOADING;
        }
        // 譽譟･譏ｯ蜷ｦ蝨ｨ蛻・棯
        if (draw.getDrawCoolDown() != 0) {
            return ShootResult.IS_DRAWING;
        }
        // 譽譟･譏ｯ蜷ｦ蝨ｨ諡画・
        if (data.isBolting) {
            return ShootResult.IS_BOLTING;
        }
        // 譽譟･譏ｯ蜷ｦ蝨ｨ螂碑ｷ・
        if (data.sprintTimeS > 0) {
            return ShootResult.IS_SPRINTING;
        }
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(shooter);
        // 蛻､譁ｭ蟄仙ｼｹ謨ｰ
        Bolt boltType = gunIndex.getGunData().getBolt();
        // 譏ｯ蜷ｦ荳ｺ閭悟桁逶ｴ隸ｻ
        boolean useInventoryAmmo = iGun.useInventoryAmmo(currentGunItem);
        // 閹帛・譏ｯ蜷ｦ譛牙ｭ仙ｼｹ
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(currentGunItem) && boltType != Bolt.OPEN_BOLT;
        // 譏ｯ蜷ｦ霑俶怏蟄仙ｼｹ (蛻幃讓｡蠑乗弍蜷ｦ豸郁苓レ蛹・､・ｼｹ)
        boolean hasInventoryAmmo = iGun.hasInventoryAmmo(shooter, currentGunItem, gunOperator.needCheckAmmo()) || hasAmmoInBarrel;
        int ammoCount = iGun.getCurrentAmmoCount(currentGunItem) + (hasAmmoInBarrel ? 1 : 0);
        // 蛻､譁ｭ豐｡譛牙ｭ仙ｼｹ逧・擅莉ｶ (閭悟桁逶ｴ隸ｻ荳泌桁蜀・ｲ｡蟄仙ｼｹ / 髱櫁レ蛹・峩隸ｻ荳疲ｻ蟄仙ｼｹ謨ｰ < 1)
        boolean noAmmo = useInventoryAmmo && !hasInventoryAmmo ||
                !useInventoryAmmo && ammoCount < 1;
        if (noAmmo) {
            return ShootResult.NO_AMMO;
        }
        //Handle Heat Data
        if(gunIndex.getGunData().hasHeatData()) {
            if(iGun.isOverheatLocked(currentGunItem)) {
                return ShootResult.OVERHEATED;
            }
        }
        // 譽譟･閹帛・蟄仙ｼｹ
        if (boltType == Bolt.MANUAL_ACTION && !hasAmmoInBarrel) {
            return ShootResult.NEED_BOLT;
        }
        // 髣ｭ閹帷噪閹帛・譽譟･騾ｻ霎・
        if (boltType == Bolt.CLOSED_BOLT && !hasAmmoInBarrel) {
            // 荳､遘堺ｸ榊酔逧・ｸ願・諠・・
            if (useInventoryAmmo) {
                consumeAmmoFromPlayer(1, currentGunItem, gunOperator.needCheckAmmo());
            } else {
                iGun.reduceCurrentAmmoCount(currentGunItem);
            }
            iGun.setBulletInBarrel(currentGunItem, true);
        }
        // 隗ｦ蜿大ｰ・・莠倶ｻｶ
        if (ForgeEventCompat.post(new GunShootEvent(shooter, currentGunItem, LogicalSide.SERVER))) {
            return ShootResult.FORGE_EVENT_CANCEL;
        }

        NetworkHandler.sendToTrackingEntity(new ServerMessageGunShoot(shooter.getId(), currentGunItem), shooter);
        data.lastShootTimestamp = data.shootTimestamp;
        data.heatTimestamp = System.currentTimeMillis();
        data.shootTimestamp = timestamp;
        data.lastShootGunId = gunId;
        data.lastShootFireMode = currentFireMode;
        GunMod.LOGGER.debug("ShootTrace[server] shoot accepted: gunId={}, fireMode={}, reqTs={}, shootTs={}, lastShootTs={}",
                gunId, currentFireMode, timestamp, data.shootTimestamp, data.lastShootTimestamp);
        // 謇ｧ陦梧棯譴ｰ蟆・・騾ｻ霎・
        if (iGun instanceof AbstractGunItem logicGun) {
            logicGun.shoot(data, currentGunItem, pitch, yaw, shooter);
        }
        return ShootResult.SUCCESS;
    }

    /**
     * 莉･蠖灘燕譌ｶ髣ｴ謌ｳ譟･隸｢蟆・・蜀ｷ蜊ｴ縲りｿ泌屓蛟ｼ荳闊ｬ荳堺ｼ夊ｶ・ｿ・棯譴ｰ逧・ｰ・・髣ｴ髫・
     * @return 蟆・・蜀ｷ蜊ｴ
     */
    public long getShootCoolDown() {
        return getShootCoolDown(System.currentTimeMillis() - data.baseTimestamp);
    }

    /**
     * 譟･隸｢謖・ｮ夂噪 timestamp 荳狗噪蟆・・蜀ｷ蜊ｴ縲よｹ謐ｮ諠・・霑泌屓蛟ｼ蜿ｯ閭ｽ雜・ｿ・棯譴ｰ逧・ｰ・・髣ｴ髫斐・
     * @param timestamp 謖・ｮ・timestamp・梧弍蛛冗ｧｻ譌ｶ髣ｴ謌ｳ・亥渕莠暫ase timestamp 逧・嶌蟇ｹ譌ｶ髣ｴ謌ｳ・・
     * @return 蟆・・蜀ｷ蜊ｴ
     */
    public long getShootCoolDown(long timestamp) {
        syncCurrentGunItemWithMainHandGun();
        if (data.currentGunItem == null) {
            return 0;
        }
        ItemStack currentGunItem = resolveCurrentGunItem();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return 0;
        }
        Identifier gunId = iGun.getGunId(currentGunItem);
        if (!Objects.equals(data.lastShootGunId, gunId) || data.lastShootFireMode != iGun.getFireMode(currentGunItem)) {
            return 0;
        }
        if (data.shootTimestamp < 0L) {
            return 0;
        }
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        FireMode fireMode = iGun.getFireMode(currentGunItem);
        long interval = timestamp - data.shootTimestamp;
        if (fireMode == FireMode.BURST) {
            return gunIndex.map(index -> {
                long coolDown = (long) (index.getGunData().getBurstData().getMinInterval() * 1000f) - interval;
                // 扈・5 ms 逧・ｪ怜哨譌ｶ髣ｴ・御ｻ･蟷ｳ陦｡蟒ｶ霑・
                coolDown = coolDown - 5;
                return Math.max(coolDown, 0L);
            }).orElse(-1L);
        }
        return gunIndex.map(index -> {
            long coolDown = index.getGunData().getShootInterval(this.shooter, fireMode, currentGunItem) - interval;
            // 扈・5 ms 逧・ｪ怜哨譌ｶ髣ｴ・御ｻ･蟷ｳ陦｡蟒ｶ霑・
            coolDown = coolDown - 5;
            return Math.max(coolDown, 0L);
        }).orElse(-1L);
    }

    private ItemStack resolveCurrentGunItem() {
        ItemStack currentGunItem = data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
        ItemStack mainHandItem = shooter.getMainHandItem();
        IGun currentGun = IGun.getIGunOrNull(currentGunItem);
        IGun mainHandGun = IGun.getIGunOrNull(mainHandItem);
        if (mainHandGun != null) {
            if (currentGun == null || !mainHandGun.getGunId(mainHandItem).equals(currentGun.getGunId(currentGunItem))) {
                data.currentGunItem = () -> shooter.getMainHandItem();
                currentGunItem = mainHandItem;
            }
        }
        return currentGunItem;
    }

    private void syncCurrentGunItemWithMainHandGun() {
        ItemStack mainHandItem = shooter.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun) {
            data.currentGunItem = () -> shooter.getMainHandItem();
        }
    }

    /**
     * 豸郁怜､・ｼｹ TODO: 髴隕∵｣譟･・梧弍蜷ｦ譛牙・莉匁峩邂蜊慕噪譁ｹ豕墓ｶ郁苓レ蛹・・逧・ｼｹ闕ｯ (霑呎ｮｵ譏ｯ逶ｴ謗･莉朱ｻ霎第惻 API 驥悟､榊宛霑・擂逧・
     */
    public void consumeAmmoFromPlayer(int neededAmount, ItemStack itemStack, boolean needCheckAmmo) {
        if (!(itemStack.getItem() instanceof AbstractGunItem abstractGunItem)) {
            return;
        }
        // 螯よ棡螟・ｺ主・騾讓｡蠑丈ｸ肴ｶ郁礼噪諠・・
        if (!needCheckAmmo) {
            return;
        }
        if (abstractGunItem.useDummyAmmo(itemStack)) {
            abstractGunItem.findAndExtractDummyAmmo(itemStack, neededAmount);
        } else {
            abstractGunItem.findAndExtractInventoryAmmo(shooter, itemStack, neededAmount);
        }
    }
}

