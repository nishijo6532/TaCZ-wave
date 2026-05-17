package com.tacz.guns.util;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

public final class EnchantmentCompat {
    private static final Method EXPLOSION_DAMPENER_METHOD = findExplosionDampenerMethod();

    private EnchantmentCompat() {
    }

    public static double getExplosionKnockbackAfterDampener(LivingEntity entity, double strength) {
        if (EXPLOSION_DAMPENER_METHOD == null) {
            return strength;
        }
        try {
            Object value = EXPLOSION_DAMPENER_METHOD.invoke(null, entity, strength);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return strength;
    }

    private static Method findExplosionDampenerMethod() {
        try {
            Class<?> clazz = Class.forName("net.minecraft.world.item.enchantment.ProtectionEnchantment");
            Method method = clazz.getMethod("getExplosionKnockbackAfterDampener", LivingEntity.class, double.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
