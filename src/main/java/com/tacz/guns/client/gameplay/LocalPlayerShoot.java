package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.input.ShootKey;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.GunMod;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerShoot;
import com.tacz.guns.network.message.ServerMessageSyncBaseTimestamp;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.ChargeData;
import com.tacz.guns.resource.pojo.data.gun.ChargeType;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.ForgeEventCompat;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class LocalPlayerShoot {
    private static final Predicate<IGunOperator> SHOOT_LOCKED_CONDITION = operator -> operator.getSynShootCoolDown() > 0;
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerShoot(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public boolean chargeShoot(boolean isCharging) {
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            data.chargeProgress = 0f;
            data.isCharging = false;
            return false;
        }
        Identifier gunId = iGun.getGunId(mainHandItem);
        Optional<ClientGunIndex> gunIndexOptional = TimelessAPI.getClientGunIndex(gunId);
        GunDisplayInstance display = TimelessAPI.getGunDisplay(mainHandItem).orElse(null);
        if (gunIndexOptional.isEmpty() || display == null) {
            data.isCharging = false;
            return false;
        }
        ClientGunIndex gunIndex = gunIndexOptional.get();
        GunData gunData = gunIndex.getGunData();
        FireMode fireMode = iGun.getFireMode(mainHandItem);
        ChargeData chargeData = gunData.getChargeData(fireMode);
        if (chargeData == null) {
            data.isCharging = false;
            data.chargeProgress = 0f;
            return isCharging;
        }

        boolean canChargeDuringCooldown = chargeData.isChargeDuringCooldown() || getCoolDown(iGun, mainHandItem, gunData) < 50;
        boolean canCharge = canChargeDuringCooldown && canStartCharge(iGun, gunOperator, mainHandItem, gunData);
        float chargeProgress = data.chargeProgress;
        ChargeType type = chargeData.getChargeType();

        if (type == ChargeType.AUTO) {
            if (isCharging && canCharge) {
                data.isCharging = true;
                data.chargeProgress = Math.min(chargeProgress + chargeData.getIncreasePerTick(), chargeData.getMaxCharge());
                return data.chargeProgress >= chargeData.getMaxCharge();
            }
            data.isCharging = false;
            data.chargeProgress = Math.max(chargeProgress - chargeData.getDecreasePerTick(), 0f);
        } else if (type == ChargeType.HOLD) {
            if (isCharging && canCharge) {
                data.isCharging = true;
                data.chargeProgress = Math.min(chargeProgress + chargeData.getIncreasePerTick(), chargeData.getMaxCharge());
            } else {
                if (canChargeDuringCooldown && chargeProgress >= chargeData.getFireThreshold()) {
                    return true;
                }
                data.isCharging = false;
                data.chargeProgress = Math.max(chargeProgress - chargeData.getDecreasePerTick(), 0f);
            }
        } else if (type == ChargeType.DELAY) {
            if ((isCharging || chargeProgress > 0) && canCharge) {
                data.isCharging = true;
                data.chargeProgress = Math.min(chargeProgress + chargeData.getIncreasePerTick(), chargeData.getMaxCharge());
                return data.chargeProgress >= chargeData.getMaxCharge();
            }
            data.isCharging = false;
            data.chargeProgress = Math.max(chargeProgress - chargeData.getDecreasePerTick(), 0f);
        }
        return false;
    }

    private boolean canStartCharge(IGun iGun, IGunOperator gunOperator, ItemStack mainHandItem, GunData gunData) {
        if (data.clientStateLock && data.lockedCondition != SHOOT_LOCKED_CONDITION && data.lockedCondition != null) {
            return false;
        }
        if (gunOperator.getSynReloadState().getStateType().isReloading()) {
            return false;
        }
        if (gunOperator.getSynDrawCoolDown() != 0 || gunOperator.getSynIsBolting() || gunOperator.getSynMeleeCoolDown() != 0) {
            return false;
        }
        if (gunOperator.getSynSprintTime() > 0) {
            return false;
        }
        Bolt boltType = gunData.getBolt();
        boolean useInventoryAmmo = iGun.useInventoryAmmo(mainHandItem);
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(mainHandItem) && boltType != Bolt.OPEN_BOLT;
        boolean hasInventoryAmmo = iGun.hasInventoryAmmo(player, mainHandItem, gunOperator.needCheckAmmo()) || hasAmmoInBarrel;
        int ammoCount = iGun.getCurrentAmmoCount(mainHandItem) + (hasAmmoInBarrel ? 1 : 0);
        boolean noAmmo = useInventoryAmmo && !hasInventoryAmmo || !useInventoryAmmo && ammoCount < 1;
        return !noAmmo && (!gunData.hasHeatData() || !iGun.isOverheatLocked(mainHandItem));
    }

    public ShootResult shoot() {
        ServerMessageSyncBaseTimestamp.applyPendingBaseTimestamp(player);
        data.ensureClientBaseTimestampSynced();
        ItemStack previewMainHandItem = player.getMainHandItem();
        IGun previewGun = IGun.getIGunOrNull(previewMainHandItem);
        if (previewGun != null) {
            ensureCurrentGunShootContext(previewGun.getGunId(previewMainHandItem), previewGun.getFireMode(previewMainHandItem));
        } else {
            data.clientShootTimestamp = -1L;
            data.clientShootGunId = null;
            data.clientShootFireMode = null;
        }
        // 謖蛾聴蜀ｷ蜊ｴ譌ｶ髣ｴ譛ｪ蛻ｰ・碁亟豁｢轤ｹ蜃ｻ謖蛾聴蜷手ｯｯ隗ｦ蠑轣ｫ
        // 鮟倩ｮ､隶ｾ鄂ｮ荳ｺ 50 ms
        if (System.currentTimeMillis() - LocalPlayerDataHolder.clientClickButtonTimestamp < 50) {
            return ShootResult.COOL_DOWN;
        }
        // 螯よ棡荳贋ｸ谺｡蠑よｭ･蠑轣ｫ逧・譜譫懆ｿ俶悴謇ｧ陦鯉ｼ悟・逶ｴ謗･霑泌屓・檎ｭ牙ｾ・ｼよｭ･蠑轣ｫ謨域棡謇ｧ陦・
        if (!data.isShootRecorded) {
            return ShootResult.COOL_DOWN;
        }
        // 螯よ棡迥ｶ諤・煤豁｣蝨ｨ蜃・､・煤螳夲ｼ御ｸ比ｸ肴弍蠑轣ｫ逧・憾諤・煤・悟・荳榊・隶ｸ蠑轣ｫ(荳ｻ隕∫畑莠朱亟豁｢蛻・棯蜷主ｼ轣ｫ蜉ｨ菴懆ｦ・尠蛻・棯蜉ｨ菴・
        if (data.clientStateLock && data.lockedCondition != SHOOT_LOCKED_CONDITION && data.lockedCondition != null) {
            data.isShootRecorded = true;
            // 蝗荳ｺ霑吝摎荳ｻ隕∫岼逧・弍髦ｲ豁｢蛻・棯蜷主ｼ轣ｫ蜉ｨ菴懆ｦ・尠蛻・棯蜉ｨ菴懶ｼ瑚ｿ泌屓 IS_DRAWING
            return ShootResult.IS_DRAWING;
        }
        // 證ょｮ壻ｸｺ蜿ｪ譛我ｸｻ謇玖・蠑譫ｪ
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            return ShootResult.NOT_GUN;
        }
        Identifier gunId = iGun.getGunId(mainHandItem);
        FireMode fireMode = iGun.getFireMode(mainHandItem);
        ensureCurrentGunShootContext(gunId, fireMode);
        Optional<ClientGunIndex> gunIndexOptional = TimelessAPI.getClientGunIndex(gunId);
        GunDisplayInstance display = TimelessAPI.getGunDisplay(mainHandItem).orElse(null);
        if (gunIndexOptional.isEmpty() || display == null) {
            return ShootResult.ID_NOT_EXIST;
        }
        ClientGunIndex gunIndex = gunIndexOptional.get();
        GunData gunData = gunIndex.getGunData();
        long coolDown = this.getCoolDown(iGun, mainHandItem, gunData);
        // 螯よ棡蟆・・蜀ｷ蜊ｴ螟ｧ莠守ｭ我ｺ・1 tick (蜊ｳ 50 ms)・悟・荳榊・隶ｸ蠑轣ｫ
        if (coolDown >= 50) {
            return ShootResult.COOL_DOWN;
        }
        // 蝗荳ｺ蠑轣ｫ蜀ｷ蜊ｴ譽豬狗畑莠・音蛻ｫ螳壼宛逧・婿豕包ｼ梧園莉･荳肴｣譟･迥ｶ諤・煤・瑚梧弍謇句勘譽譟･譏ｯ蜷ｦ謐｢蠑ｹ縲∝・譫ｪ
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        // 譽譟･譏ｯ蜷ｦ豁｣蝨ｨ謐｢蠑ｹ
        if (gunOperator.getSynReloadState().getStateType().isReloading()) {

            return ShootResult.IS_RELOADING;
        }
        // 譽譟･譏ｯ蜷ｦ豁｣蝨ｨ蛻・棯
        if (gunOperator.getSynDrawCoolDown() != 0) {
            return ShootResult.IS_DRAWING;
        }
        // 譽譟･譏ｯ蜷ｦ豁｣蝨ｨ諡画・
        if (gunOperator.getSynIsBolting()) {
            return ShootResult.IS_BOLTING;
        }
        // 蛻､譁ｭ譏ｯ蜷ｦ螟・ｺ手ｿ第・蜀ｷ蜊ｴ譌ｶ髣ｴ
        if (gunOperator.getSynMeleeCoolDown() != 0) {
            return ShootResult.IS_MELEE;
        }
        // 蛻､譁ｭ蟄仙ｼｹ謨ｰ
        Bolt boltType = gunIndex.getGunData().getBolt();
        // 譏ｯ蜷ｦ荳ｺ閭悟桁逶ｴ隸ｻ
        boolean useInventoryAmmo = iGun.useInventoryAmmo(mainHandItem);
        // 閹帛・譏ｯ蜷ｦ譛牙ｭ仙ｼｹ
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(mainHandItem) && boltType != Bolt.OPEN_BOLT;
        // 譏ｯ蜷ｦ霑俶怏蟄仙ｼｹ (蛻幃讓｡蠑乗弍蜷ｦ豸郁苓レ蛹・､・ｼｹ)
        boolean hasInventoryAmmo = iGun.hasInventoryAmmo(player, mainHandItem, gunOperator.needCheckAmmo()) || hasAmmoInBarrel;
        int ammoCount = iGun.getCurrentAmmoCount(mainHandItem) + (hasAmmoInBarrel ? 1 : 0);
        // 蛻､譁ｭ豐｡譛牙ｭ仙ｼｹ逧・擅莉ｶ (閭悟桁逶ｴ隸ｻ荳泌桁蜀・ｲ｡蟄仙ｼｹ / 髱櫁レ蛹・峩隸ｻ荳疲ｻ蟄仙ｼｹ謨ｰ < 1)
        boolean noAmmo = useInventoryAmmo && !hasInventoryAmmo ||
                !useInventoryAmmo && ammoCount < 1;
        if (noAmmo) {
            SoundPlayManager.playDryFireSound(player, display);
            return ShootResult.NO_AMMO;
        }
        //Handle Heat Data
        if(gunData.hasHeatData()) {
            if(iGun.isOverheatLocked(mainHandItem)) {
                SoundPlayManager.playDryFireSound(player, display);
                return ShootResult.OVERHEATED;
            }
        }
        // 譽譟･閹帛・蟄仙ｼｹ
        if (boltType == Bolt.MANUAL_ACTION && !hasAmmoInBarrel) {
            IClientPlayerGunOperator.fromLocalPlayer(player).bolt();
            return ShootResult.NEED_BOLT;
        }
        // 譽譟･譏ｯ蜷ｦ豁｣蝨ｨ螂碑ｷ・
        if (gunOperator.getSynSprintTime() > 0) {
            return ShootResult.IS_SPRINTING;
        }
        // 隗ｦ蜿大ｼ轣ｫ莠倶ｻｶ
        if (ForgeEventCompat.post(new GunShootEvent(player, mainHandItem, LogicalSide.CLIENT))) {
            return ShootResult.FORGE_EVENT_CANCEL;
        }
        // 蛻・困迥ｶ諤・煤・御ｸ榊・隶ｸ謐｢蠑ｹ縲∵｣隗・ｭ芽｡御ｸｺ霑幄｡後・
        data.lockState(SHOOT_LOCKED_CONDITION);
        data.isShootRecorded = false;
        // 隹・畑蠑轣ｫ騾ｻ霎・
        this.doShoot(display, iGun, mainHandItem, gunData, coolDown);
        ChargeData chargeData = gunData.getChargeData(fireMode);
        if (chargeData != null) {
            if (chargeData.getChargeType() == ChargeType.DELAY) {
                data.chargeProgress = 0f;
            } else {
                data.chargeProgress = Math.max(0f, data.chargeProgress - chargeData.getDecreaseOnFire());
            }
        }
        return ShootResult.SUCCESS;
    }

    private void doShoot(GunDisplayInstance display, IGun iGun, ItemStack mainHandItem, GunData gunData, long delay) {
        FireMode fireMode = iGun.getFireMode(mainHandItem);
        Identifier gunId = iGun.getGunId(mainHandItem);
        Bolt boltType = gunData.getBolt();
        // 闔ｷ蜿紋ｽ吝ｼｹ謨ｰ
        boolean consumeAmmo = IGunOperator.fromLivingEntity(player).consumesAmmoOrNot();
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(mainHandItem) && boltType != Bolt.OPEN_BOLT;
        int ammoCount = consumeAmmo ? iGun.getCurrentAmmoCount(mainHandItem) + (hasAmmoInBarrel ? 1 : 0) : Integer.MAX_VALUE;
        // 霑槫書蟆・・髣ｴ髫・
        long period = fireMode == FireMode.BURST ? gunData.getBurstShootInterval() : 1;
        // 譛螟ｧ霑槫書謨ｰ
        final int maxCount = Math.min(ammoCount, fireMode == FireMode.BURST ? gunData.getBurstData().getCount() : 1);
        // 霑槫書隶｡謨ｰ蝎ｨ
        AtomicInteger count = new AtomicInteger(0);

        final ScheduledFuture<?>[] scheduledTaskRef = new ScheduledFuture<?>[1];
        scheduledTaskRef[0] = LocalPlayerDataHolder.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {

            if (count.get() == 0) {
                // 霓ｬ謐｢ isRecord 迥ｶ諤・ｼ悟・隶ｸ荳倶ｸ荳ｪtick逧・ｼ轣ｫ譽豬九・
                data.isShootRecorded = true;
            }
            //Handle Heat Data
            if(gunData.hasHeatData()) {
                if(iGun.isOverheatLocked(mainHandItem)) {
                    ScheduledFuture<?> future = scheduledTaskRef[0];
                    future.cancel(false); // 蜿匁ｶ亥ｽ灘燕莉ｻ蜉｡
                    return;
                }
            }
            // 螯よ棡霎ｾ蛻ｰ譛螟ｧ霑槫書谺｡謨ｰ・梧・閠・自螳ｶ蟾ｲ扈乗ｭｻ莠｡・悟叙豸井ｻｻ蜉｡
            if (count.get() >= maxCount || player.isDeadOrDying()) {
                ScheduledFuture<?> future = scheduledTaskRef[0];
                future.cancel(false); // 蜿匁ｶ亥ｽ灘燕莉ｻ蜉｡
                return;
            }

            // 莉･荳矩ｻ霎大宵髴隕∵鴬陦御ｸ谺｡
            if (count.get() == 0) {
                // 螯よ棡迥ｶ諤・煤豁｣蝨ｨ蜃・､・煤螳夲ｼ御ｸ比ｸ肴弍蠑轣ｫ逧・憾諤・煤・悟・荳榊・隶ｸ蠑轣ｫ(荳ｻ隕∫畑莠朱亟豁｢蛻・棯蜷主ｼ轣ｫ蜉ｨ菴懆ｦ・尠蛻・棯蜉ｨ菴・
                if (data.clientStateLock && data.lockedCondition != SHOOT_LOCKED_CONDITION && data.lockedCondition != null) {
                    return;
                }
                // 隶ｰ蠖墓眠逧・ｼ轣ｫ譌ｶ髣ｴ謌ｳ
                data.clientLastShootTimestamp = data.clientShootTimestamp;
                data.clientShootTimestamp = System.currentTimeMillis();
                data.clientShootGunId = gunId;
                data.clientShootFireMode = fireMode;
                long relativeShootTimestamp = data.clientShootTimestamp - data.clientBaseTimestamp;
                if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                    GunItemRendererWrapper.captureShotMuzzleBasis(relativeShootTimestamp, player.getXRot(), player.getYRot(), player.getEyePosition());
                }
                GunMod.LOGGER.debug("ShootTrace[client] send shoot: gunId={}, fireMode={}, delayMs={}, periodMs={}, maxCount={}, shootTs={}",
                        gunId, fireMode, delay, period, maxCount, data.clientShootTimestamp);
                // 蜿鷹∝ｼ轣ｫ逧・焚謐ｮ蛹・ｼ碁夂衍譛榊苅蝎ｨ
                boolean chargeShootMode = gunData.getChargeData(fireMode) != null;
                float sentChargeProgress = chargeShootMode ? data.chargeProgress : 0f;
                NetworkHandler.sendToServer(new ClientMessagePlayerShoot(
                        relativeShootTimestamp,
                        player.getXRot(),
                        player.getYRot(),
                        sentChargeProgress
                ));
                ShootKey.consumeAutoShootInitialShotPending();
            }

            // todo 髴隕∵｣譟･
            // 謦ｭ謾ｾ螢ｰ髻ｳ蜥檎憾諤∵惻隗ｦ蜿鷹怙隕∽ｻ主ｼよｭ･郤ｿ遞倶ｸ贋ｼ蛻ｰ荳ｻ郤ｿ遞区鴬陦鯉ｼ悟凄蛻吩ｼ壼ｼ戊ｵｷcme
            Minecraft.getInstance().submitAsync(() -> {
                // 隗ｦ蜿大・蜿台ｺ倶ｻｶ
                boolean fire = !ForgeEventCompat.post(new GunFireEvent(player, mainHandItem, LogicalSide.CLIENT));
                if (fire) {
                    // 蜉ｨ逕ｻ蜥悟｣ｰ髻ｳ蠕ｪ邇ｯ謦ｭ謾ｾ
                    AnimationStateMachine<?> animationStateMachine = display.getAnimationStateMachine();
                    if (animationStateMachine != null) {
                        animationStateMachine.trigger(GunAnimationConstant.INPUT_SHOOT);
                    }
                    // 闔ｷ蜿匁ｶ磯浹
                    final boolean useSilenceSound = this.useSilenceSound();
                    // 蠑轣ｫ髴隕∵遠譁ｭ譽隗・
                    SoundPlayManager.stopPlayGunSound(display, SoundManager.INSPECT_SOUND);
                    if (useSilenceSound) {
                        SoundPlayManager.playSilenceSound(player, display, gunData);
                    } else {
                        SoundPlayManager.playShootSound(player, display, gunData);
                    }
                }
            });

            count.getAndIncrement();
        }, delay, period, TimeUnit.MILLISECONDS);
    }

    private boolean useSilenceSound() {
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(player).getCacheProperty();
        if (cacheProperty != null) {
            Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
            return silence.right();
        }
        return false;
    }

    private long getCoolDown(IGun iGun, ItemStack mainHandItem, GunData gunData) {
        FireMode fireMode = iGun.getFireMode(mainHandItem);
        long coolDown;
        if (fireMode == FireMode.BURST) {
            coolDown = (long) (gunData.getBurstData().getMinInterval() * 1000f) - (System.currentTimeMillis() - data.clientShootTimestamp);
        } else {
            coolDown = gunData.getShootInterval(this.player, fireMode, mainHandItem) - (System.currentTimeMillis() - data.clientShootTimestamp);
        }
        return Math.max(coolDown, 0);
    }

    public long getClientShootCoolDown() {
        ItemStack mainHandItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            return -1;
        }
        Identifier gunId = iGun.getGunId(mainHandItem);
        ensureCurrentGunShootContext(gunId, iGun.getFireMode(mainHandItem));
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        return gunIndexOptional.map(commonGunIndex -> getCoolDown(iGun, mainHandItem, commonGunIndex.getGunData())).orElse(-1L);
    }

    private void ensureCurrentGunShootContext(Identifier gunId, FireMode fireMode) {
        Identifier lastShootGunId = data.clientShootGunId;
        FireMode lastShootFireMode = data.clientShootFireMode;
        if (lastShootGunId == null || !lastShootGunId.equals(gunId) || lastShootFireMode != fireMode) {
            GunMod.LOGGER.debug("ShootTrace[client] context reset: {} -> {}, shootTs={}, lastShootTs={}",
                    lastShootGunId, gunId, data.clientShootTimestamp, data.clientLastShootTimestamp);
            data.clientShootTimestamp = -1L;
            data.clientLastShootTimestamp = -1L;
            data.clientShootGunId = gunId;
            data.clientShootFireMode = fireMode;
            data.chargeProgress = 0f;
            data.isCharging = false;
        }
    }
}

