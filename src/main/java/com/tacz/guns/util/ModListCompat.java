package com.tacz.guns.util;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ModListCompat {
    private ModListCompat() {
    }

    public static boolean isLoaded(String modId) {
        try {
            Method get = ModList.class.getMethod("get");
            Object modList = get.invoke(null);
            Method isLoaded = modList.getClass().getMethod("isLoaded", String.class);
            Object result = isLoaded.invoke(modList, modId);
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method isLoaded = ModList.class.getMethod("isLoaded", String.class);
            Object result = isLoaded.invoke(null, modId);
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static Optional<? extends ModContainer> getModContainerById(String modId) {
        try {
            Method get = ModList.class.getMethod("get");
            Object modList = get.invoke(null);
            Method getModContainerById = modList.getClass().getMethod("getModContainerById", String.class);
            Object result = getModContainerById.invoke(modList, modId);
            if (result instanceof Optional<?> optional) {
                return (Optional<? extends ModContainer>) optional;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method getModContainerById = ModList.class.getMethod("getModContainerById", String.class);
            Object result = getModContainerById.invoke(null, modId);
            if (result instanceof Optional<?> optional) {
                return (Optional<? extends ModContainer>) optional;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return Optional.empty();
    }
}
