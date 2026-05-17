package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerCancelReload;
import com.tacz.guns.network.message.ClientMessagePlayerReloadGun;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.util.ForgeEventCompat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;

public class LocalPlayerReload {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerReload(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void cancelReload() {
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof AbstractGunItem)) {
            return;
        }

        TimelessAPI.getGunDisplay(mainHandItem).ifPresent(display -> {
            // 螯よ棡豐｡蝨ｨ謐｢蠑ｹ・悟・霑泌屓
            IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
            ReloadState reloadState = gunOperator.getSynReloadState();
            if (!reloadState.getStateType().isReloading()) {
                return;
            }
            // 蜿大桁騾夂衍譛榊苅蝎ｨ
            NetworkHandler.sendToServer(new ClientMessagePlayerCancelReload());
            // 謇ｧ陦梧悽蝨ｰ蜿匁ｶ域困蠑ｹ騾ｻ霎・
            this.cancelReload(display);
        });
    }

    public void reload() {
        // 證ょｮ壼宵譛我ｸｻ謇句庄莉･陬・ｼｹ
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof AbstractGunItem gunItem)) {
            return;
        }
        Identifier gunId = gunItem.getGunId(mainHandItem);
        GunData gunData = TimelessAPI.getClientGunIndex(gunId).map(ClientGunIndex::getGunData).orElse(null);
        if (gunData == null) {
            return;
        }
        TimelessAPI.getGunDisplay(mainHandItem).ifPresent(display -> {
            // 譽譟･譏ｯ蜷ｦ荳ｺ閭悟桁逶ｴ隸ｻ
            if (gunItem.useInventoryAmmo(mainHandItem)) {
                return;
            }
            // 譽譟･迥ｶ諤・煤
            if (data.clientStateLock) {
                return;
            }
            // 蠑ｹ闕ｯ邂蜊墓｣譟･
            boolean canReload = gunItem.canReload(player, mainHandItem);
            if (gunItem.getCurrentAmmoCount(mainHandItem) >= AttachmentDataUtils.getAmmoCountWithAttachment(mainHandItem, gunData)) {
                return;
            }
            if (IGunOperator.fromLivingEntity(player).needCheckAmmo() && !canReload) {
                return;
            }
            // 髞∽ｸ顔憾諤・煤
            data.lockState(operator -> operator.getSynReloadState().getStateType().isReloading());
            // 隗ｦ蜿第困蠑ｹ莠倶ｻｶ
            if (ForgeEventCompat.post(new GunReloadEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                return;
            }
            // 蜿大桁騾夂衍譛榊苅蝎ｨ
            NetworkHandler.sendToServer(new ClientMessagePlayerReloadGun());
            // 謇ｧ陦悟ｮ｢謌ｷ遶ｯ reload 逶ｸ蜈ｳ蜀・ｮｹ
            this.doReload(gunItem, display, gunData, mainHandItem);
        });
    }

    private void doReload(IGun iGun, GunDisplayInstance display, GunData gunData, ItemStack mainHandItem) {
        var animationStateMachine = display.getAnimationStateMachine();
        if (animationStateMachine != null) {
            Bolt boltType = gunData.getBolt();
            boolean noAmmo;
            if (boltType == Bolt.OPEN_BOLT) {
                noAmmo = iGun.getCurrentAmmoCount(mainHandItem) <= 0;
            } else {
                noAmmo = !iGun.hasBulletInBarrel(mainHandItem);
            }
            // 隗ｦ蜿・reload・悟●豁｢謦ｭ謾ｾ螢ｰ髻ｳ
            SoundPlayManager.stopPlayGunSound();
            SoundPlayManager.playReloadSound(player, display, noAmmo);
            animationStateMachine.trigger(GunAnimationConstant.INPUT_RELOAD);
        }
    }

    private void cancelReload(GunDisplayInstance display) {
        var animationStateMachine = display.getAnimationStateMachine();
        if (animationStateMachine != null) {
            animationStateMachine.trigger(GunAnimationConstant.INPUT_CANCEL_RELOAD);
        }
    }
}

