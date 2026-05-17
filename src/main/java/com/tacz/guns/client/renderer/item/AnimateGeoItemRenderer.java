package com.tacz.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.animation.statemachine.ItemAnimationStateContext;
import com.tacz.guns.client.model.BedrockAnimatedModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.tacz.guns.client.renderer.compat.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.tacz.guns.util.RenderTypeCompat;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * 隰夲ｽｽ髮趣ｽ｡騾ｧ繝ｻ貂戊浣・ｩ霑壻ｺ･蜍倬包ｽｻ霑夲ｽｩ陷ｩ竏ｵ・ｨ・｡陜呎ｯ・WLR繝ｻ謔滓｡∬惺・ｫ闕ｳ闔蟷・ｽｻ蛟ｩ・ｮ・､陞ｳ讓帝ｴｫ
 * @param <M> 陜難ｽｺ陝ｯ・ｩ霑壼沺・ｨ・｡陜吶・
 * @param <CTX> 陷会ｽｨ騾包ｽｻ霑･・ｶ隲､竏ｵ諠ｻ闕ｳ雍具ｽｸ蛹ｺ譫・
 */
public abstract class AnimateGeoItemRenderer<M extends BedrockAnimatedModel, CTX extends ItemAnimationStateContext>
        extends BlockEntityWithoutLevelRenderer {
    @Nullable
    protected LuaAnimationStateMachine<CTX> stateMachine;
    protected M model;
    public Identifier textureLocation;

    public AnimateGeoItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public void setModel(M model) {
        this.model = model;
    }

    public M getModel(ItemStack stack) {
        return model;
    }

    @Nullable
    public LuaAnimationStateMachine<CTX> getStateMachine(ItemStack stack) {
        return stateMachine;
    }

    public Identifier getTextureLocation(ItemStack stack) {
        return textureLocation;
    }

    public RenderType getRenderType(ItemStack stack) {
        return RenderTypeCompat.entityCutout(getTextureLocation(stack));
    }

    public boolean needReInit(ItemStack stack) {
        var stateMachine = getStateMachine(stack);
        if (stateMachine == null) {
            return false;
        }
        return !stateMachine.isInitialized() && stateMachine.getExitingTime() < System.currentTimeMillis();
    }

    public abstract CTX initContext(ItemStack stack, Player player, float partialTick);

    public abstract void updateContext(CTX context, ItemStack stack, Player player, float partialTick);

    /** 髫ｶ・｡驍よ懶ｽｹ・ｶ髴第ｳ悟ｱ楢崕繝ｻ繝ｻ陷会ｽｨ騾包ｽｻ騾ｧ繝ｻ諷ｮ鬮滂ｽｿ繝ｻ謔滄ｻ定抄閧ｯs
     * @return 闖ｫ譎・亜隴鯉ｽｶ鬮｣・ｴ
     */
    public long getPutAwayTime(ItemStack stack) {
        return 0;
    }

    /**
     * 陝・凵・ｯ蜍溘・陝句唱蝟ｧ霑･・ｶ隲､竏ｵ諠ｻ陝ｷ・ｶ髫暦ｽｦ陷ｿ螟ｧ繝ｻ陷茨ｽ･闖ｫ・｡陷ｿ・ｷ
     */
    public void tryInit(ItemStack stack, Player player, float partialTick) {
        var stateMachine = getStateMachine(stack);
        if (stateMachine == null) {
            return;
        }
        if (stateMachine.isInitialized()) {
            stateMachine.exit();
        }

        stateMachine.setContext(initContext(stack, player, partialTick));
        stateMachine.initialize();

        stateMachine.trigger(GunAnimationConstant.INPUT_DRAW);
    }

    /**
     * 陝・凵・ｯ證ｮ陷・ｽｺ霑･・ｶ隲､竏ｵ諠ｻ陝ｷ・ｶ髫暦ｽｦ陷ｿ螟ｧ繝ｻ陷・ｽｺ闖ｫ・｡陷ｿ・ｷ
     */
    public void tryExit(ItemStack stack, long putAwayTime) {
        var stateMachine = getStateMachine(stack);
        if (stateMachine == null) {
            return;
        }
        stateMachine.processContextIfExist(context -> {
            context.setPutAwayTime(putAwayTime / 1000F);
        });
        if(stateMachine.isInitialized()) {
            stateMachine.trigger(GunAnimationConstant.INPUT_PUT_AWAY);
            KeepingItemRenderer.getRenderer().keep(stack, putAwayTime);
            stateMachine.exit();
            // 鬮ｴ髫補握・ｮ・ｾ驗ゑｽｮ騾ｧ繝ｻ・ｯ豕悟鋸騾包ｽｻ驕槫涵譟・滋蟷｢・ｼ遒≫茜陷郁ざﾑ崎棔荵溷飭鬩･讎翫・陝句唱蝟ｧ繝ｻ莠･蠎・妙・ｽ隴擾ｽｯ闕ｳ・｢驍奇ｽｾ陟趣ｽｦ闔繝ｻ・ｼ繝ｻ
            // 陝抵ｽｶ陷ｷ諠ｹ・ｸtick陟守｢托ｽｯ・･陜難ｽｺ隴幢ｽｬ雎撰ｽ｡隴帷判笏驕擾ｽ･繝ｻ繝ｻ
            stateMachine.setExitingTime(putAwayTime + 50);
        }
    }

    /**
     * 陝・凵・ｯ謌奇ｽｧ・ｦ陷ｿ驢肴・隲､竏ｵ諠ｻ髴難ｽｬ驕假ｽｻ
     * @param input 髴守§繝ｻ闖ｫ・｡陷ｿ・ｷ
     */
    public void triggerAnimation(ItemStack stack, String input) {
        var stateMachine = getStateMachine(stack);
        if (stateMachine == null) {
            return;
        }
        stateMachine.trigger(input);
    }

    /**
     * 隴厄ｽｴ隴・ｽｰ霑･・ｶ隲､竏ｵ諠ｻ闖ｴ繝ｻ蠑崎叉蟠趣ｽｿ蟷・ｽ｡譴ｧ・ｨ・｡陜吝唱繝ｻ陷茨ｽ･繝ｻ讙守舞闔蜿也惻隰ｾ・ｾ鬮ｻ・ｳ隰ｨ繝ｻ
     */
    public void visualUpdate(ItemStack stack) {
        var stateMachine = getStateMachine(stack);
        if (stateMachine == null) {
            return;
        }
        stateMachine.visualUpdate();
    }

    /**
     * 陟守坩逡題ｿ･・ｶ隲､竏ｵ諠ｻ騾ｧ繝ｻ・ｸ荵滄・髷ｫ繝ｻ繝ｯ隴幢ｽｺ陷会ｽｨ騾包ｽｻ繝ｻ譴ｧ蝌ｯ隴鯉ｽｶ陷ｿ・ｪ騾包ｽｨ闔螳郁・陞ｳ・ｶ
     */
    public void applyLevelCameraAnimation(ViewportEvent.ComputeCameraAngles event, ItemStack stack, LocalPlayer player) {
        this.applyLevelCameraAnimation(event, stack, 1);
    }

    public void applyLevelCameraAnimation(ViewportEvent.ComputeCameraAngles event, ItemStack stack, float multiplier) {
        var model = getModel(stack);
        if (model == null) {
            return;
        }
        Quaternionf q = MathUtil.multiplyQuaternion(model.getCameraAnimationObject().rotationQuaternion, multiplier);
        double yaw = Math.asin(2 * (q.w() * q.y() - q.x() * q.z()));
        double pitch = Math.atan2(2 * (q.w() * q.x() + q.y() * q.z()), 1 - 2 * (q.x() * q.x() + q.y() * q.y()));
        double roll = Math.atan2(2 * (q.w() * q.z() + q.x() * q.y()), 1 - 2 * (q.y() * q.y() + q.z() * q.z()));
        yaw = Math.toDegrees(yaw);
        pitch = Math.toDegrees(pitch);
        roll = Math.toDegrees(roll);
        event.setYaw((float) yaw + event.getYaw());
        event.setPitch((float) pitch + event.getPitch());
        event.setRoll((float) roll + event.getRoll());
    }

    /**
     * 陟守坩逡題ｿ･・ｶ隲､竏ｵ諠ｻ騾ｧ繝ｻ辟碑ｬ問悪鮟・惓竏ｵ讒崎恍荵玲・陷会ｽｨ騾包ｽｻ繝ｻ譴ｧ蝌ｯ隴鯉ｽｶ陷ｿ・ｪ騾包ｽｨ闔螳郁・陞ｳ・ｶ
     */
    public void applyItemInHandCameraAnimation(BeforeRenderHandEvent event, ItemStack stack, LocalPlayer player) {
        applyItemInHandCameraAnimation(event, stack, 1);
    }

    public void applyItemInHandCameraAnimation(BeforeRenderHandEvent event, ItemStack stack, float multiplier) {
        var model = getModel(stack);
        if (model == null) {
            return;
        }
        Quaternionf quaternion = MathUtil.multiplyQuaternion(model.getCameraAnimationObject().rotationQuaternion, multiplier);
        PoseStack poseStack = event.getPoseStack();
        poseStack.mulPose(quaternion);
    }

    /**
     * 隰・ｽｧ髯ｦ遒・ｽ｢譎擾ｽ､荵溷飭陷ｿ菫ｶ蝗ｰ
     */
    public void doExtraTransforms(PoseStack poseStack, M model, ItemStack stack) {
        applyFirstPersonPositioningTransform(poseStack, model, stack);
    }

    /**
     * 雋ゑｽｲ隴滄豪・ｬ・ｬ闕ｳ闔・ｺ驕假ｽｰ繝ｻ譴ｧ蝌ｯ隴鯉ｽｶ陷ｿ・ｪ騾包ｽｨ闔螳郁・陞ｳ・ｶ繝ｻ謔溘・陷ｿ・｣陷ｿ繧奇ｽｧ繝ｻ{@link com.tacz.guns.client.event.FirstPersonRenderEvent}
     */
    public void renderFirstPerson(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int light, float partialTick) {
        M model = getModel(stack);
        if (model != null) {
            poseStack.pushPose();
            float xRotOffset = Mth.lerp(partialTick, player.xBobO, player.xBob);
            float yRotOffset = Mth.lerp(partialTick, player.yBobO, player.yBob);
            float xRot = player.getViewXRot(partialTick) - xRotOffset;
            float yRot = player.getViewYRot(partialTick) - yRotOffset;
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot * -0.1F));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot * -0.1F));
            BedrockPart rootNode = model.getRootNode();
            if (rootNode != null) {
                xRot = (float) Math.tanh(xRot / 25) * 25;
                yRot = (float) Math.tanh(yRot / 25) * 25;
                rootNode.offsetX += yRot * 0.1F / 16F / 3F;
                rootNode.offsetY += -xRot * 0.1F / 16F / 3F;
                rootNode.additionalQuaternion.mul(Axis.XP.rotationDegrees(xRot * 0.05F));
                rootNode.additionalQuaternion.mul(Axis.YP.rotationDegrees(yRot * 0.05F));
            }

            // 闔牙叙・ｸ・ｲ隴溽§谺｡霓､・ｹ (0, 24, 0) 驕假ｽｻ陷会ｽｨ陋ｻ・ｰ隶難ｽ｡陜吝唱谺｡霓､・ｹ (0, 0, 0)
            poseStack.translate(0, 1.5f, 0);
            // 陜難ｽｺ陝ｯ・ｩ霑壼沺・ｨ・｡陜吝玄蠑崎叉雍具ｽｸ遏ｩ・｢・ｰ陋溷､蝎ｪ繝ｻ遒∵咎囎竏ｫ・ｿ・ｻ髴難ｽｬ髴代・謫らｸｲ繝ｻ
            poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
            doExtraTransforms(poseStack, model, stack);

            var stateMachine = getStateMachine(stack);
            if (stateMachine != null) {
                stateMachine.processContextIfExist(context -> {
                    updateContext(context, stack, player, partialTick);
                });
                stateMachine.update();
            }

            model.render(poseStack, ctx, getRenderType(stack), light, OverlayTexture.NO_OVERLAY);

            // 雋ゑｽｲ隴滄豪・ｻ謐ｺ謫夊惺蜿厄ｽｸ繝ｻ蜍∬怏・ｨ騾包ｽｻ陷ｿ菫ｶ蝗ｰ
            model.cleanAnimationTransform();
            poseStack.popPose();
        }
    }

    @ParametersAreNonnullByDefault
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                             int light, int overlay) {
        if (ctx.firstPerson()) return;
        M model = getModel(stack);
        if (model != null) {
            poseStack.pushPose();
            // 闔牙叙・ｸ・ｲ隴溽§谺｡霓､・ｹ (0, 24, 0) 驕假ｽｻ陷会ｽｨ陋ｻ・ｰ隶難ｽ｡陜吝唱谺｡霓､・ｹ (0, 0, 0)
            poseStack.translate(0.5, 1.5f, 0.5);
            // 陜難ｽｺ陝ｯ・ｩ霑壼沺・ｨ・｡陜吝玄蠑崎叉雍具ｽｸ遏ｩ・｢・ｰ陋溷､蝎ｪ繝ｻ遒∵咎囎竏ｫ・ｿ・ｻ髴難ｽｬ髴代・謫らｸｲ繝ｻ
            poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
            model.render(poseStack, ctx, RenderTypeCompat.entityCutout(
                    getTextureLocation(stack)
            ), light, overlay);
            poseStack.popPose();
        }
    }

    /**
     * 髣費ｽｷ陷ｿ蛹∵ｧ崎恍荵玲・陞ｳ螢ｻ・ｽ蜥ｲ・ｻ繝ｻ蝎ｪ陷ｿ蜥ｲ蠍碁￥・ｩ鬮ｦ・ｵ
     */
    @Nonnull
    public static Matrix4f getPositioningNodeInverse(List<BedrockPart> nodePath) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.identity();
        if (nodePath != null) {
            for (int i = nodePath.size() - 1; i >= 0; i--) {
                BedrockPart part = nodePath.get(i);
                // 髫ｶ・｡驍よ懈ｸ夊惺驢榊飭隴檎事・ｽ・ｬ
                matrix4f.rotate(Axis.XN.rotation(part.xRot));
                matrix4f.rotate(Axis.YN.rotation(part.yRot));
                matrix4f.rotate(Axis.ZN.rotation(part.zRot));
                // 髫ｶ・｡驍よ懈ｸ夊惺驢榊飭闖ｴ蜥ｲ・ｧ・ｻ
                if (part.getParent() != null) {
                    matrix4f.translate(-part.x / 16.0F, -part.y / 16.0F, -part.z / 16.0F);
                } else {
                    matrix4f.translate(-part.x / 16.0F, (1.5F - part.y / 16.0F), -part.z / 16.0F);
                }
            }
        }
        return matrix4f;
    }

    public static void applyFirstPersonPositioningTransform(PoseStack poseStack, BedrockAnimatedModel model, ItemStack stack) {
        Matrix4f transformMatrix = new Matrix4f();
        transformMatrix.identity();
        // 陟守坩逡題ｿｸ繝ｻ繩･陞ｳ螢ｻ・ｽ繝ｻ
        List<BedrockPart> idleNodePath = model.getIdleSightPath();

        Matrix4f idleViewMatrix = getPositioningNodeInverse(idleNodePath);

        // 陟守坩逡題ｿｸ繝ｻ繩･陷ｿ菫ｶ蝗ｰ
        MathUtil.applyMatrixLerp(transformMatrix, idleViewMatrix, transformMatrix, 1);

        // 陟守坩逡題愾菫ｶ蝗ｰ陋ｻ・ｰ PoseStack
        poseStack.translate(0, 1.5f, 0);
        poseStack.mulPose(transformMatrix);
        poseStack.translate(0, -1.5f, 0);
    }
}


