package com.tacz.guns.item;

import com.tacz.guns.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public enum GunTooltipPart {
    DESCRIPTION,
    AMMO_INFO,
    BASE_INFO,
    EXTRA_DAMAGE_INFO,
    UPGRADES_TIP,
    PACK_INFO;

    private final int mask = 1 << this.ordinal();

    public int getMask() {
        return this.mask;
    }

    public static int getHideFlags(ItemStack stack) {
        CompoundTag tag = NbtCompat.getTag(stack);
        if (NbtCompat.contains(tag, "HideFlags", NbtCompat.TAG_ANY_NUMERIC)) {
            return NbtCompat.getInt(tag, "HideFlags");
        }
        return stack.getItem().getDefaultTooltipHideFlags(stack);
    }

    public static void setHideFlags(ItemStack stack, int mask) {
        NbtCompat.updateTag(stack, tag -> tag.putInt("HideFlags", mask));
    }
}
