package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.init.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class GiveCommand {
    private static final String GIVE_NAME = "give";
    private static final String TARGETS = "targets";
    private static final String ID = "id";
    private static final String COUNT = "count";
    private static final List<String> AMMO_BOX_TYPES = List.of("iron", "gold", "diamond", "creative", "all_type_creative");

    private static final SuggestionProvider<CommandSourceStack> AMMO_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(TimelessAPI.getAllCommonAmmoIndex().stream().map(entry -> entry.getKey()), builder);
    private static final SuggestionProvider<CommandSourceStack> GUN_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(TimelessAPI.getAllCommonGunIndex().stream().map(entry -> entry.getKey()), builder);
    private static final SuggestionProvider<CommandSourceStack> ATTACHMENT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(TimelessAPI.getAllCommonAttachmentIndex().stream().map(entry -> entry.getKey()), builder);
    private static final SuggestionProvider<CommandSourceStack> AMMO_BOX_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(AMMO_BOX_TYPES, builder);

    private GiveCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> give = Commands.literal(GIVE_NAME);
        give.then(Commands.argument(TARGETS, EntityArgument.players())
                .then(typedGive("ammo", AMMO_SUGGESTIONS, GiveCommand::buildAmmo))
                .then(typedGive("gun", GUN_SUGGESTIONS, GiveCommand::buildGun))
                .then(typedGive("attachment", ATTACHMENT_SUGGESTIONS, GiveCommand::buildAttachment))
                .then(typedGive("ammo_box", AMMO_BOX_SUGGESTIONS, GiveCommand::buildAmmoBox)));
        return give;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> typedGive(
            String type,
            SuggestionProvider<CommandSourceStack> suggestions,
            Function<Identifier, ItemStack> stackFactory
    ) {
        return Commands.literal(type)
                .then(Commands.argument(ID, IdentifierArgument.id())
                        .suggests(suggestions)
                        .executes(context -> give(context, type, stackFactory, 1))
                        .then(Commands.argument(COUNT, IntegerArgumentType.integer(1))
                                .executes(context -> give(context, type, stackFactory, IntegerArgumentType.getInteger(context, COUNT)))));
    }

    private static int give(
            CommandContext<CommandSourceStack> context,
            String type,
            Function<Identifier, ItemStack> stackFactory,
            int requestedCount
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, TARGETS);
        Identifier id = normalizeTacId(IdentifierArgument.getId(context, ID));
        ItemStack template = stackFactory.apply(id);
        if (template.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Unknown TaCZ " + type + ": " + id));
            return 0;
        }

        int givenStacks = 0;
        for (ServerPlayer player : targets) {
            givenStacks += giveCopies(player, template, requestedCount);
        }
        context.getSource().sendSystemMessage(Component.literal("Gave " + requestedCount + " " + type + " " + id + " to " + targets.size() + " player(s)"));
        return givenStacks == 0 ? Command.SINGLE_SUCCESS : givenStacks;
    }

    private static int giveCopies(ServerPlayer player, ItemStack template, int requestedCount) {
        int remaining = requestedCount;
        int givenStacks = 0;
        int maxStackSize = Math.max(1, template.getMaxStackSize());
        while (remaining > 0) {
            int count = Math.min(maxStackSize, remaining);
            ItemStack copy = template.copy();
            copy.setCount(count);
            ItemHandlerHelper.giveItemToPlayer(player, copy);
            remaining -= count;
            givenStacks++;
        }
        return givenStacks;
    }

    private static ItemStack buildAmmo(Identifier id) {
        if (TimelessAPI.getCommonAmmoIndex(id).isEmpty()) {
            return ItemStack.EMPTY;
        }
        return AmmoItemBuilder.create().setId(id).build();
    }

    private static ItemStack buildGun(Identifier id) {
        return TimelessAPI.getCommonGunIndex(id)
                .map(index -> GunItemBuilder.create()
                        .setId(id)
                        .setFireMode(index.getGunData().getFireModeSet().isEmpty() ? FireMode.UNKNOWN : index.getGunData().getFireModeSet().get(0))
                        .setAmmoCount(index.getGunData().getAmmoAmount())
                        .setHeatData(index.getGunData().hasHeatData())
                        .setAmmoInBarrel(true)
                        .build())
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack buildAttachment(Identifier id) {
        if (TimelessAPI.getCommonAttachmentIndex(id).isEmpty()) {
            return ItemStack.EMPTY;
        }
        return AttachmentItemBuilder.create().setId(id).build();
    }

    private static ItemStack buildAmmoBox(Identifier rawType) {
        String type = rawType.getPath().toLowerCase(Locale.ROOT);
        if (!AMMO_BOX_TYPES.contains(type)) {
            return ItemStack.EMPTY;
        }
        ItemStack ammoBox = new ItemStack(ModItems.AMMO_BOX.get());
        if (!(ammoBox.getItem() instanceof IAmmoBox iAmmoBox)) {
            return ItemStack.EMPTY;
        }
        return switch (type) {
            case "gold" -> iAmmoBox.setAmmoLevel(ammoBox, 1);
            case "diamond" -> iAmmoBox.setAmmoLevel(ammoBox, 2);
            case "creative" -> iAmmoBox.setCreative(ammoBox, false);
            case "all_type_creative" -> iAmmoBox.setCreative(ammoBox, true);
            default -> iAmmoBox.setAmmoLevel(ammoBox, 0);
        };
    }

    private static Identifier normalizeTacId(Identifier id) {
        if ("minecraft".equals(id.getNamespace())) {
            return Identifier.tryParse(GunMod.MOD_ID + ":" + id.getPath());
        }
        return id;
    }
}
