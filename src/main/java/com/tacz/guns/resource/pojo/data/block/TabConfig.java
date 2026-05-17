package com.tacz.guns.resource.pojo.data.block;

import com.google.gson.*;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;

public record TabConfig(Identifier id, String name, ItemStack icon) {
    public static final Identifier TAB_AMMO = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "ammo");

    public static final Identifier TAB_PISTOL = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "pistol");
    public static final Identifier TAB_SNIPER = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "sniper");
    public static final Identifier TAB_RIFLE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "rifle");
    public static final Identifier TAB_SHOTGUN = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "shotgun");
    public static final Identifier TAB_SMG = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "smg");
    public static final Identifier TAB_RPG = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "rpg");
    public static final Identifier TAB_MG = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "mg");

    public static final Identifier TAB_SCOPE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "scope");
    public static final Identifier TAB_MUZZLE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "muzzle");
    public static final Identifier TAB_STOCK = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "stock");
    public static final Identifier TAB_GRIP = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "grip");
    public static final Identifier TAB_EXTENDED_MAG = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "extended_mag");
    public static final Identifier TAB_LASER = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "laser");

    public static final Identifier TAB_MISC = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "misc");
    public static final Identifier TAB_EMPTY = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "empty");

    public static List<TabConfig> defaultTabs() {
        return DefaultTabsHolder.TABS;
    }

    private static final class DefaultTabsHolder {
        private static final List<TabConfig> TABS = List.of(
                new TabConfig(TabConfig.TAB_AMMO, "tacz.type.ammo.name", AmmoItemBuilder.create().setId(DefaultAssets.DEFAULT_AMMO_ID).build()),
                new TabConfig(TabConfig.TAB_PISTOL, "tacz.type.pistol.name", GunItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "glock_17")).forceBuild()),
                new TabConfig(TabConfig.TAB_SNIPER, "tacz.type.sniper.name", GunItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "ai_awp")).forceBuild()),
                new TabConfig(TabConfig.TAB_RIFLE, "tacz.type.rifle.name", GunItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "ak47")).forceBuild()),
                new TabConfig(TabConfig.TAB_SHOTGUN, "tacz.type.shotgun.name", GunItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "db_short")).forceBuild()),
                new TabConfig(TabConfig.TAB_SMG, "tacz.type.smg.name", GunItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "hk_mp5a5")).forceBuild()),
                new TabConfig(TabConfig.TAB_RPG, "tacz.type.rpg.name", GunItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "rpg7")).forceBuild()),
                new TabConfig(TabConfig.TAB_MG, "tacz.type.mg.name", GunItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "m249")).forceBuild()),
                new TabConfig(TabConfig.TAB_SCOPE, "tacz.type.scope.name",  AttachmentItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "scope_acog_ta31")).build()),
                new TabConfig(TabConfig.TAB_MUZZLE, "tacz.type.muzzle.name", AttachmentItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "muzzle_compensator_trident")).build()),
                new TabConfig(TabConfig.TAB_STOCK, "tacz.type.stock.name", AttachmentItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "stock_militech_b5")).build()),
                new TabConfig(TabConfig.TAB_GRIP, "tacz.type.grip.name", AttachmentItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "grip_magpul_afg_2")).build()),
                new TabConfig(TabConfig.TAB_EXTENDED_MAG, "tacz.type.extended_mag.name", AttachmentItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "extended_mag_3")).build()),
                new TabConfig(TabConfig.TAB_LASER, "tacz.type.laser.name", AttachmentItemBuilder.create().setId(com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "laser_compact")).build()),
                new TabConfig(TabConfig.TAB_MISC, "tacz.type.misc.name", ModItems.GUN_SMITH_TABLE.get().getDefaultInstance())
        );
    }

    public static class Deserializer implements JsonDeserializer<TabConfig> {
        @Override
        public TabConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) {
                throw new JsonParseException("TabConfig must be a JSON object");
            }
            JsonObject object = json.getAsJsonObject();
            if (!object.has("id") || !object.get("id").isJsonPrimitive()) {
                throw new JsonParseException("TabConfig must have an id");
            }
            Identifier id = context.deserialize(object.get("id"), Identifier.class);
            ItemStack icon = deserializeIcon(object, id, context);
            String name = GsonHelper.getAsString(object, "name", "tacz.type.unknown.name");
            return new TabConfig(id, name, icon);
        }

        private static ItemStack deserializeIcon(JsonObject object, Identifier tabId, JsonDeserializationContext context) {
            JsonObject iconObject = GsonHelper.getAsJsonObject(object, "icon");
            try {
                return context.deserialize(iconObject, ItemStack.class);
            } catch (RuntimeException e) {
                if (!isComponentsNotBound(e)) {
                    throw e;
                }
                ItemStack fallback = getDefaultIcon(tabId);
                return fallback.isEmpty() ? ItemStack.EMPTY : fallback;
            }
        }

        private static boolean isComponentsNotBound(Throwable throwable) {
            Throwable current = throwable;
            while (current != null) {
                if (current instanceof NullPointerException && "Components not bound yet".equals(current.getMessage())) {
                    return true;
                }
                current = current.getCause();
            }
            return false;
        }

        private static ItemStack getDefaultIcon(Identifier tabId) {
            for (TabConfig tab : defaultTabs()) {
                if (tab.id().equals(tabId)) {
                    return tab.icon().copy();
                }
            }
            return ItemStack.EMPTY;
        }
    }

    @NotNull
    public Component getName() {
        return Component.translatable(name==null ? "tacz.type.unknown.name" : name);
    }
}


