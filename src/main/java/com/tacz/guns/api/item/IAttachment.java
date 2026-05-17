package com.tacz.guns.api.item;

import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IAttachment {
    /**
     * @return 螯よ棡迚ｩ蜩∫ｱｻ蝙倶ｸｺ IAttachment 蛻呵ｿ泌屓譏ｾ蠑剰ｽｬ謐｢蜷守噪螳樔ｾ具ｼ悟凄蛻呵ｿ泌屓 null縲・
     */
    @Nullable
    static IAttachment getIAttachmentOrNull(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.getItem() instanceof IAttachment iAttachment) {
            return iAttachment;
        }
        return null;
    }

    /**
     * 闔ｷ蜿夜・莉ｶ ID
     */
    @Nonnull
    Identifier getAttachmentId(ItemStack attachmentStack);

    /**
     * 隶ｾ鄂ｮ驟堺ｻｶ ID
     */
    void setAttachmentId(ItemStack attachmentStack, @Nullable Identifier attachmentId);

    /**@deprecated
     */
    @Deprecated
    @Nullable
    Identifier getSkinId(ItemStack attachmentStack);

    /**@deprecated
     */
    @Deprecated
    void setSkinId(ItemStack attachmentStack, @Nullable Identifier skinId);

    /**
     * 闔ｷ蜿也桷蜈ｷ驟堺ｻｶ逧・ｼｩ謾ｾ蛟咲紫逧・焚蟄礼ｴ｢蠑包ｼ御ｻ・桷蜈ｷ驟堺ｻｶ蜿ｯ逕ｨ
     */
    int getZoomNumber(ItemStack attachmentStack);

    /**
     * 隶ｾ鄂ｮ迸・・驟堺ｻｶ逧・ｼｩ謾ｾ蛟咲紫逧・焚蟄礼ｴ｢蠑・
     */
    void setZoomNumber(ItemStack attachmentStack, int zoomNumber);

    /**
     * 驟堺ｻｶ邀ｻ蝙・
     */
    @Nonnull
    AttachmentType getType(ItemStack attachmentStack);

    boolean hasCustomLaserColor(ItemStack attachmentStack);

    /**
     * 闔ｷ蜿夜墳蟆・・莉ｶ逧・ｿ蜈蛾｢懆牡
     * @return 髟ｭ蟆・｢懆牡・軍GB
     */
    int getLaserColor(ItemStack attachmentStack);

    void setLaserColor(ItemStack attachmentStack, int color);
}

