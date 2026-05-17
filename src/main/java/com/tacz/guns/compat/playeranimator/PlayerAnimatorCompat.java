package com.tacz.guns.compat.playeranimator;

import com.tacz.guns.client.resource.GunDisplayInstance;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;

public final class PlayerAnimatorCompat {
    private PlayerAnimatorCompat() {
    }

    public static void init() {
    }

    public static boolean isInstalled() {
        return false;
    }

    public static void registerReloadListener(Consumer<PreparableReloadListener> register) {
    }

    public static void stopAllAnimation(LivingEntity entity) {
    }

    public static void stopAllAnimation(LivingEntity entity, int blendTime) {
    }

    public static boolean hasPlayerAnimator3rd(LivingEntity entity, GunDisplayInstance display) {
        return false;
    }

    public static void playAnimation(LivingEntity entity, GunDisplayInstance display, float limbSwingAmount) {
    }
}
