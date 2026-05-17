package com.tacz.guns.api.item.nbt;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IBlock;
import com.tacz.guns.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public interface BlockItemDataAccessor extends IBlock {
    String BLOCK_ID = "BlockId";

    @Override
    @Nonnull
    default Identifier getBlockId(ItemStack block) {
        CompoundTag nbt = NbtCompat.getOrCreateTag(block);
        if (NbtCompat.contains(nbt, BLOCK_ID, Tag.TAG_STRING)) {
            Identifier blockId = Identifier.tryParse(NbtCompat.getString(nbt, BLOCK_ID));
            return Objects.requireNonNullElse(blockId, DefaultAssets.EMPTY_BLOCK_ID);
        }
        return DefaultAssets.EMPTY_BLOCK_ID;
    }

    @Override
    default void setBlockId(ItemStack block, @Nullable Identifier blockId) {
        CompoundTag nbt = NbtCompat.getOrCreateTag(block);
        if (blockId != null) {
            nbt.putString(BLOCK_ID, blockId.toString());
        } else {
            nbt.putString(BLOCK_ID, DefaultAssets.EMPTY_BLOCK_ID.toString());
        }
        NbtCompat.setTag(block, nbt);
    }

}
