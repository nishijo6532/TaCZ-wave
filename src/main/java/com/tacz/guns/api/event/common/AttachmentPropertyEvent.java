package com.tacz.guns.api.event.common;

import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.event.MutableEvent;

/**
 * 驛帷§・ｭ蛟ｬ繝ｻ闔会ｽｶ陞ｻ讓環・ｧ闖ｫ・ｮ隰ｾ・ｹ陋滂ｽｼ隴鯉ｽｶ髫暦ｽｦ陷ｿ驢榊飭闔蛟ｶ・ｻ・ｶ
 * <p>
 * 陞ｯ繧域｣｡隴帷甥繝ｻ闔牙戟・ｨ・｡謇医・ﾎｦ髫補扱・ｷ・ｻ陷会｣ｰ髢ｾ・ｪ陞ｳ螢ｻ・ｹ閾･蝎ｪ鬩溷ｺ・ｻ・ｶ陞ｻ讓環・ｧ闖ｫ・ｮ隰ｾ・ｹ陋滂ｽｼ繝ｻ謔溷ｺ・脂・･隰先・蝓ｷ雎・ｽ､闔蛟ｶ・ｻ・ｶ
 */
public class AttachmentPropertyEvent extends MutableEvent implements KubeJSGunEventPoster<AttachmentPropertyEvent> {
    private final ItemStack gunItem;
    private final AttachmentCacheProperty cacheProperty;

    public AttachmentPropertyEvent(ItemStack gunItem, AttachmentCacheProperty attachmentProperty) {
        this.gunItem = gunItem;
        this.cacheProperty = attachmentProperty;
    }

    public ItemStack getGunItem() {
        return gunItem;
    }

    public AttachmentCacheProperty getCacheProperty() {
        return cacheProperty;
    }
}

