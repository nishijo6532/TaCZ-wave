package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ListCommand {
    private static final String LIST_NAME = "list";

    private ListCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal(LIST_NAME)
                .executes(ListCommand::listAll)
                .then(Commands.literal("guns").executes(context -> listIds(context, "guns", gunIds())))
                .then(Commands.literal("ammo").executes(context -> listIds(context, "ammo", ammoIds())))
                .then(Commands.literal("attachments").executes(context -> listIds(context, "attachments", attachmentIds())));
    }

    private static int listAll(CommandContext<CommandSourceStack> context) {
        listIds(context, "guns", gunIds());
        listIds(context, "ammo", ammoIds());
        listIds(context, "attachments", attachmentIds());
        return Command.SINGLE_SUCCESS;
    }

    private static int listIds(CommandContext<CommandSourceStack> context, String label, Stream<Identifier> ids) {
        Map<String, Long> counts = ids
                .sorted(Comparator.comparing(Identifier::toString))
                .collect(Collectors.groupingBy(Identifier::getNamespace, LinkedHashMap::new, Collectors.counting()));
        if (counts.isEmpty()) {
            context.getSource().sendSystemMessage(Component.literal("TaCZ " + label + ": none"));
            return Command.SINGLE_SUCCESS;
        }

        String summary = counts.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
        context.getSource().sendSystemMessage(Component.literal("TaCZ " + label + ": " + summary));
        return Command.SINGLE_SUCCESS;
    }

    private static Stream<Identifier> gunIds() {
        return TimelessAPI.getAllCommonGunIndex().stream().map(Map.Entry::getKey);
    }

    private static Stream<Identifier> ammoIds() {
        return TimelessAPI.getAllCommonAmmoIndex().stream().map(Map.Entry::getKey);
    }

    private static Stream<Identifier> attachmentIds() {
        return TimelessAPI.getAllCommonAttachmentIndex().stream().map(Map.Entry::getKey);
    }
}
