package com.tacz.guns.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class NbtCompat {
    public static final int TAG_ANY_NUMERIC = 99;

    private NbtCompat() {
    }

    @Nullable
    public static CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return null;
        }
        return customData.copyTag();
    }

    public static boolean hasTag(ItemStack stack) {
        return getTag(stack) != null;
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag == null) {
            return new CompoundTag();
        }
        return tag;
    }

    public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            return;
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void updateTag(ItemStack stack, Consumer<CompoundTag> updater) {
        CompoundTag tag = getOrCreateTag(stack);
        updater.accept(tag);
        setTag(stack, tag);
    }

    public static boolean contains(@Nullable CompoundTag tag, String key, int type) {
        if (tag == null) {
            return false;
        }
        Tag value = tag.get(key);
        if (value == null) {
            return false;
        }
        if (type == TAG_ANY_NUMERIC) {
            byte id = value.getId();
            return id >= Tag.TAG_BYTE && id <= Tag.TAG_DOUBLE;
        }
        return value.getId() == type;
    }

    public static int getInt(@Nullable CompoundTag tag, String key) {
        if (tag == null) {
            return 0;
        }
        return tag.getIntOr(key, 0);
    }

    public static float getFloat(@Nullable CompoundTag tag, String key) {
        if (tag == null) {
            return 0f;
        }
        return tag.getFloatOr(key, 0f);
    }

    public static double getDouble(@Nullable CompoundTag tag, String key) {
        if (tag == null) {
            return 0d;
        }
        return tag.getDoubleOr(key, 0d);
    }

    public static long getLong(@Nullable CompoundTag tag, String key) {
        if (tag == null) {
            return 0L;
        }
        return tag.getLongOr(key, 0L);
    }

    public static boolean getBoolean(@Nullable CompoundTag tag, String key) {
        if (tag == null) {
            return false;
        }
        return tag.getBoolean(key).orElse(false);
    }

    public static String getString(@Nullable CompoundTag tag, String key) {
        if (tag == null) {
            return "";
        }
        return tag.getStringOr(key, "");
    }

    public static CompoundTag getCompoundOrEmpty(@Nullable CompoundTag tag, String key) {
        if (tag == null) {
            return new CompoundTag();
        }
        return tag.getCompoundOrEmpty(key);
    }

    @Nullable
    public static CompoundTag getCompound(@Nullable CompoundTag tag, String key) {
        if (tag == null) {
            return null;
        }
        return tag.getCompound(key).orElse(null);
    }

    public static int[] getIntArrayOrEmpty(@Nullable CompoundTag tag, String key) {
        if (tag == null) {
            return new int[0];
        }
        return tag.getIntArray(key).orElse(new int[0]);
    }

    public static CompoundTag serializeItemStack(ItemStack stack) {
        var encoded = ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElseGet(CompoundTag::new);
        if (encoded instanceof CompoundTag compoundTag) {
            return compoundTag;
        }
        return new CompoundTag();
    }

    public static ItemStack deserializeItemStack(@Nullable CompoundTag tag) {
        if (tag == null) {
            return ItemStack.EMPTY;
        }
        return ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(ItemStack.EMPTY);
    }
}
