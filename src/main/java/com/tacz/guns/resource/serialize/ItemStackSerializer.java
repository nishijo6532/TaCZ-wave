package com.tacz.guns.resource.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.util.CraftingHelper;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Type;

public class ItemStackSerializer implements JsonDeserializer<ItemStack> {
    @Override
    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) {
            throw new JsonSyntaxException("Expected " + json + " to be a ItemStack because it's not an object");
        }
        JsonObject jsonObject = json.getAsJsonObject();
        try {
            return CraftingHelper.getItemStack(jsonObject, true, false);
        } catch (RuntimeException e) {
            if (isComponentsNotBound(e)) {
                return ItemStack.EMPTY;
            }
            throw e;
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
}
