package com.tacz.guns.api.event.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;

/**
 * 鬯ｯ・ｨ繝ｻ・ｾ髯溘・螻ｮ繝ｻ・ｽ繝ｻ・ｺ郢晢ｽｻ繝ｻ・ｽ鬯ｲ繝ｻ・ｺ蛟･繝ｻ髮朱ｯ会ｽｽ・｣鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ鬯ｮ・ｫ繝ｻ・ｴ鬯ｲ繝ｻ・ｼ螟ｲ・ｽ・ｽ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｶ鬯ｯ・ｮ繝ｻ・ｫ髫ｴ莨夲ｽｽ・ｦ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｦ鬯ｮ・ｯ繝ｻ・ｷ郢晢ｽｻ繝ｻ・ｿ鬯ｯ・ｩ繝ｻ・｢髫ｶ諠ｹ・ｼ繝ｻ・ｽ・｣繝ｻ・ｭ鬯ｮ・｣髮具ｽｻ繝ｻ・｣繝ｻ・ｰ鬮ｯ蛹ｺ・ｻ繧托ｽｽ・ｽ繝ｻ・ｶ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｻ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｶ鬯ｩ謳ｾ・ｽ・ｵ郢晢ｽｻ繝ｻ・ｲ鬩幢ｽ｢繝ｻ・ｧ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｸ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ{@link GunFireEvent}鬯ｮ・｣陋ｹ繝ｻ・ｽ・ｽ繝ｻ・ｳ鬮ｫ・ｶ隲・ｹ繝ｻ・ｼ遶丞｣ｹ繝ｻ鬯ｯ・ｨ繝ｻ・ｾ郢晢ｽｻ繝ｻ・ｧ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ鬮ｯ貊・ｽｷ・ｹ闔繧会ｽｹ譎｢・ｽ・ｻ鬮ｫ・ｴ繝ｻ・ｴ郢晢ｽｻ繝ｻ・ｧ鬮ｫ・ｹ繝ｻ・ｿ郢晢ｽｻ繝ｻ・ｴ鬯ｮ・ｯ繝ｻ・ｷ髣費ｽｨ陞滂ｽｲ繝ｻ・ｽ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｨ鬯ｮ・｣陋ｹ繝ｻ・ｽ・ｽ繝ｻ・ｳ郢晢ｽｻ邵ｺ・､・つ鬯ｮ・ｫ繝ｻ・ｹ郢晢ｽｻ繝ｻ・ｺ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・｡鬯ｮ・ｫ繝ｻ・ｰ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｳ鬯ｮ・ｫ繝ｻ・ｴ髯晢ｽｷ繝ｻ・｢郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｺ鬯ｮ・ｯ繝ｻ・ｷ郢晢ｽｻ繝ｻ・ｿ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｪ鬯ｮ・｣髮具ｽｻ繝ｻ・ｽ繝ｻ・ｨ鬮ｯ讓奇ｽｺ・ｷ繝ｻ・･郢晢ｽｻ繝ｻ・ｽ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｧ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｦ鬯ｮ・ｯ繝ｻ・ｷ郢晢ｽｻ繝ｻ・ｿ鬮ｯ・ｷ繝ｻ・ｿ郢晢ｽｻ繝ｻ・ｰ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｸ郢晢ｽｻ邵ｺ・､・つ鬯ｮ・ｫ繝ｻ・ｹ郢晢ｽｻ繝ｻ・ｺ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・｡鬯ｯ・ｮ繝ｻ・ｴ髯樊ｻゑｽｽ・ｧ髯晉事萓ｭ郢晢ｽｻ郢晢ｽｻ繝ｻ・ｸ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｪ鬯ｮ・｣髮具ｽｻ繝ｻ・｣繝ｻ・ｰ鬮ｯ蛹ｺ・ｻ繧托ｽｽ・ｽ繝ｻ・ｶ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｻ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｶ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ鬮ｯ貅ｷ譯√・・ｽ繝ｻ・｡驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｽ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ鬮ｯ貅ｷ・ｼ・ｱ郢晢ｽｻ髯橸ｽｯ陷ｷ・ｶ郢晢ｽｻ郢晢ｽｻ繝ｻ・ｽ鬯ｮ・ｯ隶灘･・ｽｽ・ｺ繝ｻ・ｷ郢晢ｽｻ繝ｻ・｣郢晢ｽｻ繝ｻ・ｽ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｬ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・｡鬯ｯ・ｮ繝ｻ・ｫ髫ｴ莨夲ｽｽ・ｦ郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・ｦ鬯ｮ・ｯ繝ｻ・ｷ郢晢ｽｻ繝ｻ・ｿ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ{@link GunFireEvent}鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ鬮｣雋ｻ・｣・ｰ郢晢ｽｻ繝ｻ・･驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｦ鬩幢ｽ｢繝ｻ・ｧ髯懈瑳・ｻ繧托ｽｽ・ｽ繝ｻ・｣郢晢ｽｻ繝ｻ・ｯ鬯ｮ・ｫ繝ｻ・ｴ郢晢ｽｻ繝ｻ・ｴ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｰ鬯ｮ・ｯ隶灘･・ｽｽ・ｺ陋滂ｽ･郢晢ｽｻ驛｢譎｢・ｽ・ｻ郢晢ｽｻ繝ｻ・ｺ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻBurst 鬯ｮ・ｫ繝ｻ・ｶ鬯ｮ・ｮ繝ｻ・｣郢晢ｽｻ繝ｻ・ｽ郢晢ｽｻ繝ｻ・｡鬯ｮ・ｯ雋・ｽｽ繝ｻ・ｬ繝ｻ・ｬ髯溷供蟷ｲ郢晢ｽｻ郢晢ｽｻ繝ｻ・ｼ鬩幢ｽ｢隴趣ｽ｢繝ｻ・ｽ繝ｻ・ｻ
 */
public class GunShootEvent extends MutableEvent implements KubeJSGunEventPoster<GunShootEvent>, Cancellable {
    private final LivingEntity shooter;
    private final ItemStack gunItemStack;
    private final LogicalSide logicalSide;

    public GunShootEvent(LivingEntity shooter, ItemStack gunItemStack, LogicalSide side) {
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

