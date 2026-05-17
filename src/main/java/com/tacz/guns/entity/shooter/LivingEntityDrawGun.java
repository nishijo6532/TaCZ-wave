package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunDrawEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunDraw;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.util.ForgeEventCompat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;
import java.util.function.Supplier;

public class LivingEntityDrawGun {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;

    public LivingEntityDrawGun(LivingEntity shooter, ShooterDataHolder data) {
        this.shooter = shooter;
        this.data = data;
    }

    public void draw(Supplier<ItemStack> gunItemSupplier) {
        // 驥咲ｽｮ蜷・ｸｪ迥ｶ諤・
        data.initialData();

        // 譖ｴ譁ｰ蛻・棯譌ｶ髣ｴ謌ｳ
        if (data.drawTimestamp == -1) {
            data.drawTimestamp = System.currentTimeMillis();
        }
        if (data.heatTimestamp == -1) {
            data.heatTimestamp = System.currentTimeMillis();
        }
        long drawTime = System.currentTimeMillis() - data.drawTimestamp;
        if (drawTime >= 0) {
            // 螯よ棡荳榊､・ｺ取噺譫ｪ迥ｶ諤・ｼ悟・髴隕∬ｮ｡邂玲噺譫ｪ譌ｶ髟ｿ
            if (drawTime < data.currentPutAwayTimeS * 1000) {
                // 莉主ｼ蟋句・譫ｪ蛻ｰ邇ｰ蝨ｨ・梧堪譫ｪ逧・慮髣ｴ蟆丈ｺ取噺譫ｪ髴隕∫噪譌ｶ髣ｴ・悟・謖画堪譫ｪ譌ｶ髣ｴ隶｡邂励・
                data.drawTimestamp = System.currentTimeMillis() + drawTime;
            } else {
                // 莉主ｼ蟋句・譫ｪ蛻ｰ邇ｰ蝨ｨ・梧堪譫ｪ逧・慮髣ｴ螟ｧ莠取噺譫ｪ髴隕∫噪譌ｶ髣ｴ・悟・謖画噺譫ｪ譌ｶ髣ｴ隶｡邂励・
                data.drawTimestamp = System.currentTimeMillis() + (long) (data.currentPutAwayTimeS * 1000);
            }
        }
        ItemStack lastItem = data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
        ForgeEventCompat.post(new GunDrawEvent(shooter, lastItem, gunItemSupplier.get(), LogicalSide.SERVER));
        NetworkHandler.sendToTrackingEntity(new ServerMessageGunDraw(shooter.getId(), lastItem, gunItemSupplier.get()), shooter);
        data.currentGunItem = gunItemSupplier;
        // 蛻ｷ譁ｰ驟堺ｻｶ謨ｰ謐ｮ
        AttachmentPropertyManager.postChangeEvent(shooter, gunItemSupplier.get());
        updatePutAwayTime();
    }

    public long getDrawCoolDown() {
        if (data.currentGunItem == null) {
            return 0;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return 0;
        }
        Identifier gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        return gunIndex.map(index -> {
            long coolDown = (long) (index.getGunData().getDrawTime() * 1000) - (System.currentTimeMillis() - data.drawTimestamp);
            // 扈・5 ms 逧・ｪ怜哨譌ｶ髣ｴ・御ｻ･蟷ｳ陦｡蟒ｶ霑・
            coolDown = coolDown - 5;
            if (coolDown < 0) {
                return 0L;
            }
            return coolDown;
        }).orElse(-1L);
    }

    private void updatePutAwayTime() {
        ItemStack gunItem = data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun != null) {
            Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(iGun.getGunId(gunItem));
            data.currentPutAwayTimeS = gunIndex.map(index -> index.getGunData().getPutAwayTime()).orElse(0F);
        } else {
            data.currentPutAwayTimeS = 0;
        }
    }
}

