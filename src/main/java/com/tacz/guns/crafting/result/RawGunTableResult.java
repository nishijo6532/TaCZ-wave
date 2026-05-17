package com.tacz.guns.crafting.result;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import com.tacz.guns.resource.pojo.data.recipe.GunResult;
import com.tacz.guns.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;


/**
 * 驟肴婿蜉霓ｽ譌ｶ驛ｨ蛻・黄蜩∫噪荳贋ｸ区枚霑俶悴螳梧・蛻晏ｧ句喧<br/>
 * 遲牙ｾ・芦螳樣刔髴隕∽ｽｿ逕ｨ驟肴婿譌ｶ蜀崎ｿ幄｡悟・蟋句喧
 */
public class RawGunTableResult {
    private final String type;
    private final int count;
    private final Identifier id;
    @Nullable
    private GunResult extraData;
    @Nullable
    private CompoundTag nbt;

    public RawGunTableResult(@NotNull String type, @NotNull Identifier id, int count) {
        this.type = type;
        this.id = id;
        this.count = count;
    }

    public void setExtraData(@Nullable GunResult extraData) {
        this.extraData = extraData;
    }

    public void setNbt(@Nullable CompoundTag nbt) {
        this.nbt = nbt;
    }

    public static GunSmithTableResult init(RawGunTableResult raw) {
        GunSmithTableResult result = switch (raw.type) {
            case GunSmithTableResult.GUN -> raw.getGunStack();
            case GunSmithTableResult.AMMO -> raw.getAmmoStack();
            case GunSmithTableResult.ATTACHMENT -> raw.getAttachmentStack();
            default -> new GunSmithTableResult(ItemStack.EMPTY, TabConfig.TAB_EMPTY);
        };
        if (raw.nbt != null) {
            ItemStack resultStack = result.getResult();
            CompoundTag itemTag = NbtCompat.getOrCreateTag(resultStack);
            for (String key : raw.nbt.keySet()) {
                Tag tag = raw.nbt.get(key);
                if (tag != null) {
                    itemTag.put(key, tag);
                }
            }
            NbtCompat.setTag(resultStack, itemTag);
        }
        return result;
    }

    private GunSmithTableResult getGunStack() {
        int ammoCount;
        EnumMap<AttachmentType, Identifier> attachments;
        if (extraData != null) {
            ammoCount = Math.max(0, extraData.getAmmoCount());
            attachments = extraData.getAttachments();
        } else {
            ammoCount = 0;
            attachments = new EnumMap<>(AttachmentType.class);
        }

        return TimelessAPI.getCommonGunIndex(id).map(gunIndex -> {
            ItemStack itemStack = GunItemBuilder.create()
                    .setCount(count)
                    .setId(id)
                    .setAmmoCount(ammoCount)
                    .setAmmoInBarrel(false)
                    .putAllAttachment(attachments)
                    .setFireMode(gunIndex.getGunData().getFireModeSet().get(0)).build();
            String raw = gunIndex.getType();
            if (!raw.contains(":")) {
                raw = GunMod.MOD_ID + ":" + raw;
            }
            Identifier group = Identifier.tryParse(raw);
            return new GunSmithTableResult(itemStack, group);
        }).orElse(new GunSmithTableResult(ItemStack.EMPTY, TabConfig.TAB_EMPTY));
    }

    private GunSmithTableResult getAmmoStack() {
        return new GunSmithTableResult(AmmoItemBuilder.create().setCount(count).setId(id).build(), TabConfig.TAB_AMMO);
    }

    private GunSmithTableResult getAttachmentStack() {
        return TimelessAPI.getCommonAttachmentIndex(id).map(attachmentIndex -> {
            ItemStack itemStack = AttachmentItemBuilder.create().setCount(count).setId(id).build();
            String raw = attachmentIndex.getType().name().toLowerCase(Locale.US);
            if (!raw.contains(":")) {
                raw = GunMod.MOD_ID + ":" + raw;
            }
            Identifier group = Identifier.tryParse(raw);
            return new GunSmithTableResult(itemStack, group);
        }).orElse(new GunSmithTableResult(ItemStack.EMPTY, TabConfig.TAB_EMPTY));
    }
}

