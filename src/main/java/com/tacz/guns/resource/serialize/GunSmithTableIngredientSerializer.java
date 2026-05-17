package com.tacz.guns.resource.serialize;

import com.google.gson.*;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.crafting.GunSmithTableSerializer;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.util.IdHelper;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GunSmithTableIngredientSerializer implements JsonDeserializer<GunSmithTableIngredient> {
    @Override
    public GunSmithTableIngredient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            if (!jsonObject.has("item")) {
                throw new JsonSyntaxException("Expected " + jsonObject + " must has a item member");
            }
            JsonElement itemJson = jsonObject.get("item");
            Ingredient ingredient = parseIngredient(itemJson);
            int count = 1;
            if (jsonObject.has("count")) {
                count = Math.max(GsonHelper.getAsInt(jsonObject, "count"), 1);
            }
            return new GunSmithTableIngredient(ingredient, count);
        } else {
            throw new JsonSyntaxException("Expected " + json + " to be a Pair because it's not an object");
        }
    }

    private static Ingredient parseIngredient(JsonElement itemJson) {
        DynamicOps<?> activeDecodeOps = GunSmithTableSerializer.getActiveDecodeOps();
        if (itemJson.isJsonObject()) {
            JsonObject itemObject = itemJson.getAsJsonObject();
            if (itemObject.has("tag")) {
                Ingredient parsedTagIngredient = parseTagIngredient(itemObject.get("tag"), activeDecodeOps);
                if (parsedTagIngredient != null) {
                    return parsedTagIngredient;
                }
            }
        }
        if (activeDecodeOps != null) {
            Optional<Ingredient> parsedWithContext = parseIngredientWithOps(activeDecodeOps, itemJson);
            if (parsedWithContext.isPresent()) {
                return parsedWithContext.get();
            }
        }
        Optional<Ingredient> parsed = Ingredient.CODEC.parse(JsonOps.INSTANCE, itemJson).result();
        if (parsed.isPresent()) {
            return parsed.get();
        }
        if (itemJson.isJsonObject()) {
            JsonObject itemObject = itemJson.getAsJsonObject();
            if (itemObject.has("item")) {
                String itemName = GsonHelper.convertToString(itemObject.get("item"), "item");
                Identifier itemId = Identifier.tryParse(itemName);
                if (itemId != null) {
                    Item item = BuiltInRegistries.ITEM.getValue(itemId);
                    if (item != null) {
                        return Ingredient.of(item);
                    }
                }
            }
            if (itemObject.has("tag")) {
                Ingredient parsedTagIngredient = parseTagIngredient(itemObject.get("tag"), activeDecodeOps);
                if (parsedTagIngredient != null) {
                    return parsedTagIngredient;
                }
            }
        }
        throw new JsonSyntaxException("Expected valid ingredient item: " + itemJson);
    }

    private static Ingredient parseTagIngredient(JsonElement tagElement, DynamicOps<?> activeDecodeOps) {
        String tagName = GsonHelper.convertToString(tagElement, "tag");
        Identifier tagId = Identifier.tryParse(tagName);
        if (tagId == null) {
            return null;
        }
        List<Identifier> candidates = getTagCandidates(tagId);
        if (activeDecodeOps != null) {
            for (Identifier candidate : candidates) {
                JsonObject normalizedTagObject = new JsonObject();
                normalizedTagObject.addProperty("tag", candidate.toString());
                Optional<Ingredient> parsedTagObject = parseIngredientWithOps(activeDecodeOps, normalizedTagObject);
                if (parsedTagObject.isPresent()) {
                    return parsedTagObject.get();
                }
                Optional<Ingredient> parsedHashedTag = parseIngredientWithOps(activeDecodeOps, new JsonPrimitive("#" + candidate));
                if (parsedHashedTag.isPresent()) {
                    return parsedHashedTag.get();
                }
            }
        } else {
            for (Identifier candidate : candidates) {
                Ingredient resolvedTagIngredient = ingredientFromTag(candidate);
                if (resolvedTagIngredient != null) {
                    return resolvedTagIngredient;
                }
            }
        }
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, candidates.get(0));
        HolderSet.Named<Item> named = HolderSet.emptyNamed(BuiltInRegistries.ITEM, tagKey);
        return Ingredient.of(named);
    }

    private static List<Identifier> getTagCandidates(Identifier tagId) {
        String namespace = tagId.getNamespace();
        if ("c".equals(namespace)) {
            return List.of(IdHelper.id("forge", tagId.getPath()), tagId);
        }
        if ("forge".equals(namespace)) {
            return List.of(tagId, IdHelper.id("c", tagId.getPath()));
        }
        List<Identifier> single = new ArrayList<>(1);
        single.add(tagId);
        return single;
    }

    @SuppressWarnings("unchecked")
    private static Optional<Ingredient> parseIngredientWithOps(DynamicOps<?> ops, JsonElement itemJson) {
        DynamicOps<Object> typedOps = (DynamicOps<Object>) ops;
        Object converted = JsonOps.INSTANCE.convertTo(typedOps, itemJson);
        return Ingredient.CODEC.parse(typedOps, converted).result();
    }

    private static Ingredient ingredientFromTag(Identifier tagId) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
        Item[] items = BuiltInRegistries.ITEM.stream()
                .filter(item -> item.builtInRegistryHolder().is(tagKey))
                .toArray(Item[]::new);
        if (items.length == 0) {
            return null;
        }
        return Ingredient.of(items);
    }
}
