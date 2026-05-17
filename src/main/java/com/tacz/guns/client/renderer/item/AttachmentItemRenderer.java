package com.tacz.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.util.RenderDistance;
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
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class AttachmentItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final SlotModel SLOT_ATTACHMENT_MODEL = new SlotModel();

    public AttachmentItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (stack.getItem() instanceof IAttachment iAttachment) {
            Identifier attachmentId = iAttachment.getAttachmentId(stack);
            poseStack.pushPose();
            TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresentOrElse(attachmentIndex -> {
                // GUI 霑夲ｽｹ隹ｿ鬆托ｽｸ・ｲ隴溘・
                if (transformType == ItemDisplayContext.GUI) {
                    poseStack.translate(0.5, 1.5, 0.5);
                    poseStack.mulPose(Axis.ZN.rotationDegrees(180));
                    VertexConsumer buffer = pBuffer.getBuffer(RenderTypeCompat.entityTranslucent(attachmentIndex.getSlotTexture()));
                    SLOT_ATTACHMENT_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    return;
                }
                poseStack.translate(0.5, 2, 0.5);
                // 陷ｿ蟠趣ｽｽ・ｬ隶難ｽ｡陜吶・
                poseStack.scale(-1, -1, 1);
                if (transformType == ItemDisplayContext.FIXED) {
                    poseStack.mulPose(Axis.YN.rotationDegrees(90f));
                }
                this.renderDefaultAttachment(transformType, poseStack, pBuffer, pPackedLight, pPackedOverlay, attachmentIndex);
            }, () -> {
                // 雎撰ｽ｡隴幄歓・ｿ蜷ｩ・ｸ・ｪ attachmentId繝ｻ譴ｧ・ｸ・ｲ隴滄ｦｴ・ｻ驢搾ｽｴ・ｫ隴壼頃・ｴ・ｨ闔会ｽ･隰螳｣繝ｻ
                poseStack.translate(0.5, 1.5, 0.5);
                poseStack.mulPose(Axis.ZN.rotationDegrees(180));
                VertexConsumer buffer = pBuffer.getBuffer(RenderTypeCompat.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
                SLOT_ATTACHMENT_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
            });
            poseStack.popPose();
        }
    }

    private void renderDefaultAttachment(@NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay, ClientAttachmentIndex attachmentIndex) {
        BedrockAttachmentModel model = attachmentIndex.getAttachmentModel();
        Identifier texture = attachmentIndex.getModelTexture();
        // 隴帷判・ｨ・｡陜吝・・ｼ貊難ｽｭ・｣陝ｶ・ｸ雋ゑｽｲ隴溘・
        if (model != null && texture != null) {
            // 髫ｹ繝ｻ逡題抄蜿厄ｽｨ・｡
            Pair<BedrockAttachmentModel, Identifier> lodModel = attachmentIndex.getLodModel();
            // 隴帶・・ｽ蜿厄ｽｨ・｡邵ｲ竏晄Β鬯ｮ菫ｶ・ｨ・｡雋ゑｽｲ隴滓･｢豼陜暦ｽｴ陞滓じ竏ｽ・ｸ閧ｴ蠑埼圷・ｬ闕ｳ闔・ｺ驕假ｽｰ
            if (lodModel != null && !RenderDistance.inRenderHighPolyModelDistance(poseStack) && !transformType.firstPerson()) {
                model = lodModel.getLeft();
                texture = lodModel.getRight();
            }
            RenderType renderType = RenderTypeCompat.entityCutout(texture);
            model.render(null, null, poseStack, transformType, renderType, pPackedLight, pPackedOverlay);
        }
        // 陷ｷ・ｦ陋ｻ蜻ｻ・ｼ蠕｡・ｻ・･ GUI 陟厄ｽ｢陟台ｹ暦ｽｸ・ｲ隴溘・
        else {
            poseStack.translate(0, 0.5, 0);
            // 陞ｻ諷包ｽ､・ｺ隴ｯ繝ｻ繹ｹ隴擾ｽｾ驕会ｽｺ雎・ｽ｣陝ｶ・ｸ
            if (transformType == ItemDisplayContext.FIXED) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
            }
            VertexConsumer buffer = pBuffer.getBuffer(RenderTypeCompat.entityTranslucent(attachmentIndex.getSlotTexture()));
            SLOT_ATTACHMENT_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}


