package com.tacz.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.TransformScale;
import com.tacz.guns.util.ClientRenderCompat;
import com.tacz.guns.util.RenderDistance;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.tacz.guns.util.RenderTypeCompat;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.minecraft.world.item.ItemDisplayContext.*;

/**
 * 髮肴ｺｯ・ｴ・｣闕ｳ・ｻ髫補悪蝎ｪ隴ｫ・ｪ隴ｴ・ｰ陷会ｽｨ騾包ｽｻ隶難ｽ｡陜吝玄・ｸ・ｲ隴溯侭繧具ｽ｢譎擾ｽ､荵溷飭隰ｨ蝓滓｣｡髫励・{@link com.tacz.guns.client.event.FirstPersonRenderGunEvent}
 */
public class GunItemRendererWrapper extends AnimateGeoItemRenderer<BedrockGunModel, GunAnimationStateContext> {
    private static final double MIN_VALID_FOV = 10.0;
    private static final double MAX_VALID_FOV = 170.0;
    private static final float MIN_VALID_MUZZLE_Z = -3.0F;
    private static final float MAX_VALID_MUZZLE_Z = 3.0F;
    private static final float MAX_VALID_MUZZLE_X = 4.0F;
    private static final float MIN_VALID_MUZZLE_Y = -3.0F;
    private static final float MAX_VALID_MUZZLE_Y = 3.0F;
    private static final float HIP_STABLE_AIMING_PROGRESS = 0.05F;
    private static final float ADS_STABLE_AIMING_PROGRESS = 0.95F;
    private static final float MAX_HIP_STABLE_DELTA = 0.18F;
    private static final float MAX_ADS_STABLE_DELTA = 0.12F;
    private static final SlotModel SLOT_GUN_MODEL = new SlotModel();
    private static BedrockGunModel lastModel = null;
    public static final Vector3f muzzleRenderOffset = new Vector3f();
    private static final Vector3f latestShotMuzzleOffset = new Vector3f();
    private static final Vector3f hipStableMuzzleOffset = new Vector3f();
    private static final Vector3f adsStableMuzzleOffset = new Vector3f();
    private static long latestShotMuzzleCapturedAt = 0L;
    private static float latestShotCameraXRot = 0F;
    private static float latestShotCameraYRot = 0F;
    private static boolean hasHipStableMuzzleOffset = false;
    private static boolean hasAdsStableMuzzleOffset = false;
    private static final Map<Long, ShotMuzzleBasis> shotMuzzleBasisByTimestamp = new LinkedHashMap<>();
    private static final long SHOT_MUZZLE_BASIS_EXPIRE_MS = 1500L;
    private static final int MAX_SHOT_MUZZLE_BASIS_ENTRIES = 64;
    private static long firstPersonRenderPassSerial = 0L;
    private static long lastStateMachineUpdatePassSerial = Long.MIN_VALUE;
    private static boolean hasLastCameraLocalView = false;
    private static float lastCameraLocalPitch = 0F;
    private static float lastCameraLocalYaw = 0F;
    private static final float VISUAL_RECOIL_MODEL_ROTATION_SCALE = -1.0F;

    public GunItemRendererWrapper() {
        super();
    }

    public static void captureShotMuzzleBasis(long shootTimestamp, float pitch, float yaw, Vec3 eyePosition) {
        latestShotMuzzleOffset.set(resolveShotMuzzleOffsetForShot());
        latestShotCameraXRot = Mth.wrapDegrees(pitch);
        latestShotCameraYRot = Mth.wrapDegrees(yaw);
        latestShotMuzzleCapturedAt = System.currentTimeMillis();
        pruneShotMuzzleBasis();
        shotMuzzleBasisByTimestamp.put(shootTimestamp, new ShotMuzzleBasis(
                new Vector3f(latestShotMuzzleOffset),
                latestShotCameraXRot,
                latestShotCameraYRot,
                eyePosition,
                latestShotMuzzleCapturedAt
        ));
    }

    public static boolean hasRecentShotMuzzleBasis() {
        return System.currentTimeMillis() - latestShotMuzzleCapturedAt <= 250L;
    }

    public static Vector3f copyLatestShotMuzzleOffset() {
        return new Vector3f(latestShotMuzzleOffset);
    }

    public static float getLatestShotCameraXRot() {
        return latestShotCameraXRot;
    }

    public static float getLatestShotCameraYRot() {
        return latestShotCameraYRot;
    }

    @Nullable
    public static ShotMuzzleBasis getShotMuzzleBasis(long shootTimestamp) {
        pruneShotMuzzleBasis();
        return shotMuzzleBasisByTimestamp.get(shootTimestamp);
    }

