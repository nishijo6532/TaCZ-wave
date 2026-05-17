package com.tacz.guns.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeyMappingCompat {
    private static final int DEFAULT_SORT_ORDER = 0;
    private static final Map<String, KeyMapping.Category> CATEGORIES = new ConcurrentHashMap<>();

    private KeyMappingCompat() {
    }

    public static KeyMapping create(String name, IKeyConflictContext conflictContext, KeyModifier keyModifier,
                                    InputConstants.Type inputType, int keyCode, String categoryKey) {
        InputConstants.Key key = inputType.getOrCreate(keyCode);
        for (Constructor<?> constructor : KeyMapping.class.getConstructors()) {
            Object[] args = buildConstructorArgs(constructor.getParameterTypes(), name, conflictContext, keyModifier,
                    inputType, keyCode, key, categoryKey);
            if (args == null) {
                continue;
            }
            try {
                return (KeyMapping) constructor.newInstance(args);
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            }
        }
        throw new IllegalStateException("No compatible KeyMapping constructor found");
    }

    public static boolean matches(KeyMapping mapping, InputEvent.Key event) {
        Object result = invokeBooleanMethod(mapping, "matches", event.getInfo(), event, event.getKey(), event.getScanCode());
        return result instanceof Boolean value && value;
    }

    public static boolean matchesMouse(KeyMapping mapping, InputEvent.MouseButton.Post event) {
        Object mouseButtonEvent = createMouseButtonEventCandidate(event);
        Object result = invokeBooleanMethod(mapping, "matchesMouse", mouseButtonEvent, event.getInfo(), event, event.getButton());
        return result instanceof Boolean value && value;
    }

    private static Object invokeBooleanMethod(KeyMapping mapping, String name, Object... candidates) {
        for (Method method : KeyMapping.class.getMethods()) {
            if (!method.getName().equals(name)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1) {
                for (Object candidate : candidates) {
                    if (candidate == null) {
                        continue;
                    }
                    if (parameterTypes[0].isAssignableFrom(candidate.getClass())
                            || (parameterTypes[0] == int.class && candidate instanceof Integer)) {
                        try {
                            return method.invoke(mapping, candidate);
                        } catch (ReflectiveOperationException ignored) {
                        }
                    }
                }
                continue;
            }
            if (parameterTypes.length == 2 && parameterTypes[0] == int.class && parameterTypes[1] == int.class) {
                Integer first = null;
                Integer second = null;
                for (Object candidate : candidates) {
                    if (candidate instanceof Integer value) {
                        if (first == null) {
                            first = value;
                        } else {
                            second = value;
                            break;
                        }
                    }
                }
                if (first != null && second != null) {
                    try {
                        return method.invoke(mapping, first, second);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
                continue;
            }
        }
        return Boolean.FALSE;
    }

    private static Object createMouseButtonEventCandidate(InputEvent.MouseButton.Post event) {
        try {
            Class<?> mouseButtonInfoClass = Class.forName("net.minecraft.client.input.MouseButtonInfo");
            Object info = event.getInfo();
            if (info == null || !mouseButtonInfoClass.isInstance(info)) {
                return null;
            }
            Class<?> mouseButtonEventClass = Class.forName("net.minecraft.client.input.MouseButtonEvent");
            Constructor<?> constructor = mouseButtonEventClass.getConstructor(double.class, double.class, mouseButtonInfoClass);
            return constructor.newInstance(0d, 0d, info);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object[] buildConstructorArgs(Class<?>[] parameterTypes, String name, IKeyConflictContext conflictContext,
                                                 KeyModifier keyModifier, InputConstants.Type inputType, int keyCode,
                                                 InputConstants.Key key, String categoryKey) {
        if (parameterTypes.length < 5 || parameterTypes[0] != String.class) {
            return null;
        }
        if (!parameterTypes[1].isAssignableFrom(conflictContext.getClass())) {
            return null;
        }
        Object[] args = new Object[parameterTypes.length];
        args[0] = name;
        args[1] = conflictContext;

        if (parameterTypes.length == 6 && parameterTypes[2] == InputConstants.Type.class && parameterTypes[3] == int.class) {
            args[2] = inputType;
            args[3] = keyCode;
            args[4] = resolveCategory(parameterTypes[4], categoryKey);
            args[5] = parameterTypes[5] == int.class ? DEFAULT_SORT_ORDER : null;
            return args;
        }
        if (parameterTypes.length == 6 && parameterTypes[2].isAssignableFrom(keyModifier.getClass())) {
            args[2] = keyModifier;
            args[3] = resolveKey(parameterTypes[3], inputType, keyCode, key);
            args[4] = resolveCategory(parameterTypes[4], categoryKey);
            args[5] = parameterTypes[5] == int.class ? DEFAULT_SORT_ORDER : null;
            return args;
        }
        if (parameterTypes.length == 5 && parameterTypes[2] == InputConstants.Type.class && parameterTypes[3] == int.class) {
            args[2] = inputType;
            args[3] = keyCode;
            args[4] = resolveCategory(parameterTypes[4], categoryKey);
            return args;
        }
        if (parameterTypes.length == 5 && parameterTypes[2].isAssignableFrom(keyModifier.getClass())) {
            args[2] = keyModifier;
            args[3] = resolveKey(parameterTypes[3], inputType, keyCode, key);
            args[4] = resolveCategory(parameterTypes[4], categoryKey);
            return args;
        }
        return null;
    }

    private static Object resolveKey(Class<?> targetType, InputConstants.Type inputType, int keyCode, InputConstants.Key key) {
        if (targetType == InputConstants.Type.class) {
            return inputType;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return keyCode;
        }
        if (targetType.isInstance(key)) {
            return key;
        }
        return null;
    }

    private static Object resolveCategory(Class<?> targetType, String categoryKey) {
        if (targetType == String.class) {
            return categoryKey;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return DEFAULT_SORT_ORDER;
        }
        if (targetType == KeyMapping.Category.class) {
            return CATEGORIES.computeIfAbsent(categoryKey, KeyMappingCompat::registerCategory);
        }
        Object fromFactory = invokeFactory(targetType, categoryKey, targetType);
        if (fromFactory != null) {
            return fromFactory;
        }
        Object fromTypeFactory = invokeFactory(targetType, categoryKey, KeyMapping.class);
        if (fromTypeFactory != null) {
            return fromTypeFactory;
        }
        try {
            Constructor<?> constructor = targetType.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(categoryKey);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static KeyMapping.Category registerCategory(String categoryKey) {
        String path = categoryKey.startsWith("key.category.")
                ? categoryKey.substring("key.category.".length())
                : categoryKey;
        Identifier id = path.indexOf(':') >= 0
                ? Identifier.parse(path)
                : Identifier.fromNamespaceAndPath("tacz", path.replace('.', '_'));
        try {
            return KeyMapping.Category.register(id);
        } catch (IllegalArgumentException duplicate) {
            return new KeyMapping.Category(id);
        }
    }

    private static Object invokeFactory(Class<?> returnType, String categoryKey, Class<?> owner) {
        for (Method method : owner.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != String.class) {
                continue;
            }
            if (!returnType.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            try {
                return method.invoke(null, categoryKey);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }
}
