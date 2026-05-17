package com.tacz.guns.api.event.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;

/**
 * 鬯ｯ・ｨ繝ｻ・ｾ髯溘・螻ｮ繝ｻ・ｽ繝ｻ・ｺ郢晢ｽｻ繝ｻ・ｽ鬯ｲ繝ｻ・ｺ蛟･繝ｻ髯晢｣ｰ隲ｷ蛹・ｽｽ・ｹ隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ鬮ｯ諛亥惧繝ｻ・ｽ繝ｻ・ｰ鬯ｮ・ｫ繝ｻ・ｴ郢晢ｽｻ繝ｻ・ｫ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｪ鬯ｮ・ｫ繝ｻ・ｴ郢晢ｽｻ繝ｻ・ｴ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｰ鬯ｮ・ｯ雋・ｽｷ繝ｻ・ｰ鬮｢蜿ｯﾂ鬯ｮ・ｴ鬮ｮ・｣繝ｻ・ｽ繝ｻ・｣驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｫ鬯ｮ・ｫ繝ｻ・ｶ鬯ｮ・ｮ繝ｻ・｣郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・｡鬯ｮ・ｯ雋・ｽｷ陟守ｿｫ繝ｻ繝ｻ・ｹ鬩阪・・ｽ・ｲ驛｢譎｢・ｽ・ｻ鬯ｯ・ｮ繝ｻ・ｫ髫ｴ莨夲ｽｽ・ｦ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｦ鬯ｮ・ｯ繝ｻ・ｷ郢晢ｽｻ繝ｻ・ｿ鬯ｯ・ｩ繝ｻ・｢髫ｶ諠ｹ・ｼ繝ｻ・ｽ・｣繝ｻ・ｭ鬯ｮ・｣髮具ｽｻ繝ｻ・｣繝ｻ・ｰ鬮ｯ蛹ｺ・ｻ繧托ｽｽ・ｽ繝ｻ・ｶ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｻ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｶ
 */
public class GunFireSelectEvent extends MutableEvent implements KubeJSGunEventPoster<GunFireSelectEvent>, Cancellable {
    private final LivingEntity shooter;
    private final ItemStack gunItemStack;
    private final LogicalSide logicalSide;

    public GunFireSelectEvent(LivingEntity shooter, ItemStack gunItemStack, LogicalSide side) {
        this.shooter = shooter;
        this.gunItemStack = gunItemStack;
        this.logicalSide = side;
        postEventToKubeJS(this);
    }
    public LivingEntity getShooter() {
        return shooter;
    }

    public ItemStack getGunItemStack() {
        return gunItemStack;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }
}

