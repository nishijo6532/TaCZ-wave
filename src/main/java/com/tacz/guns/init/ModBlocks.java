package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.*;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.block.entity.TargetBlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GunMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GunMod.MOD_ID);

    // 譌ｧ譁ｹ蝮怜ｰｱ隶ｩ莉也峡蜊荳荳ｪ莠・
    public static RegistryObject<Block> GUN_SMITH_TABLE = BLOCKS.register("gun_smith_table",
            () -> new GunSmithTableBlockB(BlockBehaviour.Properties.of().setId(BLOCKS.key("gun_smith_table")).sound(net.minecraft.world.level.block.SoundType.WOOD).strength(2.0F, 3.0F).noOcclusion()));
    public static RegistryObject<Block> WORKBENCH_111 = BLOCKS.register("workbench_a",
            () -> new GunSmithTableBlockA(BlockBehaviour.Properties.of().setId(BLOCKS.key("workbench_a")).sound(net.minecraft.world.level.block.SoundType.WOOD).strength(2.0F, 3.0F).noOcclusion()));
    public static RegistryObject<Block> WORKBENCH_211 = BLOCKS.register("workbench_b",
            () -> new GunSmithTableBlockB(BlockBehaviour.Properties.of().setId(BLOCKS.key("workbench_b")).sound(net.minecraft.world.level.block.SoundType.WOOD).strength(2.0F, 3.0F).noOcclusion()));
    public static RegistryObject<Block> WORKBENCH_121 = BLOCKS.register("workbench_c",
            () -> new GunSmithTableBlockC(BlockBehaviour.Properties.of().setId(BLOCKS.key("workbench_c")).sound(net.minecraft.world.level.block.SoundType.WOOD).strength(2.0F, 3.0F).noOcclusion()));

    public static RegistryObject<Block> TARGET = BLOCKS.register("target",
            () -> new TargetBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("target")).sound(net.minecraft.world.level.block.SoundType.WOOD).strength(2.0F, 3.0F).noOcclusion()));
    public static RegistryObject<Block> STATUE = BLOCKS.register("statue",
            () -> new StatueBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("statue")).sound(net.minecraft.world.level.block.SoundType.STONE).strength(2.0F, 3.0F).noOcclusion()));

    public static RegistryObject<BlockEntityType<GunSmithTableBlockEntity>> GUN_SMITH_TABLE_BE = TILE_ENTITIES.register("gun_smith_table", () -> GunSmithTableBlockEntity.TYPE);
    public static RegistryObject<BlockEntityType<TargetBlockEntity>> TARGET_BE = TILE_ENTITIES.register("target", () -> TargetBlockEntity.TYPE);
    public static RegistryObject<BlockEntityType<StatueBlockEntity>> STATUE_BE = TILE_ENTITIES.register("statue", () -> StatueBlockEntity.TYPE);
    public static final TagKey<Block> BULLET_IGNORE_BLOCKS = BlockTags.create(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "bullet_ignore"));
}


