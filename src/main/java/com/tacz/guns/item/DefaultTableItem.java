package com.tacz.guns.item;

import com.tacz.guns.GunMod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultTableItem extends GunSmithTableItem{
    public static final Identifier ID = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "gun_smith_table");
    public DefaultTableItem(Block block) {
        super(block);
    }

    public DefaultTableItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    @NotNull
    public Identifier getBlockId(ItemStack block) {
        return ID;
    }

    @Override
    public void setBlockId(ItemStack block, @Nullable Identifier blockId) {
        // 鮟倩ｮ､蝮礼噪id譌謨茨ｼ碁煤螳壻ｸｺ"tacz:gun_smith_table"
    }
}


