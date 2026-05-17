package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.nbt.AmmoItemDataAccessor;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    private static final int DEFAULT_AMMO_STACK_SIZE = 60;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GunMod.MOD_ID);

    public static RegistryObject<ModernKineticGunItem> MODERN_KINETIC_GUN = ITEMS.register("modern_kinetic_gun",
            () -> new ModernKineticGunItem(new Item.Properties().setId(ITEMS.key("modern_kinetic_gun")).stacksTo(1)
                    .component(DataComponents.CUSTOM_DATA, stringData(GunItemDataAccessor.GUN_ID_TAG, com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "ak47")))));

//    public static RegistryObject<ThrowableItem> M67 = ITEMS.register("m67", ThrowableItem::new);

    public static RegistryObject<Item> AMMO = ITEMS.register("ammo",
            () -> new AmmoItem(new Item.Properties().setId(ITEMS.key("ammo")).stacksTo(DEFAULT_AMMO_STACK_SIZE)
                    .component(DataComponents.CUSTOM_DATA, stringData(AmmoItemDataAccessor.AMMO_ID_TAG, DefaultAssets.DEFAULT_AMMO_ID))));
    public static RegistryObject<AttachmentItem> ATTACHMENT = ITEMS.register("attachment",
            () -> new AttachmentItem(new Item.Properties().setId(ITEMS.key("attachment")).stacksTo(1)
                    .component(DataComponents.CUSTOM_DATA, stringData(AttachmentItemDataAccessor.ATTACHMENT_ID_TAG, DefaultAssets.DEFAULT_ATTACHMENT_ID))));

    public static RegistryObject<GunSmithTableItem> GUN_SMITH_TABLE = ITEMS.register("gun_smith_table",
            () -> new DefaultTableItem(ModBlocks.GUN_SMITH_TABLE.get(), new Item.Properties().setId(ITEMS.key("gun_smith_table")).stacksTo(1)));
    public static RegistryObject<GunSmithTableItem> WORKBENCH_111 = ITEMS.register("workbench_a",
            () -> new GunSmithTableItem(ModBlocks.WORKBENCH_111.get(), new Item.Properties().setId(ITEMS.key("workbench_a")).stacksTo(1)));
    public static RegistryObject<GunSmithTableItem> WORKBENCH_211 = ITEMS.register("workbench_b",
            () -> new GunSmithTableItem(ModBlocks.WORKBENCH_211.get(), new Item.Properties().setId(ITEMS.key("workbench_b")).stacksTo(1)));
    public static RegistryObject<GunSmithTableItem> WORKBENCH_121 = ITEMS.register("workbench_c",
            () -> new GunSmithTableItem(ModBlocks.WORKBENCH_121.get(), new Item.Properties().setId(ITEMS.key("workbench_c")).stacksTo(1)));


    public static RegistryObject<Item> TARGET = ITEMS.register("target",
            () -> new BlockItem(ModBlocks.TARGET.get(), new Item.Properties().setId(ITEMS.key("target")).useBlockDescriptionPrefix()));
    public static RegistryObject<Item> STATUE = ITEMS.register("statue",
            () -> new BlockItem(ModBlocks.STATUE.get(), new Item.Properties().setId(ITEMS.key("statue")).useBlockDescriptionPrefix()));
    public static RegistryObject<Item> AMMO_BOX = ITEMS.register("ammo_box",
            () -> new AmmoBoxItem(new Item.Properties().setId(ITEMS.key("ammo_box")).stacksTo(1)));
    public static RegistryObject<Item> TARGET_MINECART = ITEMS.register("target_minecart",
            () -> new TargetMinecartItem(new Item.Properties().setId(ITEMS.key("target_minecart")).stacksTo(1)));

    private static CustomData stringData(String key, Identifier value) {
        CompoundTag tag = new CompoundTag();
        tag.putString(key, value.toString());
        return CustomData.of(tag);
    }

    @SubscribeEvent
    public static void onItemRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.ITEMS.getRegistryKey())) {
            GunItemManager.registerGunItem(ModernKineticGunItem.TYPE_NAME, MODERN_KINETIC_GUN);
        }
    }
}
