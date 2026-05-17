package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.command.RootCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public final class CommandRegistry {
    private CommandRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        RootCommand.register(event.getDispatcher());
    }
}
