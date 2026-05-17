package com.tacz.guns.event;

import com.tacz.guns.util.CycleTaskHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ServerTickEvent {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Pre event) {
        CycleTaskHelper.tick();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        CycleTaskHelper.tick();
    }
}
