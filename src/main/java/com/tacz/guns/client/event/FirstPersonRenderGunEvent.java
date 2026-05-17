package com.tacz.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.api.client.event.RenderLevelBobEvent;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.math.Easing;
import com.tacz.guns.util.math.MathUtil;
import com.tacz.guns.util.math.PerlinNoise;
import com.tacz.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 雍溯ｴ｣隨ｬ荳莠ｺ遘ｰ逧・棯譴ｰ讓｡蝙矩｢晏､匁譜譫懃噪貂ｲ譟薙ょ・莉夜Κ蛻・盾隗・{@link GunItemRendererWrapper}
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class FirstPersonRenderGunEvent {
    // 逕ｨ莠守函謌千桷蜃・勘菴懃噪霑仙勘譖ｲ郤ｿ・御ｽｿ蜉ｨ菴懃恚襍ｷ譚･譖ｴ蟷ｳ貊・
    private static final SecondOrderDynamics AIMING_DYNAMICS = new SecondOrderDynamics(1.2f, 1.2f, 0.5f, 0);
    private static SecondOrderDynamics SWITCH_VIEW_DYNAMICS;
    // 逕ｨ莠取遠蠑謾ｹ陬・阜髱｢譌ｶ譫ｪ譴ｰ霑仙勘逧・ｹｳ貊・
    private static final SecondOrderDynamics REFIT_OPENING_DYNAMICS = new SecondOrderDynamics(1f, 1.2f, 0.5f, 0);
    // 逕ｨ莠手ｷｳ霍・ｻｶ貊槫勘逕ｻ逧・ｹｳ貊・
    private static final SecondOrderDynamics JUMPING_DYNAMICS = new SecondOrderDynamics(0.28f, 1f, 0.65f, 0);
    private static final float JUMPING_Y_SWAY = -2f;
    private static final float JUMPING_SWAY_TIME = 0.3f;
    private static final float LANDING_SWAY_TIME = 0.15f;
    // 逕ｨ莠取棯譴ｰ蜷主ｺｧ逧・ｨ句ｺ丞勘逕ｻ
    private static final PerlinNoise SHOOT_X_SWAY_NOISE = new PerlinNoise(-0.2f, 0.2f, 400);
    private static final PerlinNoise SHOOT_Y_ROTATION_NOISE = new PerlinNoise(-0.0136f, 0.0136f, 100);
    private static final float SHOOT_Y_SWAY = -0.1f;
    private static final float SHOOT_ANIMATION_TIME = 0.3f;

    private static float jumpingSwayProgress = 0;
    private static boolean lastOnGround = false;
    private static long jumpingTimeStamp = -1;
    private static long shootTimeStamp = -1;
    private static Matrix4f oldAimingViewMatrix;
    private static float oldViewIndex;
    private static int currentViewIndex = -1;
    private static float lastAppliedAimingProgress = 0;

    /**
     * 蠖謎ｸｻ謇区響逹譫ｪ譴ｰ迚ｩ蜩∫噪譌ｶ蛟呻ｼ悟叙豸亥ｺ皮畑蝨ｨ螳・ｸ企擇逧・viewBobbing・御ｻ･萓ｿ蠎皮畑閾ｪ螳壻ｹ臥噪霍第ｭ･/襍ｰ霍ｯ蜉ｨ逕ｻ縲・
     */
    @SubscribeEvent
    public static boolean cancelItemInHandViewBobbing(RenderItemInHandBobEvent.BobView event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        ItemStack itemStack = KeepingItemRenderer.getRenderer().getCurrentItem();
        if (IGun.getIGunOrNull(itemStack) != null) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static boolean cancelLevelViewBobbing(RenderLevelBobEvent.BobView event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        ItemStack itemStack = KeepingItemRenderer.getRenderer().getCurrentItem();
        return IGun.getIGunOrNull(itemStack) != null;
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (event.getLogicalSide().isClient()) {
            LivingEntity shooter = event.getShooter();
            LocalPlayer player = Minecraft.getInstance().player;
            if (!shooter.equals(player)) {
                return;
            }
            ItemStack mainHandItem = player.getMainHandItem();
            IGun iGun = IGun.getIGunOrNull(mainHandItem);
            if (iGun == null) {
                return;
            }
            TimelessAPI.getClientGunIndex(iGun.getGunId(mainHandItem)).ifPresent(gunIndex -> {
                // 隶ｰ蠖募ｼ轣ｫ譌ｶ髣ｴ謌ｳ・檎畑莠主錘蝮仙鴨遞句ｺ丞勘逕ｻ
                shootTimeStamp = System.currentTimeMillis();
                // 隶ｰ蠖墓棯蜿｣轣ｫ辟ｰ謨ｰ謐ｮ
                MuzzleFlashRender.onShoot();
            });
        }
    }

    private static boolean bulletFromPlayer(Entity entity) {
        if (entity instanceof EntityKineticBullet entityBullet) {
            return entityBullet.getOwner() instanceof LocalPlayer;
        }
        return false;
    }

    public static void applyFirstPersonGunTransform(LocalPlayer player, ItemStack gunItemStack, PoseStack poseStack, BedrockGunModel model, float partialTicks) {
        // 驟榊粋霑仙勘譖ｲ郤ｿ・瑚ｮ｡邂玲隼陬・棯蜿｣逧・遠蠑霑帛ｺｦ
        float refitScreenOpeningProgress = REFIT_OPENING_DYNAMICS.update(RefitTransform.getOpeningProgress());
        // 驟榊粋霑仙勘譖ｲ郤ｿ・瑚ｮ｡邂礼桷蜃・ｿ帛ｺｦ
        float aimingProgress = AIMING_DYNAMICS.update(IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(partialTicks));
        lastAppliedAimingProgress = aimingProgress;
        // 蠎皮畑譫ｪ譴ｰ蜉ｨ諤・ｼ悟ｦょ錘蝮仙鴨縲∵戟譫ｪ霍ｳ霍・ｭ・
        applyGunMovements(model, aimingProgress, partialTicks);
        // 蠎皮畑蜷・ｧ肴槍蜒乗惻螳壻ｽ咲ｻ・噪蜿俶困・磯ｻ倩ｮ､謖∵棯縲∫桷蜃・∵隼陬・阜髱｢遲会ｼ・
        applyFirstPersonPositioningTransform(poseStack, model, gunItemStack, aimingProgress, refitScreenOpeningProgress);
        // 蠎皮畑蜉ｨ逕ｻ郤ｦ譚溷序謐｢
        applyAnimationConstraintTransform(poseStack, model, aimingProgress * (1 - refitScreenOpeningProgress));
    }

    public static float getLastAppliedAimingProgress() {
        return lastAppliedAimingProgress;
    }

    private static void applyGunMovements(BedrockGunModel model, float aimingProgress, float partialTicks) {
        applyShootSwayAndRotation(model, aimingProgress);
        applyJumpingSway(model, partialTicks);
    }

    /**
     * 蠎皮畑迸・・鞫・ワ譛ｺ螳壻ｽ咲ｻ・∵惻迸・槍蜒乗惻螳壻ｽ咲ｻ・柱 Idle 鞫・ワ譛ｺ螳壻ｽ咲ｻ・噪蜿俶困縲ゆｼ壼惠蜃荳ｪ鞫・ワ譛ｺ螳壻ｽ堺ｹ矩龍謠貞ｼ縲・
     */
    private static void applyFirstPersonPositioningTransform(PoseStack poseStack, BedrockGunModel model, ItemStack stack, float aimingProgress, float refitScreenOpeningProgress) {
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun == null) {
            return;
        }
        Matrix4f transformMatrix = new Matrix4f();
        transformMatrix.identity();
        // 蠎皮畑迸・㊥螳壻ｽ・
        List<BedrockPart> idleNodePath = model.getIdleSightPath();
        List<BedrockPart> aimingNodePath = null;
        Identifier scopeId = iGun.getAttachmentId(stack, AttachmentType.SCOPE);
        if (scopeId.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
            scopeId = iGun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        }
        CompoundTag scopeTag = iGun.getAttachmentTag(stack, AttachmentType.SCOPE);
        int zoomNumber = AttachmentItemDataAccessor.getZoomNumberFromTag(scopeTag);
        int viewIndex = 1;
        if (DefaultAssets.isEmptyAttachmentId(scopeId)) {
            // 譛ｪ螳芽｣・桷蜈ｷ・御ｽｿ逕ｨ譛ｺ迸・ｮ壻ｽ咲ｻ・
            aimingNodePath = model.getIronSightPath();
        } else {
            // 螳芽｣・桷蜈ｷ・檎ｻ・粋迸・・螳壻ｽ咲ｻ・柱迸・・隗・㍽螳壻ｽ咲ｻ・
            List<BedrockPart> scopeNodePath = model.getScopePosPath();
            if (scopeNodePath != null) {
                aimingNodePath = new ArrayList<>(scopeNodePath);
                Optional<ClientAttachmentIndex> indexOptional = TimelessAPI.getClientAttachmentIndex(scopeId);
                if (indexOptional.isPresent()) {
                    BedrockAttachmentModel attachmentModel = indexOptional.get().getAttachmentModel();
                    int[] views = indexOptional.get().getViews();
                    viewIndex = views[zoomNumber % views.length] - 1;
                    if (attachmentModel != null) {
                        List<BedrockPart> scopeViewPath = attachmentModel.getScopeViewPath(currentViewIndex == -1 ? viewIndex : currentViewIndex);
                        if (scopeViewPath != null) {
                            aimingNodePath.addAll(scopeViewPath);
                        }
                    }
                }
            }
        }
        Matrix4f aimingViewMatrix = getPositioningNodeInverse(aimingNodePath);
        // 謇ｧ陦御ｸ､荳ｪ scope view 荵矩龍逧・薯蛟ｼ
        if (currentViewIndex == -1) {
            currentViewIndex = viewIndex;
            oldViewIndex = viewIndex;
            oldAimingViewMatrix = aimingViewMatrix;
            SWITCH_VIEW_DYNAMICS = new SecondOrderDynamics(0.35f, 1.2f, 0.3f, viewIndex);
        }
        float view_interpret = SWITCH_VIEW_DYNAMICS.update(viewIndex);
        float span = currentViewIndex - oldViewIndex;
        float switchingProgress = Math.abs(span) < 0.05 ? 1 : (view_interpret - oldViewIndex) / span;
        MathUtil.applyMatrixLerp(aimingViewMatrix, oldAimingViewMatrix, aimingViewMatrix, 1 - switchingProgress);
        if (currentViewIndex != viewIndex) {
            oldAimingViewMatrix = aimingViewMatrix;
            oldViewIndex = view_interpret;
            currentViewIndex = viewIndex;
        }
        // 蠎皮畑迸・㊥蜿俶困
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(idleNodePath), transformMatrix, (1 - refitScreenOpeningProgress));
        MathUtil.applyMatrixLerp(transformMatrix, aimingViewMatrix, transformMatrix, (1 - refitScreenOpeningProgress) * aimingProgress);
        // 蠎皮畑謾ｹ陬・阜髱｢蠑蜷ｯ譌ｶ逧・ｮ壻ｽ・
        float refitTransformProgress = (float) Easing.easeOutCubic(RefitTransform.getTransformProgress());
        AttachmentType oldType = RefitTransform.getOldTransformType();
        AttachmentType currentType = RefitTransform.getCurrentTransformType();
        List<BedrockPart> fromNode = model.getRefitAttachmentViewPath(oldType);
        List<BedrockPart> toNode = model.getRefitAttachmentViewPath(currentType);
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(fromNode), transformMatrix, refitScreenOpeningProgress);
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(toNode), transformMatrix, refitScreenOpeningProgress * refitTransformProgress);
        // 蠎皮畑蜿俶困蛻ｰ PoseStack
        poseStack.translate(0, 1.5f, 0);
        poseStack.mulPose(transformMatrix);
        poseStack.translate(0, -1.5f, 0);
    }

    /**
     * 闔ｷ蜿匁槍蜒乗惻螳壻ｽ咲ｻ・噪蜿咲嶌遏ｩ髦ｵ
     */
    @Nonnull
    private static Matrix4f getPositioningNodeInverse(List<BedrockPart> nodePath) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.identity();
        if (nodePath != null) {
            for (int i = nodePath.size() - 1; i >= 0; i--) {
                BedrockPart part = nodePath.get(i);
                // 隶｡邂怜渚蜷醍噪譌玖ｽｬ
                matrix4f.rotate(Axis.XN.rotation(part.xRot));
                matrix4f.rotate(Axis.YN.rotation(part.yRot));
                matrix4f.rotate(Axis.ZN.rotation(part.zRot));
                // 隶｡邂怜渚蜷醍噪菴咲ｧｻ
                if (part.getParent() != null) {
                    matrix4f.translate(-part.x / 16.0F, -part.y / 16.0F, -part.z / 16.0F);
                } else {
                    matrix4f.translate(-part.x / 16.0F, (1.5F - part.y / 16.0F), -part.z / 16.0F);
                }
            }
        }
        return matrix4f;
    }

    private static void applyShootSwayAndRotation(BedrockGunModel model, float aimingProgress) {
        BedrockPart rootNode = model.getRootNode();
        if (rootNode != null) {
            float progress = 1 - (System.currentTimeMillis() - shootTimeStamp) / (SHOOT_ANIMATION_TIME * 1000);
            if (progress < 0) {
                progress = 0;
            }
            progress = (float) Easing.easeOutCubic(progress);
            float xSway = SHOOT_X_SWAY_NOISE.getValue();
            float yRot = SHOOT_Y_ROTATION_NOISE.getValue();
            float horizontalShootWeight = aimingProgress > 0.7F ? 0.0F : 1.0F - aimingProgress;
            rootNode.offsetX += xSway / 16 * progress * horizontalShootWeight;
            rootNode.offsetY += -SHOOT_Y_SWAY / 16 * progress * (1 - aimingProgress);
            // 蝓ｺ蟯ｩ迚域ｨ｡蝙・y 霓ｴ荳贋ｸ矩｢蛟抵ｼ茎way 蛟ｼ蜿也嶌蜿肴焚
            rootNode.additionalQuaternion.mul(Axis.YP.rotation(yRot * progress * (1 - aimingProgress)));
        }
    }

    private static void applyJumpingSway(BedrockGunModel model, float partialTicks) {
        if (jumpingTimeStamp == -1) {
            jumpingTimeStamp = System.currentTimeMillis();
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            double posY = Mth.lerp(partialTicks, Minecraft.getInstance().player.yOld, Minecraft.getInstance().player.getY());
            float velocityY = (float) (posY - Minecraft.getInstance().player.yOld) / partialTicks;
            if (player.onGround()) {
                if (!lastOnGround) {
                    jumpingSwayProgress = velocityY / -0.1f;
                    if (jumpingSwayProgress > 1) {
                        jumpingSwayProgress = 1;
                    }
                    lastOnGround = true;
                } else {
                    jumpingSwayProgress -= (System.currentTimeMillis() - jumpingTimeStamp) / (LANDING_SWAY_TIME * 1000);
                    if (jumpingSwayProgress < 0) {
                        jumpingSwayProgress = 0;
                    }
                }
            } else {
                if (lastOnGround) {
                    // 0.42 譏ｯ邇ｩ螳ｶ閾ｪ辟ｶ襍ｷ霍ｳ逧・溷ｺｦ
                    jumpingSwayProgress = velocityY / 0.42f;
                    if (jumpingSwayProgress > 1) {
                        jumpingSwayProgress = 1;
                    }
                    lastOnGround = false;
                } else {
                    jumpingSwayProgress -= (System.currentTimeMillis() - jumpingTimeStamp) / (JUMPING_SWAY_TIME * 1000);
                    if (jumpingSwayProgress < 0) {
                        jumpingSwayProgress = 0;
                    }
                }
            }
        }
        jumpingTimeStamp = System.currentTimeMillis();
        float ySway = JUMPING_DYNAMICS.update(JUMPING_Y_SWAY * jumpingSwayProgress);
        BedrockPart rootNode = model.getRootNode();
        if (rootNode != null) {
            // 蝓ｺ蟯ｩ迚域ｨ｡蝙・y 霓ｴ荳贋ｸ矩｢蛟抵ｼ茎way 蛟ｼ蜿也嶌蜿肴焚
            rootNode.offsetY += -ySway / 16;
        }
    }

    /**
     * 闔ｷ蜿門勘逕ｻ郤ｦ譚溽せ逧・序謐｢謨ｰ謐ｮ縲・
     *
     * @param originTranslation   逕ｨ莠手ｾ灘・郤ｦ譚溽せ逧・次蝮先・
     * @param animatedTranslation 逕ｨ莠手ｾ灘・郤ｦ譚溽せ扈剰ｿ・勘逕ｻ蜿俶困荵句錘逧・攝譬・
     * @param rotation            逕ｨ莠手ｾ灘・郤ｦ譚溽せ逧・雷霓ｬ
     */
    private static void getAnimationConstraintTransform(List<BedrockPart> nodePath, @Nonnull Vector3f originTranslation, @Nonnull Vector3f animatedTranslation, @Nonnull Vector3f rotation) {
        if (nodePath == null) {
            return;
        }
        // 郤ｦ譚溽せ蜉ｨ逕ｻ蜿俶困遏ｩ髦ｵ
        Matrix4f animeMatrix = new Matrix4f();
        // 郤ｦ譚溽せ蛻晏ｧ句序謐｢遏ｩ髦ｵ
        Matrix4f originMatrix = new Matrix4f();
        animeMatrix.identity();
        originMatrix.identity();
        BedrockPart constrainNode = nodePath.get(nodePath.size() - 1);
        for (BedrockPart part : nodePath) {
            // 荵伜勘逕ｻ菴咲ｧｻ
            if (part != constrainNode) {
                animeMatrix.translate(part.offsetX, part.offsetY, part.offsetZ);
            }
            // 荵倡ｻ・ｽ咲ｧｻ
            if (part.getParent() != null) {
                animeMatrix.translate(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
            } else {
                animeMatrix.translate(part.x / 16.0F, (part.y / 16.0F - 1.5F), part.z / 16.0F);
            }
            // 荵伜勘逕ｻ譌玖ｽｬ
            if (part != constrainNode) {
                animeMatrix.rotate(part.additionalQuaternion);
            }
            // 荵倡ｻ・雷霓ｬ
            animeMatrix.rotate(Axis.ZP.rotation(part.zRot));
            animeMatrix.rotate(Axis.YP.rotation(part.yRot));
            animeMatrix.rotate(Axis.XP.rotation(part.xRot));

            // 荵倡ｻ・ｽ咲ｧｻ
            if (part.getParent() != null) {
                originMatrix.translate(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
            } else {
                originMatrix.translate(part.x / 16.0F, (part.y / 16.0F - 1.5F), part.z / 16.0F);
            }
            // 荵倡ｻ・雷霓ｬ
            originMatrix.rotate(Axis.ZP.rotation(part.zRot));
            originMatrix.rotate(Axis.YP.rotation(part.yRot));
            originMatrix.rotate(Axis.XP.rotation(part.xRot));

        }
        // 謚雁序謐｢謨ｰ謐ｮ蜀吝・霎灘・
        animeMatrix.getTranslation(animatedTranslation);
        originMatrix.getTranslation(originTranslation);
        Vector3f animatedRotation = MathUtil.getEulerAngles(animeMatrix);
        Vector3f originRotation = MathUtil.getEulerAngles(originMatrix);
        animatedRotation.sub(originRotation);
        rotation.set(animatedRotation.x(), animatedRotation.y(), animatedRotation.z());
    }

    /**
     * 蠎皮畑蜉ｨ逕ｻ郤ｦ譚溷序謐｢縲・
     *
     * @param weight 謗ｧ蛻ｶ郤ｦ譚溷序謐｢逧・揀驥搾ｼ檎畑莠取薯蛟ｼ縲・
     */
    public static void applyAnimationConstraintTransform(PoseStack poseStack, BedrockGunModel gunModel, float weight) {
        List<BedrockPart> nodePath = gunModel.getConstraintPath();
        if (nodePath == null) {
            return;
        }
        if (gunModel.getConstraintObject() == null) {
            return;
        }
        // 闔ｷ蜿門勘逕ｻ郤ｦ譚溽せ逧・序謐｢菫｡諱ｯ
        Vector3f originTranslation = new Vector3f();
        Vector3f animatedTranslation = new Vector3f();
        Vector3f rotation = new Vector3f();
        Vector3f translationICA = gunModel.getConstraintObject().translationConstraint;
        Vector3f rotationICA = gunModel.getConstraintObject().rotationConstraint;
        getAnimationConstraintTransform(nodePath, originTranslation, animatedTranslation, rotation);
        // 驟榊粋郤ｦ譚溽ｳｻ謨ｰ・瑚ｮ｡邂礼ｺｦ譚滉ｽ咲ｧｻ髴隕∫噪蜿榊髄菴咲ｧｻ
        Vector3f inverseTranslation = new Vector3f(originTranslation);
        inverseTranslation.sub(animatedTranslation);
        boolean useCameraLocalTranslation = CameraSetupEvent.isVisualRecoilActive() && lastAppliedAimingProgress > 0.7F;
        if (!useCameraLocalTranslation) {
            inverseTranslation.mulDirection(poseStack.last().pose());
        }
        inverseTranslation.mul(translationICA.x() - 1, translationICA.y() - 1, 1 - translationICA.z()); // 蝓ｺ蟯ｩ迚域ｨ｡蝙狗噪譌玖ｽｬ蟇ｼ閾ｴ xy 霓ｴ隕∝渚霑・擂
        // 隶｡邂礼ｺｦ譚滓雷霓ｬ髴隕∫噪蜿榊髄譌玖ｽｬ縲ょ屏髴隕∵薯蛟ｼ・瑚執蜿也噪譏ｯ谺ｧ諡芽ｧ・
        Vector3f inverseRotation = new Vector3f(rotation);
        inverseRotation.mul(rotationICA.x() - 1, rotationICA.y() - 1, rotationICA.z() - 1);
        // 郤ｦ譚滓雷霓ｬ
        poseStack.translate(animatedTranslation.x(), animatedTranslation.y() + 1.5f, animatedTranslation.z());
        poseStack.mulPose(Axis.XP.rotation(inverseRotation.x() * weight));
        poseStack.mulPose(Axis.YP.rotation(inverseRotation.y() * weight));
        poseStack.mulPose(Axis.ZP.rotation(inverseRotation.z() * weight));
        poseStack.translate(-animatedTranslation.x(), -animatedTranslation.y() - 1.5f, -animatedTranslation.z());
        // 郤ｦ譚滉ｽ咲ｧｻ
        if (useCameraLocalTranslation) {
            poseStack.translate(-inverseTranslation.x() * weight, -inverseTranslation.y() * weight, inverseTranslation.z() * weight);
        } else {
            Matrix4f poseMatrix = poseStack.last().pose();
            poseMatrix.m30(poseMatrix.m30() - inverseTranslation.x() * weight);
            poseMatrix.m31(poseMatrix.m31() - inverseTranslation.y() * weight);
            poseMatrix.m32(poseMatrix.m32() + inverseTranslation.z() * weight);
        }
    }
}
