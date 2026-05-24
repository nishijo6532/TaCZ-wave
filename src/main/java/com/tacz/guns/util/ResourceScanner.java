package com.tacz.guns.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import com.tacz.guns.GunMod;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

public class ResourceScanner {
    /**
     * 謇ｫ謠乗欠螳夂岼蠖穂ｸ狗噪謇譛泳son譁・ｻｶ<br>
     * 荳主次迚育噪scanDirectory譁ｹ豕慕噪蛹ｺ蛻ｫ蝨ｨ莠趣ｼ梧衍隸｢扈捺棡譏ｯ菴應ｸｺ霑泌屓蛟ｼ霑泌屓逧・ｼ瑚御ｸ泌・隶ｸ豕ｨ驥・
     * 蟇ｹ莠守嶌蜷檎噪譁・ｻｶ霍ｯ蠕・ｼ悟宵隸ｻ蜿紋ｼ伜・郤ｧ譛鬮倡噪譁・ｻｶ
     * @param pResourceManager 襍・ｺ千ｮ｡逅・勣
     * @param pName 逶ｮ蠖募錐
     * @param pGson Gson螳樔ｾ・
     * @return 謇ｫ謠丞芦逧・son譁・ｻｶ
     */
    public static Map<Identifier, JsonElement> scanDirectory(ResourceManager pResourceManager, String pName, Gson pGson) {
        return scanDirectory(pResourceManager, FileToIdConverter.json(pName), pGson);
    }

    public static Map<Identifier, JsonElement> scanDirectory(ResourceManager pResourceManager, FileToIdConverter filetoidconverter, Gson pGson) {
        Map<Identifier, JsonElement> output = Maps.newHashMap();
        for(Map.Entry<Identifier, Resource> entry : filetoidconverter.listMatchingResources(pResourceManager).entrySet()) {
            Identifier Identifier = entry.getKey();
            Identifier resourcelocation1 = filetoidconverter.fileToId(Identifier);

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonReader jsonReader = new JsonReader(reader);
                jsonReader.setStrictness(Strictness.LENIENT);
                JsonElement jsonelement = pGson.fromJson(jsonReader, JsonElement.class);
                JsonElement jsonelement1 = output.put(resourcelocation1, jsonelement);
                if (jsonelement1 != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + resourcelocation1);
                }
            } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                GunMod.LOGGER.error("Couldn't parse data file {} from {}", resourcelocation1, Identifier, jsonparseexception);
            }
        }
        return output;
    }

    public static Map<Identifier, Identifier> scanDirectoryResources(ResourceManager pResourceManager, FileToIdConverter filetoidconverter) {
        Map<Identifier, Identifier> output = Maps.newHashMap();
        for (Identifier resourceLocation : filetoidconverter.listMatchingResources(pResourceManager).keySet()) {
            Identifier id = filetoidconverter.fileToId(resourceLocation);
            Identifier previous = output.put(id, resourceLocation);
            if (previous != null) {
                throw new IllegalStateException("Duplicate data file ignored with ID " + id);
            }
        }
        return output;
    }

    /**
     * 謇ｫ謠乗欠螳夂岼蠖穂ｸ狗噪謇譛泳son譁・ｻｶ<br/>
     * 荳施@link #scanDirectory(ResourceManager, String, Gson)}荳榊酔逧・弍・瑚ｯ･譁ｹ豕穂ｼ夊ｯｻ蜿匁園譛泳son譁・ｻｶ菴應ｸｺ蛻苓｡ｨ霑泌屓
     * @param pResourceManager 襍・ｺ千ｮ｡逅・勣
     * @param filetoidconverter 譁・ｻｶ霍ｯ蠕・柱id逧・丐蟆・
     * @param pGson Gson螳樔ｾ・
     * @return 謇ｫ謠丞芦逧・son譁・ｻｶ
     */
    public static Map<Identifier, List<JsonElement>> scanDirectoryAll(ResourceManager pResourceManager, FileToIdConverter filetoidconverter, Gson pGson) {
        Map<Identifier, List<JsonElement>> output = Maps.newHashMap();
        for(Map.Entry<Identifier, List<Resource>> entry : filetoidconverter.listMatchingResourceStacks(pResourceManager).entrySet()) {
            Identifier Identifier = entry.getKey();
            Identifier resourcelocation1 = filetoidconverter.fileToId(Identifier);

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonReader jsonReader = new JsonReader(reader);
                    jsonReader.setStrictness(Strictness.LENIENT);
                    JsonElement jsonelement = pGson.fromJson(jsonReader, JsonElement.class);
                    List<JsonElement> list = output.computeIfAbsent(resourcelocation1, k -> Lists.newArrayList());
                    list.add(jsonelement);
                } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                    GunMod.LOGGER.error("Couldn't parse data file {} from {}", resourcelocation1, Identifier, jsonparseexception);
                }
            }
        }
        return output;
    }
}

