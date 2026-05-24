package com.tacz.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.client.resource.index.ClientAmmoIndex;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.tacz.guns.util.RenderTypeCompat;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;

public class EntityBulletRenderer extends EntityRenderer<EntityKineticBullet, EntityBulletRenderer.RenderState> {
    private static final float FIRST_PERSON_MUZZLE_DEPTH_SCALE = 0.0F;
    private static final double FIRST_PERSON_MIN_TRAIL_LENGTH = 0.12D;
    private static final RenderType BULLET_TRACER_RENDER_TYPE = RenderTypeCompat.energySwirl(InternalAssetLoader.DEFAULT_BULLET_TEXTURE, 15, 15);

    public EntityBulletRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(EntityKineticBullet entity, RenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.bullet = entity;
        state.partialTicks = partialTicks;
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        if (state.bullet == null) {
            return;
        }
        renderBullet(state.bullet, state.partialTicks, poseStack, state.lightCoords);
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.DEFAULT_BULLET_MODEL);
    }

    private void renderBullet(EntityKineticBullet bullet, float partialTicks, PoseStack poseStack, int packedLight) {
        Identifier gunId = bullet.getGunId();
        Identifier gunDisplayId = bullet.getGunDisplayId();
        Optional<GunDisplayInstance> displayOptional = TimelessAPI.getGunDisplay(gunDisplayId, gunId);
        if (displayOptional.isEmpty()) {
            return;
        }
        GunDisplayInstance display = displayOptional.get();
        float @Nullable [] tracerColor = bullet.getTracerColorOverride().orElse(display.getTracerColor());
        Entity shooter = bullet.getOwner();
        boolean isFirstPerson = this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && shooter instanceof LocalPlayer;
        Vec3 bulletPosition = bullet.getPosition(partialTicks);
        Vec3 firstPersonRenderOrigin = isFirstPerson ? Minecraft.getInstance().gameRenderer.getMainCamera().position() : null;
        boolean useFirstPersonPrediction = isFirstPerson && !bullet.isExplosionBullet()
                && bullet.hasClientFirstPersonRenderPrediction();
        Vec3 renderPosition = useFirstPersonPrediction
                ? bullet.getClientFirstPersonRenderPosition(firstPersonRenderOrigin, partialTicks)
                : bulletPosition;
        Vec3 renderOffset = useFirstPersonPrediction ? renderPosition.subtract(bulletPosition) : Vec3.ZERO;
        double disToEye = isFirstPerson ? renderPosition.distanceTo(firstPersonRenderOrigin) : shooter == null ? 0 : renderPosition.distanceTo(shooter.getEyePosition(partialTicks));
        Identifier ammoId = bullet.getAmmoId();
        Optional<ClientAmmoIndex> ammoIndexOptional = TimelessAPI.getClientAmmoIndex(ammoId);
        if (ammoIndexOptional.isEmpty()) {
            return;
        }
        ClientAmmoIndex ammoIndex = ammoIndexOptional.get();
        BedrockAmmoModel ammoEntityModel = ammoIndex.getAmmoEntityModel();
        Identifier textureLocation = ammoIndex.getAmmoEntityTextureLocation();
        if (ammoEntityModel != null && textureLocation != null) {
            poseStack.pushPose();
            poseStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
            poseStack.translate(0, 1.5, 0);
            poseStack.scale(-1, -1, 1);
            ammoEntityModel.render(poseStack, ItemDisplayContext.GROUND, RenderTypeCompat.entityTranslucentCull(textureLocation), packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        if (bullet.isTracerAmmo()) {
            float[] actualTracerColor = tracerColor != null ? tracerColor : ammoIndex.getTracerColor();
            renderTracerAmmo(bullet, actualTracerColor, partialTicks, poseStack, packedLight, shooter, isFirstPerson, disToEye, renderOffset);
        }
    }

    public void renderTracerAmmo(EntityKineticBullet bullet, float[] tracerColor, float partialTicks, PoseStack poseStack, int packedLight,
                                 @Nullable Entity shooter, boolean isFirstPerson, double disToEye, Vec3 renderOffset) {
        if (shooter == null) {
            return;
        }
        if (isFirstPerson && !RenderConfig.FIRST_PERSON_BULLET_TRACER_ENABLE.get()) {
            return;
        }
        Optional<BedrockModel> modelOptional = getModel();
        if (modelOptional.isEmpty()) {
            return;
        }
        BedrockModel model = modelOptional.get();
        poseStack.pushPose();
        {
            poseStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
            float width = 0.005f;
            double trailLength = 0.85 * bullet.getDeltaMovement().length();
            trailLength = Math.min(trailLength, disToEye * 0.8);
            if (isFirstPerson) {
                trailLength = Math.max(trailLength, FIRST_PERSON_MIN_TRAIL_LENGTH);
            }

            if (isFirstPerson) {
                applyFirstPersonOffsetTransform(bullet, poseStack, disToEye);
            }
            width *= bullet.getTracerSizeOverride();
            width *= (float) Math.max(1.0, disToEye / 3.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
            poseStack.translate(0, isFirstPerson ? 0 : -0.2, trailLength / 2.0);
            poseStack.scale(width, width, (float) trailLength);
            if (bullet.tickCount >= 5 || disToEye > 2) {
                model.render(poseStack, ItemDisplayContext.NONE, BULLET_TRACER_RENDER_TYPE, packedLight, OverlayTexture.NO_OVERLAY,
                        tracerColor[0], tracerColor[1], tracerColor[2], 1);
            }
        }
        poseStack.popPose();
    }

    private static void applyFirstPersonOffsetTransform(EntityKineticBullet bullet, PoseStack poseStack, double disToEye) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f capturedOffset = bullet.getFirstPersonRenderOffset();
        Vector3f offset;
        if (isUsableFirstPersonOffset(capturedOffset)) {
            offset = capturedOffset;
        } else {
            offset = new Vector3f(GunItemRendererWrapper.muzzleRenderOffset);
            bullet.setFirstPersonRenderOffset(offset);
            bullet.setCameraXRot(camera.xRot());
            bullet.setCameraYRot(camera.yRot());
        }
        double offsetReducer = Math.max(0, (50 - disToEye)) / 50;
        poseStack.mulPose(Axis.YN.rotationDegrees(bullet.getCameraYRot() + 180.0F));
        poseStack.mulPose(Axis.XN.rotationDegrees(bullet.getCameraXRot()));
        poseStack.translate(offset.x * offsetReducer, offset.y * offsetReducer, -offset.z * FIRST_PERSON_MUZZLE_DEPTH_SCALE * offsetReducer);
        poseStack.mulPose(Axis.XP.rotationDegrees(bullet.getCameraXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(bullet.getCameraYRot() + 180.0F));
    }

    private static boolean isUsableFirstPersonOffset(@Nullable Vector3f offset) {
        return offset != null
                && Float.isFinite(offset.x)
                && Float.isFinite(offset.y)
                && Float.isFinite(offset.z)
                && offset.lengthSquared() > 1.0E-6F;
    }

    @Override
    protected int getBlockLightLevel(@NotNull EntityKineticBullet entityBullet, @NotNull BlockPos blockPos) {
        return 15;
    }

    @Override
    public boolean shouldRender(EntityKineticBullet bullet, Frustum camera, double pCamX, double pCamY, double pCamZ) {
        AABB aabb = bullet.getBoundingBox().inflate(0.5);
        if (aabb.hasNaN() || aabb.getSize() == 0) {
            aabb = new AABB(bullet.getX() - 2.0, bullet.getY() - 2.0, bullet.getZ() - 2.0, bullet.getX() + 2.0, bullet.getY() + 2.0, bullet.getZ() + 2.0);
        }
        return camera.isVisible(aabb);
    }

    public static class RenderState extends EntityRenderState {
        public EntityKineticBullet bullet;
        public float partialTicks;
    }
}
