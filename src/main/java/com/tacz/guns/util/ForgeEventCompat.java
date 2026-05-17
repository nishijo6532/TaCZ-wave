package com.tacz.guns.util;

import net.minecraftforge.eventbus.api.event.characteristic.SelfPosting;

public final class ForgeEventCompat {
    private ForgeEventCompat() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean post(Object event) {
        if (event instanceof SelfPosting selfPosting) {
            return selfPosting.post();
        }
        return false;
    }
}