    private static void pruneShotMuzzleBasis() {
        long now = System.currentTimeMillis();
        shotMuzzleBasisByTimestamp.entrySet().removeIf(entry -> now - entry.getValue().capturedAt > SHOT_MUZZLE_BASIS_EXPIRE_MS);
        while (shotMuzzleBasisByTimestamp.size() > MAX_SHOT_MUZZLE_BASIS_ENTRIES) {
            Long firstKey = shotMuzzleBasisByTimestamp.keySet().iterator().next();
            shotMuzzleBasisByTimestamp.remove(firstKey);
        }
    }

    public static void beginFirstPersonRenderPass() {
        firstPersonRenderPassSerial++;
    }

    private static Vector3f resolveShotMuzzleOffset() {
        float aimingProgress = FirstPersonRenderGunEvent.getLastAppliedAimingProgress();
        Vector3f liveOffset = muzzleRenderOffset;
        if (aimingProgress <= HIP_STABLE_AIMING_PROGRESS) {
            return preferStableShotBasis(hipStableMuzzleOffset, hasHipStableMuzzleOffset, liveOffset, MAX_HIP_STABLE_DELTA);
        }
        if (aimingProgress >= ADS_STABLE_AIMING_PROGRESS) {
            return preferStableShotBasis(adsStableMuzzleOffset, hasAdsStableMuzzleOffset, liveOffset, MAX_ADS_STABLE_DELTA);
        }
        if (isValidLiveShotBasis(liveOffset)) {
            return liveOffset;
        }
        if (aimingProgress < 0.5F && hasHipStableMuzzleOffset) {
            return hipStableMuzzleOffset;
        }
        if (aimingProgress >= 0.5F && hasAdsStableMuzzleOffset) {
            return adsStableMuzzleOffset;
        }
        return liveOffset;
    }

    private static Vector3f resolveShotMuzzleOffsetForShot() {
        float aimingProgress = FirstPersonRenderGunEvent.getLastAppliedAimingProgress();
        Vector3f liveOffset = muzzleRenderOffset;
        if (aimingProgress >= ADS_STABLE_AIMING_PROGRESS && hasAdsStableMuzzleOffset) {
            return alignAdsShotOffsetToSightCenter(preferStableShotBasis(adsStableMuzzleOffset, true, liveOffset, MAX_ADS_STABLE_DELTA));
        }
        if (isValidLiveShotBasis(liveOffset)) {
            if (aimingProgress >= ADS_STABLE_AIMING_PROGRESS) {
                return alignAdsShotOffsetToSightCenter(liveOffset);
            }
            return liveOffset;
        }
        return resolveShotMuzzleOffset();
    }

    private static Vector3f alignAdsShotOffsetToSightCenter(Vector3f offset) {
        Vector3f sightCenteredOffset = new Vector3f(offset);
        sightCenteredOffset.x = 0.0F;
        return sightCenteredOffset;
    }

    public static final class ShotMuzzleBasis {
        private final Vector3f offset;
        private final float cameraXRot;
        private final float cameraYRot;
        private final Vec3 eyePosition;
        private final long capturedAt;

        private ShotMuzzleBasis(Vector3f offset, float cameraXRot, float cameraYRot, Vec3 eyePosition, long capturedAt) {
            this.offset = offset;
            this.cameraXRot = cameraXRot;
            this.cameraYRot = cameraYRot;
            this.eyePosition = eyePosition;
            this.capturedAt = capturedAt;
        }

        public Vector3f offset() {
            return new Vector3f(offset);
        }

        public float cameraXRot() {
            return cameraXRot;
        }

        public float cameraYRot() {
            return cameraYRot;
        }

        public Vec3 eyePosition() {
            return eyePosition;
        }

        public long capturedAt() {
            return capturedAt;
        }
    }

    @Override
    public GunAnimationStateContext initContext(ItemStack stack, Player player, float partialTick) {
        GunAnimationStateContext context = new GunAnimationStateContext();
        this.updateContext(context, stack, player, partialTick);
        return context;
    }

    @Override
    public void updateContext(GunAnimationStateContext context, ItemStack stack, Player player, float partialTick) {
        context.setPartialTicks(partialTick);
        context.setCurrentGunItem(stack);
    }

    @Override
    public void tryInit(ItemStack stack, Player player, float partialTick) {
        super.tryInit(stack, player, partialTick);
    }

    @Override
    public void tryExit(ItemStack stack, long putAwayTime) {
        var stateMachine = getStateMachine(stack);
        if (stateMachine == null) {
            return;
        }
        stateMachine.processContextIfExist(context -> {
            context.setPutAwayTime(putAwayTime / 1000F);
            context.setCurrentGunItem(stack);
        });
        if(stateMachine.isInitialized()) {
            stateMachine.trigger(GunAnimationConstant.INPUT_PUT_AWAY);
            KeepingItemRenderer.getRenderer().keep(stack, putAwayTime);
            stateMachine.exit();
            stateMachine.setExitingTime(putAwayTime + 50);
        }
    }

