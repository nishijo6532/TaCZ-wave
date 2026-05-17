package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.custom.AdsModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class LivingEntityAim {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;

    public LivingEntityAim(LivingEntity shooter, ShooterDataHolder data) {
        this.shooter = shooter;
        this.data = data;
    }

    public void aim(boolean isAim) {
        data.isAiming = isAim;
    }

    public void zoom() {
        if (data.currentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        Identifier scopeId = iGun.getAttachmentId(currentGunItem, AttachmentType.SCOPE);
        CompoundTag scopeTag = iGun.getAttachmentTag(currentGunItem, AttachmentType.SCOPE);
        if (!DefaultAssets.isEmptyAttachmentId(scopeId) && scopeTag != null) {
            TimelessAPI.getCommonAttachmentIndex(scopeId).ifPresent(index -> {
                int zoomNumber = AttachmentItemDataAccessor.getZoomNumberFromTag(scopeTag);
                ++zoomNumber;
                // 驕ｿ蜈堺ｸ頑ｺ｢蜿俶・雍溽噪
                zoomNumber = zoomNumber % (Integer.MAX_VALUE - 1);
                ItemStack scopeStack = iGun.getAttachment(currentGunItem, AttachmentType.SCOPE);
                if (scopeStack.isEmpty()) {
                    return;
                }
                CompoundTag writableScopeTag = NbtCompat.getOrCreateTag(scopeStack);
                AttachmentItemDataAccessor.setZoomNumberToTag(writableScopeTag, zoomNumber);
                NbtCompat.setTag(scopeStack, writableScopeTag);
                iGun.installAttachment(currentGunItem, scopeStack);
            });
        }
    }

    public void tickAimingProgress() {
        // currentGunItem 螯よ棡荳ｺ null・悟・蜿匁ｶ育桷蜃・憾諤∝ｹｶ蟆・aimingProgress 蠖帝峺縲・
        if (data.currentGunItem == null || !(data.currentGunItem.get().getItem() instanceof IGun iGun)) {
            data.aimingProgress = 0;
            data.aimingTimestamp = System.currentTimeMillis();
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        // 螯よ棡闔ｷ蜿紋ｸ榊芦 gunIndex・悟・蜿匁ｶ育桷蜃・憾諤∝ｹｶ蟆・aimingProgress 蠖帝峺・瑚ｿ泌屓縲・
        Identifier gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            data.aimingProgress = 0;
            return;
        }
        GunData gunData = gunIndexOptional.get().getGunData();
        float aimTime = gunData.getAimTime();
        if (this.data.cacheProperty != null) {
            aimTime = this.data.cacheProperty.<Float>getCache(AdsModifier.ID);
        }
        aimTime = Math.max(0, aimTime);
        float alphaProgress = (System.currentTimeMillis() - data.aimingTimestamp + 1) / (aimTime * 1000);
        if (data.isAiming) {
            // 螟・ｺ取鴬陦檎桷蜃・憾諤・ｼ悟｢槫刈 aimingProgress
            data.aimingProgress += alphaProgress;
            if (data.aimingProgress > 1) {
                data.aimingProgress = 1;
            }
        } else {
            // 螟・ｺ主叙豸育桷蜃・憾諤・ｼ悟㍼蟆・aimingProgress
            data.aimingProgress -= alphaProgress;
            if (data.aimingProgress < 0) {
                data.aimingProgress = 0;
            }
        }
        data.aimingTimestamp = System.currentTimeMillis();
    }

    public void tickSprint() {
        IGunOperator operator = IGunOperator.fromLivingEntity(shooter);
        ReloadState reloadState = operator.getSynReloadState();
        if (data.isAiming || (reloadState.getStateType().isReloading() && !reloadState.getStateType().isReloadFinishing())) {
            shooter.setSprinting(false);
        }
        if (data.sprintTimestamp == -1) {
            data.sprintTimestamp = System.currentTimeMillis();
        }
        if (data.currentGunItem == null) {
            return;
        }
        ItemStack gunItem = data.currentGunItem.get();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getCommonGunIndex(iGun.getGunId(gunItem)).ifPresentOrElse(gunIndex -> {
            float gunSprintTime = gunIndex.getGunData().getSprintTime();
            if (shooter.isSprinting() && !shooter.isCrouching()) {
                data.sprintTimeS += (System.currentTimeMillis() - data.sprintTimestamp) / 1000f;
                if (data.sprintTimeS > gunSprintTime) {
                    data.sprintTimeS = gunSprintTime;
                }
            } else {
                data.sprintTimeS -= (System.currentTimeMillis() - data.sprintTimestamp) / 1000f;
                if (data.sprintTimeS < 0) {
                    data.sprintTimeS = 0;
                }
            }
        }, () -> data.sprintTimeS = 0);
        data.sprintTimestamp = System.currentTimeMillis();
    }
}

