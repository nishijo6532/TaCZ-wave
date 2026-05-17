package com.tacz.guns.api.item.nbt;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.NbtCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public interface AmmoItemDataAccessor extends IAmmo {
    String AMMO_ID_TAG = "AmmoId";

    @Override
    @Nonnull
    default Identifier getAmmoId(ItemStack ammo) {
        CompoundTag nbt = NbtCompat.getOrCreateTag(ammo);
        if (NbtCompat.contains(nbt, AMMO_ID_TAG, Tag.TAG_STRING)) {
            Identifier ammoId = Identifier.tryParse(NbtCompat.getString(nbt, AMMO_ID_TAG));
            return Objects.requireNonNullElse(ammoId, DefaultAssets.EMPTY_AMMO_ID);
        }
        return DefaultAssets.EMPTY_AMMO_ID;
    }

    @Override
    default void setAmmoId(ItemStack ammo, @Nullable Identifier ammoId) {
        CompoundTag nbt = NbtCompat.getOrCreateTag(ammo);
        Identifier resolvedAmmoId;
        if (ammoId != null) {
            resolvedAmmoId = ammoId;
        } else {
            resolvedAmmoId = DefaultAssets.DEFAULT_AMMO_ID;
        }
        nbt.putString(AMMO_ID_TAG, resolvedAmmoId.toString());
        NbtCompat.setTag(ammo, nbt);
        TimelessAPI.getCommonAmmoIndex(resolvedAmmoId)
                .ifPresent(index -> ammo.set(DataComponents.MAX_STACK_SIZE, index.getStackSize()));
    }

    @Override
    default boolean isAmmoOfGun(ItemStack gun, ItemStack ammo) {
        if (gun.getItem() instanceof IGun iGun && ammo.getItem() instanceof IAmmo iAmmo) {
            Identifier gunId = iGun.getGunId(gun);
            Identifier ammoId = iAmmo.getAmmoId(ammo);
            return TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> gunIndex.getGunData().getAmmoId().equals(ammoId)).orElse(false);
        }
        return false;
    }
}
