package com.tacz.guns.api.item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface IBlock {
    /**
     * 闔ｷ蜿匁婿蝮・ID
     *
     * @param block 霎灘・迚ｩ蜩・
     * @return 譁ｹ蝮・ID
     */
    Identifier getBlockId(ItemStack block);

    /**
     * 隶ｾ鄂ｮ譁ｹ蝮・ID
     */
    void setBlockId(ItemStack block, @Nullable Identifier blockId);
}

