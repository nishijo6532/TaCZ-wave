package com.tacz.guns.block.entity;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class GunSmithTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final BlockEntityType<GunSmithTableBlockEntity> TYPE = new BlockEntityType<>(
            GunSmithTableBlockEntity::new,
            Set.of(
                    ModBlocks.GUN_SMITH_TABLE.get(),
                    ModBlocks.WORKBENCH_111.get(),
                    ModBlocks.WORKBENCH_121.get(),
                    ModBlocks.WORKBENCH_211.get()
            )
    );

    private static final String ID_TAG = "BlockId";

    @Nullable
    private Identifier id = null;

    public GunSmithTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
    }

    public void setId(Identifier id) {
        this.id = id;
    }

    @Nullable
    public Identifier getId() {
        return id;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 2.0D,
                worldPosition.getY(),
                worldPosition.getZ() - 2.0D,
                worldPosition.getX() + 2.0D,
                worldPosition.getY() + 1.0D,
                worldPosition.getZ() + 2.0D
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Gun Smith Table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GunSmithTableMenu(id, inventory, getId());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.id = input.getString(ID_TAG).map(Identifier::tryParse).orElse(DefaultAssets.DEFAULT_BLOCK_ID);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (id != null) {
            tag.putString(ID_TAG, id.toString());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}

