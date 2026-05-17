package com.tacz.guns.mixin.client;

import com.tacz.guns.client.renderer.item.LegacyRendererItemModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    @Inject(method = "appendItemLayers", at = @At("HEAD"), cancellable = true)
    private void tacz$appendLegacyRendererFallbackLayer(
            ItemStackRenderState output,
            ItemStack item,
            ItemDisplayContext displayContext,
            Level level,
            ItemOwner owner,
            int seed,
            CallbackInfo ci
    ) {
        ClientLevel clientLevel = level instanceof ClientLevel cl ? cl : null;
        if (LegacyRendererItemModel.applyFallback(
                output,
                item,
                (ItemModelResolver) (Object) this,
                displayContext,
                clientLevel,
                owner,
                seed
        )) {
            ci.cancel();
        }
    }
}
