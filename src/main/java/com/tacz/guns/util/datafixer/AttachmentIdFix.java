package com.tacz.guns.util.datafixer;

import com.google.common.collect.ImmutableMap;
import com.tacz.guns.api.DefaultAssets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import java.util.Map;

import static com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor.*;
import static com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor.ATTACHMENT_ID_TAG;

public final class AttachmentIdFix {
    private AttachmentIdFix() {
    }

    public static final Map<Identifier, Identifier> OLD_TO_NEW;
    static  {
        OLD_TO_NEW = ImmutableMap.<Identifier, Identifier>builder()
                .put(com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silence_knight_qd"), com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silencer_knight_qd"))
                .put(com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silence_mirage"), com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silencer_mirage"))
                .put(com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silence_phantom_s1"), com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silencer_phantom_s1"))
                .put(com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silence_ptilopsis"), com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silencer_ptilopsis"))
                .put(com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silence_ursus"), com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silencer_ursus"))
                .put(com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silence_vulture"), com.tacz.guns.util.IdHelper.id("tacz", "muzzle_silencer_vulture"))
                .build();
    }

    // 鬚・蕗 boolean 霑泌屓蛟ｼ逕ｨ莠取悴譚･菴ｿ逕ｨ・・蟆ｽ蜿ｯ閭ｽ驕ｿ蜈堺ｽｿ逕ｨ void ・碁勁髱櫁ｿ吩ｸｪ謫堺ｽ應ｸ崎・謠蝉ｾ帑ｻｻ菴墓怏逕ｨ逧・ｿ｡諱ｯ"
    public static boolean updateAttachmentIdInTag(CompoundTag tag) {
        Identifier old = getAttachmentIdFromTag(tag);
        if (!old.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
            Identifier fixed = updateAttachmentId(old);
            if (!old.equals(fixed)) {
                tag.putString(ATTACHMENT_ID_TAG, fixed.toString());
                return true;
            }
        }
        return false;
    }

    public static Identifier updateAttachmentId(Identifier old) {
        return OLD_TO_NEW.getOrDefault(old, old);
    }
}


