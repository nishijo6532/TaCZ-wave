package com.tacz.guns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.renderer.other.HumanoidOffhandRender;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At(value = "TAIL"))
    private void submit(PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int packedLight, ArmedEntityRenderState renderState, float f, float g, CallbackInfo ci) {
        MuzzleFlashRender.isSelf = false;
        ShellRender.isSelf = false;
        LivingEntity livingEntity = tacz$resolveLivingEntity(renderState);
        if (livingEntity != null) {
            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
            HumanoidOffhandRender.renderGun(livingEntity, matrixStack, buffer, packedLight);
            buffer.endBatch();
        }
    }

    @Inject(method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V", at = @At(value = "HEAD"), cancellable = true)
    private void submitArmWithItemHead(ArmedEntityRenderState renderState, ItemStackRenderState itemStackRenderState, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, CallbackInfo ci) {
        LivingEntity livingEntity = tacz$resolveLivingEntity(renderState);
        LocalPlayer player = Minecraft.getInstance().player;
        if (livingEntity != null && livingEntity.equals(player)) {
            MuzzleFlashRender.isSelf = true;
            ShellRender.isSelf = true;
        }
        if (renderState.getMainHandItemStack().getItem() instanceof IGun && arm == HumanoidArm.LEFT) {
            ci.cancel();
        }
    }

    @Inject(method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V", at = @At(value = "TAIL"))
    private void submitArmWithItemTail(ArmedEntityRenderState renderState, ItemStackRenderState itemStackRenderState, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, CallbackInfo ci) {
        MuzzleFlashRender.isSelf = false;
        ShellRender.isSelf = false;
    }

    private static LivingEntity tacz$resolveLivingEntity(ArmedEntityRenderState renderState) {
        if (!(renderState instanceof AvatarRenderState avatarRenderState)) {
            return null;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        Entity entity = level.getEntity(avatarRenderState.id);
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return null;
    }
}
