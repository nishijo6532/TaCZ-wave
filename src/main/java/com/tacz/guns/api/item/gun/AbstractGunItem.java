package com.tacz.guns.api.item.gun;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.*;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.FeedType;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.client.renderer.compat.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.PlayerEquipmentInvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AbstractGunItem extends Item implements IGun, IAnimationItem {
    protected AbstractGunItem(Properties pProperties) {
        super(pProperties);
    }

    private static Comparator<Map.Entry<Identifier, CommonGunIndex>> idNameSort() {
        return Comparator.comparingInt(m -> m.getValue().getSort());
    }

    /**
     * 蠑蟋区級譬捺慮隹・畑・瑚ｿ泌屓 bolt 迥ｶ諤・
     * @return bolt 迥ｶ諤√Ｕure 莉｣陦ｨ蠑蟋・bolt・掲alse 蛻吩ｻ｣陦ｨ荳榊ｼ蟋九・
     */
    public abstract boolean startBolt(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter);

    /**
     * 諡画・tick 譌ｶ隹・畑・瑚ｿ泌屓譏ｯ蜷ｦ莉榊惠 bolt 迥ｶ諤・
     * @return 譏ｯ蜷ｦ莉榊惠 bolt 迥ｶ諤・
     */
    public abstract boolean tickBolt(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter);

    /**
     * 蟆・・譌ｶ隗ｦ蜿・
     */
    public abstract void shoot(ShooterDataHolder dataHolder, ItemStack gunItem, Supplier<Float> pitch, Supplier<Float> yaw, LivingEntity shooter);

    /**
     * 蠑蟋区困蠑ｹ譌ｶ隹・畑
     */
    public abstract boolean startReload(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter);

    /**
     * 謐｢蠑ｹ譌ｶ豈丈ｸｪ tick 隹・畑
     * @return 螯よ棡霑泌屓逧・ｱｻ蝙区弍 NOT_RELOADING 蛻吩ｸ倶ｸ荳ｪ tick 荳榊・扈ｧ扈ｭ隹・畑
     */
    public abstract ReloadState tickReload(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter);

    /**
     * 蟆晁ｯ墓遠譁ｭ謐｢蠑ｹ譌ｶ隹・畑
     */
    public abstract void interruptReload(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter);

    /**
     * 蛻・困蠑轣ｫ讓｡蠑乗慮隹・畑
     */
    public abstract void fireSelect(ShooterDataHolder dataHolder, ItemStack gunItem);

    /**
     * 霑第・譌ｶ隹・畑
     */
    public abstract void melee(ShooterDataHolder dataHolder, LivingEntity user, ItemStack gunItem);

    /**
     * 霑・Ο tick 螟・炊<br/>
     * 鮟倩ｮ､荳榊★莉ｻ菴穂ｺ区ュ
     */
    public void tickHeat(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter) {};

    /**
     * 蛻晏ｧ句喧蟄仙ｼｹ隗貞ｺｦ蜥碁溷ｺｦ
     * @param dataHolder 迥ｶ諤∵焚謐ｮ
     * @param gunItem 譫ｪ譴ｰ迚ｩ蜩・
     * @param shooter 蟆・・閠・
     * @param projectile 蟄仙ｼｹ
     * @param bulletCnt 螟壼ｼｹ荳ｸ逧・ｭ仙ｼｹ蠎乗焚
     * @param processedSpeed 菫ｮ豁｣蜷守噪蟄仙ｼｹ蛻晞・
     * @param inaccuracy 菫ｮ豁｣蜷守噪蟄仙ｼｹ荳榊㊥遑ｮ蠎ｦ
     * @param pitch 蟆・・譁ｹ蜷・
     * @param yaw 蟆・・譁ｹ蜷・
     */
    public void doBulletSpread(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter, Projectile projectile,
                                        int bulletCnt, float processedSpeed, float inaccuracy, float pitch, float yaw) {
        projectile.shootFromRotation(shooter, pitch, yaw, 0.0F, processedSpeed, inaccuracy);
    }

    /**
     * 謐｢蠑ｹ蜑咲噪譽譟･・悟ｮ梧・螯ゆｸ区｣譟･・壽棯蜀・ｼｹ闕ｯ譏ｯ蜷ｦ蟾ｲ扈丞｡ｫ貊｡・溽自螳ｶ閭悟桁譏ｯ蜷ｦ譛牙庄逕ｨ蠑ｹ闕ｯ・滓弍蜷ｦ荳ｺ閭悟桁逶ｴ隸ｻ・・
     * @param shooter 蜃・､・困蠑ｹ逧・ｮ樔ｽ・
     * @param gunItem 譫ｪ譴ｰ迚ｩ蜩・
     * @return 譏ｯ蜷ｦ貊｡雜ｳ謐｢蠑ｹ譚｡莉ｶ
     */
    public boolean canReload(LivingEntity shooter, ItemStack gunItem) {
        Identifier gunId = this.getGunId(gunItem);
        CommonGunIndex gunIndex = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        if (gunIndex == null) {
            return false;
        }

        int currentAmmoCount = getCurrentAmmoCount(gunItem);
        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, gunIndex.getGunData());
        if (currentAmmoCount >= maxAmmoCount) {
            return false;
        }
        // 閭悟桁逶ｴ隸ｻ荳崎ｿ幄｡梧困蠑ｹ
        if (useInventoryAmmo(gunItem)) {
            return false;
        }
        // 譌髯仙､・ｼｹ荳埼怙隕∵ｶ郁怜ｮ樣刔蟄仙ｼｹ
        if (gunIndex.getGunData().getReloadData().isInfinite()) {
            return true;
        }
        // 陌壽供螟・ｼｹ螟・炊
        if (useDummyAmmo(gunItem)) {
            return getDummyAmmoAmount(gunItem) > 0;
        }
        // 譽譟･閭悟桁蜀・噪蠑ｹ闕ｯ謨ｰ驥・
        return hasConsumableAmmo(shooter, gunItem);
    }

    /**
     * 蟆・棯蜀・噪蠑ｹ闕ｯ蜈ｨ驛ｨ騾閾ｳ閭悟桁・亥ｦよ棡閭悟桁貊｡莠・ｼ壻ｸ｢蛻ｰ蝨ｰ荳奇ｼ峨ゆｸ堺ｼ夐譫ｪ閹帛・逧・ｼｹ闕ｯ縲・
     * 逶ｮ蜑搾ｼ御ｻ・峩謐｢蠑ｹ蛹｣驟堺ｻｶ譌ｶ隹・畑縲・
     * @param player 邇ｩ螳ｶ
     * @param gunItem 譫ｪ譴ｰ迚ｩ蜩・
     */
    @Override
    public void dropAllAmmo(Player player, ItemStack gunItem) {
        // 閭悟桁逶ｴ隸ｻ譌ｶ荳崎ｰ・畑騾蠑ｹ
        if (useInventoryAmmo(gunItem)) {
            return;
        }
        //TODO 霑咎㈹謫堺ｽ懃噪蟇ｹ雎｡荳榊ｺ碑ｯ･譏ｯ Player 閠梧弍 LivingEntity縲よｭ､螟匁棯閹帛・逧・ｭ仙ｼｹ荵溯ｦ・
        int ammoCount = getCurrentAmmoCount(gunItem);
        if (ammoCount <= 0) {
            return;
        }
        Identifier gunId = getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
            // 螯よ棡菴ｿ逕ｨ逧・弍陌壽供螟・ｼｹ・瑚ｿ碑ｿ倩・陌壽供螟・ｼｹ
            if (useDummyAmmo(gunItem)) {
                setCurrentAmmoCount(gunItem, 0);
                // 辯・侭鄂千ｱｻ蝙狗噪謐｢蠑ｹ荳崎ｿ碑ｿ・
                if (index.getGunData().getReloadData().getType().equals(FeedType.FUEL)) {
                    return;
                }
                addDummyAmmoAmount(gunItem, ammoCount);
                return;
            }

            Identifier ammoId = index.getGunData().getAmmoId();
            // 蛻幃讓｡蠑冗ｱｻ蝙狗噪謐｢蠑ｹ・悟宵蝪ｫ貊｡蟄仙ｼｹ諤ｻ謨ｰ・御ｸ崎ｿ幄｡御ｻｻ菴募査霓ｽ蠑ｹ闕ｯ騾ｻ霎・
            if (player.isCreative()) {
                int maxAmmCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, index.getGunData());
                setCurrentAmmoCount(gunItem, maxAmmCount);
                return;
            }
            // 辯・侭鄂千ｱｻ蝙狗噪蜿ｪ貂・ｩｺ荳崎ｿ碑ｿ・
            if (index.getGunData().getReloadData().getType().equals(FeedType.FUEL)) {
                setCurrentAmmoCount(gunItem, 0);
                return;
            }
            TimelessAPI.getCommonAmmoIndex(ammoId).ifPresent(ammoIndex -> {
                int stackSize = ammoIndex.getStackSize();
                int tmpAmmoCount = ammoCount;
                int roundCount = tmpAmmoCount / (stackSize + 1);
                for (int i = 0; i <= roundCount; i++) {
                    int count = Math.min(tmpAmmoCount, stackSize);
                    ItemStack ammoItem = AmmoItemBuilder.create().setId(ammoId).setCount(count).build();
                    ItemHandlerHelper.giveItemToPlayer(player, ammoItem);
                    tmpAmmoCount -= stackSize;
                }
                setCurrentAmmoCount(gunItem, 0);
            });
        });
    }

    /**
     * 譫ｪ譴ｰ蟇ｻ蠑ｹ蜥梧殴髯､閭悟桁蠑ｹ闕ｯ騾ｻ霎・
     * @param itemHandler 逶ｮ譬・ｮ樔ｽ鍋噪閭悟桁
     * @param gunItem 譫ｪ譴ｰ迚ｩ蜩・
     * @param needAmmoCount 髴隕∫噪蠑ｹ闕ｯ (迚ｩ蜩・ 謨ｰ驥・
     * @return 蟇ｻ謇ｾ蛻ｰ逧・ｼｹ闕ｯ (迚ｩ蜩・ 謨ｰ驥・
     */
    @Deprecated
    public int findAndExtractInventoryAmmos(IItemHandler itemHandler, ItemStack gunItem, int needAmmoCount) {
        return findAndExtractInventoryAmmo(itemHandler, gunItem, needAmmoCount);
    }

    /**
     * 譫ｪ譴ｰ蟇ｻ蠑ｹ蜥梧殴髯､閭悟桁蠑ｹ闕ｯ騾ｻ霎・
     * @param itemHandler 逶ｮ譬・ｮ樔ｽ鍋噪閭悟桁
     * @param gunItem 譫ｪ譴ｰ迚ｩ蜩・
     * @param needAmmoCount 髴隕∫噪蠑ｹ闕ｯ (迚ｩ蜩・ 謨ｰ驥・
     * @return 蟇ｻ謇ｾ蛻ｰ逧・ｼｹ闕ｯ (迚ｩ蜩・ 謨ｰ驥・
     */
    public int findAndExtractInventoryAmmo(IItemHandler itemHandler, ItemStack gunItem, int needAmmoCount) {
        int cnt = needAmmoCount;
        // 閭悟桁譽譟･
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack checkAmmoStack = itemHandler.getStackInSlot(i);
            if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(gunItem, checkAmmoStack)) {
                ItemStack extractItem = itemHandler.extractItem(i, cnt, false);
                cnt = cnt - extractItem.getCount();
                if (cnt <= 0) {
                    break;
                }
            }
            if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(gunItem, checkAmmoStack)) {
                int boxAmmoCount = iAmmoBox.getAmmoCount(checkAmmoStack);
                int extractCount = Math.min(boxAmmoCount, cnt);
                int remainCount = boxAmmoCount - extractCount;
                iAmmoBox.setAmmoCount(checkAmmoStack, remainCount);
                if (remainCount <= 0) {
                    iAmmoBox.setAmmoId(checkAmmoStack, DefaultAssets.EMPTY_AMMO_ID);
                }
                cnt = cnt - extractCount;
                if (cnt <= 0) {
                    break;
                }
            }
        }
        return needAmmoCount - cnt;
    }

    public int findAndExtractInventoryAmmo(LivingEntity shooter, ItemStack gunItem, int needAmmoCount) {
        int extracted = getItemHandler(shooter)
                .map(cap -> findAndExtractInventoryAmmo(cap, gunItem, needAmmoCount))
                .orElse(0);
        if (extracted >= needAmmoCount) {
            return extracted;
        }
        if (shooter instanceof Player player) {
            return extracted + extractAmmoFromPlayerInventory(player, gunItem, needAmmoCount - extracted);
        }
        return extracted;
    }

    public Optional<IItemHandler> getItemHandler(LivingEntity shooter) {
        IItemHandler capabilityHandler = shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (capabilityHandler != null) {
            return Optional.of(capabilityHandler);
        }
        if (shooter instanceof Player player) {
            return Optional.of(new CombinedInvWrapper(
                    new PlayerMainInvWrapper(player.getInventory()),
                    new PlayerEquipmentInvWrapper(player.getInventory())
            ));
        }
        return Optional.empty();
    }

    public boolean hasConsumableAmmo(LivingEntity shooter, ItemStack gunItem) {
        boolean hasAmmo = getItemHandler(shooter)
                .map(cap -> hasAmmoInItemHandler(cap, gunItem))
                .orElse(false);
        if (!hasAmmo && shooter instanceof Player player) {
            hasAmmo = hasAmmoInPlayerInventory(player, gunItem);
        }
        return hasAmmo;
    }

    private boolean hasAmmoInItemHandler(IItemHandler itemHandler, ItemStack gunItem) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (isAmmoStackForGun(gunItem, itemHandler.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAmmoInPlayerInventory(Player player, ItemStack gunItem) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isAmmoStackForGun(gunItem, inventory.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private int extractAmmoFromPlayerInventory(Player player, ItemStack gunItem, int needAmmoCount) {
        int cnt = needAmmoCount;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack checkAmmoStack = inventory.getItem(i);
            if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(gunItem, checkAmmoStack)) {
                int extractCount = Math.min(checkAmmoStack.getCount(), cnt);
                checkAmmoStack.shrink(extractCount);
                cnt -= extractCount;
                if (cnt <= 0) {
                    break;
                }
            }
            if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(gunItem, checkAmmoStack)) {
                int boxAmmoCount = iAmmoBox.getAmmoCount(checkAmmoStack);
                int extractCount = Math.min(boxAmmoCount, cnt);
                int remainCount = boxAmmoCount - extractCount;
                iAmmoBox.setAmmoCount(checkAmmoStack, remainCount);
                if (remainCount <= 0) {
                    iAmmoBox.setAmmoId(checkAmmoStack, DefaultAssets.EMPTY_AMMO_ID);
                }
                cnt -= extractCount;
                if (cnt <= 0) {
                    break;
                }
            }
        }
        if (cnt != needAmmoCount) {
            inventory.setChanged();
        }
        return needAmmoCount - cnt;
    }

    private boolean isAmmoStackForGun(ItemStack gunItem, ItemStack checkAmmoStack) {
        if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(gunItem, checkAmmoStack)) {
            return true;
        }
        return checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(gunItem, checkAmmoStack);
    }

    /**
     * 謇｣髯､陌壽供蠑ｹ闕ｯ騾ｻ霎托ｼ瑚ｯ･譁ｹ豕募・譛蛾夂畑逧・ｮ樒鴫・梧叛蝨ｨ豁､螟・
     * @param gunItem 譫ｪ譴ｰ迚ｩ蜩・
     * @param needAmmoCount 髴隕∫噪蠑ｹ闕ｯ(迚ｩ蜩・謨ｰ驥・
     * @return 謇ｾ蛻ｰ逧・ｼｹ闕ｯ(迚ｩ蜩・謨ｰ驥・
     */
    public int findAndExtractDummyAmmo(ItemStack gunItem, int needAmmoCount) {
        int dummyAmmoCount = getDummyAmmoAmount(gunItem);
        int extractCount = Math.min(dummyAmmoCount, needAmmoCount);
        addDummyAmmoAmount(gunItem, -extractCount);
        return extractCount;
    }

    /**
     * 譽譟･譫ｪ譴ｰ譏ｯ蜷ｦ蜈∬ｮｸ螳芽｣・欠螳夂噪迚ｩ蜩∽ｽ應ｸｺ驟堺ｻｶ
     */
    @Override
    public boolean allowAttachment(ItemStack gun, ItemStack attachmentItem) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null && iAttachment != null) {
            Identifier gunId = iGun.getGunId(gun);
            Identifier attachmentId = iAttachment.getAttachmentId(attachmentItem);
            return AllowAttachmentTagMatcher.match(gunId, attachmentId);
        }
        return false;
    }

    /**
     * 譽譟･譫ｪ譴ｰ譏ｯ蜷ｦ蜈∬ｮｸ螳芽｣・汾遘咲ｱｻ蝙狗噪驟堺ｻｶ
     */
    @Override
    public boolean allowAttachmentType(ItemStack gun, AttachmentType type) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null) {
            return TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).map(gunIndex -> {
                List<AttachmentType> allowAttachments = gunIndex.getGunData().getAllowAttachments();
                if (allowAttachments == null) {
                    return false;
                }
                return allowAttachments.contains(type);
            }).orElse(false);
        } else {
            return false;
        }
    }

    /**
     * 闔ｷ蜿匁棯譴ｰ逧・仞遉ｺ蜷咲ｧｰ
     */
    @Override
    @Nonnull
    public Component getName(@Nonnull ItemStack stack) {
        Identifier gunId = this.getGunId(stack);
        Optional<ClientGunIndex> gunIndex = TimelessAPI.getClientGunIndex(gunId);
        if (gunIndex.isPresent()) {
            return Component.translatable(gunIndex.get().getName());
        }
        return super.getName(stack);
    }

    /**
     * 闔ｷ蜿匁汾荳邀ｻ TabType 逧・園譛画棯譴ｰ迚ｩ蜩∫噪螳樔ｾ九ら畑莠主｡ｫ蜈・・騾迚ｩ蜩∵丞柱譫ｪ譴ｰ蛻ｶ騾蜿ｰ縲・
     */
    public static NonNullList<ItemStack> fillItemCategory(GunTabType type) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        TimelessAPI.getAllCommonGunIndex().stream().sorted(idNameSort()).forEach(entry -> {
            CommonGunIndex index = entry.getValue();
            GunData gunData = index.getGunData();
            String key = type.name().toLowerCase(Locale.US);
            String indexType = index.getType();
            if (key.equals(indexType)) {
                ItemStack itemStack = GunItemBuilder.create()
                        .setId(entry.getKey())
                        .setFireMode(gunData.getFireModeSet().get(0))
                        .setAmmoCount(gunData.getAmmoAmount())
                        .setHeatData(gunData.hasHeatData())
                        .setAmmoInBarrel(true)
                        .build();
                stacks.add(itemStack);
            }
        });
        return stacks;
    }

    /**
     * 髦ｻ豁｢邇ｩ螳ｶ謇玖№謖･蜉ｨ
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            GunItemRendererWrapper renderer;

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GunItemRendererWrapper();
                }
                return renderer;
            }
        });
    }

    /**
     * 闔ｷ蜿門惠 Tooltip 荳ｭ貂ｲ譟鍋噪蝗ｾ迚・
     */
    @Override
    @Nonnull
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (stack.getItem() instanceof IGun iGun) {
            Optional<CommonGunIndex> optional = TimelessAPI.getCommonGunIndex(this.getGunId(stack));
            if (optional.isPresent()) {
                CommonGunIndex gunIndex = optional.get();
                Identifier ammoId = gunIndex.getGunData().getAmmoId();
                return Optional.of(new GunTooltip(stack, iGun, ammoId, gunIndex));
            }
        }
        return Optional.empty();
    }

    /**
     * 闔ｷ蜿匁弍蜷ｦ菴ｿ逕ｨ蠑ｹ闕ｯ逶ｴ隸ｻ
     * @param gun 譫ｪ譴ｰ
     * @return 譏ｯ蜷ｦ菴ｿ逕ｨ蠑ｹ闕ｯ逶ｴ隸ｻ
     */
    @Override
    public boolean useInventoryAmmo(ItemStack gun) {
        if (gun.getItem() instanceof IGun) {
            Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(this.getGunId(gun));
            if (gunIndexOptional.isEmpty()) {
                return false;
            }
            CommonGunIndex gunIndex = gunIndexOptional.get();
            // 譏ｯ蜷ｦ荳ｺ蠑ｹ闕ｯ逶ｴ隸ｻ
            return gunIndex.getGunData().getReloadData().getType().equals(FeedType.INVENTORY);
        }
        return false;
    }

    /**
     * 闔ｷ蜿匁弍蜷ｦ譛我ｾ帷ｻ吝ｼｹ闕ｯ逶ｴ隸ｻ逧・ｼｹ闕ｯ
     * @param gun 譫ｪ譴ｰ
     * @return 譏ｯ蜷ｦ譛我ｾ帷ｻ吝ｼｹ闕ｯ逶ｴ隸ｻ逧・ｼｹ闕ｯ
     */
    @Override
    public boolean hasInventoryAmmo(LivingEntity shooter, ItemStack gun, boolean needCheckAmmo) {
        // 螯よ棡荳肴弍閭悟桁逶ｴ隸ｻ・悟・逶ｴ謗･霑泌屓 false
        if (!useInventoryAmmo(gun)) {
            return false;
        }
        // 螯よ棡荳埼怙隕∵｣譟･蟄仙ｼｹ・悟・逶ｴ謗･霑泌屓 true
        if (!needCheckAmmo) {
            return true;
        }
        // 陌壽供螟・ｼｹ螟・炊
        if (useDummyAmmo(gun)) {
            return getDummyAmmoAmount(gun) > 0;
        }
        // 譽譟･閭悟桁蜀・噪蠑ｹ闕ｯ謨ｰ驥・
        return hasConsumableAmmo(shooter, gun);
    }

    /**
     * 闔ｷ蜿・RPM
     * @param gun 譫ｪ譴ｰ
     * @return RPM 謨ｰ蛟ｼ
     */
    public int getRPM(ItemStack gun) {
        if (gun.getItem() instanceof IGun iGun) {
            return TimelessAPI.getCommonGunIndex(this.getGunId(gun))
                    .map(CommonGunIndex::getGunData)
                    .map(gunData -> {
                        FireMode fireMode = getFireMode(gun);
                        int rpm = gunData.getRoundsPerMinute(fireMode);
                        if (iGun.hasHeatData(gun)) {
                            rpm *= (int) iGun.lerpRPM(gun);
                        }
                        return rpm;
                    }).orElse(300);
        }
        return 300;
    }

    /**
     * 闔ｷ蜿匁弍蜷ｦ蜿ｯ莉･雜ｴ荳句ｰ・・
     * @param gun 譫ｪ譴ｰ
     * @return 譏ｯ蜷ｦ蜿ｯ莉･雜ｴ荳句ｰ・・
     */
    public boolean isCanCrawl(ItemStack gun) {
        if (gun.getItem() instanceof IGun) {
            return TimelessAPI.getCommonGunIndex(this.getGunId(gun))
                    .map(CommonGunIndex::getGunData)
                    .map(GunData::isCanCrawl)
                    .orElse(false);
        }
        return false;
    }

    @Override
    public boolean isSame(ItemStack i, ItemStack j) {
        IGun iGun1 = IGun.getIGunOrNull(i);
        IGun iGun2 = IGun.getIGunOrNull(j);
        if (iGun1 != null && iGun2 != null) {
            return iGun1.getGunId(i).equals(iGun2.getGunId(j));
        }
        if (i.isEmpty() || j.isEmpty()) {
            return i.isEmpty() && j.isEmpty();
        }
        return ItemStack.matches(i, j);
    }
}

