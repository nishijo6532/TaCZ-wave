package com.tacz.guns.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CraftingHelper {
    private CraftingHelper() {
    }

    public static ItemStack getItemStack(JsonObject json, boolean readNbt) {
        return getItemStack(json, readNbt, true);
    }

    public static ItemStack getItemStack(JsonObject json, boolean readNbt, boolean ignored) {
        String itemName = json.has("item") ? GsonHelper.getAsString(json, "item") : GsonHelper.getAsString(json, "id");
        Identifier itemId = Identifier.tryParse(itemName);
        if (itemId == null) {
            throw new JsonSyntaxException("Invalid item id: " + itemName);
        }

        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (item == null) {
            throw new JsonSyntaxException("Unknown item id: " + itemName);
        }

        int count = Math.max(1, GsonHelper.getAsInt(json, "count", 1));
        ItemStack stack = new ItemStack(item, count);
        if (readNbt && json.has("nbt")) {
            NbtCompat.setTag(stack, getNBT(json.get("nbt")));
        }
        return stack;
    }

    public static CompoundTag getNBT(JsonElement json) {
        String raw = json.isJsonPrimitive() ? GsonHelper.convertToString(json, "nbt") : json.toString();
        try {
            return TagParser.parseCompoundFully(raw);
        } catch (CommandSyntaxException e) {
            throw new JsonSyntaxException("Invalid nbt: " + raw, e);
        }
    }
}
