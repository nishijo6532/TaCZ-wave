package com.tacz.guns.mixin.client;

import com.mojang.serialization.MapCodec;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.renderer.item.LegacyRendererItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModels.class)
public abstract class ItemModelsMixin {
    @Shadow
    @Final
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemModel.Unbaked>> ID_MAPPER;

    @Unique
    private static boolean tacz$legacyRendererRegistered = false;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void tacz$registerLegacyRendererType(CallbackInfo ci) {
        if (tacz$legacyRendererRegistered) {
            return;
        }
        Identifier id = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "legacy_renderer");
        ID_MAPPER.put(id, LegacyRendererItemModel.Unbaked.MAP_CODEC);
        tacz$legacyRendererRegistered = true;
    }
}
