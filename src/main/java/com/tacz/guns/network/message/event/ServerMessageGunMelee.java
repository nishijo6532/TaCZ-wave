package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.util.NbtCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import com.tacz.guns.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageGunMelee {
    private final int shooterId;
    private final ItemStack gunItemStack;

    public ServerMessageGunMelee(int shooterId, ItemStack gunItemStack) {
        this.shooterId = shooterId;
        this.gunItemStack = gunItemStack;
    }

    public static void encode(ServerMessageGunMelee message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        buf.writeNbt(NbtCompat.serializeItemStack(message.gunItemStack));
    }

    public static ServerMessageGunMelee decode(FriendlyByteBuf buf) {
        int shooterId = buf.readVarInt();
        ItemStack gunItemStack = NbtCompat.deserializeItemStack(buf.readNbt());
        return new ServerMessageGunMelee(shooterId, gunItemStack);
    }

    public static void handle(ServerMessageGunMelee message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
        context.setPacketHandled(true);
    }

    private static void doClientEvent(ServerMessageGunMelee message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.shooterId) instanceof LivingEntity shooter) {
            GunMeleeEvent gunMeleeEvent = new GunMeleeEvent(shooter, message.gunItemStack, LogicalSide.CLIENT);
        com.tacz.guns.util.ForgeEventCompat.post(gunMeleeEvent);
        }
    }
}
