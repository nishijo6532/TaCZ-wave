package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerMelee;
import com.tacz.guns.resource.pojo.data.attachment.MeleeData;
import com.tacz.guns.resource.pojo.data.gun.GunDefaultMeleeData;
import com.tacz.guns.util.ForgeEventCompat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nullable;

public class LocalPlayerMelee {
    public static final String MELEE_STOCK_ANIMATION = "melee_stock";
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerMelee(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void melee() {
        // 譽譟･迥ｶ諤・煤
        if (data.clientStateLock) {
            return;
        }
        // 證ょｮ壻ｸｺ荳ｻ謇・
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        GunDisplayInstance display = TimelessAPI.getGunDisplay(mainHandItem).orElse(null);
        if (display == null) {
            return;
        }
        Identifier gunId = iGun.getGunId(mainHandItem);
        // 蜈域｣譟･譫ｪ蜿｣譛画ｲ｡譛芽ｿ第・螻樊ｧ
        Identifier muzzleId = iGun.getAttachmentId(mainHandItem, AttachmentType.MUZZLE);
        MeleeData muzzleMeleeData = getMeleeData(muzzleId);
        if (muzzleMeleeData != null) {
            this.doMuzzleMelee(display);
            return;
        }

        Identifier stockId = iGun.getAttachmentId(mainHandItem, AttachmentType.STOCK);
        MeleeData stockMeleeData = getMeleeData(stockId);
        if (stockMeleeData != null) {
            this.doStockMelee(display);
            return;
        }

        TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> {
            GunDefaultMeleeData defaultMeleeData = index.getGunData().getMeleeData().getDefaultMeleeData();
            if (defaultMeleeData == null) {
                return;
            }
            String animationType = defaultMeleeData.getAnimationType();
            if (MELEE_STOCK_ANIMATION.equals(animationType)) {
                this.doStockMelee(display);
                return;
            }
            this.doPushMelee(display);
        });
    }

    private boolean prepareMelee() {
        // 髞∽ｸ顔憾諤・煤
        data.lockState(operator -> operator.getSynMeleeCoolDown() > 0);
        // 隗ｦ蜿題ｿ第・莠倶ｻｶ
        GunMeleeEvent gunMeleeEvent = new GunMeleeEvent(player, player.getMainHandItem(), LogicalSide.CLIENT);
        return !ForgeEventCompat.post(gunMeleeEvent);
    }

    private void doMuzzleMelee(GunDisplayInstance display) {
        if (prepareMelee()) {
            SoundPlayManager.playMeleeBayonetSound(player, display);
            // 蜿鷹∵鴬陦瑚ｿ第・逧・焚謐ｮ蛹・ｼ碁夂衍譛榊苅蝎ｨ
            NetworkHandler.sendToServer(new ClientMessagePlayerMelee());
            // 蜉ｨ逕ｻ迥ｶ諤∵惻霓ｬ遘ｻ迥ｶ諤・
            AnimationStateMachine<?> animationStateMachine = display.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_BAYONET_MUZZLE);
            }
        }
    }

    private void doStockMelee(GunDisplayInstance display) {
        if (prepareMelee()) {
            SoundPlayManager.playMeleeStockSound(player, display);
            // 蜿鷹∵鴬陦瑚ｿ第・逧・焚謐ｮ蛹・ｼ碁夂衍譛榊苅蝎ｨ
            NetworkHandler.sendToServer(new ClientMessagePlayerMelee());
            // 蜉ｨ逕ｻ迥ｶ諤∵惻霓ｬ遘ｻ迥ｶ諤・
            AnimationStateMachine<?> animationStateMachine = display.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_BAYONET_STOCK);
            }
        }
    }

    private void doPushMelee(GunDisplayInstance display) {
        if (prepareMelee()) {
            // 謦ｭ謾ｾ髻ｳ謨・
            SoundPlayManager.playMeleePushSound(player, display);
            // 蜿鷹∵鴬陦瑚ｿ第・逧・焚謐ｮ蛹・ｼ碁夂衍譛榊苅蝎ｨ
            NetworkHandler.sendToServer(new ClientMessagePlayerMelee());
            // 蜉ｨ逕ｻ迥ｶ諤∵惻霓ｬ遘ｻ迥ｶ諤・
            AnimationStateMachine<?> animationStateMachine = display.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_BAYONET_PUSH);
            }
        }
    }

    @Nullable
    private MeleeData getMeleeData(Identifier attachmentId) {
        if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
            return null;
        }
        return TimelessAPI.getClientAttachmentIndex(attachmentId).map(index -> index.getData().getMeleeData()).orElse(null);
    }
}

