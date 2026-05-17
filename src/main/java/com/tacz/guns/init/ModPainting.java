package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

public class ModPainting {
    public static final DeferredRegister<PaintingVariant> PAINTINGS = DeferredRegister.create(ForgeRegistries.PAINTING_VARIANTS, GunMod.MOD_ID);

    public static final RegistryObject<PaintingVariant> BLOOD_STRIKE_1 = PAINTINGS.register("blood_strike_1", () -> new PaintingVariant(32, 32, com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "blood_strike_1"), Optional.<Component>empty(), Optional.<Component>empty()));
//    public static final RegistryObject<PaintingVariant> BLOOD_STRIKE_2 = PAINTINGS.register("blood_strike_2", () -> new PaintingVariant(32, 32));
}
