package com.tacz.guns.util;

import net.minecraft.resources.Identifier;

public final class IdHelper {
    private IdHelper() {
    }

    public static Identifier id(String raw) {
        return Identifier.parse(raw);
    }

    public static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
