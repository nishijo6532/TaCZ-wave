package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunFireEvent;
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

public class ServerMessageGunFire {
    private final int shooterId;
    private final ItemStack gunItemStack;

    public ServerMessageGunFire(int shooterId, ItemStack gunItemStack) {
        this.shooterId = shooterId;
        this.gunItemStack = gunItemStack;
    }

    public static void encode(ServerMessageGunFire message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        buf.writeNbt(NbtCompat.serializeItemStack(message.gunItemStack));
    }

    public static ServerMessageGunFire decode(FriendlyByteBuf buf) {
        int shooterId = buf.readVarInt();
        ItemStack gunItemStack = NbtCompat.deserializeItemStack(buf.readNbt());
        return new ServerMessageGunFire(shooterId, gunItemStack);
    }

    public static void handle(ServerMessageGunFire message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
        context.setPacketHandled(true);
    }

    private static void doClientEvent(ServerMessageGunFire message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.shooterId) instanceof LivingEntity shooter) {
            GunFireEvent gunFireEvent = new GunFireEvent(shooter, message.gunItemStack, LogicalSide.CLIENT);
        com.tacz.guns.util.ForgeEventCompat.post(gunFireEvent);
        }
    }
}