    @Override
    public long getPutAwayTime(ItemStack stack) {
        if (stack.getItem() instanceof IGun iGun) {
            return TimelessAPI.getCommonGunIndex(iGun.getGunId(stack))
                    .map(index -> (long) (index.getGunData().getPutAwayTime() * 1000L))
                    .orElse(0L);
        }
        return 0;
    }

    @Nullable
    @Override
    public LuaAnimationStateMachine<GunAnimationStateContext> getStateMachine(ItemStack stack) {
        return TimelessAPI.getGunDisplay(stack).map(GunDisplayInstance::getAnimationStateMachine).orElse(null);
    }

    @Override
    public BedrockGunModel getModel(ItemStack stack) {
        return TimelessAPI.getGunDisplay(stack).map(GunDisplayInstance::getGunModel).orElse(null);
    }

    @Override
    public Identifier getTextureLocation(ItemStack stack) {
        return TimelessAPI.getGunDisplay(stack).map(GunDisplayInstance::getModelTexture).orElse(null);
    }

    @Override
    public void applyLevelCameraAnimation(ViewportEvent.ComputeCameraAngles event, ItemStack stack, LocalPlayer player) {
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }
        Optional.ofNullable(getModel(stack)).ifPresent(model -> {
            if (lastModel != model) {
                // 陋ｻ繝ｻ蝗ｰ隴ｫ・ｪ隴ｴ・ｰ隶難ｽ｡陜咏距蝎ｪ隴鯉ｽｶ陋溷綜・ｸ繝ｻ轤願叉闕ｳ蛹ｺ讒崎恍荵玲・陷会ｽｨ騾包ｽｻ隰ｨ・ｰ隰撰ｽｮ繝ｻ蠕｡・ｻ・･鬩包ｽｿ陷亥ｺ・ｸ雍具ｽｸ隹ｺ・｡隰ｦ・ｭ隰ｾ・ｾ陋ｻ・ｰ闕ｳ陷企｡泌飭髷ｫ繝ｻ繝ｯ隴幢ｽｺ陷会ｽｨ騾包ｽｻ陟厄ｽｱ陷ｩ蟠趣ｽｧ繧遺楳邵ｲ繝ｻ
                model.cleanCameraAnimationTransform();
                lastModel = model;
            }
            IClientPlayerGunOperator clientPlayerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            float partialTicks = ClientRenderCompat.getPartialTicks();
            float aimingProgress = clientPlayerGunOperator.getClientAimingProgress(partialTicks);
            float zoom = iGun.getAimingZoom(stack);
            float multiplier = 1 - aimingProgress + aimingProgress / (float) Math.sqrt(zoom);
            Quaternionf quaternion = MathUtil.multiplyQuaternion(model.getCameraAnimationObject().rotationQuaternion, multiplier);
            if (usesCameraLocalVisualRecoil(aimingProgress)) {
                model.cleanCameraAnimationTransform();
                return;
            }
            applyCameraLocalAnimation(event, quaternion);
        });
    }

    private static boolean usesCameraLocalVisualRecoil(float aimingProgress) {
        return aimingProgress > 0.7F && CameraSetupEvent.isVisualRecoilActive();
    }

    private static void applyCameraLocalAnimation(ViewportEvent.ComputeCameraAngles event, Quaternionf localAnimation) {
        if (Math.abs(localAnimation.x()) < 1.0E-7F
                && Math.abs(localAnimation.y()) < 1.0E-7F
                && Math.abs(localAnimation.z()) < 1.0E-7F) {
            return;
        }
        double yaw = Math.asin(2 * (localAnimation.w() * localAnimation.y() - localAnimation.x() * localAnimation.z()));
        double pitch = Math.atan2(2 * (localAnimation.w() * localAnimation.x() + localAnimation.y() * localAnimation.z()),
                1 - 2 * (localAnimation.x() * localAnimation.x() + localAnimation.y() * localAnimation.y()));
        double roll = Math.atan2(2 * (localAnimation.w() * localAnimation.z() + localAnimation.x() * localAnimation.y()),
                1 - 2 * (localAnimation.y() * localAnimation.y() + localAnimation.z() * localAnimation.z()));
        event.setYaw(event.getYaw() + (float) Math.toDegrees(yaw));
        event.setPitch(event.getPitch() + (float) Math.toDegrees(pitch));
        event.setRoll(event.getRoll() + (float) Math.toDegrees(roll));
    }

    @Override
    public void applyItemInHandCameraAnimation(BeforeRenderHandEvent event, ItemStack stack, LocalPlayer player) {
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }
        Optional.ofNullable(getModel(stack)).ifPresent(model -> {
            PoseStack poseStack = event.getPoseStack();
            IClientPlayerGunOperator clientPlayerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            float partialTicks = ClientRenderCompat.getPartialTicks();
            float aimingProgress = clientPlayerGunOperator.getClientAimingProgress(partialTicks);
            float zoom = iGun.getAimingZoom(stack);
            float multiplier = 1 - aimingProgress + aimingProgress / (float) Math.sqrt(zoom);
            Quaternionf quaternion = MathUtil.multiplyQuaternion(model.getCameraAnimationObject().rotationQuaternion, multiplier);
            if (usesCameraLocalVisualRecoil(aimingProgress)) {
                model.cleanCameraAnimationTransform();
                return;
            }
            poseStack.mulPose(quaternion);
            // 隰鯉ｽｪ髢ｾ・ｳ騾ｶ・ｮ陷第誓・ｼ譴ｧ讒崎恍荵玲・陷会ｽｨ騾包ｽｻ隰ｨ・ｰ隰撰ｽｮ陝ｾ・ｲ雎ｸ驛・ｽｴ・ｹ陞ｳ譴ｧ・ｯ霈斐ｈ蠑崎惺・ｦ隴帷判蟲ｩ陞ゑｽｽ騾ｧ繝ｻ・ｸ繝ｻ轤願怏・ｨ騾包ｽｻ隰ｨ・ｰ隰撰ｽｮ騾ｧ繝ｻ蟀ｿ雎募桁・ｼ繝ｻ
            model.cleanCameraAnimationTransform();
        });
    }

    @Override
    public void renderFirstPerson(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int light, float partialTick) {
        if (!(stack.getItem() instanceof IGun)) {
            return;
        }

        TimelessAPI.getGunDisplay(stack).ifPresent(display -> {
            BedrockGunModel gunModel = display.getGunModel();
            var animationStateMachine = display.getAnimationStateMachine();
            if (gunModel == null) {
                return;
            }

            // 陜ｨ・ｨ雋ゑｽｲ隴溯ｬ趣ｽｹ蜿･辯輔・謔溘・隴厄ｽｴ隴・ｽｰ陷会ｽｨ騾包ｽｻ繝ｻ迹夲ｽｮ・ｩ陷会ｽｨ騾包ｽｻ隰ｨ・ｰ隰撰ｽｮ陷蜷昴・隶難ｽ｡陜吶・
            animationStateMachine.processContextIfExist(context -> {
                updateContext(context, stack, player, partialTick);
            });
            if (lastStateMachineUpdatePassSerial != firstPersonRenderPassSerial) {
                animationStateMachine.update();
                lastStateMachineUpdatePassSerial = firstPersonRenderPassSerial;
            }

            poseStack.pushPose();
            // 鬨ｾ繝ｻ・ｽ・ｬ陷ｴ貅ｽ豐ｿ隴・ｽｽ陷会｣ｰ陜ｨ・ｨ隰・ｶ・ｸ鬘泌飭陝抵ｽｶ雋頑ｨ願ｭ懆ｭｫ諛ｶ・ｼ譴ｧ髫ｼ闕ｳ・ｺ陷蜷昴・隶難ｽ｡陜吝唱蜍倬包ｽｻ隰ｨ・ｰ隰撰ｽｮ闕ｳ・ｭ
            float xRotOffset = Mth.lerp(partialTick, player.xBobO, player.xBob);
            float yRotOffset = Mth.lerp(partialTick, player.yBobO, player.yBob);
            float vanillaXRot = player.getViewXRot(partialTick) - xRotOffset;
            float vanillaYRot = player.getViewYRot(partialTick) - yRotOffset;
            float cameraLocalPitch = vanillaXRot;
            float cameraLocalYaw = Mth.wrapDegrees(vanillaYRot);
            float xRot = hasLastCameraLocalView ? cameraLocalPitch - lastCameraLocalPitch : 0F;
            float yRot = hasLastCameraLocalView ? Mth.wrapDegrees(cameraLocalYaw - lastCameraLocalYaw) : 0F;
            lastCameraLocalPitch = cameraLocalPitch;
            lastCameraLocalYaw = cameraLocalYaw;
            hasLastCameraLocalView = true;
            float aimingProgress = FirstPersonRenderGunEvent.getLastAppliedAimingProgress();
            if (aimingProgress > 0.7F && CameraSetupEvent.isVisualRecoilActive()) {
                xRot = 0F;
                yRot = 0F;
            }
            float adsSwayWeight = 1.0F;
            xRot *= adsSwayWeight;
            yRot *= adsSwayWeight;
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot * -0.1F));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot * -0.1F));
            BedrockPart rootNode = gunModel.getRootNode();
            if (rootNode != null) {
                xRot = (float) Math.tanh(xRot / 25) * 25;
                yRot = (float) Math.tanh(yRot / 25) * 25;
                rootNode.offsetX += yRot * 0.1F / 16F / 3F;
                rootNode.offsetY += -xRot * 0.1F / 16F / 3F;
                rootNode.additionalQuaternion.mul(Axis.XP.rotationDegrees(xRot * 0.05F));
                rootNode.additionalQuaternion.mul(Axis.YP.rotationDegrees(yRot * 0.05F));
            }
            applyCameraLocalVisualRecoil(poseStack, aimingProgress);
            // 闔牙叙・ｸ・ｲ隴溽§谺｡霓､・ｹ (0, 24, 0) 驕假ｽｻ陷会ｽｨ陋ｻ・ｰ隶難ｽ｡陜吝唱谺｡霓､・ｹ (0, 0, 0)
            poseStack.translate(0, 1.5f, 0);
            // 陜難ｽｺ陝ｯ・ｩ霑壼沺・ｨ・｡陜吝玄蠑崎叉雍具ｽｸ遏ｩ・｢・ｰ陋溷､蝎ｪ繝ｻ遒∵咎囎竏ｫ・ｿ・ｻ髴難ｽｬ髴代・謫らｸｲ繝ｻ
            poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
            // 陟守坩逡題ｬ問扱譽ｯ陝具ｽｿ隲､竏晏ｺ剰ｬ撰ｽ｢繝ｻ謔滂ｽｦ繧会ｽｬ・ｬ闕ｳ闔・ｺ驕假ｽｰ髷ｫ繝ｻ繝ｯ隴幢ｽｺ陞ｳ螢ｻ・ｽ繝ｻ
            FirstPersonRenderGunEvent.applyFirstPersonGunTransform(player, stack, poseStack, gunModel, partialTick);

            // 陟題惺・ｯ髫ｨ・ｬ闕ｳ闔・ｺ驕假ｽｰ陟托ｽｹ陞｢・ｳ陷･讙寂・霎滂ｽｰ雋ゑｽｲ隴溘・
            MuzzleFlashRender.isSelf = true;
            ShellRender.isSelf = true;
            // 陞ｯ繧域｣｡雎・ｽ｣陜ｨ・ｨ隰・§・ｼ隰ｾ・ｹ髯ｬ繝ｻ髦憺ｫｱ・｢繝ｻ謔溘・陷ｿ蛹・ｽｶ蝓溽・髢ｾ繧茨ｽｸ・ｲ隴溘・
            boolean renderHand = gunModel.getRenderHand();
            if (RefitTransform.getOpeningProgress() != 0) {
                gunModel.setRenderHand(false);
            }
            // 髫ｹ繝ｻ逡題ｭｫ・ｪ隴ｴ・ｰ隶難ｽ｡陜吝玄・ｸ・ｲ隴溘・
            RenderType renderType = RenderTypeCompat.entityCutout(display.getModelTexture());
            gunModel.render(poseStack, stack, ctx, renderType, light, OverlayTexture.NO_OVERLAY);
            // 驛帷§・ｭ菫ｶ譽ｯ陷ｿ・｣闖ｴ蜥ｲ・ｽ・ｮ繝ｻ蠕｡・ｸ・ｺ髫ｨ・ｬ闕ｳ闔・ｺ驕假ｽｰ隴厄ｽｳ陷育甥・ｼ・ｹ雋ゑｽｲ隴溯ｬ趣ｽｽ諛ｷ繩･陞溘・
            cacheMuzzlePosition(poseStack, gunModel, player, partialTick);
            // 隲ｱ・｢陞溯ざ辟秘明繧茨ｽｸ・ｲ隴溘・
            gunModel.setRenderHand(renderHand);
            // 雋ゑｽｲ隴溽§・ｮ譴ｧ繝ｻ陷ｷ雜｣・ｼ謔滂ｽｰ繝ｻ蜍倬包ｽｻ隰ｨ・ｰ隰撰ｽｮ闔牙叙・ｨ・｡陜吝ｶ・ｸ・ｭ雋ゅ・蜍√・蠕｡・ｸ讎奇ｽｯ・ｹ陷茨ｽｶ闔牙・・ｧ繝ｻ・ｧ蜑・ｽｸ迢怜飭隶難ｽ｡陜吝玄・ｸ・ｲ隴溯ｬ趣ｽｺ・ｧ騾墓ｺｷ・ｽ・ｱ陷ｩ繝ｻ
            poseStack.popPose();
            gunModel.cleanAnimationTransform();
            // 陷茨ｽｳ鬮｣・ｭ髫ｨ・ｬ闕ｳ闔・ｺ驕假ｽｰ陟托ｽｹ陞｢・ｳ陷･讙寂・霎滂ｽｰ雋ゑｽｲ隴溘・
            MuzzleFlashRender.isSelf = false;
            ShellRender.isSelf = false;
        });
    }

    private static void cacheMuzzlePosition(PoseStack poseStack, BedrockGunModel gunModel, LocalPlayer player, float partialTick) {
        if (gunModel.getMuzzleFlashPosPath() != null) {
            // 髫ｶ・｡驍よ懊・隴ｫ・ｪ陷ｿ・｣騾ｶ・ｸ陝・ｽｹ闔蜿匁ｧ崎恍荵玲・闕ｳ・ｭ陟｢繝ｻ蝎ｪ陜ｮ蜈茨｣ｰ繝ｻ
            poseStack.pushPose();
            for (BedrockPart bedrockPart : gunModel.getMuzzleFlashPosPath()) {
                bedrockPart.translateAndRotateAndScale(poseStack);
            }
            Matrix4f pose = poseStack.last().pose();
            double itemRenderFov = CameraSetupEvent.ITEM_MODEL_FOV_DYNAMICS.get();
            double levelRenderFov = CameraSetupEvent.WORLD_FOV_DYNAMICS.get();
            double configuredFov = Minecraft.getInstance().options.fov().get();
            if (!Double.isFinite(itemRenderFov) || itemRenderFov <= 0.0) {
                itemRenderFov = configuredFov;
            }
            if (!Double.isFinite(levelRenderFov) || levelRenderFov <= 0.0) {
                levelRenderFov = configuredFov;
            }
            double tanLevelFov = Math.tan(levelRenderFov * Math.PI / 360.0);
            double fovScale = Math.abs(tanLevelFov) > 1.0E-6
                    ? Math.tan(itemRenderFov * Math.PI / 360.0) / tanLevelFov
                    : 1.0;
            poseStack.popPose();
            // 驛帷§・ｭ蛟ｩ・ｽ・ｬ隰撰ｽ｢陷ｷ螳亥飭陋帛・・ｧ・ｻ陜ｮ蜈茨｣ｰ繝ｻ
            Vector3f candidateOffset = new Vector3f(
                    pose.m30(),
                    pose.m31(),
                    pose.m32()
            );
            float viewPitch = Mth.wrapDegrees(player.getViewXRot(partialTick));
            float viewYaw = Mth.wrapDegrees(player.getViewYRot(partialTick));
            candidateOffset.rotateY(viewYaw * Mth.DEG_TO_RAD);
            candidateOffset.rotateX(-viewPitch * Mth.DEG_TO_RAD);
            candidateOffset.x = -candidateOffset.x;
            candidateOffset.z *= (float) fovScale;
            if (!isValidMuzzleCapture(itemRenderFov, levelRenderFov, candidateOffset)) {
                return;
            }
            muzzleRenderOffset.set(candidateOffset);
            float aimingProgress = FirstPersonRenderGunEvent.getLastAppliedAimingProgress();
            if (aimingProgress <= HIP_STABLE_AIMING_PROGRESS) {
                if (!hasHipStableMuzzleOffset || acceptsStableMuzzleUpdate(hipStableMuzzleOffset, candidateOffset, MAX_HIP_STABLE_DELTA)) {
                    hipStableMuzzleOffset.set(candidateOffset);
                    hasHipStableMuzzleOffset = true;
                }
            } else if (aimingProgress >= ADS_STABLE_AIMING_PROGRESS) {
                if (!hasAdsStableMuzzleOffset || acceptsStableMuzzleUpdate(adsStableMuzzleOffset, candidateOffset, MAX_ADS_STABLE_DELTA)) {
                    adsStableMuzzleOffset.set(candidateOffset);
                    hasAdsStableMuzzleOffset = true;
                }
            }
        } else {
            muzzleRenderOffset.set(0, 0, 0);
        }
    }

    private static void applyCameraLocalVisualRecoil(PoseStack poseStack, float aimingProgress) {
        if (aimingProgress <= 0.7F || !CameraSetupEvent.isVisualRecoilActive()) {
            return;
        }
        float pitch = CameraSetupEvent.getVisualRecoilPitch();
        if (Math.abs(pitch) < 1.0E-4F) {
            return;
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * VISUAL_RECOIL_MODEL_ROTATION_SCALE));
    }

    private static boolean isValidMuzzleCapture(double itemRenderFov, double levelRenderFov, Vector3f candidateOffset) {
        if (!Double.isFinite(itemRenderFov) || !Double.isFinite(levelRenderFov)) {
            return false;
        }
        if (itemRenderFov < MIN_VALID_FOV || itemRenderFov > MAX_VALID_FOV) {
            return false;
        }
        if (levelRenderFov < MIN_VALID_FOV || levelRenderFov > MAX_VALID_FOV) {
            return false;
        }
        if (!Float.isFinite(candidateOffset.x) || !Float.isFinite(candidateOffset.y) || !Float.isFinite(candidateOffset.z)) {
            return false;
        }
        if (Math.abs(candidateOffset.x) > MAX_VALID_MUZZLE_X) {
            return false;
        }
        if (candidateOffset.y < MIN_VALID_MUZZLE_Y || candidateOffset.y > MAX_VALID_MUZZLE_Y) {
            return false;
        }
        return candidateOffset.z >= MIN_VALID_MUZZLE_Z && candidateOffset.z <= MAX_VALID_MUZZLE_Z;
    }

    private static boolean acceptsStableMuzzleUpdate(Vector3f stableOffset, Vector3f candidateOffset, float maxDelta) {
        return stableOffset.distanceSquared(candidateOffset) <= maxDelta * maxDelta;
    }

    private static Vector3f preferStableShotBasis(Vector3f stableOffset, boolean hasStableOffset, Vector3f liveOffset, float maxDelta) {
        if (!hasStableOffset) {
            return liveOffset;
        }
        if (!isValidLiveShotBasis(liveOffset)) {
            return stableOffset;
        }
        float maxStableDistance = maxDelta * 2.0F;
        if (stableOffset.distanceSquared(liveOffset) <= maxStableDistance * maxStableDistance) {
            return stableOffset;
        }
        return liveOffset;
    }

    private static boolean isValidLiveShotBasis(Vector3f liveOffset) {
        return Float.isFinite(liveOffset.x) && Float.isFinite(liveOffset.y) && Float.isFinite(liveOffset.z)
                && Math.abs(liveOffset.x) <= MAX_VALID_MUZZLE_X
                && liveOffset.y >= MIN_VALID_MUZZLE_Y && liveOffset.y <= MAX_VALID_MUZZLE_Y
                && liveOffset.z >= MIN_VALID_MUZZLE_Z && liveOffset.z <= MAX_VALID_MUZZLE_Z;
    }


    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer,
                             int pPackedLight, int pPackedOverlay) {
        if (!(stack.getItem() instanceof IGun)) {
            return;
        }
        poseStack.pushPose();
        TimelessAPI.getGunDisplay(stack).ifPresentOrElse(gunIndex -> {
            // 髫ｨ・ｬ闕ｳ闔・ｺ驕假ｽｰ陝・ｽｱ闕ｳ閧ｴ・ｸ・ｲ隴溯ｬ趣ｽｺ繝ｻ・ｼ蠕｡・ｺ・､謇亥雀謔ｪ騾ｧ繝ｻ諷崎ｭ・ｽｹ
            if (transformType == FIRST_PERSON_LEFT_HAND || transformType == FIRST_PERSON_RIGHT_HAND) {
                return;
            }
            // 髫ｨ・ｬ闕ｳ謌托ｽｺ・ｺ驕假ｽｰ陷托ｽｯ隰・ｶ・ｹ貊会ｽｸ閧ｴ・ｸ・ｲ隴溯ｬ趣ｽｺ繝ｻ
            if (transformType == THIRD_PERSON_LEFT_HAND) {
                return;
            }
            // GUI 霑夲ｽｹ隹ｿ鬆托ｽｸ・ｲ隴溘・
            if (transformType == GUI) {
                poseStack.translate(0.5, 1.5, 0.5);
                poseStack.mulPose(Axis.ZN.rotationDegrees(180));
                VertexConsumer buffer = pBuffer.getBuffer(RenderTypeCompat.entityTranslucent(gunIndex.getSlotTexture()));
                SLOT_GUN_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                return;
            }
            // 陷托ｽｩ闕ｳ迢怜飭雋ゑｽｲ隴溘・
            BedrockGunModel gunModel;
            Identifier gunTexture;
            Pair<BedrockGunModel, Identifier> lodModel = gunIndex.getLodModel();
            if (lodModel == null || RenderDistance.inRenderHighPolyModelDistance(poseStack)) {
                gunModel = gunIndex.getGunModel();
                gunTexture = gunIndex.getModelTexture();
            } else {
                gunModel = lodModel.getLeft();
                gunTexture = lodModel.getRight();
            }
            // 驕假ｽｻ陷会ｽｨ陋ｻ・ｰ隶難ｽ｡陜吝唱谺｡霓､・ｹ
            poseStack.translate(0.5, 2, 0.5);
            // 陷ｿ蟠趣ｽｽ・ｬ隶難ｽ｡陜吶・
            poseStack.scale(-1, -1, 1);
            // 陟守坩逡題楜螢ｻ・ｽ蜥ｲ・ｻ繝ｻ蝎ｪ陷ｿ菫ｶ蝗ｰ繝ｻ莠包ｽｽ蜥ｲ・ｧ・ｻ陷･譴ｧ髮ｷ髴難ｽｬ繝ｻ蠕｡・ｸ讎頑｡∬ｫ｡・ｬ驛幢ｽｩ隰ｾ・ｾ繝ｻ繝ｻ
            applyPositioningTransform(transformType, gunIndex.getTransform().getScale(), gunModel, poseStack);
            // 陟守坩逡・display 隰ｨ・ｰ隰撰ｽｮ闕ｳ・ｭ騾ｧ繝ｻ・ｼ・ｩ隰ｾ・ｾ
            applyScaleTransform(transformType, gunIndex.getTransform().getScale(), poseStack);
            // 雋ゑｽｲ隴滓瑳譽ｯ隴ｴ・ｰ隶難ｽ｡陜吶・
            RenderType renderType = RenderTypeCompat.entityCutout(gunTexture);
            gunModel.render(poseStack, stack, transformType, renderType, pPackedLight, pPackedOverlay);
        }, () -> {
            // 雎撰ｽ｡隴幄歓・ｿ蜷ｩ・ｸ・ｪ gunID繝ｻ譴ｧ・ｸ・ｲ隴溯ｬ趣ｽｸ・ｪ鬮槫揃・ｯ・ｯ隴壼頃・ｴ・ｨ隰螳｣繝ｻ陋ｻ・ｫ闔・ｺ
            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            VertexConsumer buffer = pBuffer.getBuffer(RenderTypeCompat.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
            SLOT_GUN_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        });
        poseStack.popPose();
    }

    private static void applyPositioningTransform(ItemDisplayContext transformType, TransformScale scale, BedrockGunModel model,
                                                  PoseStack poseStack) {
        switch (transformType) {
            case FIXED -> applyPositioningNodeTransform(model.getFixedOriginPath(), poseStack, scale.getFixed());
            case GROUND -> applyPositioningNodeTransform(model.getGroundOriginPath(), poseStack, scale.getGround());
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> applyPositioningNodeTransform(model.getThirdPersonHandOriginPath(), poseStack, scale.getThirdPerson());
        }
    }

    private static void applyScaleTransform(ItemDisplayContext transformType, TransformScale scale, PoseStack poseStack) {
        if (scale == null) {
            return;
        }
        Vector3f vector3f = null;
        switch (transformType) {
            case FIXED -> vector3f = scale.getFixed();
            case GROUND -> vector3f = scale.getGround();
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> vector3f = scale.getThirdPerson();
        }
        if (vector3f != null) {
            poseStack.translate(0, 1.5, 0);
            poseStack.scale(vector3f.x(), vector3f.y(), vector3f.z());
            poseStack.translate(0, -1.5, 0);
        }
    }

    private static void applyPositioningNodeTransform(List<BedrockPart> nodePath, PoseStack poseStack, Vector3f scale) {
        if (nodePath == null) {
            return;
        }
        if (scale == null) {
            scale = new Vector3f(1, 1, 1);
        }
        // 陟守坩逡題楜螢ｻ・ｽ蜥ｲ・ｻ繝ｻ蝎ｪ陷ｿ讎企ｫ・抄蜥ｲ・ｧ・ｻ邵ｲ竏ｵ髮ｷ髴難ｽｬ繝ｻ蠕｡・ｽ・ｿ陞ｳ螢ｻ・ｽ蜥ｲ・ｻ繝ｻ蝎ｪ闖ｴ蜥ｲ・ｽ・ｮ陝・ｽｱ隴擾ｽｯ雋ゑｽｲ隴溯ｬ趣ｽｸ・ｭ陟｢繝ｻ
        poseStack.translate(0, 1.5, 0);
        for (int i = nodePath.size() - 1; i >= 0; i--) {
            BedrockPart t = nodePath.get(i);
            poseStack.mulPose(Axis.XN.rotation(t.xRot));
            poseStack.mulPose(Axis.YN.rotation(t.yRot));
            poseStack.mulPose(Axis.ZN.rotation(t.zRot));
            if (t.getParent() != null) {
                poseStack.translate(-t.x * scale.x() / 16.0F, -t.y * scale.y() / 16.0F, -t.z * scale.z() / 16.0F);
            } else {
                poseStack.translate(-t.x * scale.x() / 16.0F, (1.5F - t.y / 16.0F) * scale.y(), -t.z * scale.z() / 16.0F);
            }
        }
        poseStack.translate(0, -1.5, 0);
    }
}


