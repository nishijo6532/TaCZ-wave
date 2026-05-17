package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.display.gun.MuzzleFlash;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.tacz.guns.util.RenderTypeCompat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MuzzleFlashRender implements IFunctionalRenderer {
    private static final SlotModel MUZZLE_FLASH_MODEL = new SlotModel(true);
    /**
     * 50ms 隴擾ｽｾ驕会ｽｺ隴鯉ｽｶ鬮｣・ｴ
     */
    private static final long TIME_RANGE = 50;
    public static boolean isSelf = false;
    private static long shootTimeStamp = -1;
    private static boolean muzzleFlashStartMark = false;
    private static float muzzleFlashRandomRotate = 0;
    private static Matrix3f muzzleFlashNormal = new Matrix3f();
    private static Matrix4f muzzleFlashPose = new Matrix4f();

    private final BedrockGunModel bedrockGunModel;

    public MuzzleFlashRender(BedrockGunModel bedrockGunModel) {
        this.bedrockGunModel = bedrockGunModel;
    }

    public static void onShoot() {
        // 髫ｶ・ｰ陟門供・ｼ霓｣・ｫ隴鯉ｽｶ鬮｣・ｴ隰鯉ｽｳ
        shootTimeStamp = System.currentTimeMillis();
        // 髫ｶ・ｰ陟門｢捺｣ｯ陷ｿ・｣霓｣・ｫ霎滂ｽｰ陷ｷ・ｯ陷会ｽｨ隴ｬ繝ｻ・ｮ・ｰ
        muzzleFlashStartMark = true;
        // 鬮ｫ荵玲・謇亥姓・ｺ蝓滓｣ｯ陷ｿ・｣霓｣・ｫ霎滂ｽｰ騾ｧ繝ｻ髮ｷ髴難ｽｬ
        muzzleFlashRandomRotate = (float) (Math.random() * 360);
    }

    private static void renderMuzzleFlash(GunDisplayInstance display, PoseStack poseStack, BedrockModel bedrockModel, long time) {
        MuzzleFlash muzzleFlash = display.getMuzzleFlash();
        if (muzzleFlash == null) {
            return;
        }
        muzzleFlashNormal = new Matrix3f(poseStack.last().normal());
        muzzleFlashPose = new Matrix4f(poseStack.last().pose());
        bedrockModel.delegateRender((poseStack1, vertexConsumer1, transformType1, light, overlay) -> doRender(light, overlay, muzzleFlash, time));
    }

    private static void doRender(int light, int overlay, MuzzleFlash muzzleFlash, long time) {
        if (muzzleFlashNormal != null && muzzleFlashPose != null) {
            float scale = 0.5f * muzzleFlash.getScale();
            muzzleFlashStartMark = false;
            MultiBufferSource multiBufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

            // 隰暦ｽｨ鬨ｾ竏晁寒隰悶・・ｮ螢ｻ・ｽ蜥ｲ・ｽ・ｮ
            PoseStack poseStack2 = new PoseStack();
            poseStack2.last().normal().mul(muzzleFlashNormal);
            poseStack2.last().pose().mul(muzzleFlashPose);

            // 陷亥沺・ｸ・ｲ隴溯ｬ趣ｽｸ鬩墓ｦ頑ｿ鬨ｾ荵励・髢ｭ譴ｧ蜍ｹ
            poseStack2.pushPose();
            {
                poseStack2.scale(scale, scale, scale);
                poseStack2.mulPose(Axis.ZP.rotationDegrees(muzzleFlashRandomRotate));
                poseStack2.translate(0, -1, 0);
                RenderType renderTypeBg = RenderTypeCompat.entityTranslucent(muzzleFlash.getTexture());
                MUZZLE_FLASH_MODEL.renderToBuffer(poseStack2, multiBufferSource.getBuffer(renderTypeBg), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            poseStack2.popPose();

            // 霎滂ｽｶ陷ｷ蜿厄ｽｸ・ｲ隴溽§譖ｸ陷育判隴懆ｭｫ繝ｻ
            poseStack2.pushPose();
            {
                poseStack2.scale(scale / 2, scale / 2, scale / 2);
                poseStack2.mulPose(Axis.ZP.rotationDegrees(muzzleFlashRandomRotate));
                poseStack2.translate(0, -0.9, 0);
                RenderType renderTypeLight = RenderTypeCompat.energySwirl(muzzleFlash.getTexture(), 1, 1);
                MUZZLE_FLASH_MODEL.renderToBuffer(poseStack2, multiBufferSource.getBuffer(renderTypeLight), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            poseStack2.popPose();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay) {
        if (OculusCompat.isRenderShadow()) {
            return;
        }
        if (!isSelf) {
            return;
        }
        long time = System.currentTimeMillis() - shootTimeStamp;
        if (time > TIME_RANGE) {
            return;
        }
        ItemStack currentGunItem = bedrockGunModel.getCurrentGunItem();

        TimelessAPI.getGunDisplay(currentGunItem).ifPresent(display -> {
            ItemStack muzzleAttachment = bedrockGunModel.getCurrentAttachmentItem().get(AttachmentType.MUZZLE);
            IAttachment iAttachment = IAttachment.getIAttachmentOrNull(muzzleAttachment);
            if (iAttachment != null) {
                Identifier attachmentId = iAttachment.getAttachmentId(muzzleAttachment);
                TimelessAPI.getCommonAttachmentIndex(attachmentId).ifPresent(index -> {
                    var modifier = index.getData().getModifier();
                    if (modifier.containsKey(SilenceModifier.ID) && modifier.get(SilenceModifier.ID).getValue() instanceof Pair<?, ?> pair) {
                        // 陞ｯ繧域｣｡陞ｳ闃ｽ・｣繝ｻ・ｺ繝ｻ・ｶ逎ｯ豬ｹ陜趣ｽｨ繝ｻ謔溘・闕ｳ閧ｴ・ｸ・ｲ隴滓瑳譽ｯ陷ｿ・｣霓｣・ｫ陷医・
                        if (((Pair<Integer, Boolean>) pair).right()) {
                            return;
                        }
                    }
                    renderMuzzleFlash(display, poseStack, bedrockGunModel, time);
                });
            } else {
                renderMuzzleFlash(display, poseStack, bedrockGunModel, time);
            }
        });
    }
}


