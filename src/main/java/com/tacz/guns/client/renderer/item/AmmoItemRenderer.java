package com.tacz.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.pojo.TransformScale;
import net.minecraft.client.model.geom.EntityModelSet;
import com.tacz.guns.client.renderer.compat.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.tacz.guns.util.RenderTypeCompat;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.List;

import static net.minecraft.world.item.ItemDisplayContext.GUI;


public class AmmoItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final SlotModel SLOT_AMMO_MODEL = new SlotModel();

    public AmmoItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
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

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (!(stack.getItem() instanceof IAmmo iAmmo)) {
            return;
        }
        Identifier ammoId = iAmmo.getAmmoId(stack);
        poseStack.pushPose();
        TimelessAPI.getClientAmmoIndex(ammoId).ifPresentOrElse(ammoIndex -> {
            // 陷磯メ蝓ｷ陷ｿ繝ｻ3D 隶難ｽ｡陜吝・・ｼ謔滂ｽｦ繧域｣｡闕ｳ・ｺ驕ｨ・ｺ繝ｻ讙趣ｽｻ貊会ｽｸ闖ｴ・ｿ騾包ｽｨ GUI 雋ゑｽｲ隴溘・
            BedrockAmmoModel ammoModel = ammoIndex.getAmmoModel();
            Identifier modelTexture = ammoIndex.getModelTextureLocation();
            // GUI 霑夲ｽｹ隹ｿ鬆托ｽｸ・ｲ隴溘・
            if (transformType == GUI || ammoModel == null || modelTexture == null) {
                poseStack.translate(0.5, 1.5, 0.5);
                poseStack.mulPose(Axis.ZN.rotationDegrees(180));
                VertexConsumer buffer = pBuffer.getBuffer(RenderTypeCompat.entityTranslucent(ammoIndex.getSlotTextureLocation()));
                SLOT_AMMO_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                return;
            }
            // 陷托ｽｩ闕ｳ迢怜飭雋ゑｽｲ隴溘・
            // 驕假ｽｻ陷会ｽｨ陋ｻ・ｰ隶難ｽ｡陜吝唱谺｡霓､・ｹ
            poseStack.translate(0.5, 2, 0.5);
            // 陷ｿ蟠趣ｽｽ・ｬ隶難ｽ｡陜吶・
            poseStack.scale(-1, -1, 1);
            // 陟守坩逡題楜螢ｻ・ｽ蜥ｲ・ｻ繝ｻ蝎ｪ陷ｿ菫ｶ蝗ｰ繝ｻ莠包ｽｽ蜥ｲ・ｧ・ｻ陷･譴ｧ髮ｷ髴難ｽｬ繝ｻ蠕｡・ｸ讎頑｡∬ｫ｡・ｬ驛幢ｽｩ隰ｾ・ｾ繝ｻ繝ｻ
            applyPositioningTransform(transformType, ammoIndex.getTransform().getScale(), ammoModel, poseStack);
            // 陟守坩逡・display 隰ｨ・ｰ隰撰ｽｮ闕ｳ・ｭ騾ｧ繝ｻ・ｼ・ｩ隰ｾ・ｾ
            applyScaleTransform(transformType, ammoIndex.getTransform().getScale(), poseStack);
            // 雋ゑｽｲ隴溽§・ｭ莉呻ｽｼ・ｹ騾ｶ蜻茨ｽｨ・｡陜吶・
            RenderType renderType = RenderTypeCompat.entityCutout(modelTexture);
            ammoModel.render(poseStack, transformType, renderType, pPackedLight, pPackedOverlay);
        }, () -> {
            // 雎撰ｽ｡隴幄歓・ｿ蜷ｩ・ｸ・ｪ ammoID繝ｻ譴ｧ・ｸ・ｲ隴溯ｬ趣ｽｸ・ｪ鬮槫揃・ｯ・ｯ隴壼頃・ｴ・ｨ隰螳｣繝ｻ陋ｻ・ｫ闔・ｺ
            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            VertexConsumer buffer = pBuffer.getBuffer(RenderTypeCompat.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
            SLOT_AMMO_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        });
        poseStack.popPose();
    }

    private void applyPositioningTransform(ItemDisplayContext transformType, TransformScale scale, BedrockAmmoModel model, PoseStack poseStack) {
        switch (transformType) {
            case FIXED -> applyPositioningNodeTransform(model.getFixedOriginPath(), poseStack, scale.getFixed());
            case GROUND -> applyPositioningNodeTransform(model.getGroundOriginPath(), poseStack, scale.getGround());
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
                    applyPositioningNodeTransform(model.getThirdPersonHandOriginPath(), poseStack, scale.getThirdPerson());
        }
    }

    private void applyScaleTransform(ItemDisplayContext transformType, TransformScale scale, PoseStack poseStack) {
        if (scale == null) {
            return;
        }
        Vector3f vector3f = null;
        switch (transformType) {
            case FIXED -> vector3f = scale.getFixed();
            case GROUND -> vector3f = scale.getGround();
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
                    vector3f = scale.getThirdPerson();
        }
        if (vector3f != null) {
            poseStack.translate(0, 1.5, 0);
            poseStack.scale(vector3f.x(), vector3f.y(), vector3f.z());
            poseStack.translate(0, -1.5, 0);
        }
    }
}


