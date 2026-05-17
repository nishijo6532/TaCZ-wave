package com.tacz.guns.api.item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * 蟄仙ｼｹ逶呈磁蜿｣
 */
public interface IAmmoBox {
    /**
     * 闔ｷ蜿門ｭ仙ｼｹ逶剃ｸｭ逧・ｭ仙ｼｹ ID
     *
     * @param ammoBox 蟄仙ｼｹ逶・
     * @return 蟄仙ｼｹ逶剃ｸｭ逧・ｭ仙ｼｹ ID
     */
    Identifier getAmmoId(ItemStack ammoBox);

    /**
     * 闔ｷ蜿門ｭ仙ｼｹ逶剃ｸｭ逧・ｭ仙ｼｹ謨ｰ驥・
     *
     * @param ammoBox 蟄仙ｼｹ逶・
     * @return 蟄仙ｼｹ謨ｰ驥・
     */
    int getAmmoCount(ItemStack ammoBox);

    /**
     * 隶ｾ鄂ｮ蟄仙ｼｹ逶剃ｸｭ蟄仙ｼｹ逧・ID
     */
    void setAmmoId(ItemStack ammoBox, Identifier ammoId);

    /**
     * 隶ｾ鄂ｮ蟄仙ｼｹ逶剃ｸｭ蟄仙ｼｹ謨ｰ驥・
     */
    void setAmmoCount(ItemStack ammoBox, int count);

    /**
     * 蟄仙ｼｹ逶剃ｸｭ逧・ｭ仙ｼｹ譏ｯ蜷ｦ螻樔ｺ手ｿ呎滑譫ｪ
     *
     * @param gun     譫ｪ
     * @param ammoBox 蟄仙ｼｹ逶・
     * @return 譏ｯ蜷ｦ螻樔ｺ手ｿ呎滑譫ｪ
     */
    boolean isAmmoBoxOfGun(ItemStack gun, ItemStack ammoBox);

    /**
     * 隶ｾ鄂ｮ蟄仙ｼｹ逶堤噪遲臥ｺｧ・・
     *
     * @param ammoBox   蟄仙ｼｹ逶・
     * @param ammoLevel 蟄仙ｼｹ逶堤ｭ臥ｺｧ
     * @return 菫ｮ謾ｹ蜷守噪蟄仙ｼｹ逶・
     */
    ItemStack setAmmoLevel(ItemStack ammoBox, int ammoLevel);

    /**
     * 闔ｷ蜿門ｭ仙ｼｹ逶堤噪遲臥ｺｧ・・
     *
     * @param ammoBox 蟄仙ｼｹ逶・
     * @return 遲臥ｺｧ・御ｻ・0 蠑蟋・
     */
    int getAmmoLevel(ItemStack ammoBox);

    /**
     * 譏ｯ蜷ｦ譏ｯ譌髯仙ｭ仙ｼｹ逶・
     *
     * @param ammoBox 蟄仙ｼｹ逶・
     * @return 譏ｯ蜷ｦ譏ｯ譌髯仙ｭ仙ｼｹ逶・
     */
    boolean isCreative(ItemStack ammoBox);

    /**
     * 譏ｯ蜷ｦ譏ｯ蜈ｨ遘咲ｱｻ譌髯仙ｭ仙ｼｹ逶・
     *
     * @param ammoBox 蟄仙ｼｹ逶・
     * @return 譏ｯ蜷ｦ譏ｯ蜈ｨ遘咲ｱｻ譌髯仙ｭ仙ｼｹ逶・
     */
    boolean isAllTypeCreative(ItemStack ammoBox);

    /**
     * 蟆・ｯ･蠑ｹ闕ｯ邂ｱ隶ｾ鄂ｮ荳ｺ譌髯千ｧ咲ｱｻ
     *
     * @param ammoBox   蟄仙ｼｹ逶・
     * @param isAllType 譏ｯ蜷ｦ譏ｯ蜈ｨ蠑ｹ遘咲ｱｻ蝙・
     * @return 菫ｮ謾ｹ蜷守噪蟄仙ｼｹ逶・
     */
    ItemStack setCreative(ItemStack ammoBox, boolean isAllType);
}

