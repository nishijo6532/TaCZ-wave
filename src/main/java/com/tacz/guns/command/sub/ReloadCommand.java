package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.concurrent.TimeUnit;

public final class ReloadCommand {
    private static final String RELOAD_NAME = "reload";

    private ReloadCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal(RELOAD_NAME)
                .executes(ReloadCommand::reloadAllPack);
    }

    private static int reloadAllPack(CommandContext<CommandSourceStack> context) {
        long start = System.nanoTime();
        if (FMLLoader.getDist().isClient()) {
            reloadClient();
        } else {
            CommonAssetsManager.reloadAllPack();
        }
        double time = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start) / 1000.0;
        context.getSource().sendSystemMessage(Component.translatable("commands.tacz.reload.success", time));
        return Command.SINGLE_SUCCESS;
    }

    public static void reloadClient() {
        ClientAssetsManager.reloadAllPack();
    }
}
