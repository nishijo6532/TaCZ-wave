package com.tacz.guns.api.event.common;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;

/**
 * 鬯ｯ・ｨ繝ｻ・ｾ髯溘・螻ｮ繝ｻ・ｽ繝ｻ・ｺ郢晢ｽｻ繝ｻ・ｽ鬯ｲ繝ｻ・ｺ蛟･繝ｻ髮取腸・ｽ・ｿ鬮ｫ・ｰ隰ｦ・ｰ繝ｻ・ｽ繝ｻ・ｺ鬮ｫ・ｰ繝ｻ・ｫ髯樊ｺｷ・･繝ｻ・ｽ・ｽ繝ｻ・ｭ髯ｷ・ｴ郢晢ｽｻ繝ｻ・ｽ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｴ鬯ｮ・ｫ繝ｻ・ｰ髫ｰ・ｦ繝ｻ・ｰ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・｢鬯ｮ・ｫ繝ｻ・ｴ郢晢ｽｻ繝ｻ・ｫ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｪ鬯ｮ・ｫ繝ｻ・ｴ郢晢ｽｻ繝ｻ・ｴ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｰ鬯ｮ・ｯ雋頑瑳・ｱ螢ｹ繝ｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｹ鬯ｯ・ｮ繝ｻ・｣髯具ｽｹ郢晢ｽｻ繝ｻ・ｽ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｯ鬯ｮ・ｫ繝ｻ・ｴ鬯ｲ繝ｻ・ｼ螟ｲ・ｽ・ｽ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｶ鬯ｯ・ｮ繝ｻ・ｫ髫ｴ莨夲ｽｽ・ｦ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｦ鬯ｮ・ｯ繝ｻ・ｷ郢晢ｽｻ繝ｻ・ｿ鬯ｯ・ｩ繝ｻ・｢髫ｶ諠ｹ・ｼ繝ｻ・ｽ・｣繝ｻ・ｭ鬯ｮ・｣髮具ｽｻ繝ｻ・｣繝ｻ・ｰ鬮ｯ蛹ｺ・ｻ繧托ｽｽ・ｽ繝ｻ・ｶ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｻ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｶ鬯ｩ謳ｾ・ｽ・ｵ郢晢ｽｻ繝ｻ・ｲ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ
 */
public class GunFinishReloadEvent extends MutableEvent implements KubeJSGunEventPoster<GunFinishReloadEvent>, Cancellable {
    private final ItemStack gunItemStack;
    private final LogicalSide logicalSide;

    public GunFinishReloadEvent(ItemStack gunItemStack, LogicalSide side) {
        this.gunItemStack = gunItemStack;
        this.logicalSide = side;
        postEventToKubeJS(this);
    }
    public ItemStack getGunItemStack() {
        return gunItemStack;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }
}

