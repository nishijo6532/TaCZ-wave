package com.tacz.guns.api.event.common;

import net.minecraftforge.eventbus.api.event.MutableEvent;

public interface KubeJSGunEventPoster<E extends MutableEvent> {
    default void postEventToKubeJS(E event) {
        // KubeJS integration is optional in Forge64 migration builds.
    }

    default void postClientEventToKubeJS(E event) {
        // KubeJS integration is optional in Forge64 migration builds.
    }

    default void postServerEventToKubeJS(E event) {
        // KubeJS integration is optional in Forge64 migration builds.
    }
}
