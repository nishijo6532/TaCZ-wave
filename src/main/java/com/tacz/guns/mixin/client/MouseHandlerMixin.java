package com.tacz.guns.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.config.client.ZoomConfig;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    public void reduceSensitivity(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        ItemStack mainHandItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            original.call(player, yaw, pitch);
            return;
        }
        Identifier scopeId = iGun.getAttachmentId(mainHandItem, AttachmentType.SCOPE);
        if (scopeId.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
            scopeId = iGun.getBuiltInAttachmentId(mainHandItem, AttachmentType.SCOPE);
        }
        float zoomLevel = 1;
        if (DefaultAssets.isEmptyAttachmentId(scopeId)) {
            // 郛ｩ謾ｾ蛟咲紫
            zoomLevel = TimelessAPI.getGunDisplay(mainHandItem).map(GunDisplayInstance::getIronZoom).orElse(1f);
        } else {
            Optional<ClientAttachmentIndex> optional = TimelessAPI.getClientAttachmentIndex(scopeId);
            if (optional.isPresent()) {
                float[] zoom = optional.get().getZoom();
                if (zoom != null && zoom.length > 0) {
                    CompoundTag attachmentTag = iGun.getAttachmentTag(mainHandItem, AttachmentType.SCOPE);
                    zoomLevel = zoom[AttachmentItemDataAccessor.getZoomNumberFromTag(attachmentTag) % zoom.length];
                }
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        float progress = IGunOperator.fromLivingEntity(player).getSynAimingProgress();
        // 蠑髟懃・謨丞ｺｦ邉ｻ謨ｰ
        double sensitivityMultiplier = ZoomConfig.ZOOM_SENSITIVITY_BASE_MULTIPLIER.get();
        sensitivityMultiplier = 1 + (sensitivityMultiplier - 1) * progress;
        // 荳､遘咲憾諤∽ｸ狗噪 fov 隶｡邂・
        double originalFov = minecraft.options.fov().get();
        double currentFov = MathUtil.magnificationToFov(1 + (zoomLevel - 1) * progress, originalFov);
        // 闕ｧ蟷戊ｷ晉ｦｻ邉ｻ謨ｰ・勲C 蜥・COD 荳譬ｷ菴ｿ逕ｨ MDV 譬・㊥・碁ｻ倩ｮ､荳ｺ MDV133・育ｳｻ謨ｰ荳ｺ 1.33・・
        double coefficient = ZoomConfig.SCREEN_DISTANCE_COEFFICIENT.get();
        double denominator = MathUtil.zoomSensitivityRatio(currentFov, originalFov, coefficient) * sensitivityMultiplier;
        // 譛扈育ｻ捺棡
        double finalYaw = yaw * denominator;
        double finalPitch = getCrawlPitch(player, pitch, denominator);
        original.call(player, finalYaw, finalPitch);
    }

    @Unique
    private static double getCrawlPitch(LocalPlayer player, double pitch, double denominator) {
        double finalPitch = pitch * denominator;
        // 螯よ棡譏ｯ雜ｴ荳具ｼ碁ぅ荵郁ｿ倬怙隕・剞蛻ｶ pitch 闌・峩
        if (!player.isSwimming() && player.getPose() == Pose.SWIMMING) {
            // 莉ｰ隗呈ｭ｣雍滓弍蜿咲噪
            float playerPitch = -player.getXRot();
            // 螯よ棡邇ｩ螳ｶ荳贋ｻｰ雜・ｿ・25 蠎ｦ・御ｸ榊・隶ｸ荳・
            if (playerPitch > 45) {
                finalPitch = Math.max(finalPitch, 0);
            }
            // 荳倶ｿｯ雜・ｿ・25 蠎ｦ・御ｸ榊・隶ｸ荳・
            if (playerPitch < -30) {
                finalPitch = Math.min(finalPitch, 0);
            }
        }
        return finalPitch;
    }
}

