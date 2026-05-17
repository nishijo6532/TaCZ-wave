package com.tacz.guns.init;

import com.tacz.guns.resource.GunPackLoader;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class PackFinderRegistry {
    private PackFinderRegistry() {
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        event.addRepositorySource(onLoad -> {
            GunPackLoader.INSTANCE.packType = event.getPackType();
            GunPackLoader.INSTANCE.loadPacks(onLoad);
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeModificationEvent event) {
        var attributeHolder = ModAttributes.BULLET_RESISTANCE.getHolder().orElse(null);
        if (attributeHolder == null) {
            return;
        }
        event.getTypes().forEach(type -> {
            event.add(type, attributeHolder);
        });
    }
}
