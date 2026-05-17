package com.tacz.guns.resource.manager;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.tacz.guns.resource.network.DataType;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * 譛榊苅遶ｯ萓ｧ謨ｰ謐ｮ邂｡逅・勣<br>
 * 隸･邀ｻ蝙狗噪謨ｰ謐ｮ邂｡逅・勣逕ｨ莠取恪蜉｡遶ｯ謨ｰ謐ｮ蜉霓ｽ蟷ｶ蜷大ｮ｢謌ｷ遶ｯ蜷梧ｭ･
 * @param <T> 謨ｰ謐ｮ邀ｻ蝙・
 */
public class CommonDataManager<T> extends JsonDataManager<T> implements INetworkCacheReloadListener {
    private final DataType type;
    protected Map<Identifier, String> networkCache;

    public CommonDataManager(DataType type, Class<T> dataClass, Gson pGson, String directory, String marker) {
        super(dataClass, pGson, directory, marker);
        this.type = type;
    }

    public CommonDataManager(DataType type, Class<T> dataClass, Gson pGson, FileToIdConverter fileToIdConverter, String marker) {
        super(dataClass, pGson, fileToIdConverter, marker);
        this.type = type;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        super.apply(pObject, pResourceManager, pProfiler);

        ImmutableMap.Builder<Identifier, String> builder = ImmutableMap.builder();
        pObject.forEach((id, element) -> builder.put(id, element.toString()));
        this.networkCache = builder.build();
    }

    public void clear() {
        this.dataMap.clear();
    }

    public Map<Identifier, String> getNetworkCache() {
        return this.networkCache;
    }

    public DataType getType() {
        return this.type;
    }
}

