package com.tacz.guns.crafting;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class GunSmithTableSerializer {
    private static final Identifier EMPTY_ID = Identifier.withDefaultNamespace("empty");
    private static final GunSmithTableRecipe EMPTY_RECIPE = new GunSmithTableRecipe(
            EMPTY_ID,
            new GunSmithTableResult(ItemStack.EMPTY, EMPTY_ID),
            List.of()
    );
    private static final ThreadLocal<DynamicOps<?>> ACTIVE_DECODE_OPS = new ThreadLocal<>();

    private static final MapCodec<GunSmithTableRecipe> MAP_CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of("materials", "result", "type").map(ops::createString);
        }

        @Override
        public <T> DataResult<GunSmithTableRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
            JsonObject jsonObject = new JsonObject();
            input.entries().forEach(entry -> {
                var keyElement = ops.convertTo(JsonOps.INSTANCE, entry.getFirst());
                if (keyElement.isJsonPrimitive()) {
                    jsonObject.add(keyElement.getAsString(), ops.convertTo(JsonOps.INSTANCE, entry.getSecond()));
                }
            });
            GunSmithTableRecipe recipe;
            ACTIVE_DECODE_OPS.set(ops);
            try {
                recipe = fromJson(EMPTY_ID, jsonObject);
            } finally {
                ACTIVE_DECODE_OPS.remove();
            }
            return recipe != null ? DataResult.success(recipe) : DataResult.error(() -> "Invalid gun smith table recipe json: " + jsonObject);
        }

        @Override
        public <T> RecordBuilder<T> encode(GunSmithTableRecipe input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            prefix.add("type", ops.createString("tacz:gun_smith_table_crafting"));
            prefix.add("materials", JsonOps.INSTANCE.convertTo(ops, CommonAssetsManager.GSON.toJsonTree(input.getInputs())));
            prefix.add("result", JsonOps.INSTANCE.convertTo(ops, CommonAssetsManager.GSON.toJsonTree(input.getResult())));
            return prefix;
        }
    };

    public static final RecipeSerializer<GunSmithTableRecipe> SERIALIZER = new RecipeSerializer<>(
            MAP_CODEC,
            StreamCodec.of(GunSmithTableSerializer::toNetwork, GunSmithTableSerializer::fromNetwork)
    );

    private GunSmithTableSerializer() {
    }

    public static RecipeSerializer<GunSmithTableRecipe> createSerializer() {
        return SERIALIZER;
    }

    public static DynamicOps<?> getActiveDecodeOps() {
        return ACTIVE_DECODE_OPS.get();
    }

    public static GunSmithTableRecipe fromJson(Identifier id, JsonObject jsonObject) {
        TableRecipe tableRecipe = CommonAssetsManager.GSON.fromJson(jsonObject, TableRecipe.class);
        if (tableRecipe != null) {
            return new GunSmithTableRecipe(id, tableRecipe);
        }
        return null;
    }

    public static GunSmithTableRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<GunSmithTableIngredient> ingredients = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ingredients.add(new GunSmithTableIngredient(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readInt()));
        }
        ItemStack resultItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        Identifier group = buffer.readIdentifier();
        GunSmithTableResult result = new GunSmithTableResult(resultItem, group);
        return new GunSmithTableRecipe(EMPTY_ID, result, ingredients);
    }

    public static void toNetwork(RegistryFriendlyByteBuf buffer, GunSmithTableRecipe recipe) {
        buffer.writeInt(recipe.getInputs().size());
        for (GunSmithTableIngredient ingredient : recipe.getInputs()) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.getIngredient());
            buffer.writeInt(ingredient.getCount());
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.getResult().getResult());
        buffer.writeIdentifier(recipe.getResult().getGroup());
    }
}
