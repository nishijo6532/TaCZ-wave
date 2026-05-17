package com.tacz.guns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.api.client.event.RenderLevelBobEvent;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Unique
    private boolean tacz$useFovSetting;

    @Shadow
    public abstract Minecraft getMinecraft();

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void onBobHurt(CameraRenderState cameraRenderState, PoseStack matrixStack, CallbackInfo ci) {
        if (this.getMinecraft().getCameraEntity() instanceof LocalPlayer player && !player.isDeadOrDying()) {
            if (GunHurtBobTweak.onHurtBobTweak(player, matrixStack, 0.0f)) {
                ci.cancel();
                return;
            }
        }
        boolean cancel;
        if (!tacz$useFovSetting) {
            cancel = com.tacz.guns.util.ForgeEventCompat.post(new RenderItemInHandBobEvent.BobHurt());
        } else {
            cancel = com.tacz.guns.util.ForgeEventCompat.post(new RenderLevelBobEvent.BobHurt());
        }
        if (cancel) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    public void onBobView(CameraRenderState cameraRenderState, PoseStack matrixStack, CallbackInfo ci) {
        boolean cancel;
        if (!tacz$useFovSetting) {
            cancel = IGun.getIGunOrNull(KeepingItemRenderer.getRenderer().getCurrentItem()) != null
                    || com.tacz.guns.util.ForgeEventCompat.post(new RenderItemInHandBobEvent.BobView());
        } else {
            cancel = com.tacz.guns.util.ForgeEventCompat.post(new RenderLevelBobEvent.BobView());
        }
        if (cancel) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    public void switchRenderType$hand(CameraRenderState cameraRenderState, float partialTick, Matrix4fc matrix4fc, CallbackInfo ci) {
        this.tacz$useFovSetting = false;
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void switchRenderType$level(DeltaTracker deltaTracker, CallbackInfo ci) {
        this.tacz$useFovSetting = true;
    }
}
