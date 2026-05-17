package com.tacz.guns.client.event;

import com.tacz.guns.client.resource.InternalAssetLoader;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ReloadResourceEvent {
    public static final Identifier BLOCK_ATLAS_TEXTURE = com.tacz.guns.util.IdHelper.id("textures/atlas/blocks.png");

    @SubscribeEvent
    public static void onTextureStitchEventPost(TextureStitchEvent.Post event) {
        if (BLOCK_ATLAS_TEXTURE.equals(event.getAtlas().location())) {
            // InternalAssetLoader 髴隕∝刈霓ｽ荳莠幃ｻ倩ｮ､逧・勘逕ｻ縲∵ｨ｡蝙具ｼ碁怙隕∝・莠取棯蛹・刈霓ｽ縲・
            InternalAssetLoader.onResourceReload();
//            ClientReloadManager.reloadAllPack();
        }
    }
}


