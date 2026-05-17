package com.tacz.guns.util;

import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AllowAttachmentTagMatcher {
    private static final String TAG_PREFIX = "#";
    private static final Cache CACHE = new Cache();

    public record Cache(
            Map<Pair<Identifier, Identifier>, Boolean> allowAttachmentCache,
            Map<Pair<Identifier, Identifier>, Boolean> tagMatchCache
    ) {
        public Cache() {
            this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        }
    }

    public static boolean match(Identifier gunId, Identifier attachmentId) {
        var key = Pair.of(gunId, attachmentId);
        return CACHE.allowAttachmentCache().computeIfAbsent(key, AllowAttachmentTagMatcher::match0);
    }

    public static boolean match0(Pair<Identifier, Identifier> record) {
        Identifier gunId = record.getLeft();
        Identifier attachmentId = record.getRight();
        Set<String> allowAttachmentTags = CommonAssetsManager.get().getAllowAttachmentTags(gunId);
        // 螯よ棡譫ｪ譴ｰ蟇ｹ蠎皮噪 allowAttachmentTags 荳ｺ遨ｺ・瑚ｯｴ譏守岼蜑肴ｲ｡譛我ｻｻ菴募庄莉･陬・噪驟堺ｻｶ
        if (allowAttachmentTags == null || allowAttachmentTags.isEmpty()) {
            return false;
        }
        // 蠑蟋矩″蜴・allowAttachmentTags・悟ｯｻ謇ｾ驟堺ｻｶ id
        AtomicBoolean searchSignal = new AtomicBoolean(false);
        treeSearch(allowAttachmentTags, attachmentId, searchSignal);
        return searchSignal.get();
    }

    /**
     * 蛹ｹ驟埼・莉ｶ譏ｯ蜷ｦ譛画欠螳夂噪譬・ｭｾ縲・
     * 逶ｮ蜑榊・驛ｨ逕ｨ莠守峡螟ｴ蠑ｹ迚ｹ谿頑・ｭｾ逧・愛譁ｭ・・
     * 荵溯・譁ｹ萓ｿ蛻ｰ螟夜Κ・磯刋螻橸ｼ梧紛蜷亥桁遲会ｼ牙宛菴懷ｮ・ｻｬ逧・音谿頑・ｭｾ縲・
     *
     * @param tag tacz 驟堺ｻｶ譬・ｭｾ
     * @param attachmentId 驟堺ｻｶ id
     * @return 驟堺ｻｶ id 譏ｯ蜷ｦ譛芽ｿ吩ｸｪ驟堺ｻｶ譬・ｭｾ
     * @since 1.1.7
     */
    public static boolean matchTag(Identifier tag, Identifier attachmentId) {
        var key = Pair.of(tag, attachmentId);
        return CACHE.tagMatchCache().computeIfAbsent(key, AllowAttachmentTagMatcher::matchTag0);
    }

    public static boolean matchTag0(Pair<Identifier, Identifier> record) {
        Identifier tag = record.getLeft();
        Identifier attachmentId = record.getRight();
        Set<String> tagContent = CommonAssetsManager.get().getAttachmentTags(tag);
        // 螯よ棡 tag 蟇ｹ蠎皮噪蜀・ｮｹ髮・ｸｺ遨ｺ・瑚ｯｴ譏守岼蜑肴ｲ｡譛我ｻｻ菴募・螳ｹ
        if (tagContent == null || tagContent.isEmpty()) {
            return false;
        }
        // 蠑蟋矩″蜴・・螳ｹ髮・ｼ悟ｯｻ謇ｾ驟堺ｻｶ id
        AtomicBoolean searchSignal = new AtomicBoolean(false);
        treeSearch(tagContent, attachmentId, searchSignal);
        return searchSignal.get();
    }

    private static void treeSearch(Set<String> tags, Identifier attachmentId, AtomicBoolean searchSignal) {
        // 蠑蟋矩″蜴・tags・悟ｯｻ謇ｾ驟堺ｻｶ id
        for (String tag : tags) {
            // 螯よ棡譏ｯ tag・悟・蜴ｻ attachment tag 蟇ｻ謇ｾ謌台ｻｬ逧・ｸ懆･ｿ
            if (tag.startsWith(TAG_PREFIX)) {
                Identifier tagId = com.tacz.guns.util.IdHelper.id(tag.substring(TAG_PREFIX.length()));
                Set<String> attachmentTags = CommonAssetsManager.get().getAttachmentTags(tagId);
                // 螯よ棡譽邏｢逧・ｿ吩ｸｪ驟堺ｻｶ tag 荳堺ｸｺ遨ｺ・悟ｼ蟋矩貞ｽ呈衍謇ｾ
                if (attachmentTags != null && !attachmentTags.isEmpty()) {
                    treeSearch(attachmentTags, attachmentId, searchSignal);
                }
            }
            // 螯よ棡譏ｯ驟堺ｻｶ id・檎峩謗･蟇ｹ豈・
            else {
                Identifier matchAttachmentId = com.tacz.guns.util.IdHelper.id(tag);
                if (attachmentId.equals(matchAttachmentId)) {
                    searchSignal.set(true);
                    return;
                }
            }
        }
    }

    public static void resetCache() {
        CACHE.allowAttachmentCache().clear();
        CACHE.tagMatchCache().clear();
    }
}


