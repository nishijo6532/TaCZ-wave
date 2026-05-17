package com.tacz.guns.client.resource.manager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.pojo.display.IDisplay;
import com.tacz.guns.resource.manager.JsonDataManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * 騾夂畑謨ｰ謐ｮ邂｡逅・勣<br>
 * 莉手ｵ・ｺ仙桁/謨ｰ謐ｮ蛹・ｸｭ隸ｻ蜿頬son譁・ｻｶ蟷ｶ隗｣譫蝉ｸｺ謨ｰ謐ｮ
 * @param <T> 謨ｰ謐ｮ邀ｻ蝙・
 */
public class DisplayManager<T extends IDisplay> extends JsonDataManager<T> {

    public DisplayManager(Class<T> dataClass, Gson pGson, String directory, String marker) {
        super(dataClass, pGson, FileToIdConverter.json(directory), marker);
    }

    public DisplayManager(Class<T> dataClass, Gson pGson, FileToIdConverter fileToIdConverter, String marker) {
        super(dataClass, pGson, fileToIdConverter, marker);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        dataMap.clear();
        for (Map.Entry<Identifier, JsonElement> entry : pObject.entrySet()) {
            Identifier id = entry.getKey();
            JsonElement element = entry.getValue();
            try {
                T data = getGson().fromJson(element, getDataClass());
                if (data != null) {
                    data.init();
                    dataMap.put(id, data);
                }
            } catch (JsonParseException | IllegalArgumentException e) {
                GunMod.LOGGER.error(getMarker(), "Failed to load data file {}", id, e);
            }
        }
    }

}

