package com.tacz.guns.client.resource.manager;

import com.google.common.collect.Maps;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.animation.gltf.AnimationStructure;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.animation.gltf.RawAnimationStructure;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;


public class GltfManager extends SimplePreparableReloadListener<Map<Identifier, AnimationStructure>> {
    private static final Marker MARKER = MarkerManager.getMarker("GltfAnimationLoader");

    private final Map<Identifier, AnimationStructure> dataMap = Maps.newHashMap();
    private final FileToIdConverter filetoidconverter = new FileToIdConverter("animations", ".gltf");

    @Override
    @NotNull
    protected Map<Identifier, AnimationStructure> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<Identifier, AnimationStructure> output = Maps.newHashMap();
        for(Map.Entry<Identifier, Resource> entry : filetoidconverter.listMatchingResources(pResourceManager).entrySet()) {
            Identifier Identifier = entry.getKey();
            Identifier resourcelocation1 = filetoidconverter.fileToId(Identifier);

            try (Reader reader = entry.getValue().openAsReader()) {
                RawAnimationStructure rawStructure = ClientAssetsManager.GSON.fromJson(reader, RawAnimationStructure.class);
                AnimationStructure animationStructure = new AnimationStructure(rawStructure);
                output.put(resourcelocation1, animationStructure);
            } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read gltf animation file: {}", Identifier);
            }
        }
        return output;
    }

    @Override
    protected void apply(Map<Identifier, AnimationStructure> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        dataMap.clear();
        dataMap.putAll(pObject);
    }

    public AnimationStructure getGltfAnimation(Identifier id) {
        return dataMap.get(id);
    }
}

