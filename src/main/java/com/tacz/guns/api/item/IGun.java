package com.tacz.guns.api.item;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.GunProperty;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 霑咎㈹荳榊桁蜷ｫ譫ｪ譴ｰ逧・ｻ霎托ｼ悟宵蛹・性譫ｪ譴ｰ逧・推遘・nbt 隶ｿ髣ｮ縲・br>
 * 菴蜿ｯ莉･蝨ｨ {@link AbstractGunItem} 逵句芦譫ｪ譴ｰ騾ｻ霎・
 */
public interface IGun {
    /**
     * @return 螯よ棡迚ｩ蜩∫ｱｻ蝙倶ｸｺ IGun 蛻呵ｿ泌屓譏ｾ蠑剰ｽｬ謐｢蜷守噪螳樔ｾ具ｼ悟凄蛻呵ｿ泌屓 null縲・
     */
    @Nullable
    static IGun getIGunOrNull(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.getItem() instanceof IGun iGun) {
            return iGun;
        }
        return null;
    }

    /**
     * 譏ｯ蜷ｦ荳ｻ謇区戟譫ｪ
     */
    @Deprecated
    static boolean mainhandHoldGun(LivingEntity livingEntity) {
        return livingEntity.getMainHandItem().getItem() instanceof IGun;
    }

    /**
     * 譏ｯ蜷ｦ荳ｻ謇区戟譫ｪ
     */
    static boolean mainHandHoldGun(LivingEntity livingEntity) {
        return livingEntity.getMainHandItem().getItem() instanceof IGun;
    }

    /**
     * 闔ｷ蜿紋ｸｻ謇区棯譴ｰ逧・ｼ轣ｫ讓｡蠑・
     */
    @Deprecated
    static FireMode getMainhandFireMode(LivingEntity livingEntity) {
        ItemStack mainHandItem = livingEntity.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            return iGun.getFireMode(mainHandItem);
        }
        return FireMode.UNKNOWN;
    }

    /**
     * 闔ｷ蜿紋ｸｻ謇区棯譴ｰ逧・ｼ轣ｫ讓｡蠑・
     */
    static FireMode getMainHandFireMode(LivingEntity livingEntity) {
        ItemStack mainHandItem = livingEntity.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            return iGun.getFireMode(mainHandItem);
        }
        return FireMode.UNKNOWN;
    }

    /**
     * 闔ｷ蜿也桷蜃・叛螟ｧ蛟咲紫
     */
    float getAimingZoom(ItemStack gunItem);

    /**
     * 譫ｪ譴ｰ謐｢蠑ｹ譌ｶ譏ｯ蜷ｦ菴ｿ逕ｨ"陌壽供螟・ｼｹ"閠御ｸ肴弍閭悟桁驥檎噪螳樣刔蠑ｹ闕ｯ
     */
    boolean useDummyAmmo(ItemStack gun);

    /**
     * 闔ｷ蜿匁棯譴ｰ蠖灘燕逧・陌壽供螟・ｼｹ"謨ｰ驥・
     */
    int getDummyAmmoAmount(ItemStack gun);

    /**
     * 隶ｾ鄂ｮ譫ｪ譴ｰ蠖灘燕逧・陌壽供螟・ｼｹ"謨ｰ驥・
     */
    void setDummyAmmoAmount(ItemStack gun, int amount);

    /**
     * 豺ｻ蜉譫ｪ譴ｰ蠖灘燕逧・陌壽供螟・ｼｹ"謨ｰ驥・
     */
    void addDummyAmmoAmount(ItemStack gun, int amount);

    /**
     * 譽譟･譏ｯ蜷ｦ譛芽ｮｾ鄂ｮ"陌壽供螟・ｼｹ"譛螟ｧ謨ｰ驥・
     */
    boolean hasMaxDummyAmmo(ItemStack gun);

    /**
     * 闔ｷ蜿匁棯譴ｰ蠖灘燕逧・陌壽供螟・ｼｹ"譛螟ｧ謨ｰ驥・
     */
    int getMaxDummyAmmoAmount(ItemStack gun);

    /**
     * 隶ｾ鄂ｮ譫ｪ譴ｰ蠖灘燕逧・陌壽供螟・ｼｹ"譛螟ｧ謨ｰ驥・
     */
    void setMaxDummyAmmoAmount(ItemStack gun, int amount);

    /**
     * 闔ｷ蜿匁棯譴ｰ逧・驟堺ｻｶ髞・諠・・
     */
    boolean hasAttachmentLock(ItemStack gun);

    /**
     * 隶ｾ鄂ｮ譫ｪ譴ｰ逧・驟堺ｻｶ髞・
     */
    void setAttachmentLock(ItemStack gun, boolean locked);

    /**
     * 闔ｷ蜿匁棯譴ｰ ID
     */
    @NotNull
    Identifier getGunId(ItemStack gun);

    /**
     * 隶ｾ鄂ｮ譫ｪ譴ｰ ID
     */
    void setGunId(ItemStack gun, @Nullable Identifier gunId);

    /**
     * 闔ｷ蜿匁棯譴ｰ螳｢謌ｷ遶ｯ謨域棡 ID, 螯よ棡譏ｯ鮟倩ｮ､逧ｮ閧､蟆・ｿ泌屓 {@link DefaultAssets#DEFAULT_GUN_DISPLAY_ID}<br/>
     * 菴蠎碑ｯ･菴ｿ逕ｨ {@link com.tacz.guns.api.TimelessAPI#getGunDisplay(ItemStack)} 闔ｷ蜿匁ｭ｣遑ｮ逧・ｮ｢謌ｷ遶ｯ謨域棡
     */
    @NotNull
    Identifier getGunDisplayId(ItemStack gun);

    /**
     * 隶ｾ鄂ｮ譫ｪ譴ｰ螳｢謌ｷ遶ｯ謨域棡 ID
     */
    void setGunDisplayId(ItemStack gun, @Nullable Identifier displayId);

    /**
     * 闔ｷ蜿冶ｾ灘・逧・ｻ城ｪ悟ｼ蟇ｹ蠎皮噪遲臥ｺｧ縲・
     *
     * @param exp 扈城ｪ悟ｼ
     * @return 蟇ｹ蠎皮噪遲臥ｺｧ
     */
    int getLevel(int exp);

    /**
     * 闔ｷ蜿冶ｾ灘・逧・ｭ臥ｺｧ髴隕∬・蟆大､壼ｰ醍噪扈城ｪ悟ｼ縲・
     *
     * @param level 遲臥ｺｧ
     * @return 閾ｳ蟆鷹怙隕∫噪扈城ｪ悟ｼ
     */
    int getExp(int level);

    /**
     * 霑泌屓蜈∬ｮｸ逧・怙螟ｧ遲臥ｺｧ縲・
     *
     * @return 譛螟ｧ遲臥ｺｧ
     */
    int getMaxLevel();

    /**
     * 闔ｷ蜿匁棯譴ｰ蠖灘燕遲臥ｺｧ
     */
    int getLevel(ItemStack gun);

    /**
     * 闔ｷ蜿也ｧｯ邏ｯ逧・・驛ｨ扈城ｪ悟ｼ縲・
     *
     * @param gun 霎灘・迚ｩ蜩・
     * @return 蜈ｨ驛ｨ扈城ｪ悟ｼ
     */
    int getExp(ItemStack gun);

    /**
     * 闔ｷ蜿門芦荳倶ｸｪ遲臥ｺｧ髴隕∫噪扈城ｪ悟ｼ縲・
     *
     * @param gun 霎灘・迚ｩ蜩・
     * @return 蛻ｰ荳倶ｸｪ遲臥ｺｧ髴隕∫噪扈城ｪ悟ｼ縲ょｦよ棡遲臥ｺｧ蟾ｲ扈丞芦霎ｾ譛螟ｧ・悟・霑泌屓 0
     */
    int getExpToNextLevel(ItemStack gun);

    /**
     * 闔ｷ蜿門ｽ灘燕遲臥ｺｧ蟾ｲ扈冗ｧｯ邏ｯ逧・ｻ城ｪ悟ｼ縲・
     *
     * @param gun 霎灘・迚ｩ蜩・
     * @return 蠖灘燕遲臥ｺｧ蟾ｲ扈冗ｧｯ邏ｯ逧・ｻ城ｪ悟ｼ
     */
    int getExpCurrentLevel(ItemStack gun);

    /**
     * 闔ｷ蜿門ｼ轣ｫ讓｡蠑・
     *
     * @param gun 譫ｪ
     * @return 蠑轣ｫ讓｡蠑・
     */
    FireMode getFireMode(ItemStack gun);

    /**
     * 隶ｾ鄂ｮ蠑轣ｫ讓｡蠑・
     */
    void setFireMode(ItemStack gun, @Nullable FireMode fireMode);

    /**
     * 闔ｷ蜿門ｽ灘燕譫ｪ譴ｰ蠑ｹ闕ｯ謨ｰ
     */
    int getCurrentAmmoCount(ItemStack gun);

    /**
     * 隶ｾ鄂ｮ蠖灘燕譫ｪ譴ｰ蠑ｹ闕ｯ謨ｰ
     */
    void setCurrentAmmoCount(ItemStack gun, int ammoCount);

    /**
     * 蜃丞ｰ台ｸ荳ｪ蠖灘燕譫ｪ譴ｰ蠑ｹ闕ｯ謨ｰ
     */
    void reduceCurrentAmmoCount(ItemStack gun);

    /**
     * 蜉ｨ諤∽ｿｮ謾ｹ譫ｪ譴ｰ逧・ｱ樊ｧ縲・
     * 豕ｨ諢擾ｼ壼ｯｹ莠取汾莠帛､肴揩螻樊ｧ譚･隸ｴ・鶏@code GunProperty} 逧・ｱｻ蝙句庄閭ｽ莨壼柱蛟ｼ逧・ｱｻ蝙倶ｸ堺ｸ譬ｷ縲・
     * 豈泌ｦゆｼ､螳ｳ蜥檎ｲｾ蜃・ｺｦ霑呎ｷ逧・､肴揩螻樊ｧ・隈unProperty 逧・ｱｻ蝙区弍螟肴揩逧・焚謐ｮ扈捺桷・御ｼ蜈･蜥瑚ｿ泌屓逧・ｼ蟆ｱ蜿ｪ譏ｯ邂蜊慕噪豬ｮ轤ｹ謨ｰ縲・
     *
     * @param dataHolder 迥ｶ諤∵焚謐ｮ
     * @param gunItem 譫ｪ譴ｰ迚ｩ蜩・
     * @param shooter 蟆・・閠・
     * @param id 螻樊ｧ id・瑚ｯｷ蜿る・ {@link com.tacz.guns.api.GunProperties}
     * @param type 螻樊ｧ逧・焚謐ｮ邀ｻ蝙・
     * @param original 螻樊ｧ蜴滓擂逧・ｼ
     * @return 閼壽悽謌門ｭ千ｱｻ菫ｮ謾ｹ蜷守噪螻樊ｧ
     * @param <T> 螻樊ｧ逧・焚謐ｮ邀ｻ蝙・
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    default <T> T modifyProperty(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter,
                                 GunProperty<?> id, Class<T> type, T original) {
        return modifyProperty(dataHolder, gunItem ,shooter, id.name(), type, original);
    }

    /**
     * 蜉ｨ諤∽ｿｮ謾ｹ譫ｪ譴ｰ逧・ｱ樊ｧ
     *
     * @param dataHolder 迥ｶ諤∵焚謐ｮ
     * @param gunItem 譫ｪ譴ｰ迚ｩ蜩・
     * @param shooter 蟆・・閠・
     * @param id 螻樊ｧ id・瑚ｯｷ蜿る・ {@link com.tacz.guns.api.GunProperties}
     * @param type 螻樊ｧ逧・焚謐ｮ邀ｻ蝙・
     * @param original 螻樊ｧ蜴滓擂逧・ｼ
     * @return 閼壽悽謌門ｭ千ｱｻ菫ｮ謾ｹ蜷守噪螻樊ｧ
     * @param <T> 螻樊ｧ逧・焚謐ｮ邀ｻ蝙・
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    default <T> T modifyProperty(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter,
                                 String id, Class<T> type, T original) {
        return modifyProperty(dataHolder, gunItem ,shooter, "modify_property", id, type, original);
    }

    /**
     * 蜉ｨ諤∽ｿｮ謾ｹ譫ｪ譴ｰ逧・ｱ樊ｧ・・
     * 蜈∬ｮｸ謖・ｮ壻ｿｮ謾ｹ逕ｨ逧・lua 蜃ｽ謨ｰ逧・錐遘ｰ
     *
     * @param dataHolder 迥ｶ諤∵焚謐ｮ
     * @param gunItem 譫ｪ譴ｰ迚ｩ蜩・
     * @param shooter 蟆・・閠・
     * @param luaMethodName 菫ｮ謾ｹ螻樊ｧ逧・lua 蜃ｽ謨ｰ逧・・謨ｰ蜷・
     * @param id 螻樊ｧ id・瑚ｯｷ蜿る・ {@link com.tacz.guns.api.GunProperties}
     * @param type 螻樊ｧ逧・焚謐ｮ邀ｻ蝙・
     * @param original 螻樊ｧ蜴滓擂逧・ｼ
     * @return 閼壽悽謌門ｭ千ｱｻ菫ｮ謾ｹ蜷守噪螻樊ｧ
     * @param <T> 螻樊ｧ逧・焚謐ｮ邀ｻ蝙・
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    default <T> T modifyProperty(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter,
                                 String luaMethodName, String id, Class<T> type, T original) {
        return original;
    }

    /**
     * 蜿紋ｸ区棯蜀・園譛牙ｭ仙ｼｹ縲ら自螳ｶ逧・音谿頑婿豕包ｼ碁ｻ倩ｮ､蜊ｸ霓ｽ蠑ｹ闕ｯ譌ｶ菴ｿ逕ｨ
     */
    void dropAllAmmo(Player player, ItemStack gun);

    /**
     * 闔ｷ蜿門ｽ灘燕譫ｪ譴ｰ謖・ｮ夂ｱｻ蝙狗噪驟堺ｻｶ
     */
    @Nonnull
    ItemStack getAttachment(ItemStack gun, AttachmentType type);

    @Nonnull
    ItemStack getBuiltinAttachment(ItemStack gun, AttachmentType type);

    /**
     * 闔ｷ蜿門ｽ灘燕譫ｪ譴ｰ謖・ｮ夂ｱｻ蝙狗噪驟堺ｻｶ逧・NBT 謨ｰ謐ｮ
     *
     * @return 螯よ棡荳ｺ遨ｺ・碁ぅ荵域ｲ｡譛蛾・莉ｶ謨ｰ謐ｮ
     */
    @Nullable
    CompoundTag getAttachmentTag(ItemStack gun, AttachmentType type);

    @Nonnull
    Identifier getBuiltInAttachmentId(ItemStack gun, AttachmentType type);

    /**
     * 闔ｷ蜿匁棯譴ｰ逧・・莉ｶ ID
     * <p>
     * 螯よ棡荳榊ｭ伜惠・瑚ｿ泌屓 {@link DefaultAssets#EMPTY_ATTACHMENT_ID};
     */
    @Nonnull
    Identifier getAttachmentId(ItemStack gun, AttachmentType type);

    /**
     * 螳芽｣・・莉ｶ
     */
    void installAttachment(@Nonnull ItemStack gun, @Nonnull ItemStack attachment);

    /**
     * 蜊ｸ霓ｽ驟堺ｻｶ
     */
    void unloadAttachment(@Nonnull ItemStack gun, AttachmentType type);

    /**
     * 隸･譫ｪ譴ｰ譏ｯ蜷ｦ蜈∬ｮｸ陬・・隸･驟堺ｻｶ
     */
    boolean allowAttachment(ItemStack gun, ItemStack attachmentItem);

    /**
     * 隸･譫ｪ譴ｰ譏ｯ蜷ｦ蜈∬ｮｸ譟千ｱｻ蝙矩・莉ｶ
     */
    boolean allowAttachmentType(ItemStack gun, AttachmentType type);

    /**
     * 譫ｪ邂｡荳ｭ譏ｯ蜷ｦ譛牙ｭ仙ｼｹ・檎畑莠朱溜閹帛ｾ・・逧・棯譴ｰ
     */
    boolean hasBulletInBarrel(ItemStack gun);

    /**
     * 隶ｾ鄂ｮ譫ｪ邂｡荳ｭ逧・ｭ仙ｼｹ譛画裏・檎畑莠朱溜閹帛ｾ・・逧・棯譴ｰ
     */
    void setBulletInBarrel(ItemStack gun, boolean bulletInBarrel);

    /**
     * 譫ｪ譴ｰ譏ｯ蜷ｦ荳ｺ螟・ｼｹ逶ｴ隸ｻ
     */
    boolean useInventoryAmmo(ItemStack gun);

    /**
     * 闔ｷ蜿匁棯譴ｰ譏ｯ蜷ｦ譛牙､・ｼｹ (蜿ｪ髓亥ｯｹ閭悟桁逶ｴ隸ｻ隸ｻ逧・惻蛻ｶ菴ｿ逕ｨ)
     */
    boolean hasInventoryAmmo(LivingEntity shooter, ItemStack gun, boolean needCheckAmmo);

    /**
     * 闔ｷ蜿・RPM
     */
    int getRPM(ItemStack gun);

    /**
     * 闔ｷ蜿匁弍蜷ｦ蜿ｯ莉･雜ｴ荳・
     */
    boolean isCanCrawl(ItemStack gun);

    boolean hasCustomLaserColor(ItemStack gun);

    int getLaserColor(ItemStack gun);

    void setLaserColor(ItemStack gun, int color);

    /**
     * Heat Data
     */
    boolean hasHeatData(ItemStack gun);

    /**
     * 譏ｯ蜷ｦ螳悟・霑・Ο
     */
    boolean isOverheatLocked(ItemStack gun);

    void setOverheatLocked(ItemStack gun, boolean locked);

    /**
     * 隶ｾ鄂ｮ蠖灘燕霑・Ο蛟ｼ
     */
    void setHeatAmount(ItemStack gun, float amount);

    float lerpRPM(ItemStack gun);

    float lerpInaccuracy(ItemStack gun);

    float getHeatAmount(ItemStack gun);
}
