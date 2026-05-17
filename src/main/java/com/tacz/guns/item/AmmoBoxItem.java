package com.tacz.guns.item;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.nbt.AmmoBoxItemDataAccessor;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.inventory.tooltip.AmmoBoxTooltip;
import com.tacz.guns.util.NbtCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class AmmoBoxItem extends Item implements AmmoBoxItemDataAccessor {
    public static final Identifier PROPERTY_NAME = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "ammo_statue");

    public static final int IRON_LEVEL = 0;
    public static final int GOLD_LEVEL = 1;
    public static final int DIAMOND_LEVEL = 2;

    private static final String DISPLAY_TAG = "display";
    private static final String COLOR_TAG = "color";

    private static final int OPEN = 0;
    private static final int CLOSE = 1;

    private static final int CREATIVE_INDEX = 6;
    private static final int ALL_TYPE_CREATIVE_INDEX = 8;

    public AmmoBoxItem() {
        this(new Properties().stacksTo(1));
    }

    public AmmoBoxItem(Properties properties) {
        super(properties);
    }

    public static int getColor(ItemStack stack, int tintIndex) {
        return tintIndex > 0 ? -1 : getTagColor(stack);
    }

    public static float getStatue(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        int openStatue = OPEN;
        int ammoLevel = IRON_LEVEL;
        if (stack.getItem() instanceof IAmmoBox iAmmoBox) {
            if (iAmmoBox.isAllTypeCreative(stack)) {
                return ALL_TYPE_CREATIVE_INDEX;
            }
            openStatue = getOpenStatue(stack, iAmmoBox);
            if (iAmmoBox.isCreative(stack)) {
                return openStatue + CREATIVE_INDEX;
            }
            ammoLevel = getLevelStatue(stack, iAmmoBox);
        }
        return openStatue + 2 * ammoLevel;
    }

    private static int getOpenStatue(ItemStack stack, IAmmoBox iAmmoBox) {
        boolean idIsEmpty = iAmmoBox.getAmmoId(stack).equals(DefaultAssets.EMPTY_AMMO_ID);
        boolean countIsZero = iAmmoBox.getAmmoCount(stack) <= 0;
        if (idIsEmpty || countIsZero) {
            return OPEN;
        }
        return CLOSE;
    }

    private static int getLevelStatue(ItemStack stack, IAmmoBox iAmmoBox) {
        return iAmmoBox.getAmmoLevel(stack);
    }

    private static int getTagColor(ItemStack stack) {
        CompoundTag stackTag = NbtCompat.getTag(stack);
        if (!NbtCompat.contains(stackTag, DISPLAY_TAG, Tag.TAG_COMPOUND)) {
            return 0x727d6b;
        }
        CompoundTag displayTag = NbtCompat.getCompound(stackTag, DISPLAY_TAG);
        if (!NbtCompat.contains(displayTag, COLOR_TAG, NbtCompat.TAG_ANY_NUMERIC)) {
            return 0x727d6b;
        }
        return NbtCompat.getInt(displayTag, COLOR_TAG);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack pOther, Slot slot, ClickAction action, Player player, SlotAccess access) {
        return super.overrideOtherStackedOnMe(stack, pOther, slot, action, player, access);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack ammoBox, Slot slot, ClickAction action, Player player) {
        // 蜿ｳ蜃ｻ
        if (action == ClickAction.SECONDARY) {
            // 轤ｹ蜃ｻ逧・ｼ蟄・
            ItemStack slotItem = slot.getItem();
            Identifier boxAmmoId = this.getAmmoId(ammoBox);

            // 譬ｼ蟄蝉ｸｺ遨ｺ・碁ぅ蟆ｱ譏ｯ蜿門・迚ｩ蜩・
            if (slotItem.isEmpty()) {
                // 蛻幃讓｡蠑丞ｼｹ闕ｯ邂ｱ荳崎・蜿門・莉ｻ菴穂ｸ懆･ｿ
                if (isAllTypeCreative(ammoBox) || isCreative(ammoBox)) {
                    return false;
                }
                // 蝠･荵滓ｲ｡譛会ｼ御ｸ崎・蜿門・
                if (boxAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
                    return false;
                }
                // 謨ｰ驥丈ｸ榊ｯｹ・御ｸ崎・蜿門・
                int boxAmmoCount = this.getAmmoCount(ammoBox);
                if (boxAmmoCount <= 0) {
                    return false;
                }
                TimelessAPI.getCommonAmmoIndex(boxAmmoId).ifPresent(index -> {
                    int takeCount = Math.min(index.getStackSize(), boxAmmoCount);
                    ItemStack takeAmmo = AmmoItemBuilder.create().setId(boxAmmoId).setCount(takeCount).build();
                    slot.safeInsert(takeAmmo);

                    int remainCount = boxAmmoCount - takeCount;
                    this.setAmmoCount(ammoBox, remainCount);
                    if (remainCount <= 0) {
                        this.setAmmoId(ammoBox, DefaultAssets.EMPTY_AMMO_ID);
                    }
                    this.playRemoveOneSound(player);
                });
                return true;
            }

            // 螯よ棡譏ｯ蟄仙ｼｹ
            if (slotItem.getItem() instanceof IAmmo iAmmo) {
                // 蜈ｨ邀ｻ蝙句ｼｹ闕ｯ邂ｱ荳崎・蟄伜・
                if (isAllTypeCreative(ammoBox)) {
                    return false;
                }
                Identifier slotAmmoId = iAmmo.getAmmoId(slotItem);
                // 譬ｼ蟄宣㈹逧・ｭ仙ｼｹ ID 荳榊ｯｹ・御ｸ崎・謾ｾ
                if (slotAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
                    return false;
                }
                // 螯よ棡逶貞ｭ千噪蟄仙ｼｹ ID 荳ｺ遨ｺ・悟序謌仙ｽ灘燕轤ｹ蜃ｻ逧・ｱｻ蝙・
                if (boxAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
                    this.setAmmoId(ammoBox, slotAmmoId);
                } else if (!slotAmmoId.equals(boxAmmoId)) {
                    return false;
                }
                TimelessAPI.getCommonAmmoIndex(slotAmmoId).ifPresent(index -> {
                    // 蛻幃讓｡蠑丞ｼｹ闕ｯ邂ｱ・碁ぅ蟆ｱ逶ｴ謗･蟄伜・譛螟ｧ
                    if (isCreative(ammoBox)) {
                        this.setAmmoCount(ammoBox, Integer.MAX_VALUE);
                        return;
                    }
                    int boxAmmoCount = this.getAmmoCount(ammoBox);
                    int boxLevelMultiplier = this.getAmmoLevel(ammoBox) + 1;
                    int maxSize = index.getStackSize() * SyncConfig.AMMO_BOX_STACK_SIZE.get() * boxLevelMultiplier;
                    int needCount = maxSize - boxAmmoCount;
                    ItemStack takeItem = slot.safeTake(slotItem.getCount(), needCount, player);
                    this.setAmmoCount(ammoBox, boxAmmoCount + takeItem.getCount());
                });
                // 謦ｭ謾ｾ蜿門・螢ｰ髻ｳ
                this.playInsertSound(player);
                return true;
            }
        }
        return false;
    }

    private void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (isAllTypeCreative(stack) || isCreative(stack)) {
            return false;
        }
        return !this.getAmmoId(stack).equals(DefaultAssets.EMPTY_AMMO_ID) && this.getAmmoCount(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        Identifier ammoId = this.getAmmoId(stack);
        int ammoCount = this.getAmmoCount(stack);
        int boxLevelMultiplier = this.getAmmoLevel(stack) + 1;
        double widthPercent = TimelessAPI.getCommonAmmoIndex(ammoId).map(index -> {
            double totalCount = index.getStackSize() * SyncConfig.AMMO_BOX_STACK_SIZE.get() * boxLevelMultiplier;
            return ammoCount / totalCount;
        }).orElse(0d);
        return (int) Math.min(1 + 12 * widthPercent, 13);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isAllTypeCreative(stack)) {
            return Component.translatable("item.tacz.ammo_box.all_type_creative").withStyle(ChatFormatting.DARK_PURPLE);
        }
        if (isCreative(stack)) {
            return Component.translatable("item.tacz.ammo_box.creative").withStyle(ChatFormatting.DARK_PURPLE);
        }
        int ammoLevel = getAmmoLevel(stack);
        switch (ammoLevel) {
            case GOLD_LEVEL -> {
                return Component.translatable("item.tacz.ammo_box.gold").withStyle(ChatFormatting.YELLOW);
            }
            case DIAMOND_LEVEL -> {
                return Component.translatable("item.tacz.ammo_box.diamond").withStyle(ChatFormatting.AQUA);
            }
            default -> {
                return Component.translatable("item.tacz.ammo_box.iron");
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        if (isAllTypeCreative(stack) || isCreative(stack)) {
            return true;
        }
        return super.isFoil(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(1 / 3f, 1.0F, 1.0F);
    }

    public static void fillItemCategory(CreativeModeTab.Output output) {
        ItemStack ammoBox = ModItems.AMMO_BOX.get().getDefaultInstance();
        if (ammoBox.getItem() instanceof IAmmoBox iAmmoBox) {
            // 豺ｻ蜉譎ｮ騾夂沿譛ｬ逧・ｼｹ闕ｯ逶・
            output.accept(iAmmoBox.setAmmoLevel(ammoBox.copy(), IRON_LEVEL));
            output.accept(iAmmoBox.setAmmoLevel(ammoBox.copy(), GOLD_LEVEL));
            output.accept(iAmmoBox.setAmmoLevel(ammoBox.copy(), DIAMOND_LEVEL));

            // 豺ｻ蜉蛻幃讓｡蠑丞ｼｹ闕ｯ逶・
            output.accept(iAmmoBox.setCreative(ammoBox.copy(), false));
            output.accept(iAmmoBox.setCreative(ammoBox.copy(), true));
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (!(stack.getItem() instanceof IAmmoBox iAmmoBox)) {
            return Optional.empty();
        }
        Identifier ammoId = iAmmoBox.getAmmoId(stack);
        if (ammoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
            return Optional.empty();
        }
        int ammoCount = iAmmoBox.getAmmoCount(stack);
        if (ammoCount <= 0) {
            return Optional.empty();
        }
        ItemStack ammoStack = AmmoItemBuilder.create().setId(ammoId).build();
        return Optional.of(new AmmoBoxTooltip(stack, ammoStack, ammoCount));
    }

    public void appendHoverText(ItemStack stack, @Nullable Level pLevel, List<Component> components, TooltipFlag isAdvanced) {
        if (isAllTypeCreative(stack)) {
            components.add(Component.translatable("tooltip.tacz.ammo_box.usage.all_type_creative").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (isCreative(stack)) {
            components.add(Component.translatable("tooltip.tacz.ammo_box.usage.creative.1").withStyle(ChatFormatting.YELLOW));
            components.add(Component.translatable("tooltip.tacz.ammo_box.usage.creative.2").withStyle(ChatFormatting.YELLOW));
            return;
        }
        components.add(Component.translatable("tooltip.tacz.ammo_box.usage.deposit").withStyle(ChatFormatting.GRAY));
        components.add(Component.translatable("tooltip.tacz.ammo_box.usage.remove").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag isAdvanced) {
        List<Component> components = new ArrayList<>();
        appendHoverText(stack, null, components, isAdvanced);
        components.forEach(consumer);
    }
}


