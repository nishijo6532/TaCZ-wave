package com.tacz.guns.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tacz.guns.command.sub.GiveCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class RootCommand {
    private static final String ROOT_NAME = "tacz";

    private RootCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        root.then(GiveCommand.get());
        dispatcher.register(root);
    }
}
