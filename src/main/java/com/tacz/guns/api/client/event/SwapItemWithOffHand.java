package com.tacz.guns.api.client.event;

import com.tacz.guns.api.event.common.KubeJSGunEventPoster;
import net.minecraftforge.eventbus.api.event.MutableEvent;

/**
 * 驍・ｽｩ陞ｳ・ｶ闔・､隰撰ｽ｢闕ｳ・ｻ陷托ｽｯ隰・距鮟・惓竏ｵ諷ｮ髫暦ｽｦ陷ｿ鬘鯉ｽｯ・･闔蛟ｶ・ｻ・ｶ
 */
public class SwapItemWithOffHand extends MutableEvent implements KubeJSGunEventPoster<SwapItemWithOffHand> {
    public SwapItemWithOffHand() {
        postClientEventToKubeJS(this);
    }
}

