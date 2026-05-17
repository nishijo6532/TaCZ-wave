package com.tacz.guns.event.ammo;

import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber
public class DestroyGlassBlock {
    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        Level level = event.getLevel();
        BlockState state = event.getState();
        BlockPos pos = event.getHitResult().getBlockPos();
        EntityKineticBullet ammo = event.getAmmo();
        Block stateBlock = state.getBlock();
        if (AmmoConfig.DESTROY_GLASS.get() && isGlassLike(stateBlock, state)) {
            level.destroyBlock(pos, false, ammo.getOwner());
        }
    }

    private static boolean isGlassLike(Block block, BlockState state) {
        NoteBlockInstrument instrument = state.instrument();
        if (block instanceof TransparentBlock ||
                block instanceof StainedGlassPaneBlock ||
                (block instanceof IronBarsBlock && instrument.equals(NoteBlockInstrument.HAT))) {
            return true;
        }
        Identifier blockId = ForgeRegistries.BLOCKS.getKey(block);
        return blockId != null && blockId.getPath().contains("glass");
    }
}

