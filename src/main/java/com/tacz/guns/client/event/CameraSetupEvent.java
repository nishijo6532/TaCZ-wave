package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.api.modifier.ParameterizedCachePair;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.RecoilModifier;
import com.tacz.guns.util.ClientRenderCompat;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.math.MathUtil;
import com.tacz.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.Optional;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class CameraSetupEvent {
    /**
     * 逕ｨ莠主ｹｳ貊・FOV 蜿伜喧
     */
    public static final SecondOrderDynamics WORLD_FOV_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    public static final SecondOrderDynamics ITEM_MODEL_FOV_DYNAMICS = new SecondOrderDynamics(0.5f, 1.2f, 0.5f, 0);
    private static PolynomialSplineFunction pitchSplineFunction;
    private static PolynomialSplineFunction yawSplineFunction;
    private static long shootTimeStamp = -1L;
    private static float visualRecoilPitch = 0;
    private static float visualRecoilYaw = 0;
    private static final float VISUAL_RECOIL_CAMERA_SCALE = 0.35F;
    private static final VisualRecoilState VISUAL_RECOIL = new VisualRecoilState();

    @SubscribeEvent
    public static void applyLevelCameraAnimation(ViewportEvent.ComputeCameraAngles event) {
        if (!Minecraft.getInstance().options.bobView().get()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        // 蟆晁ｯ戊ｰ・畑迚ｩ蜩∫噪閾ｪ螳壻ｹ臥嶌譛ｺ蜉ｨ逕ｻ
        if (ClientRenderCompat.getCustomRenderer(stack) instanceof AnimateGeoItemRenderer<?, ?> renderer) {
            renderer.applyLevelCameraAnimation(event, stack, player);
        }

    }

    @SubscribeEvent
    public static void applyItemInHandCameraAnimation(BeforeRenderHandEvent event) {
        if (!Minecraft.getInstance().options.bobView().get()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        // 蟆晁ｯ戊ｰ・畑迚ｩ蜩∫噪閾ｪ螳壻ｹ臥嶌譛ｺ蜉ｨ逕ｻ
        if (ClientRenderCompat.getCustomRenderer(stack) instanceof AnimateGeoItemRenderer<?, ?> renderer) {
            renderer.applyItemInHandCameraAnimation(event, stack, player);
        }
    }

    @SubscribeEvent
    public static void applyScopeMagnification(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) {
            return; // 蜿ｪ菫ｮ謾ｹ荳也阜貂ｲ譟鍋噪 fov・悟屏豁､螯よ棡譏ｯ謇矩Κ貂ｲ譟・fov 莠倶ｻｶ・悟・霑泌屓
        }
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
            if (!(stack.getItem() instanceof IGun iGun)) {
                float fov = WORLD_FOV_DYNAMICS.update((float) event.getFOV());
                event.setFOV(fov);
                return;
            }
            float zoom = iGun.getAimingZoom(stack);
            if (livingEntity instanceof LocalPlayer localPlayer) {
                IClientPlayerGunOperator gunOperator = IClientPlayerGunOperator.fromLocalPlayer(localPlayer);
                float aimingProgress = gunOperator.getClientAimingProgress((float) event.getPartialTick());
                float fov = WORLD_FOV_DYNAMICS.update((float) MathUtil.magnificationToFov(1 + (zoom - 1) * aimingProgress, event.getFOV()));
                event.setFOV(fov);
            } else {
                IGunOperator gunOperator = IGunOperator.fromLivingEntity(livingEntity);
                float aimingProgress = gunOperator.getSynAimingProgress();
                float fov = WORLD_FOV_DYNAMICS.update((float) MathUtil.magnificationToFov(1 + (zoom - 1) * aimingProgress, event.getFOV()));
                event.setFOV(fov);
            }
        }
    }

    @SubscribeEvent
    public static void applyGunModelFovModifying(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) {
            return; // 蜿ｪ菫ｮ謾ｹ謇矩Κ迚ｩ蜩∫噪 fov・悟屏豁､螯よ棡譏ｯ荳也阜貂ｲ譟・fov 莠倶ｻｶ・悟・霑泌屓
        }
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
            if (!(stack.getItem() instanceof IGun iGun)) {
                float fov = ITEM_MODEL_FOV_DYNAMICS.update((float) event.getFOV());
                event.setFOV(fov);
                return;
            }
            Identifier scopeItemId = iGun.getAttachmentId(stack, AttachmentType.SCOPE);
            if (scopeItemId.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
                scopeItemId = iGun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
            }
            CompoundTag scopeTag = iGun.getAttachmentTag(stack, AttachmentType.SCOPE);
            int zoomNumber = AttachmentItemDataAccessor.getZoomNumberFromTag(scopeTag);
            // 蟆晁ｯ穂ｽｿ逕ｨ驟堺ｻｶfov菫ｮ謾ｹ・瑚凶譌蛻吝ｰ晁ｯ穂ｽｿ逕ｨ譫ｪ譴ｰ譛ｬ霄ｫfov菫ｮ謾ｹ・悟凄蛻咏ｻｴ謖∽ｸ榊序
            float modifiedFov = TimelessAPI.getClientAttachmentIndex(scopeItemId)
                    .map(index -> {
                        float[] viewsFov = index.getViewsFov();
                        return viewsFov[zoomNumber % viewsFov.length];
                    })
                    .orElse(
                        TimelessAPI.getGunDisplay(stack)
                                .map(GunDisplayInstance::getZoomModelFov)
                                .orElse((float) event.getFOV())
                    );
            if (livingEntity instanceof LocalPlayer localPlayer) {
                IClientPlayerGunOperator gunOperator = IClientPlayerGunOperator.fromLocalPlayer(localPlayer);
                float aimingProgress = gunOperator.getClientAimingProgress((float) event.getPartialTick());
                float fov = ITEM_MODEL_FOV_DYNAMICS.update(Mth.lerp(aimingProgress, (float) event.getFOV(), modifiedFov));
                event.setFOV(fov);
            } else {
                IGunOperator gunOperator = IGunOperator.fromLivingEntity(livingEntity);
                float aimingProgress = gunOperator.getSynAimingProgress();
                float fov = ITEM_MODEL_FOV_DYNAMICS.update(Mth.lerp(aimingProgress, (float) event.getFOV(), modifiedFov));
                event.setFOV(fov);
            }
        }
    }

    @SubscribeEvent
    public static void initialCameraRecoil(GunFireEvent event) {
        if (event.getLogicalSide().isClient()) {
            LivingEntity shooter = event.getShooter();
            LocalPlayer player = Minecraft.getInstance().player;
            if (!shooter.equals(player)) {
                return;
            }
            ItemStack mainHandItem = player.getMainHandItem();
            if (!(mainHandItem.getItem() instanceof IGun iGun)) {
                return;
            }
            AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(player).getCacheProperty();
            if (cacheProperty == null) {
                return;
            }
            Identifier gunId = iGun.getGunId(mainHandItem);
            Optional<ClientGunIndex> gunIndexOptional = TimelessAPI.getClientGunIndex(gunId);
            if (gunIndexOptional.isEmpty()) {
                return;
            }
            ClientGunIndex gunIndex = gunIndexOptional.get();
            GunData gunData = gunIndex.getGunData();
            // 闔ｷ蜿匁園譛蛾・莉ｶ蟇ｹ鞫・ワ譛ｺ蜷主攝蜉帷噪菫ｮ謾ｹ
            ParameterizedCachePair<Float, Float> attachmentRecoilModifier = cacheProperty.getCache(RecoilModifier.ID);
            IClientPlayerGunOperator clientPlayerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            float aimingProgress = clientPlayerGunOperator.getClientAimingProgress(partialTicks);
            float zoom = iGun.getAimingZoom(mainHandItem);
            float aimingRecoilModifier = 1 - aimingProgress + aimingProgress / (float) Math.min(Math.sqrt(zoom), 1.5);
            // 螯よ棡譏ｯ雜ｴ荳具ｼ碁ぅ荵亥錘蝮仙鴨謖・data 隶ｾ隶｡蜃丞ｰ托ｼ磯ｻ倩ｮ､荳ｺ髯堺ｽ惹ｸ蜊奇ｼ・
            if (!player.isSwimming() && player.getPose() == Pose.SWIMMING) {
                aimingRecoilModifier = aimingRecoilModifier * gunData.getCrawlRecoilMultiplier();
            }
            pitchSplineFunction = gunData.getRecoil().genPitchSplineFunction((float) attachmentRecoilModifier.left().eval(aimingRecoilModifier));
            yawSplineFunction = gunData.getRecoil().genYawSplineFunction((float) attachmentRecoilModifier.right().eval(aimingRecoilModifier));
            shootTimeStamp = System.currentTimeMillis();
            VISUAL_RECOIL.start(pitchSplineFunction, yawSplineFunction, shootTimeStamp);
        }
    }

    @SubscribeEvent
    public static void applyCameraRecoil(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        VISUAL_RECOIL.update(now);
        visualRecoilPitch = VISUAL_RECOIL.pitch() * VISUAL_RECOIL_CAMERA_SCALE;
        visualRecoilYaw = VISUAL_RECOIL.yaw() * VISUAL_RECOIL_CAMERA_SCALE;
        float appliedPitch = VISUAL_RECOIL.deltaPitch();
        float appliedYaw = VISUAL_RECOIL.deltaYaw();
        if (appliedPitch != 0 || appliedYaw != 0) {
            player.setXRot(player.getXRot() - appliedPitch);
            player.setYRot(player.getYRot() - appliedYaw);
        }
    }

    private static final class VisualRecoilState {
        private static final long MAX_RECOIL_MS = 1200L;

        private PolynomialSplineFunction pitchSpline;
        private PolynomialSplineFunction yawSpline;
        private long startedAt = -1L;
        private float pitch;
        private float yaw;
        private float previousPitch;
        private float previousYaw;
        private float deltaPitch;
        private float deltaYaw;
        private boolean active;

        private void start(PolynomialSplineFunction pitchSpline, PolynomialSplineFunction yawSpline, long startedAt) {
            this.pitchSpline = pitchSpline;
            this.yawSpline = yawSpline;
            this.startedAt = startedAt;
            this.pitch = 0;
            this.yaw = 0;
            this.previousPitch = 0;
            this.previousYaw = 0;
            this.deltaPitch = 0;
            this.deltaYaw = 0;
            this.active = pitchSpline != null || yawSpline != null;
        }

        private void update(long now) {
            if (!active) {
                pitch = 0;
                yaw = 0;
                deltaPitch = 0;
                deltaYaw = 0;
                return;
            }
            long elapsed = now - startedAt;
            if (elapsed < 0 || elapsed > MAX_RECOIL_MS) {
                clear();
                return;
            }
            boolean hasPitch = pitchSpline != null && pitchSpline.isValidPoint(elapsed);
            boolean hasYaw = yawSpline != null && yawSpline.isValidPoint(elapsed);
            pitch = hasPitch ? (float) pitchSpline.value(elapsed) : 0;
            yaw = hasYaw ? (float) yawSpline.value(elapsed) : 0;
            deltaPitch = pitch - previousPitch;
            deltaYaw = yaw - previousYaw;
            previousPitch = pitch;
            previousYaw = yaw;
        }

        private boolean isActive() {
            return active;
        }

        private float pitch() {
            return pitch;
        }

        private float yaw() {
            return yaw;
        }

        private float deltaPitch() {
            return deltaPitch;
        }

        private float deltaYaw() {
            return deltaYaw;
        }

        private void clear() {
            pitchSpline = null;
            yawSpline = null;
            startedAt = -1L;
            pitch = 0;
            yaw = 0;
            previousPitch = 0;
            previousYaw = 0;
            deltaPitch = 0;
            deltaYaw = 0;
            active = false;
        }
    }

    public static float getVisualRecoilPitch() {
        return visualRecoilPitch;
    }

    public static float getVisualRecoilYaw() {
        return visualRecoilYaw;
    }

    public static boolean isVisualRecoilActive() {
        return VISUAL_RECOIL.isActive();
    }

    @SubscribeEvent(priority = -64)
    public static void onComputeMovementFov(ComputeFovModifierEvent event) {
        if (!RenderConfig.DISABLE_MOVEMENT_ATTRIBUTE_FOV.get()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float f = 1.0f;
        if (player.getMainHandItem().getItem() instanceof AbstractGunItem) {
            if (player.getAbilities().flying) {
                f *= 1.1F;
            }
            event.setNewFovModifier(player.isSprinting() ? 1.15f * f : f);
        }
    }
}

