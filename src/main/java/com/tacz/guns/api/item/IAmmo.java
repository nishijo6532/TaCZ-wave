package com.tacz.guns.api.item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface IAmmo {
    /**
     * @return 螯よ棡迚ｩ蜩∫ｱｻ蝙倶ｸｺ IAttachment 蛻呵ｿ泌屓譏ｾ蠑剰ｽｬ謐｢蜷守噪螳樔ｾ具ｼ悟凄蛻呵ｿ泌屓 null縲・
     */
    @Nullable
    static IAmmo getIAmmoOrNull(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.getItem() instanceof IAmmo iAmmo) {
            return iAmmo;
        }
        return null;
    }

    /**
     * 闔ｷ蜿門ｼｹ闕ｯ ID
     *
     * @param ammo 霎灘・迚ｩ蜩・
     * @return 蠑ｹ闕ｯ ID
     */
    Identifier getAmmoId(ItemStack ammo);

    /**
     * 隶ｾ鄂ｮ蠑ｹ闕ｯ ID
     */
    void setAmmoId(ItemStack ammo, @Nullable Identifier ammoId);

    /**
     * 蠑ｹ闕ｯ譏ｯ蜷ｦ螻樔ｺ手ｿ呎滑譫ｪ
     *
     * @param gun  譽譟･逧・棯譴ｰ迚ｩ蜩・
     * @param ammo 譽譟･逧・ｭ仙ｼｹ迚ｩ蜩・
     * @return 譏ｯ蜷ｦ螻樔ｺ手ｿ呎滑譫ｪ
     */
    boolean isAmmoOfGun(ItemStack gun, ItemStack ammo);
}

